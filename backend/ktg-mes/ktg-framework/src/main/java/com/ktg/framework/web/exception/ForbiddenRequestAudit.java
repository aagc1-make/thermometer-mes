package com.ktg.framework.web.exception;

import com.ktg.common.core.domain.model.LoginUser;
import com.ktg.common.enums.BusinessStatus;
import com.ktg.common.enums.OperatorType;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.common.utils.ip.IpUtils;
import com.ktg.framework.manager.AsyncManager;
import com.ktg.framework.manager.factory.AsyncFactory;
import com.ktg.system.domain.SysOperLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Component
public class ForbiddenRequestAudit {
    private static final Logger log = LoggerFactory.getLogger(ForbiddenRequestAudit.class);
    private static final Pattern CREDENTIAL_NAME = Pattern.compile(
        "token|password|passwd|pwd|secret|apikey|authorization|credential|signature|cookie|sessionid");

    public void record(HttpServletRequest request, String reason) {
        SysOperLog entry = new SysOperLog();
        entry.setTitle("越权访问");
        entry.setStatus(BusinessStatus.FAIL.ordinal());
        entry.setOperUrl(StringUtils.substring(request.getRequestURI(), 0, 255));
        entry.setRequestMethod(StringUtils.substring(request.getMethod(), 0, 10));
        entry.setOperIp(StringUtils.substring(IpUtils.getIpAddr(request), 0, 128));
        entry.setOperParam(StringUtils.substring(redactQuery(request.getQueryString()), 0, 2000));
        entry.setErrorMsg(StringUtils.substring(reason, 0, 2000));

        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser user = (LoginUser) authentication.getPrincipal();
            entry.setOperName(user.getUsername());
            entry.setOperatorType(OperatorType.MANAGE.ordinal());
        }

        try {
            AsyncManager.me().execute(AsyncFactory.recordOper(entry));
        } catch (RuntimeException e) {
            log.error("未能提交越权请求审计记录", e);
        }
    }

    private static String redactQuery(String query) {
        if (query == null) {
            return null;
        }
        StringJoiner result = new StringJoiner("&");
        for (String parameter : query.split("&", -1)) {
            int equals = parameter.indexOf('=');
            if (equals < 0) {
                result.add(parameter);
                continue;
            }
            String name = parameter.substring(0, equals);
            result.add(isCredentialName(name) ? name + "=[REDACTED]" : parameter);
        }
        return result.toString();
    }

    private static boolean isCredentialName(String encodedName) {
        try {
            String name = UriUtils.decode(encodedName, StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            return CREDENTIAL_NAME.matcher(name).find();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
