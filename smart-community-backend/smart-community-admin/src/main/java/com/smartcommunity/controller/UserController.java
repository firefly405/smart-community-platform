package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.util.EncryptUtils;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$");
    private static final Set<String> AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    private final SysUserService userService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (username != null) qw.like(SysUser::getUsername, username);
        if (phone != null) qw.like(SysUser::getPhone, phone);
        if (status != null) qw.eq(SysUser::getStatus, status);
        if (deptId != null) qw.eq(SysUser::getDeptId, deptId);
        qw.orderByDesc(SysUser::getId);
        return Result.success(userService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/info")
    public Result<SysUser> info() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getById(userId));
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping("/profile")
    public Result<SysUser> updateProfile(@RequestBody SysUser request) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String realName = trimToNull(request.getRealName());
        String phone = trimToNull(request.getPhone());
        String email = trimToNull(request.getEmail());
        Integer gender = request.getGender() == null ? 0 : request.getGender();
        if (realName != null && realName.length() > 50) {
            throw new BusinessException(400, "姓名不能超过50个字符");
        }
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "请输入正确的手机号");
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(400, "请输入正确的邮箱");
        }
        if (gender < 0 || gender > 2) {
            throw new BusinessException(400, "性别参数不正确");
        }
        if (phone != null && userService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone).ne(SysUser::getId, userId)) > 0) {
            throw new BusinessException(400, "该手机号已被其他账号使用");
        }
        if (Integer.valueOf(3).equals(user.getUserType())) {
            if (phone != null && !Objects.equals(user.getPhone(), phone)) {
                throw new BusinessException(400, "租户手机号用于关联租约，如需变更请联系物业");
            }
            phone = user.getPhone();
        }

        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setGender(gender);
        user.setBirthday(request.getBirthday());
        userService.updateById(user);
        return Result.success(user);
    }

    @Operation(summary = "上传当前用户头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "头像文件不能为空且不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !AVATAR_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 JPG、PNG 或 WebP 图片");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String fileName = UUID.randomUUID() + extension;
        Path directory = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        Path target = directory.resolve(fileName);
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(500, "头像保存失败", ex);
        }

        String url = "/uploads/avatars/" + fileName;
        SysUser user = new SysUser();
        user.setId(UserContext.getUserId());
        user.setAvatar(url);
        userService.updateById(user);
        // shortcut: replaced avatar files are retained; add scheduled cleanup if storage growth becomes material.
        return Result.success(Map.of("url", url));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@RequestBody SysUser user) {
        user.setPassword(EncryptUtils.passwordEncode(user.getPassword()));
        userService.save(user);
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        user.setPassword(null);
        userService.updateById(user);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    public Result<java.util.List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        userService.assignRoles(id, body.get("roleIds"));
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody java.util.Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(400, "新密码需为8-20位，且同时包含字母和数字");
        }
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
