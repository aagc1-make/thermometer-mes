package com.ktg.mes.ai.controller;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.service.BomImpactService;
import com.ktg.mes.ai.service.KbDocumentService;
import com.ktg.mes.ai.service.KbGraphService;
import com.ktg.mes.ai.service.KbNeo4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/mes/ai/kb/libraries/{libraryId}/graph")
@PreAuthorize("@ss.hasPermi('mes:ai:query')")
public class MesKbGraphController {
    private final KbDocumentService documents;
    private final KbGraphService graphs;
    private final KbNeo4j neo;
    private final BomImpactService impacts;

    public MesKbGraphController(KbDocumentService documents, KbGraphService graphs, KbNeo4j neo,
                                BomImpactService impacts) {
        this.documents = documents;
        this.graphs = graphs;
        this.neo = neo;
        this.impacts = impacts;
    }

    @GetMapping
    public AjaxResult browse(@PathVariable("libraryId") long libraryId,
                             @RequestParam(value = "search", defaultValue = "") String search,
                             @RequestParam(value = "focus", defaultValue = "") String focus,
                             @RequestParam(value = "hops", defaultValue = "1") int hops,
                             @RequestParam(value = "types", required = false) List<String> types,
                             @RequestParam(value = "relation", defaultValue = "") String relation) {
        KbDocument.Library scope = documents.readScope(libraryId);
        if (!"online".equals(neo.status())) return AjaxResult.error(503, "知识图谱服务暂不可用。");
        try {
            return AjaxResult.success(graphs.browse(scope, search.trim(), focus.trim(), hops,
                types == null ? Collections.emptyList() : types, relation));
        } catch (IOException e) {
            return AjaxResult.error(503, "图谱查询未完成，请重试。");
        }
    }

    @PostMapping("/versions/{versionId}/retry")
    @Log(title = "知识图谱抽取重试", businessType = BusinessType.UPDATE)
    public AjaxResult retry(@PathVariable("libraryId") long libraryId, @PathVariable("versionId") long versionId) {
        graphs.retry(documents.readScope(libraryId), versionId);
        return AjaxResult.success();
    }

    /** 物料影响路径：按完整编码反向查出子件被哪些上级产品使用。 */
    @GetMapping("/impact")
    public AjaxResult impact(@PathVariable("libraryId") long libraryId,
                             @RequestParam("itemCode") String itemCode) {
        KbDocument.Library scope = documents.readScope(libraryId);
        return AjaxResult.success(BomImpactService.payload(impacts.impact(itemCode)));
    }

    /** 某一段的 BOM 行与文档佐证：逐段校验链条相接、仍启用，且最后一行为所请求的行。 */
    @GetMapping("/impact/edges/{bomId}")
    public AjaxResult impactEdge(@PathVariable("libraryId") long libraryId,
                                 @PathVariable("bomId") long bomId,
                                 @RequestParam("itemCode") String itemCode,
                                 @RequestParam("via") String via,
                                 @RequestParam("parentCode") String parentCode) {
        KbDocument.Library scope = documents.readScope(libraryId);
        return AjaxResult.success(impacts.edge(scope, bomId, itemCode, via, parentCode));
    }
}
