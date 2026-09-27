package com.dentallab;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端流程测试（真实 PostgreSQL，dentallab_test 库）：
 * 下单→接单→八道工序→发货→签收→试戴→返工→责任判定→重做→再签收→结算→月结复核。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DentalLabFlowTest {

    @org.junit.jupiter.api.BeforeEach
    void configureRestTemplate() {
        // JDK HttpClient：避免 HttpURLConnection 对 401 的流式重试问题
        plain = new RestTemplate(new JdkClientHttpRequestFactory());
        plain.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
                return false;
            }
        });
    }

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    private RestTemplate plain;

    private static String doctorToken;
    private static String receiverToken;
    private static String tech1Token;
    private static String tech2Token;
    private static String qcToken;
    private static String clinicAdminToken;
    private static String labAdminToken;
    private static String serviceToken;
    private static String financeToken;

    private static Long orderId;
    private static Long item1;
    private static Long item2;
    private static Long reworkId;
    private static Long zirconiaBatchId;
    private static Long statementId;

    // ---------- 主流程 ----------

    @Test
    @Order(1)
    void loginAllRoles() {
        doctorToken = login("doctor1", "doctor123");
        receiverToken = login("receiver", "lab123");
        tech1Token = login("tech1", "tech123");
        tech2Token = login("tech2", "tech123");
        qcToken = login("qc1", "qc123");
        clinicAdminToken = login("clinicadmin1", "admin123");
        labAdminToken = login("labadmin", "lab123");
        serviceToken = login("service1", "service123");
        financeToken = login("finance1", "finance123");

        // 错误密码应 401
        ResponseEntity<Map> bad = plain.postForEntity(url("/api/auth/login"),
                Map.of("username", "doctor1", "password", "wrong"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, bad.getStatusCode());
        // 无令牌访问受保护接口应 401
        ResponseEntity<Map> noAuth = plain.getForEntity(url("/api/orders"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noAuth.getStatusCode());
    }

    @Test
    @Order(2)
    void clinicCreatesOrder() {
        Map<String, Object> body = new HashMap<>();
        body.put("patientCode", "PAT-2026-0001");
        body.put("prosthesisType", "ALL_CERAMIC_CROWN");
        body.put("impressionMethod", "DIGITAL_SCAN");
        body.put("toothPositions", List.of("11", "12"));
        body.put("biteRelation", "正常覆合覆盖");
        body.put("shade", "VITA A2");
        body.put("doctorNotes", "邻面接触稍紧");
        body.put("expectedTryInDate", LocalDate.now().plusDays(7).toString());
        body.put("materialRequirement", "氧化锆全瓷");
        body.put("difficulty", 3);
        Map<String, Object> resp = data(post(doctorToken, "/api/orders", body));
        orderId = toLong(resp.get("id"));
        assertNotNull(resp.get("orderNo"));
        assertEquals("SUBMITTED", resp.get("status"));
        List<Map<String, Object>> items = list(resp, "toothItems");
        assertEquals(2, items.size());
        item1 = toLong(items.get(0).get("id"));
        item2 = toLong(items.get(1).get("id"));

        // 数字口扫文件登记
        Map<String, Object> scan = data(post(doctorToken, "/api/orders/" + orderId + "/scan-files",
                Map.of("fileName", "pat0001_upper.stl", "format", "STL",
                        "sizeBytes", 2048576, "storagePath", "oss://scans/pat0001_upper.stl")));
        assertEquals("STL", scan.get("format"));

        // 诊所医生不能给别的角色下单权限之外的操作：技工角色不能下单
        ResponseEntity<Map> forbidden = postRaw(tech1Token, "/api/orders", body);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    @Order(3)
    void labAcceptsAndProduces() {
        // 未接单前不能登记工序
        ResponseEntity<Map> early = postRaw(tech1Token, "/api/orders/" + orderId + "/items/" + item1 + "/stages",
                Map.of("stage", "MODEL_RECEIVING"));
        assertEquals(HttpStatus.BAD_REQUEST, early.getStatusCode());

        data(post(receiverToken, "/api/orders/" + orderId + "/accept", null));

        for (Long item : List.of(item1, item2)) {
            // 模型接收（前台）
            data(post(receiverToken, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("MODEL_RECEIVING", "收到口扫数据与模型", null, null, null)));
            // 扫描设计（技工1），写设计摘要
            Map<String, Object> design = stageBody("SCAN_DESIGN", "设计完成", null, null, null);
            design.put("designSummary", "全瓷冠设计，边缘位于龈上0.5mm");
            data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item + "/stages", design));
        }

        // 医生对 11 牙位在线批注（设计未冻结，不触发异常）
        data(post(doctorToken, "/api/orders/" + orderId + "/items/" + item1 + "/annotations",
                Map.of("content", "切端透明度再低一点")));

        for (Long item : List.of(item1, item2)) {
            // 医生确认
            data(post(doctorToken, "/api/orders/" + orderId + "/items/" + item + "/confirm",
                    Map.of("note", "设计确认，可以制作")));
            // 排牙（技工1）
            data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("TEETH_ARRANGEMENT", "排牙完成", null, null, null)));
        }

        // 材料批次：查氧化锆瓷块批次
        Map<String, Object> materials = data(get(labAdminToken, "/api/inventory/materials"));
        List<Map<String, Object>> mats = list(materials, "$data");
        Map<String, Object> zirconia = mats.stream()
                .filter(m -> "ZIRC".equals(m.get("code"))).findFirst().orElseThrow();
        List<Map<String, Object>> batches = list(zirconia, "batches");
        zirconiaBatchId = toLong(batches.get(0).get("id"));
        BigDecimal before = new BigDecimal(String.valueOf(batches.get(0).get("remainingQty")));

        for (Long item : List.of(item1, item2)) {
            // 烧结（技工2，使用氧化锆瓷块 1 块）
            data(post(tech2Token, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("SINTERING", "烧结完成", zirconiaBatchId, "1", List.of("photo_sinter_" + item + ".jpg"))));
            // 抛光（技工1）
            data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("POLISHING", "抛光完成", null, null, null)));
            // 质检（质检员）
            data(post(qcToken, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("QC", "质检合格", null, null, List.of("photo_qc_" + item + ".jpg"))));
            // 配送工序登记（前台）
            data(post(receiverToken, "/api/orders/" + orderId + "/items/" + item + "/stages",
                    stageBody("DELIVERY", "已打包待发货", null, null, null)));
        }

        // 库存被扣减 2 块
        Map<String, Object> after = data(get(labAdminToken, "/api/inventory/materials"));
        Map<String, Object> zircAfter = list(after, "$data").stream()
                .filter(m -> "ZIRC".equals(m.get("code"))).findFirst().orElseThrow();
        assertEquals(0, before.subtract(new BigDecimal("2"))
                .compareTo(new BigDecimal(String.valueOf(zircAfter.get("remainingQty")))));

        // 工序顺序错误应失败：已完成全部工序的牙位不能再登记
        ResponseEntity<Map> wrongSeq = postRaw(tech1Token, "/api/orders/" + orderId + "/items/" + item1 + "/stages",
                stageBody("POLISHING", "重复抛光", null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, wrongSeq.getStatusCode());

        Map<String, Object> detail = data(get(doctorToken, "/api/orders/" + orderId));
        assertEquals("QC_PASSED", detail.get("status"));
    }

    @Test
    @Order(4)
    void shipAndSignThenTryIn() {
        // 发货
        Map<String, Object> shipment = data(post(receiverToken, "/api/orders/" + orderId + "/shipments", null));
        Long shipmentId = toLong(shipment.get("id"));
        assertNotNull(shipment.get("trackingNo"));

        // 诊所完好签收 → 自动生成试戴安排
        data(post(clinicAdminToken, "/api/shipments/" + shipmentId + "/sign",
                Map.of("signResult", "GOOD", "signedByName", "陈前台")));
        Map<String, Object> detail = data(get(doctorToken, "/api/orders/" + orderId));
        assertEquals("TRY_IN", detail.get("status"));
        assertFalse(list(detail, "tryIns").isEmpty());

        // 牙位 11 试戴密合
        Map<String, Object> fit = new HashMap<>();
        fit.put("toothItemId", item1);
        fit.put("result", "FIT");
        fit.put("feedback", "密合良好，患者满意");
        fit.put("experienceScore", 5);
        data(post(doctorToken, "/api/orders/" + orderId + "/try-ins", fit));

        // 牙位 12 试戴不密合 → 自动异常 + 自动返工
        Map<String, Object> misfit = new HashMap<>();
        misfit.put("toothItemId", item2);
        misfit.put("result", "MISFIT");
        misfit.put("feedback", "边缘不密合，食物嵌塞");
        misfit.put("experienceScore", 2);
        data(post(doctorToken, "/api/orders/" + orderId + "/try-ins", misfit));

        Map<String, Object> after = data(get(doctorToken, "/api/orders/" + orderId));
        List<Map<String, Object>> exceptions = list(after, "exceptions");
        assertTrue(exceptions.stream().anyMatch(e -> "TRY_IN_MISFIT".equals(e.get("type"))));
        List<Map<String, Object>> reworks = list(after, "reworks");
        assertEquals(1, reworks.size());
        assertEquals("PENDING", reworks.get(0).get("responsibility"));
        reworkId = toLong(reworks.get(0).get("id"));
    }

    @Test
    @Order(5)
    void reworkResponsibilityAndRedo() {
        // 客服上报“返工责任不清”异常
        data(post(serviceToken, "/api/orders/" + orderId + "/exceptions",
                Map.of("type", "REWORK_DISPUTE", "description", "诊所与技工所对返工责任有分歧",
                        "toothItemId", item2)));

        // 牙位 12 重做：从模型接收重新开始
        data(post(receiverToken, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("MODEL_RECEIVING", "返工模型接收", null, null, null)));
        data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("SCAN_DESIGN", "返工设计", null, null, null)));
        data(post(doctorToken, "/api/orders/" + orderId + "/items/" + item2 + "/confirm", null));
        data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("TEETH_ARRANGEMENT", "返工排牙", null, null, null)));
        data(post(tech2Token, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("SINTERING", "返工烧结", zirconiaBatchId, "1", null)));
        data(post(tech1Token, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("POLISHING", "返工抛光", null, null, null)));
        data(post(qcToken, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("QC", "返工质检合格", null, null, null)));
        data(post(receiverToken, "/api/orders/" + orderId + "/items/" + item2 + "/stages",
                stageBody("DELIVERY", "返工打包", null, null, null)));

        // 返工单应已自动完成
        Map<String, Object> reworks = data(get(doctorToken, "/api/orders/" + orderId + "/reworks"));
        Map<String, Object> rework = list(reworks, "$data").get(0);
        assertEquals("COMPLETED", rework.get("status"));

        // 只重发牙位 12
        Map<String, Object> shipment = data(post(receiverToken, "/api/orders/" + orderId + "/shipments",
                Map.of("itemIds", List.of(item2))));
        Long shipmentId = toLong(shipment.get("id"));
        data(post(clinicAdminToken, "/api/shipments/" + shipmentId + "/sign",
                Map.of("signResult", "GOOD", "signedByName", "陈前台")));

        // 牙位 12 再次试戴密合 → 订单完成
        Map<String, Object> fit = new HashMap<>();
        fit.put("toothItemId", item2);
        fit.put("result", "FIT");
        fit.put("feedback", "返工后密合");
        fit.put("experienceScore", 4);
        data(post(doctorToken, "/api/orders/" + orderId + "/try-ins", fit));

        Map<String, Object> detail = data(get(doctorToken, "/api/orders/" + orderId));
        assertEquals("COMPLETED", detail.get("status"));
        assertNotNull(detail.get("completedAt"));

        // 返工责任未判定前不能结算
        ResponseEntity<Map> blocked = postRaw(financeToken, "/api/orders/" + orderId + "/settlement", null);
        assertEquals(HttpStatus.BAD_REQUEST, blocked.getStatusCode());
        assertTrue(String.valueOf(Objects.requireNonNull(blocked.getBody()).get("error")).contains("返工责任"));

        // 技工所管理员判定责任：技工所承担 200
        data(post(labAdminToken, "/api/reworks/" + reworkId + "/assign-responsibility",
                Map.of("responsibility", "LAB", "reworkCost", 200)));
    }

    @Test
    @Order(6)
    void settlementReflectsReworkResponsibility() {
        Map<String, Object> s = data(post(financeToken, "/api/orders/" + orderId + "/settlement", null));
        // 基础费 500×2=1000，难度3系数1.30 → 1300；技工所承担返工 200 扣减 → 1100
        assertMoney("1000.00", s.get("baseAmount"));
        assertMoney("1.30", s.get("difficultyMultiplier"));
        assertMoney("0.00", s.get("rushFee"));
        assertMoney("0.00", s.get("reworkClinicShare"));
        assertMoney("200.00", s.get("reworkLabDeduction"));
        assertMoney("0.00", s.get("deliveryPenalty"));
        assertMoney("1100.00", s.get("totalAmount"));
    }

    @Test
    @Order(7)
    void monthlyStatementRequiresReworkReview() {
        String ym = java.time.YearMonth.now().toString();
        // 诊所未复核返工 → 月结单生成但确认被拒
        Map<String, Object> statement = data(post(financeToken, "/api/statements/generate",
                Map.of("clinicId", 1, "yearMonth", ym)));
        statementId = toLong(statement.get("id"));
        assertEquals(1, ((Number) statement.get("unreviewedReworkCount")).intValue());
        assertMoney("1100.00", statement.get("totalAmount"));

        ResponseEntity<Map> confirmBlocked = postRaw(clinicAdminToken, "/api/statements/" + statementId + "/confirm", null);
        assertEquals(HttpStatus.BAD_REQUEST, confirmBlocked.getStatusCode());

        // 诊所复核全部返工
        Map<String, Object> unreviewed = data(get(clinicAdminToken, "/api/reworks/unreviewed"));
        assertEquals(1, list(unreviewed, "$data").size());
        data(post(clinicAdminToken, "/api/reworks/" + reworkId + "/clinic-review", null));

        // 复核后确认成功
        Map<String, Object> confirmed = data(post(clinicAdminToken, "/api/statements/" + statementId + "/confirm", null));
        assertEquals("CONFIRMED", confirmed.get("status"));
    }

    @Test
    @Order(8)
    void toothBoardAndPerformance() {
        // 按牙位看板：每颗牙的设计、工序、返工责任
        Map<String, Object> board = data(get(labAdminToken, "/api/orders/" + orderId + "/tooth-board"));
        List<Map<String, Object>> teeth = list(board, "teeth");
        assertEquals(2, teeth.size());
        Map<String, Object> tooth12 = teeth.stream()
                .filter(t -> "12".equals(t.get("toothPosition"))).findFirst().orElseThrow();
        assertFalse(list(tooth12, "stages").isEmpty());
        assertEquals(1, list(tooth12, "reworks").size());
        assertEquals("LAB", list(tooth12, "reworks").get(0).get("responsibility"));

        // 技工绩效
        Map<String, Object> perf = data(get(labAdminToken, "/api/performance/technicians"));
        List<Map<String, Object>> techs = list(perf, "$data");
        assertEquals(2, techs.size());
        Map<String, Object> tech1 = techs.stream()
                .filter(t -> "张技工".equals(t.get("technicianName"))).findFirst().orElseThrow();
        assertTrue(((Number) tech1.get("stagesCompleted")).longValue() >= 5);
    }

    // ---------- 第二场景：加急 + 配送延误 + 异常处理 ----------

    @Test
    @Order(9)
    void rushOrderWithLateDelivery() {
        // 诊所下单（烤瓷冠，单牙位，难度1）
        Map<String, Object> body = new HashMap<>();
        body.put("patientCode", "PAT-2026-0002");
        body.put("prosthesisType", "PFM_CROWN");
        body.put("impressionMethod", "TRADITIONAL");
        body.put("toothPositions", List.of("36"));
        body.put("shade", "VITA B1");
        body.put("expectedTryInDate", LocalDate.now().plusDays(5).toString());
        body.put("difficulty", 1);
        Map<String, Object> order = data(post(doctorToken, "/api/orders", body));
        Long oid = toLong(order.get("id"));
        Long item = toLong(list(order, "toothItems").get(0).get("id"));

        // 取模气泡异常上报 → 客服处理
        Map<String, Object> exc = data(post(doctorToken, "/api/orders/" + oid + "/exceptions",
                Map.of("type", "IMPRESSION_BUBBLE", "description", "模型 36 远中气泡", "toothItemId", item)));
        Long excId = toLong(exc.get("id"));
        Map<String, Object> handled = data(post(serviceToken, "/api/exceptions/" + excId + "/handle",
                Map.of("resolution", "诊所重新取模后快递至技工所，已收到", "resolved", true)));
        assertEquals("RESOLVED", handled.get("status"));

        // 患者加急
        Map<String, Object> rushed = data(post(doctorToken, "/api/orders/" + oid + "/rush",
                Map.of("reason", "患者出差，需提前试戴")));
        assertEquals(Boolean.TRUE, rushed.get("rush"));

        // 快速走完工序
        data(post(receiverToken, "/api/orders/" + oid + "/accept", null));
        data(post(receiverToken, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("MODEL_RECEIVING", null, null, null, null)));
        data(post(tech1Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("SCAN_DESIGN", null, null, null, null)));
        data(post(doctorToken, "/api/orders/" + oid + "/items/" + item + "/confirm", null));
        data(post(tech1Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("TEETH_ARRANGEMENT", null, null, null, null)));
        data(post(tech2Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("SINTERING", null, null, null, null)));
        data(post(tech1Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("POLISHING", null, null, null, null)));
        data(post(qcToken, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("QC", null, null, null, null)));
        data(post(receiverToken, "/api/orders/" + oid + "/items/" + item + "/stages",
                stageBody("DELIVERY", null, null, null, null)));

        // 发货 + 延误签收 → 试戴顺延 + 自动登记配送延误异常
        Map<String, Object> shipment = data(post(receiverToken, "/api/orders/" + oid + "/shipments", null));
        Long shipmentId = toLong(shipment.get("id"));
        data(post(clinicAdminToken, "/api/shipments/" + shipmentId + "/sign",
                Map.of("signResult", "LATE", "signedByName", "陈前台", "delayReason", "暴雨高速封路")));
        Map<String, Object> detail = data(get(doctorToken, "/api/orders/" + oid));
        assertTrue(list(detail, "exceptions").stream().anyMatch(e -> "DELIVERY_DELAY".equals(e.get("type"))));

        // 试戴密合 → 完成
        Map<String, Object> fit = new HashMap<>();
        fit.put("toothItemId", item);
        fit.put("result", "FIT");
        fit.put("experienceScore", 4);
        data(post(doctorToken, "/api/orders/" + oid + "/try-ins", fit));

        // 结算：300×1×1.0 + 加急 300×0.3=90 − 延误扣罚 300×0.05=15 → 375
        Map<String, Object> s = data(post(financeToken, "/api/orders/" + oid + "/settlement", null));
        assertMoney("300.00", s.get("baseAmount"));
        assertMoney("90.00", s.get("rushFee"));
        assertMoney("15.00", s.get("deliveryPenalty"));
        assertMoney("375.00", s.get("totalAmount"));
    }

    // ---------- 第三场景：取模缺陷退回 → 重取模 → 重算交付 → 三端同步 → 确认归档 ----------

    @Test
    @Order(10)
    void impressionDefectReturnFlow() {
        // 诊所下单（烤瓷冠，牙位 21/22，传统取模，预计试戴 10 天后）
        Map<String, Object> body = new HashMap<>();
        body.put("patientCode", "PAT-2026-0003");
        body.put("prosthesisType", "PFM_CROWN");
        body.put("impressionMethod", "TRADITIONAL");
        body.put("toothPositions", List.of("21", "22"));
        body.put("shade", "VITA A3");
        body.put("expectedTryInDate", LocalDate.now().plusDays(10).toString());
        body.put("difficulty", 1);
        Map<String, Object> order = data(post(doctorToken, "/api/orders", body));
        Long oid = toLong(order.get("id"));
        Long itemA = toLong(list(order, "toothItems").get(0).get("id"));
        Long itemB = toLong(list(order, "toothItems").get(1).get("id"));
        data(post(receiverToken, "/api/orders/" + oid + "/accept", null));

        // 牙位 21 先完成模型接收
        data(post(receiverToken, "/api/orders/" + oid + "/items/" + itemA + "/stages",
                stageBody("MODEL_RECEIVING", "模型接收", null, null, null)));

        // 技工所发现牙位 22 边缘不清+气泡，退回必须上传缺陷照片：缺照片应被拒绝
        ResponseEntity<Map> noPhoto = postRaw(receiverToken,
                "/api/orders/" + oid + "/impression-returns",
                Map.of("defectType", "BUBBLE", "description", "边缘气泡"));
        assertEquals(HttpStatus.BAD_REQUEST, noPhoto.getStatusCode());
        assertTrue(String.valueOf(Objects.requireNonNull(noPhoto.getBody()).get("error")).contains("照片"));

        // 正式上报缺陷：延期 5 天、费用 100 双方共担（50/50）
        Map<String, Object> ret = new HashMap<>();
        ret.put("toothItemId", itemB);
        ret.put("defectType", "MARGIN_UNCLEAR");
        ret.put("defectPhotos", List.of("defect_22_edge_1.jpg", "defect_22_edge_2.jpg"));
        ret.put("description", "牙位22模型边缘不清且颊侧有气泡");
        ret.put("delayDays", 5);
        ret.put("costResponsibility", "SHARED");
        ret.put("costAmount", 100);
        Map<String, Object> returned = data(post(receiverToken,
                "/api/orders/" + oid + "/impression-returns", ret));
        Long returnId = toLong(returned.get("id"));
        assertEquals("PENDING_REIMPRESSION", returned.get("status"));
        assertEquals("MARGIN_UNCLEAR", returned.get("defectType"));
        assertMoney("50", returned.get("clinicShare"));
        assertMoney("50", returned.get("labShare"));

        // 订单与牙位进入退回态；旧模型继续排产被拦截
        Map<String, Object> detail = data(get(doctorToken, "/api/orders/" + oid));
        assertEquals("IMPRESSION_RETURNED", detail.get("status"));
        Map<String, Object> toothB = list(detail, "toothItems").stream()
                .filter(t -> "22".equals(t.get("toothPosition"))).findFirst().orElseThrow();
        assertEquals("IMPRESSION_RETURNED", toothB.get("status"));
        ResponseEntity<Map> blocked = postRaw(tech1Token,
                "/api/orders/" + oid + "/items/" + itemB + "/stages",
                stageBody("SCAN_DESIGN", "旧模型继续排产", null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, blocked.getStatusCode());
        assertTrue(String.valueOf(Objects.requireNonNull(blocked.getBody()).get("error")).contains("旧模型"));

        // 诊所端收到重取模通知
        List<Map<String, Object>> notices = list(data(get(doctorToken,
                "/api/orders/" + oid + "/notifications")), "$data");
        assertEquals(1, notices.size());
        assertEquals("CLINIC", notices.get(0).get("recipientType"));

        // 技工所角色不能替诊所提交重取模
        ResponseEntity<Map> forbidden = postRaw(receiverToken,
                "/api/impression-returns/" + returnId + "/reimpression",
                Map.of("note", "新模型已寄出"));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        // 诊所重新取模 → 服务重算交付日期（原日期+5天），患者/医生/诊所三端同步
        Map<String, Object> reimpressed = data(post(clinicAdminToken,
                "/api/impression-returns/" + returnId + "/reimpression",
                Map.of("note", "已重新取模并顺丰寄出")));
        assertEquals("REIMPRESSION_SUBMITTED", reimpressed.get("status"));
        assertEquals(LocalDate.now().plusDays(15).toString(), reimpressed.get("newExpectedTryInDate"));
        detail = data(get(doctorToken, "/api/orders/" + oid));
        assertEquals(LocalDate.now().plusDays(15).toString(), detail.get("expectedTryInDate"));
        assertEquals("ACCEPTED", detail.get("status"));
        notices = list(data(get(doctorToken, "/api/orders/" + oid + "/notifications")), "$data");
        assertEquals(4, notices.size());
        assertEquals(List.of("CLINIC", "PATIENT", "DOCTOR", "CLINIC"),
                notices.stream().map(n -> String.valueOf(n.get("recipientType"))).toList());
        // 患者端提醒包含预约调整后的新日期
        assertTrue(String.valueOf(notices.get(1).get("message")).contains(LocalDate.now().plusDays(15).toString()));

        // 重复重取模被拒绝；医生未等诊所确认不能归档
        ResponseEntity<Map> dup = postRaw(clinicAdminToken,
                "/api/impression-returns/" + returnId + "/reimpression", Map.of());
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());
        ResponseEntity<Map> earlyArchive = postRaw(doctorToken,
                "/api/impression-returns/" + returnId + "/doctor-archive", null);
        assertEquals(HttpStatus.BAD_REQUEST, earlyArchive.getStatusCode());

        // 诊所端确认 → 医生确认归档
        Map<String, Object> confirmed = data(post(clinicAdminToken,
                "/api/impression-returns/" + returnId + "/clinic-confirm", null));
        assertEquals("CLINIC_CONFIRMED", confirmed.get("status"));
        Map<String, Object> archived = data(post(doctorToken,
                "/api/impression-returns/" + returnId + "/doctor-archive", null));
        assertEquals("ARCHIVED", archived.get("status"));

        // 新模型重新走八道工序（21 从扫描设计继续，22 从模型接收开始）
        data(post(tech1Token, "/api/orders/" + oid + "/items/" + itemA + "/stages",
                stageBody("SCAN_DESIGN", null, null, null, null)));
        data(post(receiverToken, "/api/orders/" + oid + "/items/" + itemB + "/stages",
                stageBody("MODEL_RECEIVING", "新模型接收", null, null, null)));
        data(post(tech1Token, "/api/orders/" + oid + "/items/" + itemB + "/stages",
                stageBody("SCAN_DESIGN", null, null, null, null)));
        for (Long item : List.of(itemA, itemB)) {
            data(post(doctorToken, "/api/orders/" + oid + "/items/" + item + "/confirm", null));
            data(post(tech1Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                    stageBody("TEETH_ARRANGEMENT", null, null, null, null)));
            data(post(tech2Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                    stageBody("SINTERING", null, null, null, null)));
            data(post(tech1Token, "/api/orders/" + oid + "/items/" + item + "/stages",
                    stageBody("POLISHING", null, null, null, null)));
            data(post(qcToken, "/api/orders/" + oid + "/items/" + item + "/stages",
                    stageBody("QC", null, null, null, null)));
            data(post(receiverToken, "/api/orders/" + oid + "/items/" + item + "/stages",
                    stageBody("DELIVERY", null, null, null, null)));
        }

        // 发货签收、两牙位试戴密合 → 订单完成
        Map<String, Object> shipment = data(post(receiverToken,
                "/api/orders/" + oid + "/shipments", null));
        Long shipmentId = toLong(shipment.get("id"));
        data(post(clinicAdminToken, "/api/shipments/" + shipmentId + "/sign",
                Map.of("signResult", "GOOD", "signedByName", "陈前台")));
        for (Long item : List.of(itemA, itemB)) {
            Map<String, Object> fit = new HashMap<>();
            fit.put("toothItemId", item);
            fit.put("result", "FIT");
            fit.put("experienceScore", 5);
            data(post(doctorToken, "/api/orders/" + oid + "/try-ins", fit));
        }
        assertEquals("COMPLETED", data(get(doctorToken, "/api/orders/" + oid)).get("status"));

        // 结算：基础费 300×2=600，重取模费用双方共担 100（诊所+50、技工所-50）→ 应付仍为 600
        Map<String, Object> s = data(post(financeToken, "/api/orders/" + oid + "/settlement", null));
        assertMoney("600.00", s.get("baseAmount"));
        assertMoney("50.00", s.get("impressionClinicShare"));
        assertMoney("50.00", s.get("impressionLabDeduction"));
        assertMoney("600.00", s.get("totalAmount"));
    }

    // ---------- 工具方法 ----------

    private String login(String username, String password) {
        Map<String, Object> resp = data(plain.postForEntity(url("/api/auth/login"),
                Map.of("username", username, "password", password), Map.class));
        return String.valueOf(resp.get("token"));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> postRaw(String token, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("X-Auth-Token", token);
        }
        return plain.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body == null ? Map.of() : body, headers), Map.class);
    }

    private ResponseEntity<Map> post(String token, String path, Object body) {
        ResponseEntity<Map> resp = postRaw(token, path, body);
        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                () -> "POST " + path + " -> " + resp.getStatusCode() + " " + resp.getBody());
        return resp;
    }

    private ResponseEntity<Map> get(String token, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);
        ResponseEntity<Map> resp = plain.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                () -> "GET " + path + " -> " + resp.getStatusCode() + " " + resp.getBody());
        return resp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> resp) {
        Map<String, Object> body = Objects.requireNonNull(resp.getBody());
        assertEquals(Boolean.TRUE, body.get("success"), () -> "响应失败: " + body);
        Object data = body.get("data");
        if (data instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        // 列表响应：包装一层
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("$data", data);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Map<String, Object> map, String key) {
        return (List<Map<String, Object>>) map.get(key);
    }

    private Long toLong(Object v) {
        return ((Number) v).longValue();
    }

    private void assertMoney(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(actual))),
                () -> "金额不符: expected=" + expected + " actual=" + actual);
    }

    private Map<String, Object> stageBody(String stage, String note, Long materialBatchId,
                                          String materialQty, List<String> photos) {
        Map<String, Object> body = new HashMap<>();
        body.put("stage", stage);
        if (note != null) {
            body.put("note", note);
        }
        body.put("estimatedFinishAt", "2026-12-31T23:59:00");
        body.put("finish", true);
        if (materialBatchId != null) {
            body.put("materialBatchId", materialBatchId);
            body.put("materialQty", materialQty);
        }
        if (photos != null) {
            body.put("photos", photos);
        }
        return body;
    }
}
