package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.PropRepair;

public interface PropRepairService extends IService<PropRepair> {
    void dispatch(Long id, Long handlerId, String handlerName);
    void process(Long id, String result, String images);
    void complete(Long id);
    void evaluate(Long id, Integer score, String content);
    void cancel(Long id);
}