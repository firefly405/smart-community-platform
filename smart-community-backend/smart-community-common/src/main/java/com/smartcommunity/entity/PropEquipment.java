package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_equipment")
public class PropEquipment extends BaseEntity {

    private String equipmentNo;
    private String equipmentName;
    private Integer equipmentType;
    private String brand;
    private String model;
    private String location;
    private LocalDate installDate;
    private LocalDate warrantyDate;
    private Integer lifeYears;
    private String supplier;
    private String supplierPhone;
    private Integer status;
}