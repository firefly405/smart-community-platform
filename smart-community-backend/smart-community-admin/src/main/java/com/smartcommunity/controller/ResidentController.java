package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.*;
import com.smartcommunity.mapper.BaseOwnerHouseMapper;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/resident")
@RequiredArgsConstructor
public class ResidentController {
    private final SysUserService users;
    private final BaseOwnerService owners;
    private final BaseOwnerHouseMapper ownerHouses;
    private final BaseHouseService houses;
    private final BaseVehicleService vehicles;
    private final FeeBillService bills;
    private final PropRepairService repairs;
    private final PropComplaintService complaints;
    private final PropNoticeService notices;
    private final LifeActivityService activities;
    private final SocialPhoneService phones;
    private final BizDisplayService display;

    private BaseOwner owner(boolean required) {
        SysUser user = users.getById(UserContext.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !Integer.valueOf(1).equals(user.getUserType())) {
            throw new BusinessException(403, "仅普通业主可访问智慧社区");
        }
        BaseOwner owner = owners.getOne(new LambdaQueryWrapper<BaseOwner>()
                .eq(BaseOwner::getUserId, user.getId()).eq(BaseOwner::getStatus, 1));
        if (required && owner == null) throw new BusinessException(400, "账号尚未关联业主档案，请联系物业核实");
        return owner;
    }

    List<Long> houseIds(Long ownerId) {
        LocalDate today = LocalDate.now();
        return ownerHouses.selectList(new LambdaQueryWrapper<BaseOwnerHouse>()
                .eq(BaseOwnerHouse::getOwnerId, ownerId).eq(BaseOwnerHouse::getRelationType, 1)
                .and(q -> q.isNull(BaseOwnerHouse::getStartDate).or().le(BaseOwnerHouse::getStartDate, today))
                .and(q -> q.isNull(BaseOwnerHouse::getEndDate).or().ge(BaseOwnerHouse::getEndDate, today)))
                .stream().map(BaseOwnerHouse::getHouseId).filter(Objects::nonNull).distinct().toList();
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        BaseOwner owner = owner(false);
        if (owner == null) return Result.success(Map.of("bound", false, "houses", 0, "unpaid", 0, "repairs", 0));
        return Result.success(Map.of("bound", true, "houses", houseIds(owner.getId()).size(),
                "unpaid", bills.count(new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getOwnerId, owner.getId()).in(FeeBill::getStatus, 0, 1)),
                "repairs", repairs.count(new LambdaQueryWrapper<PropRepair>().eq(PropRepair::getOwnerId, owner.getId()).in(PropRepair::getStatus, 0, 1, 2))));
    }

    @GetMapping("/{section}")
    public Result<?> list(@PathVariable String section, @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50) throw new BusinessException(400, "分页参数无效");
        owner(false);
        if (section.equals("notices")) return Result.success(notices.page(new Page<>(page, pageSize),
                new LambdaQueryWrapper<PropNotice>().eq(PropNotice::getIsPublish, 1).eq(PropNotice::getStatus, 1)
                        .le(PropNotice::getPublishTime, java.time.LocalDateTime.now()).orderByDesc(PropNotice::getIsTop, PropNotice::getPublishTime)));
        if (section.equals("activities")) return Result.success(activities.page(new Page<>(page, pageSize),
                new LambdaQueryWrapper<LifeActivity>().in(LifeActivity::getStatus, 1, 2, 3).orderByDesc(LifeActivity::getStartTime)));
        if (section.equals("phones")) return Result.success(phones.page(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SocialPhone>().eq(SocialPhone::getStatus, 1).orderByAsc(SocialPhone::getSort)));
        BaseOwner owner = owner(true);
        Long id = owner.getId();
        switch (section) {
            case "houses": {
                List<Long> ids = houseIds(id);
                Page<BaseHouse> result = ids.isEmpty() ? new Page<>(page, pageSize) : houses.page(new Page<>(page, pageSize),
                        new LambdaQueryWrapper<BaseHouse>().in(BaseHouse::getId, ids).orderByAsc(BaseHouse::getId));
                display.fillHouses(result.getRecords());
                return Result.success(result);
            }
            case "bills": {
                Page<FeeBill> result = bills.page(new Page<>(page, pageSize), new LambdaQueryWrapper<FeeBill>().eq(FeeBill::getOwnerId, id).orderByDesc(FeeBill::getId));
                display.fill(result.getRecords(), FeeBill::getOwnerId, FeeBill::getHouseId, FeeBill::setOwnerName, FeeBill::setHouseNo);
                return Result.success(result);
            }
            case "vehicles": return Result.success(vehicles.page(new Page<>(page, pageSize), new LambdaQueryWrapper<BaseVehicle>().eq(BaseVehicle::getOwnerId, id).orderByDesc(BaseVehicle::getId)));
            case "repairs": {
                Page<PropRepair> result = repairs.page(new Page<>(page, pageSize), new LambdaQueryWrapper<PropRepair>().eq(PropRepair::getOwnerId, id).orderByDesc(PropRepair::getId));
                display.fill(result.getRecords(), PropRepair::getOwnerId, PropRepair::getHouseId, PropRepair::setOwnerName, PropRepair::setHouseNo);
                return Result.success(result);
            }
            case "complaints": {
                Page<PropComplaint> result = complaints.page(new Page<>(page, pageSize), new LambdaQueryWrapper<PropComplaint>().eq(PropComplaint::getOwnerId, id).orderByDesc(PropComplaint::getId));
                display.fill(result.getRecords(), PropComplaint::getOwnerId, PropComplaint::getHouseId, PropComplaint::setOwnerName, PropComplaint::setHouseNo);
                return Result.success(result);
            }
            default: throw new BusinessException(404, "居民服务不存在");
        }
    }

    public record ServiceRequest(Long houseId, String title, String content, Integer type, Integer urgency,
                                 String contactName, String contactPhone) {}

    private BaseOwner validate(ServiceRequest input) {
        BaseOwner owner = owner(true);
        if (input.houseId() == null || !houseIds(owner.getId()).contains(input.houseId())) throw new BusinessException(403, "只能为本人关联房屋提交服务");
        if (input.title() == null || input.title().isBlank() || input.title().length() > 100
                || input.content() == null || input.content().isBlank() || input.content().length() > 2000
                || input.type() == null || input.type() < 1 || input.type() > 5
                || input.contactName() == null || input.contactName().isBlank() || input.contactName().length() > 50
                || input.contactPhone() == null || !input.contactPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请完整填写标题、描述、类型和有效联系方式");
        }
        return owner;
    }

    @PostMapping("/repairs")
    public Result<Long> repair(@RequestBody ServiceRequest input) {
        BaseOwner owner = validate(input);
        if (input.urgency() == null || input.urgency() < 1 || input.urgency() > 3) throw new BusinessException(400, "请选择紧急程度");
        PropRepair entity = new PropRepair();
        entity.setRepairNo("RP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        entity.setOwnerId(owner.getId()); entity.setHouseId(input.houseId()); entity.setStatus(0);
        entity.setTitle(input.title().trim()); entity.setContent(input.content().trim());
        entity.setRepairType(input.type()); entity.setUrgency(input.urgency());
        entity.setContactName(input.contactName().trim()); entity.setContactPhone(input.contactPhone());
        repairs.save(entity);
        return Result.success(entity.getId());
    }

    @PostMapping("/complaints")
    public Result<Long> complaint(@RequestBody ServiceRequest input) {
        BaseOwner owner = validate(input);
        PropComplaint entity = new PropComplaint();
        entity.setComplaintNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        entity.setOwnerId(owner.getId()); entity.setHouseId(input.houseId()); entity.setStatus(0);
        entity.setTitle(input.title().trim()); entity.setContent(input.content().trim()); entity.setComplaintType(input.type());
        entity.setContactName(input.contactName().trim()); entity.setContactPhone(input.contactPhone());
        complaints.save(entity);
        return Result.success(entity.getId());
    }

    @PostMapping("/repairs/{id}/evaluate")
    public Result<Void> evaluate(@PathVariable Long id, @RequestBody Evaluation input) {
        BaseOwner owner = owner(true);
        PropRepair repair = repairs.getById(id);
        if (repair == null || !owner.getId().equals(repair.getOwnerId())) throw new BusinessException(404, "报修单不存在");
        if (!Integer.valueOf(3).equals(repair.getStatus()) || repair.getEvaluateTime() != null) throw new BusinessException(400, "仅已完成且未评价的报修可评价");
        if (input.score() == null || input.score() < 1 || input.score() > 5 || input.content() != null && input.content().length() > 500) throw new BusinessException(400, "评价参数无效");
        repairs.evaluate(id, input.score(), input.content());
        return Result.success();
    }
    public record Evaluation(Integer score, String content) {}
}
