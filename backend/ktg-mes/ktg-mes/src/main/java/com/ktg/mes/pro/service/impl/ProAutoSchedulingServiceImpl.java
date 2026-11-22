package com.ktg.mes.pro.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.md.domain.MdWorkstation;
import com.ktg.mes.md.service.IMdWorkstationService;
import com.ktg.mes.pro.domain.ProRoute;
import com.ktg.mes.pro.domain.ProRouteProcess;
import com.ktg.mes.pro.domain.ProRouteProduct;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.mapper.ProWorkorderMapper;
import com.ktg.mes.pro.service.IProAutoSchedulingService;
import com.ktg.mes.pro.service.IProRouteProcessService;
import com.ktg.mes.pro.service.IProRouteProductService;
import com.ktg.mes.pro.service.IProRouteService;
import com.ktg.mes.pro.service.IProTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工单确认自动排产：按产品绑定的工艺路线生成全部工序任务。
 *
 * <p>任务编号规则：{工单号}-{两位工序序号}，例如 gd2026111400002-01。
 * 工序序号取工艺路线 pro_route_process.order_num，保证与五步流程条一致。</p>
 *
 * @author ktg
 */
@Service
public class ProAutoSchedulingServiceImpl implements IProAutoSchedulingService
{
    private static final Logger log = LoggerFactory.getLogger(ProAutoSchedulingServiceImpl.class);

    @Autowired
    private ProWorkorderMapper proWorkorderMapper;

    @Autowired
    private IProRouteService proRouteService;

    @Autowired
    private IProRouteProductService proRouteProductService;

    @Autowired
    private IProRouteProcessService proRouteProcessService;

    @Autowired
    private IMdWorkstationService mdWorkstationService;

    @Autowired
    private IProTaskService proTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scheduleOnConfirm(Long workorderId, String operator)
    {
        ProWorkorder workorder = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (workorder == null)
        {
            throw new ServiceException("生产工单不存在，无法自动排产");
        }
        if (!UserConstants.ORDER_STATUS_CONFIRMED.equals(workorder.getStatus()))
        {
            return 0;
        }
        if (workorder.getProductId() == null)
        {
            throw new ServiceException("生产工单未指定产品，无法自动排产");
        }
        if (workorder.getQuantity() == null || workorder.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("生产工单数量必须大于0，无法自动排产");
        }

        ProRoute route = resolveEnabledRoute(workorder.getProductId());
        if (route == null)
        {
            throw new ServiceException("产品[" + safeText(workorder.getProductCode(), workorder.getProductName())
                    + "]未配置启用的工艺路线，无法自动排产");
        }

        ProRouteProcess routeQuery = new ProRouteProcess();
        routeQuery.setRouteId(route.getRouteId());
        List<ProRouteProcess> routeProcesses = proRouteProcessService.selectProRouteProcessList(routeQuery);
        if (routeProcesses == null || routeProcesses.isEmpty())
        {
            throw new ServiceException("工艺路线[" + route.getRouteCode() + "]未配置工序，无法自动排产");
        }
        routeProcesses.sort(Comparator.comparing(ProRouteProcess::getOrderNum,
                Comparator.nullsLast(Comparator.naturalOrder())));

        ProTask existingQuery = new ProTask();
        existingQuery.setWorkorderId(workorderId);
        List<ProTask> existingTasks = proTaskService.selectProTaskList(existingQuery);
        Set<Long> scheduledProcessIds = new HashSet<>();
        if (existingTasks != null)
        {
            for (ProTask task : existingTasks)
            {
                if (task.getProcessId() != null)
                {
                    scheduledProcessIds.add(task.getProcessId());
                }
            }
        }

        int generated = 0;
        int index = 0;
        for (ProRouteProcess routeProcess : routeProcesses)
        {
            index++;
            if (routeProcess.getProcessId() == null || scheduledProcessIds.contains(routeProcess.getProcessId()))
            {
                // 同一工单同一工序已排过，保持幂等
                continue;
            }

            MdWorkstation workstation = resolveWorkstation(routeProcess);
            ProTask task = buildTask(workorder, route, routeProcess, workstation,
                    index, operator);
            try
            {
                proTaskService.insertProTask(task);
                generated++;
                scheduledProcessIds.add(routeProcess.getProcessId());
            }
            catch (DuplicateKeyException duplicated)
            {
                // 并发确认或历史任务占号：确认该任务已存在则跳过，否则继续抛出触发整体回滚
                if (taskExists(task.getTaskCode(), workorderId))
                {
                    log.warn("自动排产跳过已存在任务：workorderId={}, taskCode={}", workorderId, task.getTaskCode());
                    scheduledProcessIds.add(routeProcess.getProcessId());
                }
                else
                {
                    throw duplicated;
                }
            }
        }

        log.info("工单确认自动排产完成：workorderId={}, workorderCode={}, routeCode={}, 新增任务={}, 已存在工序={}",
                workorderId, workorder.getWorkorderCode(), route.getRouteCode(), generated, scheduledProcessIds.size() - generated);
        return generated;
    }

    /**
     * 取产品绑定且启用的工艺路线；同一产品配置多条时按路线ID升序取第一条，保证结果稳定。
     */
    private ProRoute resolveEnabledRoute(Long productId)
    {
        ProRouteProduct query = new ProRouteProduct();
        query.setItemId(productId);
        List<ProRouteProduct> mappings = proRouteProductService.selectProRouteProductList(query);
        if (mappings == null || mappings.isEmpty())
        {
            return null;
        }
        List<ProRouteProduct> sorted = mappings.stream()
                .filter(item -> item.getRouteId() != null)
                .sorted(Comparator.comparing(ProRouteProduct::getRouteId))
                .collect(java.util.stream.Collectors.toList());
        for (ProRouteProduct mapping : sorted)
        {
            ProRoute route = proRouteService.selectProRouteByRouteId(mapping.getRouteId());
            if (route != null && UserConstants.YES.equals(route.getEnableFlag()))
            {
                return route;
            }
        }
        return null;
    }

    /**
     * 工序对应的工作站：优先启用状态，按工作站ID升序取第一个，避免同一工序多站时结果漂移。
     */
    private MdWorkstation resolveWorkstation(ProRouteProcess routeProcess)
    {
        MdWorkstation query = new MdWorkstation();
        query.setProcessId(routeProcess.getProcessId());
        query.setEnableFlag(UserConstants.YES);
        List<MdWorkstation> workstations = mdWorkstationService.selectMdWorkstationList(query);
        if (workstations == null || workstations.isEmpty())
        {
            throw new ServiceException("工序[" + routeProcess.getProcessName() + "]未配置可用工作站，无法自动排产");
        }
        return workstations.stream()
                .sorted(Comparator.comparing(MdWorkstation::getWorkstationId))
                .findFirst()
                .orElseThrow(() -> new ServiceException("工序[" + routeProcess.getProcessName() + "]未配置可用工作站，无法自动排产"));
    }

    private ProTask buildTask(ProWorkorder workorder, ProRoute route, ProRouteProcess routeProcess,
                              MdWorkstation workstation, int index, String operator)
    {
        int sequence = routeProcess.getOrderNum() == null ? index : routeProcess.getOrderNum();
        ProTask task = new ProTask();
        task.setTaskCode(workorder.getWorkorderCode() + "-" + String.format("%02d", sequence));
        task.setTaskName(workorder.getProductName() + "【" + formatQuantity(workorder.getQuantity()) + "】"
                + (workorder.getUnitOfMeasure() == null ? "" : workorder.getUnitOfMeasure()));
        task.setWorkorderId(workorder.getWorkorderId());
        task.setWorkorderCode(workorder.getWorkorderCode());
        task.setWorkorderName(workorder.getWorkorderName());
        task.setWorkstationId(workstation.getWorkstationId());
        task.setWorkstationCode(workstation.getWorkstationCode());
        task.setWorkstationName(workstation.getWorkstationName());
        task.setRouteId(route.getRouteId());
        task.setRouteCode(route.getRouteCode());
        task.setProcessId(routeProcess.getProcessId());
        task.setProcessCode(routeProcess.getProcessCode());
        task.setProcessName(routeProcess.getProcessName());
        task.setItemId(workorder.getProductId());
        task.setItemCode(workorder.getProductCode());
        task.setItemName(workorder.getProductName());
        task.setSpecification(workorder.getProductSpc());
        task.setUnitOfMeasure(workorder.getUnitOfMeasure());
        task.setUnitName(workorder.getUnitName());
        task.setQuantity(workorder.getQuantity());
        task.setQuantityProduced(BigDecimal.ZERO);
        task.setQuantityQuanlify(BigDecimal.ZERO);
        task.setQuantityUnquanlify(BigDecimal.ZERO);
        task.setQuantityChanged(BigDecimal.ZERO);
        task.setClientId(workorder.getClientId());
        task.setClientCode(workorder.getClientCode());
        task.setClientName(workorder.getClientName());
        task.setStartTime(workorder.getRequestDate() == null ? new Date() : workorder.getRequestDate());
        task.setDuration(1L);
        task.setRequestDate(workorder.getRequestDate());
        task.setStatus("NORMAL");
        task.setCreateBy(operator);
        return task;
    }

    private boolean taskExists(String taskCode, Long workorderId)
    {
        ProTask query = new ProTask();
        query.setTaskCode(taskCode);
        List<ProTask> tasks = proTaskService.selectProTaskList(query);
        return tasks != null && tasks.stream()
                .anyMatch(task -> workorderId.equals(task.getWorkorderId()));
    }

    private String formatQuantity(BigDecimal quantity)
    {
        return quantity == null ? "0" : quantity.stripTrailingZeros().toPlainString();
    }

    private String safeText(String code, String name)
    {
        if (StringUtils.isNotEmpty(code))
        {
            return code;
        }
        return name == null ? "" : name;
    }
}
