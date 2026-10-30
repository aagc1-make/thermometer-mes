package com.ktg.miniapp.auth;

import com.ktg.miniapp.common.BizException;
import com.ktg.miniapp.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 小程序登录 / 当前账号 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthRepository accountRepo;
    private final PasswordService passwordService;
    private final TokenService tokenService;

    public AuthController(AuthRepository accountRepo, PasswordService passwordService, TokenService tokenService) {
        this.accountRepo = accountRepo;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public R login(@RequestBody Map<String, Object> payload) {
        String username = text(payload.get("username"));
        String password = text(payload.get("password"));
        if (username == null || password == null) {
            throw new BizException("请输入账号和密码");
        }
        Map<String, Object> account = accountRepo.findByUsername(username);
        if (account == null || !"Y".equalsIgnoreCase(String.valueOf(account.get("enabled")))) {
            throw new BizException("账号或密码错误");
        }
        boolean matched = passwordService.matches(
                text(account.get("salt")), password, text(account.get("passwordHash")));
        if (!matched) {
            throw new BizException("账号或密码错误");
        }

        long accountId = Long.parseLong(String.valueOf(account.get("accountId")));
        long clientId = Long.parseLong(String.valueOf(account.get("clientId")));
        accountRepo.touchLogin(accountId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", tokenService.issue(accountId, clientId, username));
        data.put("expiresIn", tokenService.getExpireSeconds());
        data.put("account", profile(account));
        return R.ok(data);
    }

    @GetMapping("/me")
    public R me(@RequestAttribute("miniappAccount") Map<String, Object> token) {
        long accountId = Long.parseLong(String.valueOf(token.get("aid")));
        Map<String, Object> account = accountRepo.findById(accountId);
        if (account == null) {
            throw new BizException("账号不存在或已停用");
        }
        return R.ok(profile(account));
    }

    @PostMapping("/logout")
    public R logout() {
        // 令牌无状态，客户端清除本地 token 即可
        return R.ok();
    }

    private Map<String, Object> profile(Map<String, Object> account) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("accountId", account.get("accountId"));
        profile.put("clientId", account.get("clientId"));
        profile.put("username", account.get("username"));
        profile.put("displayName", account.get("displayName"));
        return profile;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
