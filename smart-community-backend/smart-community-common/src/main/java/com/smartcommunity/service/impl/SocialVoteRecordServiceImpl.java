package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialVoteRecord;
import com.smartcommunity.mapper.SocialVoteRecordMapper;
import com.smartcommunity.service.SocialVoteRecordService;
import org.springframework.stereotype.Service;

@Service
public class SocialVoteRecordServiceImpl extends ServiceImpl<SocialVoteRecordMapper, SocialVoteRecord> implements SocialVoteRecordService {
}