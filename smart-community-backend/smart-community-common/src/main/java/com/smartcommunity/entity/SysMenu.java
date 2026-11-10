package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String icon;
    private String path;
    private String component;
    private String permission;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;

    /** 树形子节点（非表字段） */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
