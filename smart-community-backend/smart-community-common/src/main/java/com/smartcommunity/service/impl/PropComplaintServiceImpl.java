package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.PropComplaint;
import com.smartcommunity.mapper.PropComplaintMapper;
import com.smartcommunity.service.PropComplaintService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PropComplaintServiceImpl extends ServiceImpl<PropComplaintMapper, PropComplaint> implements PropComplaintService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id, Long handlerId, String handlerName) {
        PropComplaint complaint = getById(id);
        if (complaint == null) throw new BusinessException(404, "投诉单不存在");
        if (complaint.getStatus() == null || complaint.getStatus() != 0) {
            throw new BusinessException("仅待处理投诉可受理");
        }
        complaint.setStatus(1);
        complaint.setHandlerId(handlerId);
        complaint.setHandlerName(handlerName);
        updateById(complaint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long handlerId, String handlerName, String result, String reply) {
        PropComplaint complaint = getById(id);
        if (complaint == null) throw new BusinessException(404, "投诉单不存在");
        Integer status = complaint.getStatus() == null ? 0 : complaint.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException("仅待处理/处理中投诉可办结");
        }
        // 兼容演示：待处理可直接办结，但会先进入处理中再完成
        complaint.setStatus(2);
        if (handlerId != null) complaint.setHandlerId(handlerId);
        if (handlerName != null) complaint.setHandlerName(handlerName);
        complaint.setHandleResult(result);
        complaint.setHandleTime(LocalDateTime.now());
        complaint.setReplyContent(reply);
        complaint.setReplyTime(LocalDateTime.now());
        updateById(complaint);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        PropComplaint complaint = getById(id);
        if (complaint == null) throw new BusinessException(404, "投诉单不存在");
        if (complaint.getStatus() == null || complaint.getStatus() != 2) {
            throw new BusinessException("仅已完成投诉可关闭");
        }
        complaint.setStatus(3);
        updateById(complaint);
    }
}
