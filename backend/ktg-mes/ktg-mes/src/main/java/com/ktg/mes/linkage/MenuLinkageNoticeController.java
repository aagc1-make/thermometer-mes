package com.ktg.mes.linkage;

import com.ktg.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 关联表单菜单共享提醒接口。 */
@RestController
@RequestMapping("/system/menu-linkage-notice")
public class MenuLinkageNoticeController {

    private final MenuLinkageNoticeService noticeService;

    public MenuLinkageNoticeController(MenuLinkageNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/unread")
    public AjaxResult unread() {
        return AjaxResult.success(noticeService.listUnreadPaths());
    }

    @PutMapping("/read")
    public AjaxResult read(@RequestBody Map<String, String> request) {
        noticeService.markRead(request.get("menuPath"));
        return AjaxResult.success();
    }
}
