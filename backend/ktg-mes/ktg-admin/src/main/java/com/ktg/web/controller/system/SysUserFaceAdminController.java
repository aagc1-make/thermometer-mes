package com.ktg.web.controller.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.system.domain.SysUserFaceAdminRow;
import com.ktg.system.service.ISysUserFaceService;

/**
 * 人脸登录管理（管理员）。
 *
 * 只提供元数据的查看与启停、重置，不返回任何特征数据。
 */
@RestController
@RequestMapping("/system/user/face/admin")
public class SysUserFaceAdminController extends BaseController {

    @Autowired
    private ISysUserFaceService userFaceService;

    /** 人脸录入信息列表 */
    @PreAuthorize("@ss.hasPermi('system:user:face:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "filter", required = false) String filter) {
        List<SysUserFaceAdminRow> rows = userFaceService.selectFaceAdminList(keyword, filter);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("rows", rows);
        data.put("total", rows.size());
        data.put("enrollmentEnabled", userFaceService.isFaceServiceAvailable());
        return AjaxResult.success(data);
    }

    /** 停用或开启某账号的人脸登录 */
    @PreAuthorize("@ss.hasPermi('system:user:face:status')")
    @Log(title = "人脸登录管理", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/{userId}/status")
    public AjaxResult changeStatus(@PathVariable("userId") Long userId, @RequestBody Map<String, Object> body) {
        Object enabled = body == null ? null : body.get("enabled");
        if (!(enabled instanceof Boolean)) {
            return AjaxResult.error("请提供 enabled 参数");
        }
        userFaceService.updateFaceLoginPolicy(userId, (Boolean) enabled);
        return AjaxResult.success((Boolean) enabled ? "已允许人脸登录" : "已停用人脸登录");
    }

    /** 重置某账号的人脸模板（删除特征，需重新录入） */
    @PreAuthorize("@ss.hasPermi('system:user:face:remove')")
    @Log(title = "人脸登录管理", businessType = BusinessType.DELETE, isSaveRequestData = false)
    @DeleteMapping("/{userId}")
    public AjaxResult reset(@PathVariable("userId") Long userId) {
        userFaceService.deleteByUserId(userId);
        return AjaxResult.success("人脸模板已重置");
    }
}
