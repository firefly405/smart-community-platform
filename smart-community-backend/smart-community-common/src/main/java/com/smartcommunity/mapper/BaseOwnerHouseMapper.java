package com.smartcommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcommunity.entity.BaseOwnerHouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseOwnerHouseMapper extends BaseMapper<BaseOwnerHouse> {

    int deleteByOwnerId(@Param("ownerId") Long ownerId);

    List<Long> selectHouseIdsByOwnerId(@Param("ownerId") Long ownerId);

    int insertBatch(@Param("list") List<BaseOwnerHouse> list);
}