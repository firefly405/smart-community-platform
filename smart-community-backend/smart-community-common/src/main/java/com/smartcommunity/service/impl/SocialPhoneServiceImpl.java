package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialPhone;
import com.smartcommunity.mapper.SocialPhoneMapper;
import com.smartcommunity.service.SocialPhoneService;
import org.springframework.stereotype.Service;

@Service
public class SocialPhoneServiceImpl extends ServiceImpl<SocialPhoneMapper, SocialPhone> implements SocialPhoneService {
}