package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.*;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant-portal")
@RequiredArgsConstructor
public class TenantPortalController {
    private final SysUserService users;
    private final BaseTenantService tenants;
    private final BaseHouseService houses;
    private final FeeBillService bills;
    private final PropRepairService repairs;
    private final PropComplaintService complaints;
    private final PropNoticeService notices;
    private final LifeActivityService activities;
    private final SocialPhoneService phones;
    private final BizDisplayService display;

    private BaseTenant tenant(boolean required) {
        Long userId = UserContext.getUserId();
        SysUser user = userId == null ? null : users.getById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !Integer.valueOf(3).equals(user.getUserType())) {
            throw new BusinessException(403, "仅启用的租户账号可访问租户服务");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            if (!required) return null;
            throw new BusinessException(403, "账号尚未关联租户手机号，请联系物业核实");
        }

        LocalDate today = LocalDate.now();
        List<BaseTenant> matches = tenants.list(new LambdaQueryWrapper<BaseTenant>()
                .eq(BaseTenant::getPhone, user.getPhone())
                .eq(BaseTenant::getStatus, 1)
                .le(BaseTenant::getLeaseStartDate, today)
                .ge(BaseTenant::getLeaseEndDate, today)
                .orderByDesc(BaseTenant::getLeaseStartDate));
        if (matches.isEmpty()) {
            if (!required) return null;
            throw new BusinessException(403, "租户档案不存在、未启用或租期已结束");
        }
        if (matches.size() > 1) {
            throw new BusinessException(409, "账号关联了多个有效租约，请联系物业处理");
        }
        BaseTenant tenant = matches.get(0);
        if (tenant.getHouseId() == null) {
            throw new BusinessException(400, "租户档案尚未关联房屋，请联系物业处理");
        }
        return tenant;
    }

    private BaseHouse house(BaseTenant tenant) {
        BaseHouse house = houses.getById(tenant.getHouseId());
        if (house == null) {
            throw new BusinessException(400, "租户关联房屋不存在，请联系物业处理");
        }
        display.fillHouses(List.of(house));
        return house;
    }

    private void page(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50) {
            throw new BusinessException(400, "分页参数无效");
        }
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        BaseTenant tenant = tenant(false);
        if (tenant == null) {
            return Result.success(Map.of("bound", false, "unpaid", 0, "repairs", 0, "complaints", 0));
        }
        BaseHouse house = house(tenant);
        Long userId = UserContext.getUserId();
        Long houseId = tenant.getHouseId();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bound", true);
        result.put("tenantName", tenant.getRealName());
        result.put("houseId", houseId);
        result.put("houseNo", house.getHouseNo());
        result.put("leaseEndDate", tenant.getLeaseEndDate());
        result.put("daysRemaining", ChronoUnit.DAYS.between(LocalDate.now(), tenant.getLeaseEndDate()));
        result.put("unpaid", bills.count(new LambdaQueryWrapper<FeeBill>()
                .eq(FeeBill::getHouseId, houseId)
                .ge(FeeBill::getBillEndDate, tenant.getLeaseStartDate().atStartOfDay())
                .le(FeeBill::getBillStartDate, tenant.getLeaseEndDate().atTime(LocalTime.MAX))
                .in(FeeBill::getStatus, 0, 1)));
        result.put("repairs", repairs.count(new LambdaQueryWrapper<PropRepair>()
                .eq(PropRepair::getHouseId, houseId).eq(PropRepair::getCreateBy, userId)
                .in(PropRepair::getStatus, 0, 1, 2)));
        result.put("complaints", complaints.count(new LambdaQueryWrapper<PropComplaint>()
                .eq(PropComplaint::getHouseId, houseId).eq(PropComplaint::getCreateBy, userId)
                .in(PropComplaint::getStatus, 0, 1)));
        return Result.success(result);
    }

    @GetMapping("/lease")
    public Result<Map<String, Object>> lease() {
        BaseTenant tenant = tenant(true);
        display.fill(List.of(tenant), BaseTenant::getOwnerId, BaseTenant::getHouseId,
                BaseTenant::setOwnerName, BaseTenant::setHouseNo);
        return Result.success(Map.of("tenant", tenant, "house", house(tenant)));
    }

    @GetMapping("/house")
    public Result<BaseHouse> house() {
        return Result.success(house(tenant(true)));
    }

    @GetMapping("/{section}")
    public Result<?> list(@PathVariable String section,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize) {
        page(page, pageSize);
        BaseTenant tenant = tenant(true);
        Long houseId = tenant.getHouseId();
        Long userId = UserContext.getUserId();

        switch (section) {
            case "bills": {
                Page<FeeBill> result = bills.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getHouseId, houseId)
                                .ge(FeeBill::getBillEndDate, tenant.getLeaseStartDate().atStartOfDay())
                                .le(FeeBill::getBillStartDate, tenant.getLeaseEndDate().atTime(LocalTime.MAX))
                                .orderByDesc(FeeBill::getId));
                display.fill(result.getRecords(), FeeBill::getOwnerId, FeeBill::getHouseId,
                        FeeBill::setOwnerName, FeeBill::setHouseNo);
                return Result.success(result);
            }
            case "repairs": {
                Page<PropRepair> result = repairs.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<PropRepair>().eq(PropRepair::getHouseId, houseId)
                                .eq(PropRepair::getCreateBy, userId).orderByDesc(PropRepair::getId));
                display.fill(result.getRecords(), PropRepair::getOwnerId, PropRepair::getHouseId,
                        PropRepair::setOwnerName, PropRepair::setHouseNo);
                return Result.success(result);
            }
            case "complaints": {
                Page<PropComplaint> result = complaints.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<PropComplaint>().eq(PropComplaint::getHouseId, houseId)
                                .eq(PropComplaint::getCreateBy, userId).orderByDesc(PropComplaint::getId));
                display.fill(result.getRecords(), PropComplaint::getOwnerId, PropComplaint::getHouseId,
                        PropComplaint::setOwnerName, PropComplaint::setHouseNo);
                return Result.success(result);
            }
            case "notices":
                return Result.success(notices.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<PropNotice>().eq(PropNotice::getIsPublish, 1)
                                .eq(PropNotice::getStatus, 1).le(PropNotice::getPublishTime, LocalDateTime.now())
                                .orderByDesc(PropNotice::getIsTop, PropNotice::getPublishTime)));
            case "activities":
                return Result.success(activities.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<LifeActivity>().in(LifeActivity::getStatus, 1, 2, 3)
                                .orderByDesc(LifeActivity::getStartTime)));
            case "phones":
                return Result.success(phones.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<SocialPhone>().eq(SocialPhone::getStatus, 1)
                                .orderByAsc(SocialPhone::getSort)));
            default:
                throw new BusinessException(404, "租户服务不存在");
        }
    }

    public record ServiceRequest(Long houseId, String title, String content, Integer type, Integer urgency,
                                 String contactName, String contactPhone) {}

    private BaseTenant validate(ServiceRequest input) {
        BaseTenant tenant = tenant(true);
        if (input == null || !Objects.equals(tenant.getHouseId(), input.houseId())) {
            throw new BusinessException(403, "只能为本人当前租住房屋提交服务");
        }
        if (tenant.getOwnerId() == null) {
            throw new BusinessException(400, "租户档案尚未关联业主，请联系物业处理");
        }
        if (input.title() == null || input.title().isBlank() || input.title().length() > 100
                || input.content() == null || input.content().isBlank() || input.content().length() > 2000
                || input.type() == null
                || input.contactName() == null || input.contactName().isBlank() || input.contactName().length() > 50
                || input.contactPhone() == null || !input.contactPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请完整填写标题、描述、类型和有效联系方式");
        }
        return tenant;
    }

    @PostMapping("/repairs")
    public Result<Long> repair(@RequestBody ServiceRequest input) {
        BaseTenant tenant = validate(input);
        if (input.type() < 1 || input.type() > 6) {
            throw new BusinessException(400, "请选择报修类型");
        }
        if (input.urgency() == null || input.urgency() < 1 || input.urgency() > 3) {
            throw new BusinessException(400, "请选择紧急程度");
        }
        PropRepair entity = new PropRepair();
        entity.setRepairNo("RP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        entity.setOwnerId(tenant.getOwnerId());
        entity.setHouseId(tenant.getHouseId());
        entity.setCreateBy(UserContext.getUserId());
        entity.setStatus(0);
        entity.setTitle(input.title().trim());
        entity.setContent(input.content().trim());
        entity.setRepairType(input.type());
        entity.setUrgency(input.urgency());
        entity.setContactName(input.contactName().trim());
        entity.setContactPhone(input.contactPhone());
        repairs.save(entity);
        return Result.success(entity.getId());
    }

    @PostMapping("/complaints")
    public Result<Long> complaint(@RequestBody ServiceRequest input) {
        BaseTenant tenant = validate(input);
        if (input.type() < 1 || input.type() > 4) {
            throw new BusinessException(400, "请选择投诉类型");
        }
        PropComplaint entity = new PropComplaint();
        entity.setComplaintNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        entity.setOwnerId(tenant.getOwnerId());
        entity.setHouseId(tenant.getHouseId());
        entity.setCreateBy(UserContext.getUserId());
        entity.setStatus(0);
        entity.setTitle(input.title().trim());
        entity.setContent(input.content().trim());
        entity.setComplaintType(input.type());
        entity.setContactName(input.contactName().trim());
        entity.setContactPhone(input.contactPhone());
        complaints.save(entity);
        return Result.success(entity.getId());
    }

    @PostMapping("/repairs/{id}/evaluate")
    public Result<Void> evaluate(@PathVariable Long id, @RequestBody Evaluation input) {
        BaseTenant tenant = tenant(true);
        PropRepair repair = repairs.getById(id);
        if (repair == null || !Objects.equals(tenant.getHouseId(), repair.getHouseId())
                || !Objects.equals(UserContext.getUserId(), repair.getCreateBy())) {
            throw new BusinessException(404, "报修单不存在");
        }
        if (!Integer.valueOf(3).equals(repair.getStatus()) || repair.getEvaluateTime() != null) {
            throw new BusinessException(400, "仅已完成且未评价的报修可评价");
        }
        if (input == null || input.score() == null || input.score() < 1 || input.score() > 5
                || input.content() != null && input.content().length() > 500) {
            throw new BusinessException(400, "评价参数无效");
        }
        repairs.evaluate(id, input.score(), input.content());
        return Result.success();
    }

    public record Evaluation(Integer score, String content) {}
}
