package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    String login(String username, String password);

    String loginBySms(String phone, String code);

    String refreshToken(String token);

    void register(SysUser user);

    void registerBySms(SysUser user, String code);

    SysUser getByUsername(String username);

    SysUser getByPhone(String phone);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    java.util.List<Long> getUserRoleIds(Long userId);

    void assignRoles(Long userId, java.util.List<Long> roleIds);

    java.util.List<com.smartcommunity.entity.SysRole> getUserRoles(Long userId);
}