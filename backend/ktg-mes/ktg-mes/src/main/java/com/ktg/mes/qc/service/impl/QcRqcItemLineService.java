package com.ktg.mes.qc.service.impl;

import com.ktg.mes.qc.domain.QcRqc;
import com.ktg.mes.qc.domain.QcRqcLine;
import com.ktg.mes.qc.domain.QcTemplateIndex;
import com.ktg.mes.qc.service.IQcRqcLineService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按检验单模板生成检验项目行（RQC）。
 * 由 RQC 手工新增与“退料提交待检自动建单”共用，避免两条路径行为不一致。
 */
@Service
public class QcRqcItemLineService {
    private final IQcTemplateIndexService templateIndexes;
    private final IQcRqcLineService rqcLines;

    public QcRqcItemLineService(IQcTemplateIndexService templateIndexes, IQcRqcLineService rqcLines) {
        this.templateIndexes = templateIndexes;
        this.rqcLines = rqcLines;
    }

    /** 返回生成的项目行数（模板无项目时为 0）。 */
    public int generate(QcRqc rqc) {
        if (rqc == null || rqc.getRqcId() == null || rqc.getTemplateId() == null) {
            return 0;
        }
        QcTemplateIndex param = new QcTemplateIndex();
        param.setTemplateId(rqc.getTemplateId());
        List<QcTemplateIndex> indexes = templateIndexes.selectQcTemplateIndexList(param);
        if (indexes == null || indexes.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (QcTemplateIndex index : indexes) {
            QcRqcLine line = new QcRqcLine();
            line.setRqcId(rqc.getRqcId());
            line.setIndexId(index.getIndexId());
            line.setIndexCode(index.getIndexCode());
            line.setIndexName(index.getIndexName());
            line.setIndexType(index.getIndexType());
            line.setQcTool(index.getQcTool());
            line.setCheckMethod(index.getCheckMethod());
            line.setStanderVal(index.getStanderVal());
            line.setUnitOfMeasure(index.getUnitOfMeasure());
            line.setThresholdMax(index.getThresholdMax());
            line.setThresholdMin(index.getThresholdMin());
            line.setCrQuantity(BigDecimal.ZERO);
            line.setMajQuantity(BigDecimal.ZERO);
            rqcLines.insertQcRqcLine(line);
            created++;
        }
        return created;
    }
}
