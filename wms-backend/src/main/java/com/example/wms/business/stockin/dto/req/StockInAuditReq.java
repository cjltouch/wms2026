package com.example.wms.business.stockin.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class StockInAuditReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 入库单ID（单条审核时直接传此 id） */
    @JsonAlias({"stockInId"})
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 待审核的入库单ID列表 */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> ids;

    /** 是否审核通过（true=通过，false=不通过）。null 视作通过 */
    private Boolean pass;

    /** 审核备注（不通过时强制填审核原因） */
    @JsonAlias({"auditRemark"})
    private String remark;

    public List<Long> effectiveIds() {
        if (ids != null && !ids.isEmpty()) return ids;
        if (id != null) return Collections.singletonList(id);
        return Collections.emptyList();
    }
}
