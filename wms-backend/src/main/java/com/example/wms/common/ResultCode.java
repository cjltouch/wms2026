package com.example.wms.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),

    STOCK_INSUFFICIENT(10001, "库存不足"),
    STATUS_NOT_ALLOWED(10002, "当前状态不允许该操作"),
    DETAIL_EMPTY(10003, "明细不能为空"),
    SKU_NOT_FOUND(10004, "SKU不存在"),
    BATCH_REQUIRED(10005, "批次号必填"),
    LINE_NO_DUPLICATE(10006, "行号重复"),
    SOURCE_DOC_MISMATCH(10007, "源单不匹配"),
    OPTIMISTIC_LOCK_CONFLICT(10008, "乐观锁冲突，数据已被其他用户修改"),

    DATA_NOT_FOUND(20001, "数据不存在"),
    SPU_CODE_DUPLICATE(20002, "SPU编码已存在"),
    SKU_CODE_DUPLICATE(20003, "SKU编码已存在"),
    BARCODE_DUPLICATE(20004, "条码已存在"),
    CATEGORY_CODE_DUPLICATE(20005, "分类编码已存在"),
    BRAND_CODE_DUPLICATE(20006, "品牌编码已存在"),
    UNIT_CODE_DUPLICATE(20007, "单位编码已存在"),
    SUPPLIER_CODE_DUPLICATE(20008, "供应商编码已存在"),
    CUSTOMER_CODE_DUPLICATE(20009, "客户编码已存在"),
    WAREHOUSE_CODE_DUPLICATE(20010, "仓库编码已存在"),
    AREA_CODE_DUPLICATE(20011, "区域编码在该仓库已存在"),
    LOCATION_CODE_DUPLICATE(20012, "库位编码已存在");

    private final Integer code;
    private final String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
