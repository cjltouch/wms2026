package com.example.wms.business.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wms_sale_status_log")
public class WmsSaleStatusLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志主键ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long logId;

    /** 单据ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billId;

    /** 单据号 */
    private String billNo;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operateBy;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;
}
