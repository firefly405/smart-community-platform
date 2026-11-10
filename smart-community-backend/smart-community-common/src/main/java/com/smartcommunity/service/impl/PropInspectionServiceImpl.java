package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.PropInspection;
import com.smartcommunity.mapper.PropInspectionMapper;
import com.smartcommunity.service.PropInspectionService;
import org.springframework.stereotype.Service;

@Service
public class PropInspectionServiceImpl extends ServiceImpl<PropInspectionMapper, PropInspection> implements PropInspectionService {
}