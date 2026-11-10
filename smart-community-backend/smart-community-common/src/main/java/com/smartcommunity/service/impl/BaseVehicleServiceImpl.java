package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.BaseParking;
import com.smartcommunity.entity.BaseVehicle;
import com.smartcommunity.mapper.BaseVehicleMapper;
import com.smartcommunity.service.BaseParkingService;
import com.smartcommunity.service.BaseOwnerService;
import com.smartcommunity.service.BaseVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BaseVehicleServiceImpl extends ServiceImpl<BaseVehicleMapper, BaseVehicle> implements BaseVehicleService {

    private final BaseParkingService parkingService;
    private final BaseOwnerService ownerService;

    /** 空闲 */
    private static final int PARKING_FREE = 0;
    /** 已占用（车辆绑定） */
    private static final int PARKING_OCCUPIED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWithParking(BaseVehicle vehicle) {
        validateVehicle(vehicle, null);
        Long parkingId = normalizeParkingId(vehicle.getParkingId());
        vehicle.setParkingId(parkingId);
        bindParking(null, parkingId);
        save(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithParking(BaseVehicle vehicle) {
        if (vehicle.getId() == null) {
            throw new BusinessException("车辆ID不能为空");
        }
        BaseVehicle old = getById(vehicle.getId());
        if (old == null) {
            throw new BusinessException(404, "车辆不存在");
        }
        validateVehicle(vehicle, vehicle.getId());
        Long oldParkingId = normalizeParkingId(old.getParkingId());
        Long newParkingId = normalizeParkingId(vehicle.getParkingId());
        vehicle.setParkingId(newParkingId);
        if (!equalsId(oldParkingId, newParkingId)) {
            // 先释放旧车位（排除当前车辆）
            releaseParking(oldParkingId, vehicle.getId());
            bindParking(vehicle.getId(), newParkingId);
        }
        updateById(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithParking(Long id) {
        BaseVehicle old = getById(id);
        if (old == null) {
            return;
        }
        Long parkingId = normalizeParkingId(old.getParkingId());
        removeById(id);
        releaseParking(parkingId, null);
    }

    private void bindParking(Long currentVehicleId, Long parkingId) {
        if (parkingId == null) {
            return;
        }
        BaseParking parking = parkingService.getById(parkingId);
        if (parking == null) {
            throw new BusinessException("车位不存在");
        }
        long bound = count(new LambdaQueryWrapper<BaseVehicle>()
                .eq(BaseVehicle::getParkingId, parkingId)
                .ne(currentVehicleId != null, BaseVehicle::getId, currentVehicleId));
        if (bound > 0) {
            throw new BusinessException("该车位已被其他车辆绑定");
        }
        parking.setStatus(PARKING_OCCUPIED);
        parkingService.updateById(parking);
    }

    private void validateVehicle(BaseVehicle vehicle, Long currentVehicleId) {
        if (vehicle == null) {
            throw new BusinessException(400, "车辆信息不能为空");
        }
        String plateNo = vehicle.getPlateNo() == null ? "" : vehicle.getPlateNo().trim().toUpperCase();
        if (plateNo.isEmpty() || plateNo.length() > 20) {
            throw new BusinessException(400, "请输入正确的车牌号");
        }
        if (vehicle.getOwnerId() == null || vehicle.getOwnerId() <= 0 || ownerService.getById(vehicle.getOwnerId()) == null) {
            throw new BusinessException(400, "请选择有效业主");
        }
        long duplicate = count(new LambdaQueryWrapper<BaseVehicle>()
                .eq(BaseVehicle::getPlateNo, plateNo)
                .ne(currentVehicleId != null, BaseVehicle::getId, currentVehicleId));
        if (duplicate > 0) {
            throw new BusinessException(400, "车牌号已存在，请勿重复添加");
        }
        vehicle.setPlateNo(plateNo);
    }

    private void releaseParking(Long parkingId, Long excludeVehicleId) {
        if (parkingId == null) {
            return;
        }
        long stillBound = count(new LambdaQueryWrapper<BaseVehicle>()
                .eq(BaseVehicle::getParkingId, parkingId)
                .ne(excludeVehicleId != null, BaseVehicle::getId, excludeVehicleId));
        if (stillBound > 0) {
            return;
        }
        BaseParking parking = parkingService.getById(parkingId);
        if (parking != null) {
            parking.setStatus(PARKING_FREE);
            parkingService.updateById(parking);
        }
    }

    private Long normalizeParkingId(Long parkingId) {
        if (parkingId == null || parkingId <= 0) {
            return null;
        }
        return parkingId;
    }

    private boolean equalsId(Long a, Long b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
