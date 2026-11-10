package com.smartcommunity.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.common.util.EncryptUtils;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统初始化")
@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SysUserService userService;

    @Operation(summary = "检测是否需要初始化（存在空密码用户）")
    @GetMapping("/check")
    public Result<Map<String, Boolean>> check() {
        long count = userService.count(
                new LambdaQueryWrapper<SysUser>()
                        .isNull(SysUser::getPassword).or().eq(SysUser::getPassword, "")
        );
        Map<String, Boolean> data = new HashMap<>();
        data.put("needSetup", count > 0);
        return Result.success(data);
    }

    @Operation(summary = "初始化空密码用户")
    @PostMapping("/init")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> init(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (password.length() < 6) {
            throw new BusinessException(400, "密码至少6位");
        }

        String encodedPassword = EncryptUtils.passwordEncode(password);
        boolean updated = userService.update(
                new LambdaUpdateWrapper<SysUser>()
                        .set(SysUser::getPassword, encodedPassword)
                        .and(wrapper -> wrapper
                                .isNull(SysUser::getPassword)
                                .or()
                                .eq(SysUser::getPassword, ""))
        );
        if (!updated) {
            throw new BusinessException(409, "系统密码已初始化，请勿重复操作");
        }
        return Result.success("密码初始化成功", null);
    }
}
