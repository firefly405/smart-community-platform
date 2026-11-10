package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.PropNotice;

public interface PropNoticeService extends IService<PropNotice> {
    void publish(Long id);
    void revoke(Long id);
}