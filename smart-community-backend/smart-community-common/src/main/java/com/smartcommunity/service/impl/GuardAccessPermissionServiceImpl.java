package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardAccessPermission;
import com.smartcommunity.mapper.GuardAccessPermissionMapper;
import com.smartcommunity.service.GuardAccessPermissionService;
import org.springframework.stereotype.Service;

@Service
public class GuardAccessPermissionServiceImpl extends ServiceImpl<GuardAccessPermissionMapper, GuardAccessPermission>
        implements GuardAccessPermissionService {
}
