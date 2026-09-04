package com.example.wms.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "menu_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    @TableField(exist = false)
    private String query;

    @TableField(exist = false)
    private String routeName;

    @TableField(exist = false)
    private Integer isFrame;

    @TableField(exist = false)
    private Integer isCache;

    private String perms;

    private String icon;

    private Integer orderNum;

    private Integer visible;

    private Integer status;

    private String remark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    private LocalDateTime createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<SysMenu> children;
}
