package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String md5;
    private String module;
    private Long businessId;
}