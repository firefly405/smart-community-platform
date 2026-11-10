package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeService;
import com.smartcommunity.entity.LifeServiceOrder;
import com.smartcommunity.service.LifeServiceOrderService;
import com.smartcommunity.service.LifeServiceSrv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Tag(name = "生活服务-服务预约")
@RestController
@RequestMapping("/api/v1/service/order")
@RequiredArgsConstructor
public class LifeServiceOrderController {

    private final LifeServiceOrderService serviceOrderService;
    private final LifeServiceSrv lifeServiceSrv;

    @GetMapping("/list")
    public Result<Page<LifeServiceOrder>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<LifeServiceOrder> qw = new LambdaQueryWrapper<>();
        if (serviceId != null) qw.eq(LifeServiceOrder::getServiceId, serviceId);
        if (status != null) qw.eq(LifeServiceOrder::getStatus, status);
        qw.orderByDesc(LifeServiceOrder::getId);
        Page<LifeServiceOrder> result = serviceOrderService.page(new Page<>(page, pageSize), qw);
        for (LifeServiceOrder o : result.getRecords()) {
            fillCompat(o);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<LifeServiceOrder> getById(@PathVariable Long id) {
        LifeServiceOrder order = serviceOrderService.getById(id);
        if (order != null) fillCompat(order);
        return Result.success(order);
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeServiceOrder entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getOrderNo() == null || entity.getOrderNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
            entity.setOrderNo("SO" + ts + rnd);
        }
        // 兼容字段
        if ((entity.getContactName() == null || entity.getContactName().isBlank()) && entity.getUserName() != null) {
            entity.setContactName(entity.getUserName());
        }
        if ((entity.getContactPhone() == null || entity.getContactPhone().isBlank()) && entity.getPhone() != null) {
            entity.setContactPhone(entity.getPhone());
        }
        if (entity.getAmount() == null && entity.getPrice() != null) {
            entity.setAmount(entity.getPrice());
        }
        if (entity.getAppointmentDate() == null) {
            entity.setAppointmentDate(LocalDate.now().plusDays(1));
        }
        if ((entity.getServiceName() == null || entity.getServiceName().isBlank()) && entity.getServiceId() != null) {
            LifeService service = lifeServiceSrv.getById(entity.getServiceId());
            if (service != null) {
                entity.setServiceName(service.getServiceName());
                if (entity.getAmount() == null) {
                    entity.setAmount(service.getPrice());
                }
            }
        }
        if (entity.getAmount() == null) {
            entity.setAmount(BigDecimal.ZERO);
        }
        if (entity.getContactName() == null || entity.getContactName().isBlank()) {
            entity.setContactName(entity.getUserId() != null ? ("用户" + entity.getUserId()) : "访客");
        }
        serviceOrderService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeServiceOrder entity) {
        if (entity.getPhone() != null) entity.setContactPhone(entity.getPhone());
        if (entity.getUserName() != null) entity.setContactName(entity.getUserName());
        if (entity.getPrice() != null) entity.setAmount(entity.getPrice());
        serviceOrderService.updateById(entity);
        return Result.success();
    }

    @Operation(summary = "接单")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        LifeServiceOrder order = serviceOrderService.getById(id);
        if (order == null) throw new BusinessException(404, "服务预约不存在");
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException("仅待接单可接单");
        }
        order.setStatus(1);
        serviceOrderService.updateById(order);
        return Result.success();
    }

    @Operation(summary = "完成服务")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        LifeServiceOrder order = serviceOrderService.getById(id);
        if (order == null) throw new BusinessException(404, "服务预约不存在");
        if (order.getStatus() == null || (order.getStatus() != 0 && order.getStatus() != 1 && order.getStatus() != 2)) {
            throw new BusinessException("当前状态不可完成");
        }
        order.setStatus(3);
        serviceOrderService.updateById(order);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        LifeServiceOrder order = serviceOrderService.getById(id);
        if (order == null) throw new BusinessException(404, "服务预约不存在");
        if (order.getStatus() != null && order.getStatus() >= 3) {
            throw new BusinessException("已完成/已取消订单不可取消");
        }
        order.setStatus(4);
        serviceOrderService.updateById(order);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serviceOrderService.removeById(id);
        return Result.success();
    }

    private void fillCompat(LifeServiceOrder o) {
        o.setUserName(o.getContactName());
        o.setPhone(o.getContactPhone());
        o.setPrice(o.getAmount());
    }
}
