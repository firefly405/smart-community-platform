package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SysMenu;
import com.smartcommunity.mapper.SysMenuMapper;
import com.smartcommunity.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getTree() {
        List<SysMenu> all = list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .and(w -> w.isNull(SysMenu::getVisible).or().eq(SysMenu::getVisible, 1))
                .orderByAsc(SysMenu::getSort));
        Map<Long, List<SysMenu>> childrenMap = all.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() > 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        List<SysMenu> roots = all.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0)
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .collect(Collectors.toList());
        return buildTree(roots, childrenMap);
    }

    private List<SysMenu> buildTree(List<SysMenu> parents, Map<Long, List<SysMenu>> childrenMap) {
        List<SysMenu> result = new ArrayList<>();
        if (parents == null) return result;
        for (SysMenu parent : parents) {
            List<SysMenu> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
            children.sort(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()));
            parent.setChildren(buildTree(children, childrenMap));
            result.add(parent);
        }
        return result;
    }
}
