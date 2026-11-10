package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialVote;
import com.smartcommunity.mapper.SocialVoteMapper;
import com.smartcommunity.service.SocialVoteService;
import org.springframework.stereotype.Service;

@Service
public class SocialVoteServiceImpl extends ServiceImpl<SocialVoteMapper, SocialVote> implements SocialVoteService {
}