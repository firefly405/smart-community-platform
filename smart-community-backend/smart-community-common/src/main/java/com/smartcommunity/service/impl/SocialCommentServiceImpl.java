package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SocialComment;
import com.smartcommunity.mapper.SocialCommentMapper;
import com.smartcommunity.service.SocialCommentService;
import org.springframework.stereotype.Service;

@Service
public class SocialCommentServiceImpl extends ServiceImpl<SocialCommentMapper, SocialComment> implements SocialCommentService {
}