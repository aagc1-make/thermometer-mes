package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AiEvaluationMapper {
    @Select("select c.chunk_id from kb_document_chunk c "
        + "join kb_document_version v on v.version_id=c.version_id and v.active=1 and v.deleted_at is null "
        + "join kb_document d on d.doc_id=c.doc_id and d.deleted_at is null "
        + "where c.library_id=#{scope.libraryId} and c.dept_id=#{scope.deptId} and d.title=#{title} "
        + "and locate(#{fragment}, c.content)>0 order by c.chunk_index")
    List<Long> goldChunks(@Param("scope") KbDocument.Library scope, @Param("title") String title,
                          @Param("fragment") String fragment);

    @Select("select coalesce(sum(s.quantity_onhand),0) from md_item i "
        + "left join wm_material_stock s on s.item_id=i.item_id "
        + "where i.item_code=#{itemCode} group by i.item_id")
    BigDecimal stock(@Param("itemCode") String itemCode);

    @Select("select coalesce(quantity_produced,0) from pro_workorder where workorder_code=#{code}")
    BigDecimal produced(@Param("code") String code);

    @Select("select sum(transaction_quantity) from wm_transaction where item_code=#{code} "
        + "and transaction_flag=1 and transaction_date>=#{from} and transaction_date<date_add(#{to}, interval 1 day)")
    BigDecimal inbound(@Param("code") String code, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
