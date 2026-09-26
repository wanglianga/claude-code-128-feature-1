package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.DentureOrderRepo;
import com.dentallab.repo.OrderExceptionRepo;
import com.dentallab.repo.ToothItemRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 异常事件：取模气泡、色号争议、临时改设计、排期拥堵、试戴不密合、加急、配送延误、返工责任不清。 */
@Service
public class ExceptionService {

    private final OrderExceptionRepo exceptionRepo;
    private final DentureOrderRepo orderRepo;
    private final ToothItemRepo itemRepo;
    private final AuthService authService;

    public ExceptionService(OrderExceptionRepo exceptionRepo, DentureOrderRepo orderRepo,
                            ToothItemRepo itemRepo, AuthService authService) {
        this.exceptionRepo = exceptionRepo;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.authService = authService;
    }

    /** 上报异常（任何已登录角色都可以上报，把各方连接到同一颗牙）。 */
    @Transactional
    public Map<String, Object> report(Account reporter, Long orderId, ExceptionType type,
                                      Long toothItemId, String description) {
        DentureOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> BizException.notFound("订单#" + orderId));
        OrderException event = new OrderException();
        event.setOrder(order);
        event.setType(type);
        event.setDescription(description);
        event.setReportedBy(reporter);
        if (toothItemId != null) {
            ToothItem item = itemRepo.findById(toothItemId)
                    .orElseThrow(() -> BizException.notFound("牙位明细#" + toothItemId));
            if (!item.getOrder().getId().equals(orderId)) {
                throw new BizException("牙位明细#" + toothItemId + "不属于订单#" + orderId);
            }
            event.setToothItem(item);
        }
        exceptionRepo.save(event);
        return Views.exception(event);
    }

    /** 客服/技工所管理员处理异常。 */
    @Transactional
    public Map<String, Object> handle(Account handler, Long exceptionId, String resolution, boolean resolved) {
        authService.requireRole(handler, Role.CUSTOMER_SERVICE, Role.LAB_ADMIN);
        OrderException event = exceptionRepo.findById(exceptionId)
                .orElseThrow(() -> BizException.notFound("异常事件#" + exceptionId));
        if (event.getStatus() == ExceptionStatus.RESOLVED) {
            throw new BizException("异常事件已解决，不能重复处理");
        }
        event.setHandledBy(handler);
        event.setResolution(resolution);
        event.setHandledAt(LocalDateTime.now());
        event.setStatus(resolved ? ExceptionStatus.RESOLVED : ExceptionStatus.PROCESSING);
        exceptionRepo.save(event);
        return Views.exception(event);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOrder(Long orderId) {
        return exceptionRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(Views::exception).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOpen() {
        return exceptionRepo.findByStatusOrderByIdDesc(ExceptionStatus.OPEN).stream()
                .map(Views::exception).collect(Collectors.toList());
    }
}
