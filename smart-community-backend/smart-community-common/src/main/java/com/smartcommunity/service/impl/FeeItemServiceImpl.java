package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.FeeItem;
import com.smartcommunity.mapper.FeeItemMapper;
import com.smartcommunity.service.FeeItemService;
import org.springframework.stereotype.Service;

@Service
public class FeeItemServiceImpl extends ServiceImpl<FeeItemMapper, FeeItem> implements FeeItemService {
}