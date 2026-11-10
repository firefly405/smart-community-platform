package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.FeePayment;
import com.smartcommunity.mapper.FeePaymentMapper;
import com.smartcommunity.service.FeePaymentService;
import org.springframework.stereotype.Service;

@Service
public class FeePaymentServiceImpl extends ServiceImpl<FeePaymentMapper, FeePayment> implements FeePaymentService {
}