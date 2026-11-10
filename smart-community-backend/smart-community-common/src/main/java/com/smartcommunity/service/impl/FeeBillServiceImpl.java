package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.mapper.FeeBillMapper;
import com.smartcommunity.service.FeeBillService;
import org.springframework.stereotype.Service;

@Service
public class FeeBillServiceImpl extends ServiceImpl<FeeBillMapper, FeeBill> implements FeeBillService {
}