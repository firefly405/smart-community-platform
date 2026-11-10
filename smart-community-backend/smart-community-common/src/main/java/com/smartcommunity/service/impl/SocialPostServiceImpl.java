package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialPost;
import com.smartcommunity.mapper.SocialPostMapper;
import com.smartcommunity.service.SocialPostService;
import org.springframework.stereotype.Service;

@Service
public class SocialPostServiceImpl extends ServiceImpl<SocialPostMapper, SocialPost> implements SocialPostService {
}