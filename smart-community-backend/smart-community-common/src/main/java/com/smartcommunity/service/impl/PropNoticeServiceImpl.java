package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.PropNotice;
import com.smartcommunity.mapper.PropNoticeMapper;
import com.smartcommunity.service.PropNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PropNoticeServiceImpl extends ServiceImpl<PropNoticeMapper, PropNotice> implements PropNoticeService {

    @Override
    @Transactional
    public void publish(Long id) {
        PropNotice notice = getById(id);
        if (notice == null) throw new BusinessException(404, "公告不存在");
        notice.setIsPublish(1);
        notice.setPublishTime(LocalDateTime.now());
        updateById(notice);
    }

    @Override
    @Transactional
    public void revoke(Long id) {
        PropNotice notice = getById(id);
        if (notice == null) throw new BusinessException(404, "公告不存在");
        notice.setIsPublish(0);
        updateById(notice);
    }
}