package com.ktg.framework.web.service;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ktg.common.constant.Constants;
import com.ktg.common.core.domain.entity.SysUser;
import com.ktg.common.core.domain.model.LoginUser;
import com.ktg.common.core.redis.RedisCache;
import com.ktg.common.enums.UserStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.ServletUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.common.utils.ip.IpUtils;
import com.ktg.framework.manager.AsyncManager;
import com.ktg.framework.manager.factory.AsyncFactory;
import com.ktg.system.face.FaceMatchResult;
import com.ktg.system.face.FaceTemplate;
import com.ktg.system.service.ISysUserFaceService;
import com.ktg.system.service.ISysUserService;

/**
 * 人脸登录校验。
 *
 * 安全约定：
 * 1. 只做 1:1 比对（账号 + 人脸），不做 1:N 刷脸检索；
 * 2. 未录入、比对失败、账号不存在统一返回同一句提示，避免向未登录者暴露账号是否存在；
 * 3. 失败按账号和 IP 分别计数并短期封禁，避免暴力试探；
 * 4. 成功与失败都写入登录日志，便于审计。
 */
@Component
public class FaceLoginService {

    private static final Logger log = LoggerFactory.getLogger(FaceLoginService.class);

    private static final String FAIL_KEY = "face_login_fail:";
    private static final String IP_FAIL_KEY = "face_login_ip_fail:";
    private static final int IP_FAIL_FACTOR = 4;
    private static final String GENERIC_ERROR = "账号未录入人脸或人脸不匹配，请重试或使用密码登录";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysUserFaceService userFaceService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private RedisCache redisCache;

    @Value("${mes.face.enabled:true}")
    private boolean enabled;

    @Value("${mes.face.max-fail:5}")
    private int maxFail;

    @Value("${mes.face.fail-window:600}")
    private int failWindow;

    /**
     * 人脸登录：校验特征后签发与密码登录一致的令牌。
     *
     * @param username   登录名
     * @param descriptor 128 维特征
     * @return token
     */
    public String login(String username, double[] descriptor) {
        if (!enabled) {
            throw new ServiceException("人脸登录未启用，请使用密码登录");
        }
        if (StringUtils.isEmpty(username)) {
            throw new ServiceException("请输入用户账号");
        }
        if (!FaceTemplate.isUsable(descriptor)) {
            throw new ServiceException("人脸特征无效，请重新采集");
        }
        String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
        ensureNotBlocked(FAIL_KEY + username, maxFail);
        ensureNotBlocked(IP_FAIL_KEY + ip, maxFail * IP_FAIL_FACTOR);

        SysUser user = userService.selectUserByUserName(username.trim());
        if (user == null) {
            return fail(username, ip, "账号不存在", null);
        }
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            return fail(username, ip, "账号已删除", user);
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            return fail(username, ip, "账号已停用", user);
        }

        FaceMatchResult match = userFaceService.verify(user.getUserId(), descriptor);
        if (!match.isMatched()) {
            String detail = match.getReason();
            if (!Double.isNaN(match.getDistance())) {
                detail += "，distance=" + String.format("%.4f", match.getDistance());
            }
            log.warn("[人脸登录] 未通过 user={} ip={} reason={}", username, ip, detail);
            return fail(username, ip, detail, user);
        }

        redisCache.deleteObject(FAIL_KEY + username);
        redisCache.deleteObject(IP_FAIL_KEY + ip);
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                permissionService.getMenuPermission(user));
        loginService.recordLoginInfo(user.getUserId());
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS,
                "人脸登录成功，distance=" + String.format("%.4f", match.getDistance())));
        return tokenService.createToken(loginUser);
    }

    /** 记录失败次数、写审计日志，并抛出统一提示（不区分账号是否存在）。 */
    private String fail(String username, String ip, String reason, SysUser user) {
        // 账号不存在时不落账号维度计数，避免任意用户名撑大 Redis；IP 维度仍然限流。
        if (user != null) {
            countFail(FAIL_KEY + user.getUserName());
        }
        countFail(IP_FAIL_KEY + ip);
        String account = user == null ? username : user.getUserName();
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(account, Constants.LOGIN_FAIL, "人脸登录失败：" + reason));
        throw new ServiceException(GENERIC_ERROR);
    }

    private void ensureNotBlocked(String key, int limit) {
        Integer count = redisCache.getCacheObject(key);
        if (count != null && count >= limit) {
            throw new ServiceException("人脸登录失败次数过多，请稍后再试或使用密码登录");
        }
    }

    private void countFail(String key) {
        Integer count = redisCache.getCacheObject(key);
        int next = count == null ? 1 : count + 1;
        redisCache.setCacheObject(key, next, failWindow, TimeUnit.SECONDS);
    }
}
