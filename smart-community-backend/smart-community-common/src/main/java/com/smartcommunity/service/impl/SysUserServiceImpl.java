package com.smartcommunity.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.util.EncryptUtils;
import com.smartcommunity.entity.SysLoginLog;
import com.smartcommunity.entity.SysRole;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.entity.SysUserRole;
import com.smartcommunity.mapper.SysLoginLogMapper;
import com.smartcommunity.mapper.SysRoleMapper;
import com.smartcommunity.mapper.SysUserMapper;
import com.smartcommunity.mapper.SysUserRoleMapper;
import com.smartcommunity.security.JwtUtils;
import com.smartcommunity.service.SmsService;
import com.smartcommunity.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtils jwtUtils;
    private final SysLoginLogMapper loginLogMapper;
    private final SmsService smsService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public String login(String username, String password) {
        SysUser user = getByUsername(username);
        if (user == null) {
            // Try phone login
            user = getByPhone(username);
        }
        if (user == null) {
            recordLoginLog(username, 0, "用户不存在");
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            recordLoginLog(username, 0, "账号已禁用");
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        if (!EncryptUtils.passwordMatch(password, user.getPassword())) {
            recordLoginLog(username, 0, "密码错误");
            throw new BusinessException(400, "用户名或密码错误");
        }

        // Update login info
        user.setLoginTime(LocalDateTime.now());
        updateById(user);

        recordLoginLog(username, 1, "登录成功");
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public String loginBySms(String phone, String code) {
        if (!smsService.verifyCode(phone, code)) {
            recordLoginLog(phone, 0, "验证码错误或已过期");
            throw new BusinessException(400, "验证码错误或已过期");
        }

        SysUser user = getByPhone(phone);
        if (user == null) {
            recordLoginLog(phone, 0, "用户不存在");
            throw new BusinessException(400, "该手机号未注册");
        }
        if (user.getStatus() == 0) {
            recordLoginLog(phone, 0, "账号已禁用");
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        user.setLoginTime(LocalDateTime.now());
        updateById(user);

        recordLoginLog(phone, 1, "短信登录成功");
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public String refreshToken(String token) {
        return jwtUtils.refreshToken(token);
    }

    @Override
    @Transactional
    public void register(SysUser user) {
        if (StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException(400, "手机号不能为空");
        }
        SysUser exist = getByPhone(user.getPhone());
        if (exist != null) {
            throw new BusinessException(400, "该手机号已注册");
        }
        exist = getByUsername(user.getUsername());
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        user.setPassword(EncryptUtils.passwordEncode(user.getPassword()));
        user.setStatus(1);
        user.setUserType(1);
        save(user);
    }

    @Override
    @Transactional
    public void registerBySms(SysUser user, String code) {
        if (StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException(400, "手机号不能为空");
        }
        if (!smsService.verifyCode(user.getPhone(), code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        SysUser exist = getByPhone(user.getPhone());
        if (exist != null) {
            throw new BusinessException(400, "该手机号已注册");
        }
        exist = getByUsername(user.getUsername());
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        user.setPassword(EncryptUtils.passwordEncode(user.getPassword()));
        user.setStatus(1);
        user.setUserType(1);
        save(user);
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!EncryptUtils.passwordMatch(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(EncryptUtils.passwordEncode(newPassword));
        updateById(user);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (getById(userId) == null) {
            throw new BusinessException(404, "用户不存在");
        }
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                ur.setCreateTime(now);
                list.add(ur);
            }
            userRoleMapper.insertBatch(list);
        }
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private void recordLoginLog(String username, int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}