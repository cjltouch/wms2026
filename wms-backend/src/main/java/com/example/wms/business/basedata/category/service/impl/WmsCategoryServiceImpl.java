package com.example.wms.business.basedata.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.category.dto.req.CategoryPageReq;
import com.example.wms.business.basedata.category.dto.req.CategorySaveReq;
import com.example.wms.business.basedata.category.entity.WmsCategory;
import com.example.wms.business.basedata.category.mapper.WmsCategoryMapper;
import com.example.wms.business.basedata.category.service.WmsCategoryService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WmsCategoryServiceImpl extends ServiceImpl<WmsCategoryMapper, WmsCategory> implements WmsCategoryService {

    @Override
    public PageRsp<WmsCategory> pageCategory(CategoryPageReq req) {
        LambdaQueryWrapper<WmsCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getCategoryName())) {
            wrapper.like(WmsCategory::getCategoryName, req.getCategoryName());
        }
        if (StringUtils.hasText(req.getCategoryCode())) {
            wrapper.like(WmsCategory::getCategoryCode, req.getCategoryCode());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsCategory::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsCategory::getOrderNum);
        Page<WmsCategory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsCategory> categoryTree(WmsCategory category) {
        LambdaQueryWrapper<WmsCategory> wrapper = new LambdaQueryWrapper<>();
        if (category != null && StringUtils.hasText(category.getCategoryName())) {
            wrapper.like(WmsCategory::getCategoryName, category.getCategoryName());
        }
        if (category != null && StringUtils.hasText(category.getStatus())) {
            wrapper.eq(WmsCategory::getStatus, category.getStatus());
        }
        wrapper.orderByAsc(WmsCategory::getOrderNum);
        List<WmsCategory> categories = this.list(wrapper);
        return buildCategoryTree(categories, 0L);
    }

    private List<WmsCategory> buildCategoryTree(List<WmsCategory> categories, Long parentId) {
        List<WmsCategory> returnList = new ArrayList<>();
        for (Iterator<WmsCategory> iterator = categories.iterator(); iterator.hasNext(); ) {
            WmsCategory category = iterator.next();
            Long pid = category.getParentId() == null ? 0L : category.getParentId();
            if (pid.equals(parentId)) {
                recursionFn(categories, category);
                returnList.add(category);
            }
        }
        if (returnList.isEmpty()) {
            returnList = categories;
        }
        return returnList;
    }

    private void recursionFn(List<WmsCategory> list, WmsCategory category) {
        List<WmsCategory> childList = getChildList(list, category);
        category.setChildren(childList);
        for (WmsCategory child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private List<WmsCategory> getChildList(List<WmsCategory> list, WmsCategory category) {
        return list.stream()
                .filter(c -> {
                    Long pid = c.getParentId() == null ? 0L : c.getParentId();
                    return pid.equals(category.getCategoryId());
                })
                .collect(Collectors.toList());
    }

    private boolean hasChild(List<WmsCategory> list, WmsCategory category) {
        return list.stream().anyMatch(c -> {
            Long pid = c.getParentId() == null ? 0L : c.getParentId();
            return pid.equals(category.getCategoryId());
        });
    }

    @Override
    public List<WmsCategory> listAll() {
        return this.list(new LambdaQueryWrapper<WmsCategory>()
                .eq(WmsCategory::getStatus, "0")
                .orderByAsc(WmsCategory::getOrderNum));
    }

    @Override
    public void saveCategory(CategorySaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsCategory::getCategoryCode, req.getCategoryCode())
                .count();
        if (count > 0) {
            throw new BizException("分类编码已存在");
        }
        WmsCategory category = new WmsCategory();
        category.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        category.setCategoryName(req.getCategoryName());
        category.setCategoryCode(req.getCategoryCode());
        category.setOrderNum(req.getOrderNum() == null ? 0 : req.getOrderNum());
        category.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        updateAncestors(category);
        this.save(category);
    }

    @Override
    public void updateCategory(CategorySaveReq req) {
        if (req.getCategoryId() == null) {
            throw new BizException("分类ID不能为空");
        }
        WmsCategory exist = this.getById(req.getCategoryId());
        if (exist == null) {
            throw new BizException("分类不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsCategory::getCategoryCode, req.getCategoryCode())
                .ne(WmsCategory::getCategoryId, req.getCategoryId())
                .count();
        if (count > 0) {
            throw new BizException("分类编码已存在");
        }
        exist.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        exist.setCategoryName(req.getCategoryName());
        exist.setCategoryCode(req.getCategoryCode());
        exist.setOrderNum(req.getOrderNum() == null ? 0 : req.getOrderNum());
        exist.setStatus(req.getStatus());
        updateAncestors(exist);
        this.updateById(exist);
    }

    private void updateAncestors(WmsCategory category) {
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        if (parentId == 0L) {
            category.setAncestors("0");
        } else {
            WmsCategory parent = this.getById(parentId);
            if (parent != null) {
                category.setAncestors(parent.getAncestors() + "," + parentId);
            } else {
                category.setAncestors("0," + parentId);
            }
        }
    }

    @Override
    public void deleteCategory(Long categoryId) {
        long childCount = this.lambdaQuery()
                .eq(WmsCategory::getParentId, categoryId)
                .count();
        if (childCount > 0) {
            throw new BizException("存在子分类，不允许删除");
        }
        this.removeById(categoryId);
    }

    @Override
    public void batchDeleteCategory(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        for (Long id : categoryIds) {
            long childCount = this.lambdaQuery()
                    .eq(WmsCategory::getParentId, id)
                    .count();
            if (childCount > 0) {
                WmsCategory c = this.getById(id);
                throw new BizException("分类[" + (c != null ? c.getCategoryName() : id) + "]存在子分类，不允许删除");
            }
        }
        this.removeByIds(categoryIds);
    }
}
