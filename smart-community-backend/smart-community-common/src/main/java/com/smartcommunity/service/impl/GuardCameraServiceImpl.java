package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardCamera;
import com.smartcommunity.mapper.GuardCameraMapper;
import com.smartcommunity.service.GuardCameraService;
import org.springframework.stereotype.Service;

@Service
public class GuardCameraServiceImpl extends ServiceImpl<GuardCameraMapper, GuardCamera> implements GuardCameraService {
}