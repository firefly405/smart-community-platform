package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("base_owner_house")
public class BaseOwnerHouse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long ownerId;
    private Long houseId;
    private Integer relationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createTime;
}