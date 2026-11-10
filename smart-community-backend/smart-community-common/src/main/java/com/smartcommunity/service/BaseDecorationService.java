package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseDecoration;

public interface BaseDecorationService extends IService<BaseDecoration> {
    void approve(Long id, Integer status, String checkResult);
}