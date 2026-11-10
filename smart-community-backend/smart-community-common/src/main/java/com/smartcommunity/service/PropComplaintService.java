package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.PropComplaint;

public interface PropComplaintService extends IService<PropComplaint> {
    /** 受理：待处理(0) -> 处理中(1) */
    void accept(Long id, Long handlerId, String handlerName);
    /** 办结：处理中(1) -> 已完成(2) */
    void handle(Long id, Long handlerId, String handlerName, String result, String reply);
    /** 关闭：已完成(2) -> 已关闭(3) */
    void close(Long id);
}
