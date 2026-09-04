package com.example.wms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.system.entity.SysDept;
import com.example.wms.system.mapper.SysDeptMapper;
import com.example.wms.system.service.SysDeptService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Override
    public List<SysDept> deptTree(SysDept dept) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (dept != null && StringUtils.hasText(dept.getDeptName())) {
            wrapper.like(SysDept::getDeptName, dept.getDeptName());
        }
        if (dept != null && StringUtils.hasText(dept.getStatus())) {
            wrapper.eq(SysDept::getStatus, dept.getStatus());
        }
        wrapper.orderByAsc(SysDept::getOrderNum);
        List<SysDept> depts = this.list(wrapper);
        return buildDeptTree(depts, 0L);
    }

    private List<SysDept> buildDeptTree(List<SysDept> depts, Long parentId) {
        List<SysDept> returnList = new ArrayList<>();
        for (Iterator<SysDept> iterator = depts.iterator(); iterator.hasNext(); ) {
            SysDept dept = iterator.next();
            if (dept.getParentId() != null && dept.getParentId().equals(parentId)) {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = depts;
        }
        return returnList;
    }

    private void recursionFn(List<SysDept> list, SysDept dept) {
        List<SysDept> childList = getChildList(list, dept);
        dept.setChildren(childList);
        for (SysDept child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private List<SysDept> getChildList(List<SysDept> list, SysDept dept) {
        return list.stream()
                .filter(d -> d.getParentId() != null && d.getParentId().equals(dept.getDeptId()))
                .collect(Collectors.toList());
    }

    private boolean hasChild(List<SysDept> list, SysDept dept) {
        return list.stream().anyMatch(d -> d.getParentId() != null && d.getParentId().equals(dept.getDeptId()));
    }

    @Override
    public List<Long> getDeptAndChildren(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        result.add(deptId);
        collectChildren(deptId, result);
        return result;
    }

    private void collectChildren(Long parentId, List<Long> result) {
        List<SysDept> children = this.list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            result.add(child.getDeptId());
            collectChildren(child.getDeptId(), result);
        }
    }

    @Override
    public void deleteDept(Long deptId) {
        this.removeById(deptId);
    }
}
