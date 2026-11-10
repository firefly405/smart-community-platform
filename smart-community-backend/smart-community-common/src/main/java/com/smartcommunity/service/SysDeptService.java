package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.SysDept;

public interface SysDeptService extends IService<SysDept> {

    java.util.List<SysDept> getTree();
}