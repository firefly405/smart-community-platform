package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {

    void assignMenus(Long roleId, java.util.List<Long> menuIds);

    java.util.List<Long> getMenuIds(Long roleId);
}