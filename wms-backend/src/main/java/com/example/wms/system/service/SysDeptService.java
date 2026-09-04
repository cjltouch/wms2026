package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.system.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> deptTree(SysDept dept);

    List<Long> getDeptAndChildren(Long deptId);

    void deleteDept(Long deptId);
}
