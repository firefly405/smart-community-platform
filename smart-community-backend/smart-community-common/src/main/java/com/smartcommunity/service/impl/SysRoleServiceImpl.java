package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.SysRole;
import com.smartcommunity.entity.SysRoleMenu;
import com.smartcommunity.mapper.SysRoleMapper;
import com.smartcommunity.mapper.SysRoleMenuMapper;
import com.smartcommunity.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        // Delete old
        roleMenuMapper.deleteByRoleId(roleId);
        // Insert new
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setCreateTime(now);
                list.add(rm);
            }
            roleMenuMapper.insertBatch(list);
        }
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }
}