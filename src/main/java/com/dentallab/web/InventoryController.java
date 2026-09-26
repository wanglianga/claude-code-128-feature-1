package com.dentallab.web;

import com.dentallab.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 材料库存。 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/materials")
    public Map<String, Object> createMaterial(HttpServletRequest request,
                                              @RequestBody Map<String, Object> body) {
        return Web.ok(inventoryService.createMaterial(Web.user(request),
                Req.strReq(body, "code", "材料编码"),
                Req.strReq(body, "name", "材料名称"),
                Req.strReq(body, "unit", "计量单位"),
                Req.decimal(body, "unitPrice")));
    }

    @PostMapping("/materials/{id}/batches")
    public Map<String, Object> addBatch(HttpServletRequest request, @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        return Web.ok(inventoryService.addBatch(Web.user(request), id,
                Req.strReq(body, "batchNo", "批次号"),
                Req.decimal(body, "qty"),
                Req.date(body, "expiryDate")));
    }

    /** 库存总览（含低库存预警）。 */
    @GetMapping("/materials")
    public Map<String, Object> overview() {
        return Web.ok(inventoryService.overview());
    }

    @GetMapping("/materials/{id}/batches")
    public Map<String, Object> batches(@PathVariable Long id) {
        return Web.ok(inventoryService.batches(id));
    }
}
