package com.smartcommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcommunity.entity.BaseTenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseTenantMapper extends BaseMapper<BaseTenant> {
}