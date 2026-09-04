package com.example.wms.business.loss.entity;

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

@Data
@TableName("wms_loss_status_log")
public class WmsLossStatusLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志主键ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long logId;

    /** 单据ID */
    @TableField("loss_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billId;

    /** 单据号 */
    @TableField("loss_no")
    private String billNo;

    /** 变更前状态 */
    @TableField("before_status")
    private Integer fromStatus;

    /** 变更后状态 */
    @TableField("after_status")
    private Integer toStatus;

    /** 操作类型 */
    @TableField("operate_type")
    private String operateType;

    /** 操作人ID */
    @TableField("operate_by")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operateBy;

    /** 操作人姓名 */
    @TableField("operate_name")
    private String operateName;

    /** 操作备注 */
    @TableField("operate_remark")
    private String remark;

    /** 操作时间 */
    @TableField("operate_time")
    private LocalDateTime operateTime;
}
