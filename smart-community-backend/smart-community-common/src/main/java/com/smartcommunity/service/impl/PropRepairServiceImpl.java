package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.PropRepair;
import com.smartcommunity.mapper.PropRepairMapper;
import com.smartcommunity.service.PropRepairService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PropRepairServiceImpl extends ServiceImpl<PropRepairMapper, PropRepair> implements PropRepairService {

    @Override
    @Transactional
    public void dispatch(Long id, Long handlerId, String handlerName) {
        PropRepair repair = getById(id);
        if (repair == null) throw new BusinessException(404, "报修单不存在");
        if (repair.getStatus() != 0) throw new BusinessException(400, "仅待处理状态可派单");
        repair.setStatus(1);
        repair.setHandlerId(handlerId);
        repair.setHandlerName(handlerName);
        updateById(repair);
    }

    @Override
    @Transactional
    public void process(Long id, String result, String images) {
        PropRepair repair = getById(id);
        if (repair == null) throw new BusinessException(404, "报修单不存在");
        if (repair.getStatus() != 1) throw new BusinessException(400, "仅已派单状态可处理");
        repair.setStatus(2);
        repair.setHandleResult(result);
        repair.setHandleImages(images);
        repair.setHandleTime(LocalDateTime.now());
        updateById(repair);
    }

    @Override
    @Transactional
    public void complete(Long id) {
        PropRepair repair = getById(id);
        if (repair == null) throw new BusinessException(404, "报修单不存在");
        if (repair.getStatus() != 2) throw new BusinessException(400, "仅处理中状态可完成");
        repair.setStatus(3);
        repair.setFinishTime(LocalDateTime.now());
        updateById(repair);
    }

    @Override
    @Transactional
    public void evaluate(Long id, Integer score, String content) {
        PropRepair repair = getById(id);
        if (repair == null) throw new BusinessException(404, "报修单不存在");
        if (repair.getStatus() != 3) throw new BusinessException(400, "仅已完成状态可评价");
        repair.setEvaluateScore(score);
        repair.setEvaluateContent(content);
        repair.setEvaluateTime(LocalDateTime.now());
        updateById(repair);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        PropRepair repair = getById(id);
        if (repair == null) throw new BusinessException(404, "报修单不存在");
        repair.setStatus(4);
        updateById(repair);
    }
}