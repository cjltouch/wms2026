package com.example.wms.business.office.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.office.dto.req.OfficeRecordPageReq;
import com.example.wms.business.office.dto.req.OfficeRecordSaveReq;
import com.example.wms.business.office.entity.WmsOfficeRecord;
import com.example.wms.business.office.mapper.WmsOfficeRecordMapper;
import com.example.wms.business.office.service.WmsOfficeRecordService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class WmsOfficeRecordServiceImpl extends ServiceImpl<WmsOfficeRecordMapper, WmsOfficeRecord> implements WmsOfficeRecordService {

    @Override
    public PageRsp<WmsOfficeRecord> pageOfficeRecord(OfficeRecordPageReq req) {
        LambdaQueryWrapper<WmsOfficeRecord> wrapper = buildQueryWrapper(req);
        Page<WmsOfficeRecord> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public void exportOfficeRecord(OfficeRecordPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<WmsOfficeRecord> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsOfficeRecord::getCreateTime);
        List<WmsOfficeRecord> records = this.list(wrapper);
        java.time.format.DateTimeFormatter dtFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.time.format.DateTimeFormatter dFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<com.example.wms.business.office.dto.rsp.OfficeRecordExportVo> voList = records.stream().map(r -> {
            com.example.wms.business.office.dto.rsp.OfficeRecordExportVo vo =
                    new com.example.wms.business.office.dto.rsp.OfficeRecordExportVo();
            vo.setRecordDate(r.getRecordDate() != null ? r.getRecordDate().format(dFmt) : "");
            vo.setItemName(r.getItemName());
            vo.setTypeText(typeText(r.getType()));
            vo.setUnit(r.getUnit());
            vo.setQuantity(r.getQuantity());
            vo.setPersonName(r.getPersonName());
            vo.setSpec(r.getSpec());
            vo.setRemark(r.getRemark());
            vo.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().format(dtFmt) : "");
            return vo;
        }).toList();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode(
                    "用品登记_" + java.time.LocalDate.now().toString() + ".xlsx",
                    java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            com.alibaba.excel.EasyExcel.write(response.getOutputStream(),
                            com.example.wms.business.office.dto.rsp.OfficeRecordExportVo.class)
                    .sheet("用品登记")
                    .doWrite(voList);
        } catch (java.io.IOException e) {
            throw new RuntimeException("导出用品登记失败: " + e.getMessage(), e);
        }
    }

    /** 类型文本 */
    private String typeText(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "入库";
            case 2 -> "领取";
            case 3 -> "报损";
            default -> String.valueOf(type);
        };
    }

    @Override
    public java.util.List<java.util.Map<String, String>> suggestName(String keyword) {
        java.util.LinkedHashMap<String, java.util.Map<String, String>> seen = new java.util.LinkedHashMap<>();
        LambdaQueryWrapper<WmsOfficeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(WmsOfficeRecord::getItemName, WmsOfficeRecord::getSpec, WmsOfficeRecord::getUnit)
                .isNotNull(WmsOfficeRecord::getItemName)
                .ne(WmsOfficeRecord::getItemName, "")
                .orderByDesc(WmsOfficeRecord::getCreateTime)
                .last("LIMIT 200");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(WmsOfficeRecord::getItemName, keyword);
        }
        for (WmsOfficeRecord r : this.list(wrapper)) {
            String name = r.getItemName();
            if (!StringUtils.hasText(name) || seen.containsKey(name)) {
                continue;
            }
            java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
            item.put("itemName", name);
            item.put("spec", r.getSpec() != null ? r.getSpec() : "");
            item.put("unit", r.getUnit() != null ? r.getUnit() : "");
            seen.put(name, item);
            if (seen.size() >= 30) break;
        }
        return new java.util.ArrayList<>(seen.values());
    }

    /** 构建查询条件（名称模糊、类型、日期范围） */
    private LambdaQueryWrapper<WmsOfficeRecord> buildQueryWrapper(OfficeRecordPageReq req) {
        LambdaQueryWrapper<WmsOfficeRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getItemName())) {
            wrapper.like(WmsOfficeRecord::getItemName, req.getItemName());
        }
        if (req.getType() != null) {
            wrapper.eq(WmsOfficeRecord::getType, req.getType());
        }
        if (req.getDateStart() != null) {
            wrapper.ge(WmsOfficeRecord::getRecordDate, req.getDateStart());
        }
        if (req.getDateEnd() != null) {
            wrapper.le(WmsOfficeRecord::getRecordDate, req.getDateEnd());
        }
        wrapper.orderByDesc(WmsOfficeRecord::getRecordDate);
        wrapper.orderByDesc(WmsOfficeRecord::getCreateTime);
        return wrapper;
    }

    @Override
    public void saveOfficeRecord(OfficeRecordSaveReq req) {
        WmsOfficeRecord record = new WmsOfficeRecord();
        record.setRecordDate(req.getRecordDate() != null ? req.getRecordDate() : LocalDate.now());
        record.setItemName(req.getItemName());
        record.setType(req.getType());
        record.setUnit(req.getUnit());
        record.setQuantity(req.getQuantity());
        record.setPersonName(req.getPersonName());
        record.setSpec(req.getSpec());
        record.setRemark(req.getRemark());
        this.save(record);
    }

    @Override
    public void updateOfficeRecord(OfficeRecordSaveReq req) {
        if (req.getRecordId() == null) {
            throw new BizException("记录ID不能为空");
        }
        WmsOfficeRecord exist = this.getById(req.getRecordId());
        if (exist == null) {
            throw new BizException("记录不存在");
        }
        exist.setRecordDate(req.getRecordDate() != null ? req.getRecordDate() : exist.getRecordDate());
        exist.setItemName(req.getItemName());
        exist.setType(req.getType());
        exist.setUnit(req.getUnit());
        exist.setQuantity(req.getQuantity());
        exist.setPersonName(req.getPersonName());
        exist.setSpec(req.getSpec());
        exist.setRemark(req.getRemark());
        this.updateById(exist);
    }

    @Override
    public void deleteOfficeRecord(Long recordId) {
        this.removeById(recordId);
    }

    @Override
    public void batchDeleteOfficeRecord(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return;
        }
        this.removeByIds(recordIds);
    }
}
