package com.ktg.mes.wm.mapper;

import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.wm.domain.WmMaterialStock;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan.Document;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan.Material;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface WorkorderShortageMapper {
    ProWorkorder order(@Param("id") long id, @Param("lock") boolean lock);
    List<Material> materials(@Param("id") long id, @Param("code") String code);
    List<WmMaterialStock> sources(@Param("items") List<Long> items);
    List<Document> documents(@Param("tag") String tag);
    List<Long> lockSources(@Param("ids") List<Long> ids);
    Long lockTransferCodeRule();
}
