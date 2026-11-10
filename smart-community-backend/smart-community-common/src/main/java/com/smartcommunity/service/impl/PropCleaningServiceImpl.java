package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.PropCleaning;
import com.smartcommunity.mapper.PropCleaningMapper;
import com.smartcommunity.service.PropCleaningService;
import org.springframework.stereotype.Service;

@Service
public class PropCleaningServiceImpl extends ServiceImpl<PropCleaningMapper, PropCleaning> implements PropCleaningService {
}