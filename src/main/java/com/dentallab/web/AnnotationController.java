package com.dentallab.web;

import com.dentallab.service.ProductionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 批注处理。 */
@RestController
@RequestMapping("/api/annotations")
public class AnnotationController {

    private final ProductionService productionService;

    public AnnotationController(ProductionService productionService) {
        this.productionService = productionService;
    }

    /** 技工所标记批注已处理。 */
    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolve(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(productionService.resolveAnnotation(Web.user(request), id));
    }
}
