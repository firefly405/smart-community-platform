package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.SysMenu;

public interface SysMenuService extends IService<SysMenu> {

    java.util.List<SysMenu> getTree();
}