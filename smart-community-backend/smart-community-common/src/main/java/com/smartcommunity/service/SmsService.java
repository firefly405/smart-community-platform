package com.smartcommunity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码服务 - 开发环境使用内存存储，生产环境应替换为Redis
 */
@Slf4j
@Service
public class SmsService {

    /** 验证码有效期(秒) */
    private static final long CODE_EXPIRE_SECONDS = 300;
    /** 同一手机号发送间隔(秒) */
    private static final long SEND_INTERVAL_SECONDS = 60;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    /**
     * 发送验证码
     * @param phone 手机号
     * @return 是否发送成功
     */
    public boolean sendCode(String phone) {
        CodeEntry existing = codeStore.get(phone);
        if (existing != null) {
            long elapsed = (System.currentTimeMillis() - existing.createTimeMs) / 1000;
            if (elapsed < SEND_INTERVAL_SECONDS) {
                log.warn("验证码发送过于频繁 phone={} elapsed={}s", phone, elapsed);
                return false;
            }
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        codeStore.put(phone, new CodeEntry(code, System.currentTimeMillis()));

        // 开发环境：打印到控制台。生产环境：调用真实短信接口
        log.info("===== 短信验证码 [phone={}] : {} =====", phone, code);
        return true;
    }

    /**
     * 校验验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否校验通过
     */
    public boolean verifyCode(String phone, String code) {
        CodeEntry entry = codeStore.get(phone);
        if (entry == null) {
            return false;
        }

        long elapsed = (System.currentTimeMillis() - entry.createTimeMs) / 1000;
        if (elapsed > CODE_EXPIRE_SECONDS) {
            codeStore.remove(phone);
            return false;
        }

        if (entry.code.equals(code)) {
            codeStore.remove(phone);
            return true;
        }
        return false;
    }

    private static class CodeEntry {
        final String code;
        final long createTimeMs;

        CodeEntry(String code, long createTimeMs) {
            this.code = code;
            this.createTimeMs = createTimeMs;
        }
    }
}