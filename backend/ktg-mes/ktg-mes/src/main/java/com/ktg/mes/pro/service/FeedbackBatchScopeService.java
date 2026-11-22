package com.ktg.mes.pro.service;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.pro.domain.ProRoute;
import com.ktg.mes.pro.domain.ProRouteProductBom;
import com.ktg.mes.pro.mapper.ProRouteMapper;
import com.ktg.mes.pro.mapper.ProRouteProductBomMapper;
import com.ktg.mes.wm.mapper.FeedbackBatchSourceMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/** 对当前工序实际消耗项逐一校验批次来源，返回供库存服务临时使用的精确范围。 */
@Service
public class FeedbackBatchScopeService {
    public static final String PARAM_KEY = "feedbackBatchScope";

    private final ProRouteMapper routeMapper;
    private final ProRouteProductBomMapper bomMapper;
    private final FeedbackBatchSourceMapper sourceMapper;

    public FeedbackBatchScopeService(ProRouteMapper routeMapper, ProRouteProductBomMapper bomMapper,
                                     FeedbackBatchSourceMapper sourceMapper) {
        this.routeMapper = routeMapper;
        this.bomMapper = bomMapper;
        this.sourceMapper = sourceMapper;
    }

    public Map<String, List<String>> validate(ProFeedback feedback, Map<String, List<String>> requested) {
        if (requested == null || feedback.getWorkorderId() == null) {
            throw new ServiceException("请提供有效工单及报工消耗批次范围");
        }
        // 与 generateItemConsume 使用同一产品路线和工序 BOM 口径。
        ProRoute route = routeMapper.getRouteByProductId(feedback.getItemId());
        if (route == null) {
            throw new ServiceException("没有找到当前产品的工艺路线，无法校验消耗批次");
        }
        ProRouteProductBom query = new ProRouteProductBom();
        query.setProductId(feedback.getItemId());
        query.setRouteId(route.getRouteId());
        query.setProcessId(feedback.getProcessId());
        List<ProRouteProductBom> boms = bomMapper.selectProRouteProductBomList(query);
        Map<String, List<String>> validated = new LinkedHashMap<>();
        if (boms == null) return validated;
        for (ProRouteProductBom bom : boms) {
            String itemId = String.valueOf(bom.getItemId());
            if (validated.containsKey(itemId)) continue;
            List<String> requestedBatches = requested.get(itemId);
            if (requestedBatches == null || requestedBatches.isEmpty()) {
                throw new ServiceException("物料" + bom.getItemCode() + "缺少消耗批次范围");
            }
            List<String> sourceBatches = sourceMapper.selectAllowedBatchCodes(feedback.getWorkorderId(), bom.getItemId());
            Set<String> sources = sourceBatches == null ? Collections.emptySet() : new HashSet<>(sourceBatches);
            Set<String> accepted = new LinkedHashSet<>();
            for (String batch : requestedBatches) {
                if (batch == null || batch.trim().isEmpty()) {
                    throw new ServiceException("物料" + bom.getItemCode() + "的消耗批次不能为空");
                }
                if (!sources.contains(batch)) {
                    throw new ServiceException("物料" + bom.getItemCode() + "的批次" + batch
                            + "不属于当前工单已执行领料或直接子工单已放行合格产出");
                }
                accepted.add(batch);
            }
            validated.put(itemId, new ArrayList<>(accepted));
        }
        return validated;
    }
}
