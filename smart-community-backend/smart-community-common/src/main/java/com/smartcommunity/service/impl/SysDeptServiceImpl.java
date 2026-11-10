package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SysDept;
import com.smartcommunity.mapper.SysDeptMapper;
import com.smartcommunity.service.SysDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Override
    public List<SysDept> getTree() {
        try {
            List<SysDept> all = list(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getStatus, 1)
                    .orderByAsc(SysDept::getSort));
            Map<Long, List<SysDept>> childrenMap = all.stream()
                    .filter(d -> d.getParentId() != null && d.getParentId() > 0)
                    .collect(Collectors.groupingBy(SysDept::getParentId));
            List<SysDept> roots = new ArrayList<>();
            for (SysDept dept : all) {
                if (dept.getParentId() == null || dept.getParentId() == 0) {
                    roots.add(dept);
                }
            }
            return buildTree(roots, childrenMap);
        } catch (Exception e) {
            log.error("获取部门树失败", e);
            return new ArrayList<>();
        }
    }

    private List<SysDept> buildTree(List<SysDept> parents, Map<Long, List<SysDept>> childrenMap) {
        List<SysDept> result = new ArrayList<>();
        if (parents == null) return result;
        for (SysDept parent : parents) {
            result.add(parent);
            List<SysDept> children = childrenMap.get(parent.getId());
            if (children != null) {
                result.addAll(buildTree(children, childrenMap));
            }
        }
        return result;
    }
}