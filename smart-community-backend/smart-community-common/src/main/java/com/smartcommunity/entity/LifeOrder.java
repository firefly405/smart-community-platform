package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_order")
public class LifeOrder extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Integer orderType;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payableAmount;
    private Integer status;
    private LocalDateTime payTime;
    private Integer payMethod;
    private String payTradeNo;
    private Integer deliveryType;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime cancelTime;
    private String cancelReason;

    /** 展示兼容字段 */
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String phone;
    @TableField(exist = false)
    private String address;
    /** 下单商品明细 JSON 或展示摘要 */
    @TableField(exist = false)
    private String items;
    @TableField(exist = false)
    private List<LifeOrderItem> itemList;
}
