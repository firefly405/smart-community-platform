package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialVoteOption;
import com.smartcommunity.mapper.SocialVoteOptionMapper;
import com.smartcommunity.service.SocialVoteOptionService;
import org.springframework.stereotype.Service;

@Service
public class SocialVoteOptionServiceImpl extends ServiceImpl<SocialVoteOptionMapper, SocialVoteOption> implements SocialVoteOptionService {
}
