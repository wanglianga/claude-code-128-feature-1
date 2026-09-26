package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.MaterialBatchRepo;
import com.dentallab.repo.MaterialRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 材料库存：材料档案、批次入库、库存余量与低库存预警。 */
@Service
public class InventoryService {

    /** 低库存预警阈值。 */
    private static final BigDecimal LOW_STOCK_THRESHOLD = new BigDecimal("100");

    private final MaterialRepo materialRepo;
    private final MaterialBatchRepo batchRepo;
    private final AuthService authService;

    public InventoryService(MaterialRepo materialRepo, MaterialBatchRepo batchRepo, AuthService authService) {
        this.materialRepo = materialRepo;
        this.batchRepo = batchRepo;
        this.authService = authService;
    }

    @Transactional
    public Map<String, Object> createMaterial(Account user, String code, String name, String unit, BigDecimal unitPrice) {
        authService.requireRole(user, Role.LAB_ADMIN, Role.LAB_RECEIVER);
        materialRepo.findByCode(code).ifPresent(m -> {
            throw new BizException("材料编码已存在: " + code);
        });
        Material material = new Material();
        material.setCode(code);
        material.setName(name);
        material.setUnit(unit);
        material.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
        materialRepo.save(material);
        return Views.material(material, BigDecimal.ZERO);
    }

    @Transactional
    public Map<String, Object> addBatch(Account user, Long materialId, String batchNo,
                                        BigDecimal qty, LocalDate expiryDate) {
        authService.requireRole(user, Role.LAB_ADMIN, Role.LAB_RECEIVER);
        Material material = materialRepo.findById(materialId)
                .orElseThrow(() -> BizException.notFound("材料#" + materialId));
        if (qty == null || qty.signum() <= 0) {
            throw new BizException("入库数量必须为正数");
        }
        MaterialBatch batch = new MaterialBatch();
        batch.setMaterial(material);
        batch.setBatchNo(batchNo);
        batch.setTotalQty(qty);
        batch.setRemainingQty(qty);
        batch.setExpiryDate(expiryDate);
        batchRepo.save(batch);
        return Views.batch(batch);
    }

    /** 库存总览：每种材料的剩余量与低库存标记。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> overview() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Material material : materialRepo.findAll()) {
            BigDecimal remaining = batchRepo.findByMaterialIdOrderByIdAsc(material.getId()).stream()
                    .map(MaterialBatch::getRemainingQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> m = Views.material(material, remaining);
            m.put("lowStock", remaining.compareTo(LOW_STOCK_THRESHOLD) < 0);
            m.put("batches", batchRepo.findByMaterialIdOrderByIdAsc(material.getId()).stream()
                    .map(Views::batch).collect(Collectors.toList()));
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> batches(Long materialId) {
        return batchRepo.findByMaterialIdOrderByIdAsc(materialId).stream()
                .map(Views::batch).collect(Collectors.toList());
    }
}
