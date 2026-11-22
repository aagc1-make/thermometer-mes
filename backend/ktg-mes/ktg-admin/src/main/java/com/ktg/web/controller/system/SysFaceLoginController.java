package com.ktg.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.constant.Constants;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.domain.model.FaceLoginBody;
import com.ktg.common.utils.StringUtils;
import com.ktg.framework.web.service.FaceLoginService;
import com.ktg.system.face.FaceTemplate;

/**
 * 人脸登录入口（匿名访问）。
 *
 * 前端只提交特征向量；校验通过后签发与密码登录相同的 JWT 令牌。
 */
@RestController
public class SysFaceLoginController {

    @Autowired
    private FaceLoginService faceLoginService;

    @PostMapping("/faceLogin")
    public AjaxResult faceLogin(@RequestBody FaceLoginBody body) {
        if (body == null || StringUtils.isEmpty(body.getUsername())) {
            return AjaxResult.error("请输入用户账号");
        }
        if (body.getDescriptor() == null || body.getDescriptor().size() != FaceTemplate.DIMENSION) {
            return AjaxResult.error("人脸特征格式不正确，请重新采集");
        }
        double[] descriptor = FaceTemplate.toArray(body.getDescriptor());
        String token = faceLoginService.login(body.getUsername().trim(), descriptor);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }
}
