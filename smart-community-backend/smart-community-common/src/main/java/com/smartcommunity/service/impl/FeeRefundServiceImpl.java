package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.FeeRefund;
import com.smartcommunity.mapper.FeeRefundMapper;
import com.smartcommunity.service.FeeRefundService;
import org.springframework.stereotype.Service;

@Service
public class FeeRefundServiceImpl extends ServiceImpl<FeeRefundMapper, FeeRefund> implements FeeRefundService {
}