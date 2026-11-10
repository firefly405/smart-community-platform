package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.FeeDeposit;
import com.smartcommunity.mapper.FeeDepositMapper;
import com.smartcommunity.service.FeeDepositService;
import org.springframework.stereotype.Service;

@Service
public class FeeDepositServiceImpl extends ServiceImpl<FeeDepositMapper, FeeDeposit> implements FeeDepositService {
}