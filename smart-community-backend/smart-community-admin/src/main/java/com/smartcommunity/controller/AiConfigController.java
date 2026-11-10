package com.smartcommunity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Tag(name = "AI服务配置")
@RestController
@RequestMapping("/api/v1/system/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private static final String DEFAULT_SPRING_BASE_URL = "https://api.openai.com";
    private static final Pattern MODEL_PATTERN = Pattern.compile("^[A-Za-z0-9._:/-]{1,160}$");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicBoolean RESTARTING = new AtomicBoolean(false);

    private final Environment environment;
    private final SysUserService userService;

    @Value("${app.ai-runtime-config:.runtime/ai-config.properties}")
    private String configPath;

    @Operation(summary = "获取AI配置状态")
    @GetMapping
    public Result<AiConfigView> getConfig() {
        requireAdministrator();
        return Result.success(buildView(readProperties()));
    }

    @Operation(summary = "保存OpenAI兼容API配置")
    @PutMapping
    public Result<AiConfigView> saveConfig(@RequestBody AiConfigRequest request) {
        requireAdministrator();
        Properties current = readProperties();
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        String apiUrl = trim(request.apiUrl());
        if (apiUrl.isEmpty()) apiUrl = configuredUrl(current);
        String apiKey = trim(request.apiKey());
        if (apiKey.isEmpty()) apiKey = configuredKey(current);

        if (enabled && apiUrl.isEmpty()) throw new BusinessException(400, "启用AI服务前请填写API Base URL");
        if (enabled && apiKey.isEmpty()) throw new BusinessException(400, "启用AI服务前请填写API Key");
        if (apiKey.length() > 2048) throw new BusinessException(400, "API Key长度超出限制");
        if (!apiUrl.isEmpty()) apiUrl = normalizeApiUrl(apiUrl);

        String chatModel = validModel(request.chatModel(), "");
        String embeddingModel = validModel(request.embeddingModel(), "");
        if (enabled) {
            ModelSelection selection = discoverModels(apiUrl, apiKey, chatModel, embeddingModel);
            chatModel = selection.chatModel();
            embeddingModel = selection.embeddingModel();
        }

        Properties saved = new Properties();
        saved.setProperty("app.ai-api-url", apiUrl);
        saved.setProperty("app.ai-api-key", apiKey);
        saved.setProperty("spring.ai.openai.api-key", apiKey);
        saved.setProperty("spring.ai.openai.chat.enabled", String.valueOf(enabled));
        saved.setProperty("spring.ai.openai.embedding.enabled", String.valueOf(enabled && !embeddingModel.isEmpty()));
        saved.setProperty("spring.ai.openai.chat.options.model", chatModel);
        saved.setProperty("spring.ai.openai.embedding.options.model", embeddingModel);
        ApiEndpoint endpoint = apiUrl.isEmpty()
                ? new ApiEndpoint(DEFAULT_SPRING_BASE_URL, "/v1") : springEndpoint(apiUrl);
        saved.setProperty("spring.ai.openai.base-url", endpoint.baseUrl());
        saved.setProperty("spring.ai.openai.chat.completions-path", endpoint.apiPath() + "/chat/completions");
        saved.setProperty("spring.ai.openai.embedding.embeddings-path", endpoint.apiPath() + "/embeddings");
        writeProperties(saved);
        return Result.success("配置已自动识别并保存，重启后端后生效", buildView(saved));
    }

    @Operation(summary = "检测OpenAI兼容API并自动识别模型")
    @PostMapping("/test")
    public Result<TestResult> testConnection(@RequestBody(required = false) AiConfigRequest request) {
        requireAdministrator();
        Properties saved = readProperties();
        String apiUrl = request == null ? "" : trim(request.apiUrl());
        if (apiUrl.isEmpty()) apiUrl = configuredUrl(saved);
        String apiKey = request == null ? "" : trim(request.apiKey());
        if (apiKey.isEmpty()) apiKey = configuredKey(saved);
        if (apiUrl.isEmpty()) throw new BusinessException(400, "请先填写API Base URL");
        if (apiKey.isEmpty()) throw new BusinessException(400, "请先填写或保存API Key");

        long started = System.nanoTime();
        try {
            String chatModel = request == null ? "" : validModel(request.chatModel(), "");
            String embeddingModel = request == null ? "" : validModel(request.embeddingModel(), "");
            ModelSelection selection = discoverModels(normalizeApiUrl(apiUrl), apiKey, chatModel, embeddingModel);
            long latency = Duration.ofNanos(System.nanoTime() - started).toMillis();
            String message = selection.embeddingModel().isEmpty()
                    ? "连接成功，已识别对话模型（未发现向量模型）" : "连接成功，已自动识别对话和向量模型";
            return Result.success(new TestResult(true, message, latency, selection.models(),
                    selection.chatModel(), selection.embeddingModel()));
        } catch (BusinessException e) {
            long latency = Duration.ofNanos(System.nanoTime() - started).toMillis();
            return Result.success(new TestResult(false, e.getMessage(), latency, List.of(), "", ""));
        }
    }

    @Operation(summary = "快速重启后端服务")
    @PostMapping("/restart")
    public Result<Void> restartBackend() {
        requireAdministrator();

        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path script = workingDirectory.resolve("scripts/restart-backend.ps1");
        ProcessHandle.Info processInfo = ProcessHandle.current().info();
        String javaCommand = processInfo.command()
                .orElseGet(() -> Path.of(System.getProperty("java.home"), "bin", "java.exe").toString());
        Path jar = resolveRunningJar(workingDirectory);

        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            throw new BusinessException(500, "快速重启目前仅支持Windows运行环境");
        }
        if (!Files.isRegularFile(script)) {
            throw new BusinessException(500, "未找到后端重启脚本：" + script);
        }
        if (!Files.isRegularFile(Path.of(javaCommand)) || !Files.isRegularFile(jar)) {
            throw new BusinessException(500, "当前Java程序或后端JAR不存在，不能快速重启");
        }
        if (!RESTARTING.compareAndSet(false, true)) {
            throw new BusinessException(409, "后端服务正在重启，请勿重复操作");
        }

        Path logDirectory = jar.getParent();
        ProcessBuilder launcher = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(),
                "-ProcessId", String.valueOf(ProcessHandle.current().pid()),
                "-JavaExecutable", javaCommand,
                "-JarPath", jar.toString(),
                "-WorkingDirectory", workingDirectory.toString());
        launcher.directory(workingDirectory.toFile());
        launcher.redirectOutput(logDirectory.resolve("restart-launcher.out.log").toFile());
        launcher.redirectError(logDirectory.resolve("restart-launcher.err.log").toFile());
        try {
            launcher.start();
        } catch (IOException e) {
            RESTARTING.set(false);
            throw new BusinessException(500, "启动后端重启程序失败", e);
        }

        Thread shutdown = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }, "backend-restart-shutdown");
        shutdown.setDaemon(false);
        shutdown.start();
        return Result.success("后端服务正在重启", null);
    }

    private static Path resolveRunningJar(Path workingDirectory) {
        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(Pattern.quote(System.getProperty("path.separator")))) {
            if (entry.isBlank()) continue;
            Path candidate = Path.of(entry);
            candidate = (candidate.isAbsolute() ? candidate : workingDirectory.resolve(candidate)).normalize();
            String fileName = candidate.getFileName() == null ? "" : candidate.getFileName().toString();
            if (Files.isRegularFile(candidate) && fileName.startsWith("smart-community-admin-")
                    && fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return candidate;
            }
        }
        try {
            Path source = Path.of(AiConfigController.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            if (Files.isRegularFile(source) && source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return source;
            }
        } catch (Exception e) {
            throw new BusinessException(500, "无法定位当前后端JAR，不能快速重启", e);
        }
        throw new BusinessException(500, "当前后端不是JAR运行模式，请在IDE中手动重启");
    }

    private ModelSelection discoverModels(String apiUrl, String apiKey, String preferredChat,
                                          String preferredEmbedding) {
        URI modelsEndpoint = URI.create(apiUrl + "/models");
        ensurePublicEndpoint(modelsEndpoint);
        try {
            HttpRequest request = HttpRequest.newBuilder(modelsEndpoint)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(400, connectionError(response.statusCode()));
            }

            JsonNode data = JSON.readTree(response.body()).path("data");
            List<String> models = new ArrayList<>();
            if (data.isArray()) {
                data.forEach(item -> {
                    String id = trim(item.path("id").asText());
                    if (!id.isEmpty() && MODEL_PATTERN.matcher(id).matches()) models.add(id);
                });
            }
            if (models.isEmpty()) throw new BusinessException(400, "API未返回可用模型，请确认兼容OpenAI /v1/models接口");

            String chatModel = chooseChatModel(models, preferredChat);
            if (chatModel.isEmpty()) throw new BusinessException(400, "未识别到可用的对话模型");
            String embeddingModel = chooseEmbeddingModel(models, preferredEmbedding);
            return new ModelSelection(List.copyOf(models), chatModel, embeddingModel);
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(400, "API连接测试已中断", e);
        } catch (Exception e) {
            throw new BusinessException(400, "无法连接API，请检查地址、Key和网络", e);
        }
    }

    private AiConfigView buildView(Properties saved) {
        boolean runtimeEnabled = environment.getProperty("spring.ai.openai.chat.enabled", Boolean.class, false);
        String runtimeUrl = trim(environment.getProperty("app.ai-api-url", ""));
        String runtimeKey = trim(environment.getProperty("app.ai-api-key", ""));
        String runtimeChatModel = trim(environment.getProperty("spring.ai.openai.chat.options.model", ""));
        String runtimeEmbeddingModel = trim(environment.getProperty("spring.ai.openai.embedding.options.model", ""));
        boolean enabled = Boolean.parseBoolean(saved.getProperty("spring.ai.openai.chat.enabled", String.valueOf(runtimeEnabled)));
        String apiUrl = saved.getProperty("app.ai-api-url", runtimeUrl);
        String key = configuredKey(saved);
        String chatModel = saved.getProperty("spring.ai.openai.chat.options.model", runtimeChatModel);
        String embeddingModel = saved.getProperty("spring.ai.openai.embedding.options.model", runtimeEmbeddingModel);
        boolean hasSavedFile = Files.isRegularFile(configFile());
        boolean restartRequired = hasSavedFile && (enabled != runtimeEnabled
                || !apiUrl.equals(runtimeUrl)
                || !key.equals(runtimeKey)
                || !chatModel.equals(runtimeChatModel)
                || !embeddingModel.equals(runtimeEmbeddingModel));
        String updatedAt = "";
        try {
            if (hasSavedFile) updatedAt = Files.getLastModifiedTime(configFile()).toInstant().toString();
        } catch (IOException ignored) {
        }
        return new AiConfigView("OpenAI兼容 API", enabled, runtimeEnabled, !key.isEmpty(), mask(key),
                apiUrl, chatModel, embeddingModel, restartRequired, updatedAt);
    }

    private Properties readProperties() {
        Properties properties = new Properties();
        Path path = configFile();
        if (!Files.isRegularFile(path)) return properties;
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new BusinessException(500, "读取AI配置失败", e);
        }
    }

    private void writeProperties(Properties properties) {
        Path path = configFile();
        Path parent = path.toAbsolutePath().normalize().getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, "ai-config-", ".tmp");
            try (OutputStream output = Files.newOutputStream(temp)) {
                properties.store(output, "Smart Community AI runtime configuration - " + Instant.now());
            }
            try {
                Files.move(temp, path.toAbsolutePath().normalize(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path.toAbsolutePath().normalize(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "保存AI配置失败", e);
        }
    }

    private String configuredUrl(Properties properties) {
        String saved = trim(properties.getProperty("app.ai-api-url", ""));
        return saved.isEmpty() ? trim(environment.getProperty("app.ai-api-url", "")) : saved;
    }

    private String configuredKey(Properties properties) {
        String saved = trim(properties.getProperty("app.ai-api-key", ""));
        if (!saved.isEmpty()) return saved;
        return trim(environment.getProperty("app.ai-api-key", ""));
    }

    private Path configFile() {
        return Path.of(configPath).toAbsolutePath().normalize();
    }

    private void requireAdministrator() {
        SysUser user = userService.getById(UserContext.getUserId());
        if (user == null || user.getUserType() == null || user.getUserType() != 5) {
            throw new BusinessException(403, "仅系统管理员可配置AI服务");
        }
    }

    private static String normalizeApiUrl(String value) {
        try {
            URI uri = URI.create(trim(value));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            String path = trim(uri.getPath()).replaceAll("/+$", "");
            path = path.replaceFirst("/(models|chat/completions|embeddings)$", "");
            if (!path.endsWith("/v1")) path += "/v1";
            URI normalized = new URI("https", uri.getRawAuthority(), path, null, null).normalize();
            return normalized.toString().replaceAll("/+$", "");
        } catch (Exception e) {
            throw new BusinessException(400, "API地址格式不正确，请填写HTTPS Base URL，例如 https://api.example.com/v1");
        }
    }

    private static void ensurePublicEndpoint(URI endpoint) {
        try {
            for (InetAddress address : InetAddress.getAllByName(endpoint.getHost())) {
                byte[] raw = address.getAddress();
                boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress() || uniqueLocalV6) {
                    throw new BusinessException(400, "为保护服务器安全，API地址不能指向本机或内网");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "无法解析API地址", e);
        }
    }

    private static ApiEndpoint springEndpoint(String apiUrl) {
        String baseUrl = apiUrl.substring(0, apiUrl.length() - 3);
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return new ApiEndpoint(baseUrl, "/v1");
    }

    private static String chooseChatModel(List<String> models, String preferred) {
        if (!preferred.isEmpty() && models.contains(preferred) && !isEmbeddingModel(preferred)) return preferred;
        // shortcut: model metadata is not standardized, so choose from names until providers expose capabilities.
        return models.stream()
                .filter(model -> !isEmbeddingModel(model) && !isNonChatModel(model))
                .max(Comparator.comparingInt(AiConfigController::chatScore))
                .orElse("");
    }

    private static String chooseEmbeddingModel(List<String> models, String preferred) {
        if (!preferred.isEmpty() && models.contains(preferred) && isEmbeddingModel(preferred)) return preferred;
        return models.stream()
                .filter(AiConfigController::isEmbeddingModel)
                .max(Comparator.comparingInt(AiConfigController::embeddingScore))
                .orElse("");
    }

    private static int chatScore(String model) {
        String value = model.toLowerCase(Locale.ROOT);
        int score = value.contains("qwen") ? 50 : value.contains("deepseek") ? 45
                : value.contains("glm") ? 40 : value.contains("llama") ? 35
                : value.contains("mistral") ? 30 : value.contains("gpt") ? 25 : 10;
        if (value.contains("instruct")) score += 15;
        if (value.contains("chat")) score += 10;
        if (value.contains("free")) score += 5;
        return score;
    }

    private static int embeddingScore(String model) {
        String value = model.toLowerCase(Locale.ROOT);
        if (value.contains("bge-m3")) return 50;
        if (value.contains("text-embedding")) return 40;
        if (value.contains("embedding")) return 30;
        return 20;
    }

    private static boolean isEmbeddingModel(String model) {
        String value = model.toLowerCase(Locale.ROOT);
        return value.contains("embed") || value.contains("bge") || value.contains("gte-")
                || value.matches(".*(^|[/_-])e5([/_-]|$).*");
    }

    private static boolean isNonChatModel(String model) {
        String value = model.toLowerCase(Locale.ROOT);
        return value.contains("rerank") || value.contains("whisper") || value.contains("tts")
                || value.contains("speech") || value.contains("moderation") || value.contains("image")
                || value.contains("stable-diffusion");
    }

    private static String connectionError(int status) {
        if (status == 401 || status == 403) return "API Key无效或无访问权限";
        if (status == 402) return "API账户余额不足或已欠费";
        if (status == 429) return "API请求过于频繁或额度已用尽";
        return "API返回状态码 " + status;
    }

    private static String validModel(String value, String fallback) {
        String model = trim(value);
        if (model.isEmpty()) return fallback;
        if (!MODEL_PATTERN.matcher(model).matches()) throw new BusinessException(400, "模型名称格式不正确");
        return model;
    }

    private static String mask(String key) {
        if (key.isEmpty()) return "";
        return key.length() <= 4 ? "****" : "**** " + key.substring(key.length() - 4);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public record AiConfigRequest(Boolean enabled, String apiUrl, String apiKey,
                                  String chatModel, String embeddingModel) {}
    public record AiConfigView(String provider, boolean enabled, boolean runtimeEnabled,
                               boolean apiKeyConfigured, String maskedApiKey, String apiUrl,
                               String chatModel, String embeddingModel, boolean restartRequired,
                               String updatedAt) {}
    public record TestResult(boolean success, String message, long latencyMs, List<String> models,
                             String chatModel, String embeddingModel) {}
    private record ModelSelection(List<String> models, String chatModel, String embeddingModel) {}
    private record ApiEndpoint(String baseUrl, String apiPath) {}
}
