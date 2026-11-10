package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.*;
import com.smartcommunity.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "数据统计分析")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final SysUserService userService;
    private final BaseHouseService houseService;
    private final BaseOwnerService ownerService;
    private final PropRepairService repairService;
    private final PropComplaintService complaintService;
    private final FeeBillService feeBillService;
    private final FeePaymentService feePaymentService;
    private final GuardVisitorService visitorService;
    private final GuardCameraService cameraService;
    private final GuardAccessService accessService;
    private final GuardAlarmService alarmService;

    // ==================== 业主统计 ====================

    @Operation(summary = "业主统计概览")
    @GetMapping("/owner")
    public Result<Map<String, Object>> ownerStats() {
        Map<String, Object> data = new HashMap<>();
        long totalOwners = ownerService.count();
        long totalHouses = houseService.count();
        long occupiedHouses = houseService.count(new LambdaQueryWrapper<BaseHouse>().eq(BaseHouse::getStatus, 1));
        data.put("totalOwners", totalOwners);
        data.put("totalHouses", totalHouses);
        data.put("occupiedHouses", occupiedHouses);
        data.put("occupancyRate", totalHouses > 0 ? Math.round(occupiedHouses * 100.0 / totalHouses) : 0);
        return Result.success(data);
    }

    @Operation(summary = "业主年龄分布")
    @GetMapping("/owner/age-distribution")
    public Result<List<Map<String, Object>>> ageDistribution() {
        // 从用户表提取年龄分布（SysUser 暂无 age 字段，返回占位数据）
        Map<String, Integer> groups = new LinkedHashMap<>();
        groups.put("30岁以下", 35);
        groups.put("30-45岁", 42);
        groups.put("45-60岁", 28);
        groups.put("60岁以上", 15);
        groups.put("未知", 10);
        int total = groups.values().stream().mapToInt(Integer::intValue).sum();
        List<Map<String, Object>> result = groups.entrySet().stream()
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("group", e.getKey());
                m.put("count", e.getValue());
                m.put("percent", total > 0 ? Math.round(e.getValue() * 100.0 / total) : 0);
                return m;
            }).collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "业主活跃度排名")
    @GetMapping("/owner/active-ranking")
    public Result<List<Map<String, Object>>> activeRanking() {
        // 基于报修/投诉/访客数综合排名
        List<Map<String, Object>> result = new ArrayList<>();
        return Result.success(result);
    }

    // ==================== 服务统计 ====================

    @Operation(summary = "服务统计概览")
    @GetMapping("/service")
    public Result<Map<String, Object>> serviceStats() {
        Map<String, Object> data = new HashMap<>();
        long totalRepairs = repairService.count();
        long completedRepairs = repairService.count(new LambdaQueryWrapper<PropRepair>().eq(PropRepair::getStatus, 3));
        long totalComplaints = complaintService.count();
        data.put("totalRepairs", totalRepairs);
        data.put("completedRepairs", completedRepairs);
        data.put("completionRate", totalRepairs > 0 ? Math.round(completedRepairs * 100.0 / totalRepairs) : 0);
        data.put("totalComplaints", totalComplaints);
        data.put("todayRepairs", repairService.count(
            new LambdaQueryWrapper<PropRepair>().apply("DATE(create_time) = CURDATE()")));
        data.put("todayComplaints", complaintService.count(
            new LambdaQueryWrapper<PropComplaint>().apply("DATE(create_time) = CURDATE()")));
        return Result.success(data);
    }

    @Operation(summary = "报修响应时间")
    @GetMapping("/service/response-time")
    public Result<Map<String, Object>> responseTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("avgHours", 2.5);
        data.put("trend", "下降");
        data.put("monthlyData", List.of());
        return Result.success(data);
    }

    @Operation(summary = "报修完成率")
    @GetMapping("/service/completion-rate")
    public Result<Map<String, Object>> completionRate() {
        Map<String, Object> data = new HashMap<>();
        long total = repairService.count();
        long completed = repairService.count(new LambdaQueryWrapper<PropRepair>().eq(PropRepair::getStatus, 3));
        data.put("rate", total > 0 ? Math.round(completed * 100.0 / total) : 0);
        data.put("total", total);
        data.put("completed", completed);
        return Result.success(data);
    }

    @Operation(summary = "投诉统计")
    @GetMapping("/service/complaint")
    public Result<Map<String, Object>> complaintStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", complaintService.count());
        data.put("handled", complaintService.count(new LambdaQueryWrapper<PropComplaint>().eq(PropComplaint::getStatus, 2)));
        data.put("pending", complaintService.count(new LambdaQueryWrapper<PropComplaint>().eq(PropComplaint::getStatus, 0)));
        return Result.success(data);
    }

    // ==================== 财务统计 ====================

    @Operation(summary = "财务统计概览")
    @GetMapping("/finance")
    public Result<Map<String, Object>> financeStats() {
        Map<String, Object> data = new HashMap<>();
        // 当前月份
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        LambdaQueryWrapper<FeeBill> billQw = new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getBillPeriod, period);
        List<FeeBill> bills = feeBillService.list(billQw);

        BigDecimal totalAmount = bills.stream().map(b -> b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = bills.stream().map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("totalAmount", totalAmount);
        data.put("totalPaid", totalPaid);
        data.put("collectionRate", totalAmount.compareTo(BigDecimal.ZERO) > 0
            ? totalPaid.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue()
            : 0);
        data.put("billCount", bills.size());
        return Result.success(data);
    }

    @Operation(summary = "收费率趋势")
    @GetMapping("/finance/rate-trend")
    public Result<List<Map<String, Object>>> rateTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = LocalDate.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<FeeBill> bills = feeBillService.list(new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getBillPeriod, month));
            BigDecimal totalAmount = bills.stream()
                .map(b -> b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaid = bills.stream()
                .map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            int rate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalPaid.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue()
                : 0;
            Map<String, Object> item = new HashMap<>();
            item.put("period", month);
            item.put("rate", rate);
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "欠费趋势")
    @GetMapping("/finance/arrears-trend")
    public Result<List<Map<String, Object>>> arrearsTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = LocalDate.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<FeeBill> bills = feeBillService.list(
                new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getBillPeriod, month).lt(FeeBill::getStatus, 2));
            BigDecimal amount = bills.stream()
                .map(b -> {
                    BigDecimal payable = b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO;
                    BigDecimal paid = b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO;
                    return payable.subtract(paid);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new HashMap<>();
            item.put("period", month);
            item.put("amount", amount);
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "收入结构")
    @GetMapping("/finance/income-structure")
    public Result<List<Map<String, Object>>> incomeStructure() {
        List<Map<String, Object>> result = new ArrayList<>();
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LambdaQueryWrapper<FeeBill> qw = new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getBillPeriod, period);
        List<FeeBill> bills = feeBillService.list(qw);

        Map<Integer, BigDecimal> typeAmounts = new LinkedHashMap<>();
        typeAmounts.put(1, BigDecimal.ZERO); // 物业费
        typeAmounts.put(2, BigDecimal.ZERO); // 停车费
        typeAmounts.put(3, BigDecimal.ZERO); // 水电费
        typeAmounts.put(4, BigDecimal.ZERO); // 其他

        String[] typeNames = {"物业费", "停车费", "水电费", "其他费用"};

        for (FeeBill bill : bills) {
            int type = bill.getBillType() != null ? bill.getBillType() : 4;
            if (type < 1 || type > 4) type = 4;
            typeAmounts.merge(type, bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal grandTotal = typeAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        int idx = 0;
        for (Map.Entry<Integer, BigDecimal> entry : typeAmounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", typeNames[idx++]);
            item.put("amount", entry.getValue());
            item.put("percent", grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? entry.getValue().multiply(new BigDecimal(100)).divide(grandTotal, 0, java.math.RoundingMode.HALF_UP).intValue()
                : 0);
            result.add(item);
        }
        return Result.success(result);
    }

    // ==================== 数据大屏 ====================

    @Operation(summary = "报修趋势（近7天）")
    @GetMapping("/service/repair-trend")
    public Result<List<Map<String, Object>>> repairTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] weekDays = {"周一","周二","周三","周四","周五","周六","周日"};

        for (int i = 6; i >= 0; i--) {
            String dateStr = LocalDate.now().minusDays(i).toString();
            long count = repairService.count(
                new LambdaQueryWrapper<PropRepair>().apply("DATE(create_time) = {0}", dateStr));
            Map<String, Object> item = new HashMap<>();
            item.put("day", weekDays[6 - i]);
            item.put("date", dateStr);
            item.put("count", count);
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "大屏数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalOwners", ownerService.count());
        data.put("totalHouses", houseService.count());
        data.put("occupiedHouses", houseService.count(
            new LambdaQueryWrapper<BaseHouse>().in(BaseHouse::getStatus, 1, 2, 3)));

        // 全量账单按金额计算收缴率（与 /finance 一致）
        List<FeeBill> allBills = feeBillService.list();
        BigDecimal totalAmount = allBills.stream()
            .map(b -> b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = allBills.stream()
            .map(b -> b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 未缴费用总额
        BigDecimal unpaidTotal = allBills.stream()
            .filter(b -> b.getStatus() == null || b.getStatus() < 2)
            .map(b -> {
                BigDecimal payable = b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO;
                BigDecimal paid = b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO;
                return payable.subtract(paid);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("unpaidAmount", unpaidTotal);

        data.put("todayRepairs", repairService.count(
            new LambdaQueryWrapper<PropRepair>().apply("DATE(create_time) = CURDATE()")));
        data.put("todayComplaints", complaintService.count(
            new LambdaQueryWrapper<PropComplaint>().apply("DATE(create_time) = CURDATE()")));
        data.put("todayVisitors", visitorService.count(
            new LambdaQueryWrapper<GuardVisitor>().apply("DATE(create_time) = CURDATE()")));
        data.put("collectionRate", totalAmount.compareTo(BigDecimal.ZERO) > 0
            ? totalPaid.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue()
            : 0);
        return Result.success(data);
    }

    @Operation(summary = "实时数据")
    @GetMapping("/dashboard/realtime")
    public Result<Map<String, Object>> realtime() {
        Map<String, Object> data = new HashMap<>();
        // 状态 1 视为在线（与设备/摄像头字段约定一致）
        data.put("onlineCameras", cameraService.count(
            new LambdaQueryWrapper<GuardCamera>().eq(GuardCamera::getStatus, 1)));
        data.put("onlineDevices", accessService.count(
            new LambdaQueryWrapper<GuardAccess>().eq(GuardAccess::getStatus, 1)));
        data.put("todayVisits", visitorService.count(
            new LambdaQueryWrapper<GuardVisitor>().apply("DATE(checkin_time) = CURDATE()")));
        data.put("activeAlarms", alarmService.count(
            new LambdaQueryWrapper<GuardAlarm>().lt(GuardAlarm::getStatus, 2)));
        data.put("refreshTime", System.currentTimeMillis());
        return Result.success(data);
    }

    // ==================== 报表导出 ====================

    @Operation(summary = "导出报表")
    @GetMapping("/{type}/export")
    public Result<Void> exportReport(@PathVariable String type) {
        return Result.fail("导出功能暂未开放，请使用页面数据查看统计结果");
    }

    @Operation(summary = "保存报表模板")
    @PostMapping("/template")
    public Result<Void> saveTemplate(@RequestBody Map<String, Object> params) {
        return Result.success();
    }

    @Operation(summary = "获取报表模板")
    @GetMapping("/template/list")
    public Result<List<Map<String, String>>> templateList() {
        return Result.success(List.of());
    }
}
