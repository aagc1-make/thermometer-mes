package com.ktg.mes;
import com.ktg.mes.wm.controller.WmRtVendorDetailController;
import com.ktg.mes.wm.controller.mobile.WmRtVendorDetailMobController;
import org.junit.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class VendorDetailPermissionTest {
    @Test public void currentAndLegacyDetailPermissionsAreAcceptedOnBothClients() {
        Map<String,String> operations=new LinkedHashMap<String,String>();
        operations.put("list","list");operations.put("getInfo","query");operations.put("add","add");operations.put("edit","edit");operations.put("remove","remove");
        for(Class<?> controller:new Class<?>[]{WmRtVendorDetailController.class,WmRtVendorDetailMobController.class})
            for(Map.Entry<String,String> operation:operations.entrySet()) {
                Method found=null;for(Method m:controller.getDeclaredMethods())if(m.getName().equals(operation.getKey()))found=m;
                assertNotNull(controller.getSimpleName()+"."+operation.getKey(),found);
                PreAuthorize permission=found.getAnnotation(PreAuthorize.class);assertNotNull(permission);
                assertEquals("@ss.hasAnyPermi('mes:wm:rtvendor:"+operation.getValue()+",mes:wm:rtvendordetail:"+operation.getValue()+"')",permission.value());
            }
    }
}
