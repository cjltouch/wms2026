package com.example.wms.business.check.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class CheckAuditReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单ID（单条审核时直接传此 id，兼容前端 audit({id:xxx}) 写法） */
    @JsonAlias({"checkId"})
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 盘点单ID列表（批量审核用） */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> ids;

    /** 是否审核通过（前端 audit({pass:true})，兼容老写法） */
    private Boolean pass;

    /** 审核备注 */
    @JsonAlias({"auditRemark"})
    private String remark;

    /**
     * 返回真正的 ids：若 ids 为空而 id 不为空，则包装为单元素 list 返回，兼容前后端两种请求格式。
     */
    public List<Long> effectiveIds() {
        if (ids != null && !ids.isEmpty()) return ids;
        if (id != null) return Collections.singletonList(id);
        return Collections.emptyList();
    }
}
