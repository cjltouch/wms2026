package com.example.wms.business.loss.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class LossAuditReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报损单ID（单条审核时直接传此 id，兼容前端 audit({id:xxx}) 写法） */
    @JsonAlias({"lossId"})
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 待审核报损单ID列表 */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> ids;

    /** 是否审核通过（true=通过，false=不通过）。null 视作通过，兼容老前端 */
    private Boolean pass;

    /** 审核备注（不通过时强制填审核原因） */
    @JsonAlias({"auditRemark"})
    private String remark;

    /** 返回真正的 ids：若 ids 为空而 id 不为空，则包装为单元素 list 返回 */
    public List<Long> effectiveIds() {
        if (ids != null && !ids.isEmpty()) return ids;
        if (id != null) return Collections.singletonList(id);
        return Collections.emptyList();
    }
}
