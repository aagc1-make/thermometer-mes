package com.ktg.web.controller.system;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.domain.entity.SysUser;
import com.ktg.common.core.domain.model.FaceEnrollBody;
import com.ktg.common.core.domain.model.LoginUser;
import com.ktg.common.core.redis.RedisCache;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.system.domain.SysUserFace;
import com.ktg.system.face.FaceTemplate;
import com.ktg.system.service.ISysUserFaceService;

/**
 * 人脸登录模板管理（登录后访问）。
 *
 * 录入与解绑都要求再次输入登录密码，并要求显式同意授权说明；
 * 服务端只保存特征向量，不接收、不保存照片。
 */
@RestController
@RequestMapping("/system/user/face")
public class SysUserFaceController extends BaseController {

    private static final String ENROLL_FAIL_KEY = "face_enroll_fail:";
    private static final int MAX_PASSWORD_FAIL = 5;
    private static final int PASSWORD_FAIL_WINDOW = 600;

    @Autowired
    private ISysUserFaceService userFaceService;

    @Autowired
    private RedisCache redisCache;

    /** 当前账号的人脸录入状态 */
    @GetMapping("/status")
    public AjaxResult status() {
        Long userId = SecurityUtils.getUserId();
        SysUserFace face = userFaceService.selectByUserId(userId);
        Map<String, Object> data = new HashMap<String, Object>();
        boolean enrolled = face != null && "0".equals(face.getStatus());
        data.put("enrolled", enrolled);
        data.put("algorithm", face == null ? "" : face.getAlgorithm());
        data.put("samples", face == null ? 0 : face.getSamples());
        data.put("updateTime", face == null ? null
                : (face.getUpdateTime() != null ? face.getUpdateTime() : face.getCreateTime()));
        return AjaxResult.success(data);
    }

    /** 录入或更新人脸 */
    @Log(title = "人脸登录", businessType = BusinessType.UPDATE)
    @PostMapping("/enroll")
    public AjaxResult enroll(@RequestBody FaceEnrollBody body) {
        if (body == null) {
            return AjaxResult.error("参数不能为空");
        }
        if (!Boolean.TRUE.equals(body.getAgree())) {
            return AjaxResult.error("请先阅读并同意人脸特征使用说明");
        }
        LoginUser loginUser = getLoginUser();
        SysUser current = loginUser.getUser();
        ensurePasswordAttempts(current.getUserId());
        if (StringUtils.isEmpty(body.getPassword())
                || !SecurityUtils.matchesPassword(body.getPassword(), current.getPassword())) {
            countPasswordFail(current.getUserId());
            return AjaxResult.error("登录密码不正确");
        }
        double[] descriptor = FaceTemplate.toArray(body.getDescriptor());
        if (!FaceTemplate.isUsable(descriptor)) {
            return AjaxResult.error("人脸特征无效，请重新采集");
        }
        int samples = body.getSamples() == null ? 0 : Math.max(0, Math.min(30, body.getSamples()));
        BigDecimal quality = null;
        if (body.getQuality() != null) {
            double value = Math.max(0D, Math.min(1D, body.getQuality()));
            quality = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
        }
        SysUserFace face = new SysUserFace();
        face.setUserId(current.getUserId());
        face.setDescriptor(FaceTemplate.format(descriptor));
        face.setAlgorithm("face-api-0.22.2");
        face.setSamples(samples);
        face.setQuality(quality);
        face.setStatus("0");
        face.setCreateBy(current.getUserName());
        face.setUpdateBy(current.getUserName());
        face.setRemark("用户授权：人脸特征仅用于本系统登录比对，可随时解绑删除");
        userFaceService.enroll(face);
        redisCache.deleteObject(ENROLL_FAIL_KEY + current.getUserId());
        AjaxResult ajax = AjaxResult.success("人脸已录入，下次可直接人脸登录");
        ajax.put("enrolled", true);
        return ajax;
    }

    /** 解绑人脸：删除特征，立即失效 */
    @Log(title = "人脸登录", businessType = BusinessType.DELETE)
    @DeleteMapping
    public AjaxResult unbind(@RequestBody(required = false) FaceEnrollBody body) {
        if (body == null || StringUtils.isEmpty(body.getPassword())) {
            return AjaxResult.error("请输入登录密码");
        }
        LoginUser loginUser = getLoginUser();
        SysUser current = loginUser.getUser();
        ensurePasswordAttempts(current.getUserId());
        if (!SecurityUtils.matchesPassword(body.getPassword(), current.getPassword())) {
            countPasswordFail(current.getUserId());
            return AjaxResult.error("登录密码不正确");
        }
        userFaceService.deleteByUserId(current.getUserId());
        redisCache.deleteObject(ENROLL_FAIL_KEY + current.getUserId());
        return AjaxResult.success("人脸已解绑，将无法再使用人脸登录");
    }

    private void ensurePasswordAttempts(Long userId) {
        Integer fails = redisCache.getCacheObject(ENROLL_FAIL_KEY + userId);
        if (fails != null && fails >= MAX_PASSWORD_FAIL) {
            throw new ServiceException("密码尝试次数过多，请10分钟后再试");
        }
    }

    private void countPasswordFail(Long userId) {
        Integer fails = redisCache.getCacheObject(ENROLL_FAIL_KEY + userId);
        int next = fails == null ? 1 : fails + 1;
        redisCache.setCacheObject(ENROLL_FAIL_KEY + userId, next, PASSWORD_FAIL_WINDOW, TimeUnit.SECONDS);
    }
}
