package com.ktg.mes;

import com.ktg.mes.wm.controller.WmItemRecptDetailController;
import com.ktg.mes.wm.controller.mobile.WmItemRecptDetailMobController;
import org.junit.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** 采购入库明细权限需与菜单权限一致，并兼容旧的 itemrecept 授权。 */
public class ReceiptDetailPermissionTest {
    @Test public void pcAndMobileAcceptCurrentAndLegacyPermissionForEachOperation() {
        Map<String, String> permissions = new LinkedHashMap<String, String>();
        permissions.put("list", "list"); permissions.put("getInfo", "query");
        permissions.put("add", "add"); permissions.put("edit", "edit"); permissions.put("remove", "remove");
        for (Class<?> controller : new Class<?>[]{WmItemRecptDetailController.class, WmItemRecptDetailMobController.class}) {
            for (Map.Entry<String, String> permission : permissions.entrySet()) {
                Method method = null;
                for (Method candidate : controller.getDeclaredMethods()) {
                    if (candidate.getName().equals(permission.getKey())) method = candidate;
                }
                assertNotNull(controller.getSimpleName() + "." + permission.getKey(), method);
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertNotNull(annotation);
                assertEquals("@ss.hasAnyPermi('mes:wm:itemrecpt:" + permission.getValue()
                                + ",mes:wm:itemrecept:" + permission.getValue() + "')",
                        annotation.value());
            }
        }
    }
}
