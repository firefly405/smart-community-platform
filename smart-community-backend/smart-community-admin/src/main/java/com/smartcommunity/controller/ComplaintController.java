package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropComplaint;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.PropComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Tag(name = "投诉管理")
@RestController
@RequestMapping("/api/v1/complaint")
@RequiredArgsConstructor
public class ComplaintController {

    private final PropComplaintService complaintService;
    private final BizDisplayService bizDisplayService;

    @GetMapping("/list")
    public Result<Page<PropComplaint>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PropComplaint> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) qw.eq(PropComplaint::getOwnerId, ownerId);
        if (status != null) qw.eq(PropComplaint::getStatus, status);
        qw.orderByDesc(PropComplaint::getId);
        Page<PropComplaint> result = complaintService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                PropComplaint::getOwnerId, PropComplaint::getHouseId,
                PropComplaint::setOwnerName, PropComplaint::setHouseNo);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<PropComplaint> getById(@PathVariable Long id) {
        return Result.success(complaintService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PropComplaint complaint) {
        if (complaint.getComplaintNo() == null || complaint.getComplaintNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
            complaint.setComplaintNo("CP" + ts + rnd);
        }
        if (complaint.getStatus() == null) {
            complaint.setStatus(0);
        }
        complaintService.save(complaint);
        return Result.success();
    }

    @Operation(summary = "受理投诉")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long handlerId = null;
        String handlerName = null;
        if (body != null) {
            if (body.get("handlerId") != null) handlerId = ((Number) body.get("handlerId")).longValue();
            handlerName = (String) body.get("handlerName");
        }
        complaintService.accept(id, handlerId, handlerName);
        return Result.success();
    }

    @Operation(summary = "办结投诉")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long handlerId = body.get("handlerId") != null ? ((Number) body.get("handlerId")).longValue() : null;
        String handlerName = (String) body.get("handlerName");
        String result = (String) body.get("result");
        String reply = (String) body.get("reply");
        complaintService.handle(id, handlerId, handlerName, result, reply);
        return Result.success();
    }

    @Operation(summary = "关闭投诉")
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        complaintService.close(id);
        return Result.success();
    }
}
