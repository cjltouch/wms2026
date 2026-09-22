package com.example.wms.business.office.dto.req;

import com.example.wms.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 用品登记分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OfficeRecordPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 名称（模糊查询） */
    private String itemName;

    /** 类型：1入库 2领取 3报损 */
    private Integer type;

    /** 日期范围-开始 */
    private LocalDate dateStart;

    /** 日期范围-结束 */
    private LocalDate dateEnd;
}
