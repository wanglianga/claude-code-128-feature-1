package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 诊所月结：按账期汇总已完成订单结算；确认前必须复核账期内全部返工。 */
@Service
public class StatementService {

    private final MonthlyStatementRepo statementRepo;
    private final DentureOrderRepo orderRepo;
    private final SettlementRepo settlementRepo;
    private final ReworkRecordRepo reworkRepo;
    private final ClinicRepo clinicRepo;
    private final AuthService authService;

    public StatementService(MonthlyStatementRepo statementRepo, DentureOrderRepo orderRepo,
                            SettlementRepo settlementRepo, ReworkRecordRepo reworkRepo,
                            ClinicRepo clinicRepo, AuthService authService) {
        this.statementRepo = statementRepo;
        this.orderRepo = orderRepo;
        this.settlementRepo = settlementRepo;
        this.reworkRepo = reworkRepo;
        this.clinicRepo = clinicRepo;
        this.authService = authService;
    }

    /** 财务生成诊所月结单（按订单完成时间归属账期，需订单已结算）。 */
    @Transactional
    public Map<String, Object> generate(Account finance, Long clinicId, String yearMonth) {
        authService.requireRole(finance, Role.FINANCE, Role.LAB_ADMIN);
        Clinic clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> BizException.notFound("诊所#" + clinicId));
        YearMonth ym = parseYearMonth(yearMonth);
        if (statementRepo.findByClinicIdAndYearMonth(clinicId, ym.toString()).isPresent()) {
            throw new BizException("诊所[" + clinic.getName() + "] " + ym + " 月结单已存在");
        }
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<DentureOrder> orders = orderRepo.findByClinicIdAndCompletedAtBetween(clinicId, from, to);
        List<Long> orderIds = orders.stream().map(DentureOrder::getId).collect(Collectors.toList());

        List<Settlement> settlements = orderIds.isEmpty() ? List.of() : settlementRepo.findByOrderIdIn(orderIds);
        if (settlements.size() < orderIds.size()) {
            long missing = orderIds.size() - settlements.size();
            throw new BizException("账期内还有 " + missing + " 个已完成订单未生成结算单，请先生成结算");
        }
        BigDecimal total = settlements.stream().map(Settlement::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long unreviewed = orderIds.isEmpty() ? 0 : reworkRepo.findByOrderIdIn(orderIds).stream()
                .filter(r -> !r.isClinicReviewed()).count();

        MonthlyStatement statement = new MonthlyStatement();
        statement.setClinic(clinic);
        statement.setYearMonth(ym.toString());
        statement.setOrderCount(orders.size());
        statement.setTotalAmount(total);
        statement.setUnreviewedReworkCount((int) unreviewed);
        statementRepo.save(statement);
        return Views.statement(statement);
    }

    /** 诊所确认月结：账期内全部返工必须已复核。 */
    @Transactional
    public Map<String, Object> confirm(Account clinicUser, Long statementId) {
        authService.requireRole(clinicUser, Role.CLINIC_ADMIN);
        MonthlyStatement statement = statementRepo.findById(statementId)
                .orElseThrow(() -> BizException.notFound("月结单#" + statementId));
        if (clinicUser.getClinic() == null
                || !clinicUser.getClinic().getId().equals(statement.getClinic().getId())) {
            throw BizException.forbidden("只能确认本诊所的月结单");
        }
        if (statement.getStatus() == StatementStatus.CONFIRMED) {
            throw new BizException("月结单已确认，不能重复确认");
        }
        // 实时复核：账期内全部返工必须已被诊所复核
        YearMonth ym = parseYearMonth(statement.getYearMonth());
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<Long> orderIds = orderRepo
                .findByClinicIdAndCompletedAtBetween(statement.getClinic().getId(), from, to)
                .stream().map(DentureOrder::getId).collect(Collectors.toList());
        long unreviewed = orderIds.isEmpty() ? 0 : reworkRepo.findByOrderIdIn(orderIds).stream()
                .filter(r -> !r.isClinicReviewed()).count();
        statement.setUnreviewedReworkCount((int) unreviewed);
        if (unreviewed > 0) {
            statementRepo.save(statement);
            throw new BizException("账期内还有 " + unreviewed + " 条返工未复核，诊所对账前必须复核全部返工");
        }
        statement.setStatus(StatementStatus.CONFIRMED);
        statement.setConfirmedAt(LocalDateTime.now());
        statementRepo.save(statement);
        return Views.statement(statement);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Account user, Long clinicId) {
        Long effectiveClinicId = clinicId;
        if (user.getRole() == Role.CLINIC_ADMIN || user.getRole() == Role.CLINIC_DOCTOR) {
            if (user.getClinic() == null) {
                throw new BizException("当前账号未关联诊所");
            }
            effectiveClinicId = user.getClinic().getId();
        }
        if (effectiveClinicId == null) {
            throw new BizException("请指定 clinicId");
        }
        return statementRepo.findByClinicIdOrderByYearMonthDesc(effectiveClinicId).stream()
                .map(Views::statement).collect(Collectors.toList());
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new BizException("账期格式应为 yyyy-MM: " + yearMonth);
        }
    }
}
