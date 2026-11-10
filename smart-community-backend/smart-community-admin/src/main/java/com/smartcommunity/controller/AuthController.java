package com.smartcommunity.controller;

import cn.hutool.core.util.StrUtil;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.security.JwtUtils;
import com.smartcommunity.service.SmsService;
import com.smartcommunity.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final JwtUtils jwtUtils;
    private final SmsService smsService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (StrUtil.isBlank(phone)) {
            throw new BusinessException(400, "手机号不能为空");
        }
        boolean sent = smsService.sendCode(phone);
        if (!sent) {
            throw new BusinessException(400, "验证码发送过于频繁，请稍后再试");
        }
        return Result.<Void>success("验证码发送成功", null);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }
        String token = userService.login(username, password);
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            user = userService.getByPhone(username);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", buildUserInfo(user));
        return Result.success("登录成功", data);
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login-by-sms")
    public Result<Map<String, Object>> loginBySms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new BusinessException(400, "手机号和验证码不能为空");
        }
        String token = userService.loginBySms(phone, code);
        SysUser user = userService.getByPhone(phone);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", buildUserInfo(user));
        return Result.success("登录成功", data);
    }

    @Operation(summary = "用户注册（短信验证码）")
    @PostMapping("/register-by-sms")
    public Result<Void> registerBySms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String username = body.get("username");
        String password = body.get("password");
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            throw new BusinessException(400, "手机号和验证码不能为空");
        }
        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setUsername(StrUtil.isNotBlank(username) ? username : phone);
        user.setPassword(StrUtil.isNotBlank(password) ? password : "123456");
        userService.registerBySms(user, code);
        return Result.<Void>success("注册成功", null);
    }

    @Operation(summary = "用户注册（密码方式）")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        userService.register(user);
        return Result.success();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String newToken = jwtUtils.refreshToken(token);
        if (newToken == null) {
            throw new BusinessException(401, "Token已过期，请重新登录");
        }
        return Result.success(newToken);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("avatar", user.getAvatar());
        info.put("gender", user.getGender());
        info.put("userType", user.getUserType());
        info.put("deptId", user.getDeptId());
        info.put("status", user.getStatus());
        return info;
    }
}