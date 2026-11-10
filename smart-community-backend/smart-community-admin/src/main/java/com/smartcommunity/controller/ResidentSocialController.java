package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.*;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/resident/social")
@RequiredArgsConstructor
public class ResidentSocialController {
    private final SocialPostService posts;
    private final SocialCommentService comments;
    private final SocialSecondhandService items;
    private final SysUserService users;
    private final ObjectMapper json;
    @Value("${app.upload-dir:uploads}") private String uploadDir;

    private SysUser current() {
        SysUser user = users.getById(UserContext.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getUserType()) || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(403, "仅普通业主可使用社区互动");
        return user;
    }
    private void paging(int page, int size) {
        if (page < 1 || size < 1 || size > 30) throw new BusinessException(400, "分页参数无效");
    }
    private String text(String value, int max, String name) {
        if (value == null || value.isBlank() || value.length() > max) throw new BusinessException(400, name + "不能为空且不能超过" + max + "字");
        return value.trim();
    }
    private void category(Integer value) {
        if (value == null || value < 1 || value > 6) throw new BusinessException(400, "请选择有效分类");
    }
    private String images(List<String> values) {
        if (values == null) return "[]";
        if (values.size() > 5 || values.stream().anyMatch(url -> url == null || !url.matches("/uploads/community/[a-f0-9-]+\\.(jpg|png)"))) throw new BusinessException(400, "最多上传5张社区图片");
        try {
            String result = json.writeValueAsString(values);
            if (result.length() > 1000) throw new BusinessException(400, "图片地址过长");
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new BusinessException(400, "图片参数无效"); }
    }
    private Map<Long, String> names(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return users.listByIds(ids).stream().collect(Collectors.toMap(SysUser::getId,
                user -> user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName()));
    }
    private SocialPost readablePost(Long id, Long userId) {
        SocialPost post = posts.getById(id);
        if (post == null || (!Integer.valueOf(1).equals(post.getStatus()) && !userId.equals(post.getUserId()))) throw new BusinessException(404, "帖子不存在或不可见");
        return post;
    }
    private void published(SocialPost post) {
        if (!Integer.valueOf(1).equals(post.getStatus())) throw new BusinessException(400, "此帖已隐藏或不可操作");
    }
    public record PostInput(String title, String content, Integer category, List<String> images) {}
    public record PostView(Long id, String title, String content, String category, String images, Long userId,
                           String userName, Integer isTop, long commentCount, Integer status, LocalDateTime createTime) {}
    private PostView view(SocialPost post, String name, long count) {
        return new PostView(post.getId(), post.getTitle(), post.getContent(), post.getCategory(), post.getImages(), post.getUserId(), name, post.getIsTop(), count, post.getStatus(), post.getCreateTime());
    }
    @GetMapping("/posts")
    public Result<Page<PostView>> posts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "false") boolean mine, @RequestParam(required = false) Integer category, @RequestParam(defaultValue = "") String keyword) {
        SysUser user = current(); paging(page, pageSize);
        if (category != null) category(category);
        if (keyword.length() > 100) throw new BusinessException(400, "搜索词过长");
        LambdaQueryWrapper<SocialPost> query = new LambdaQueryWrapper<SocialPost>().eq(mine, SocialPost::getUserId, user.getId())
                .eq(!mine, SocialPost::getStatus, 1).eq(category != null, SocialPost::getCategory, category)
                .like(!keyword.isBlank(), SocialPost::getTitle, keyword.trim()).orderByDesc(SocialPost::getIsTop, SocialPost::getId);
        Page<SocialPost> result = posts.page(new Page<>(page, pageSize), query);
        Map<Long, String> authors = names(result.getRecords().stream().map(SocialPost::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, Long> counts = new HashMap<>();
        if (!result.getRecords().isEmpty()) comments.listMaps(new QueryWrapper<SocialComment>().select("post_id", "COUNT(*) AS total")
                .in("post_id", result.getRecords().stream().map(SocialPost::getId).toList()).eq("status", 1).groupBy("post_id"))
                .forEach(row -> counts.put(((Number) row.get("post_id")).longValue(), ((Number) row.get("total")).longValue()));
        Page<PostView> output = new Page<>(page, pageSize, result.getTotal());
        output.setRecords(result.getRecords().stream().map(post -> view(post, authors.getOrDefault(post.getUserId(), "社区邻居"), counts.getOrDefault(post.getId(), 0L))).toList());
        return Result.success(output);
    }
    @GetMapping("/posts/{id}")
    public Result<PostView> post(@PathVariable Long id) {
        SocialPost post = readablePost(id, current().getId());
        return Result.success(view(post, names(Set.of(post.getUserId())).getOrDefault(post.getUserId(), "社区邻居"),
                comments.count(new LambdaQueryWrapper<SocialComment>().eq(SocialComment::getPostId, id).eq(SocialComment::getStatus, 1))));
    }
    @PostMapping("/posts")
    public Result<Long> createPost(@RequestBody PostInput input) {
        SysUser user = current(); category(input.category());
        SocialPost post = new SocialPost();
        post.setPostNo("TZ" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        post.setUserId(user.getId()); post.setTitle(text(input.title(), 200, "标题")); post.setContent(text(input.content(), 5000, "正文"));
        post.setCategory(input.category().toString()); post.setImages(images(input.images())); post.setStatus(1); post.setIsTop(0);
        post.setCommentCount(0); post.setViewCount(0); post.setLikeCount(0);
        posts.save(post); return Result.success(post.getId());
    }
    @PutMapping("/posts/{id}")
    public Result<Void> editPost(@PathVariable Long id, @RequestBody PostInput input) {
        Long userId = current().getId(); SocialPost post = readablePost(id, userId);
        if (!userId.equals(post.getUserId())) throw new BusinessException(403, "只能编辑自己的帖子");
        published(post); category(input.category());
        boolean changed = posts.update(new LambdaUpdateWrapper<SocialPost>().eq(SocialPost::getId, id).eq(SocialPost::getUserId, userId).eq(SocialPost::getStatus, 1)
                .set(SocialPost::getTitle, text(input.title(), 200, "标题")).set(SocialPost::getContent, text(input.content(), 5000, "正文"))
                .set(SocialPost::getCategory, input.category()).set(SocialPost::getImages, images(input.images())));
        if (!changed) throw new BusinessException(409, "帖子状态已变化，请刷新");
        return Result.success();
    }
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long userId = current().getId(); SocialPost post = readablePost(id, userId);
        if (!userId.equals(post.getUserId())) throw new BusinessException(403, "只能删除自己的帖子");
        posts.remove(new LambdaQueryWrapper<SocialPost>().eq(SocialPost::getId, id).eq(SocialPost::getUserId, userId));
        return Result.success();
    }
    public record CommentInput(String content, Long parentId) {}
    public record CommentView(Long id, Long parentId, String content, Long userId, String userName, LocalDateTime createTime) {}
    @GetMapping("/posts/{id}/comments")
    public Result<Page<CommentView>> comments(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        SysUser user = current(); paging(page, pageSize); SocialPost post = readablePost(id, user.getId()); published(post);
        Page<SocialComment> result = comments.page(new Page<>(page, pageSize), new LambdaQueryWrapper<SocialComment>()
                .eq(SocialComment::getPostId, id).eq(SocialComment::getStatus, 1).orderByAsc(SocialComment::getId));
        Map<Long, String> authors = names(result.getRecords().stream().map(SocialComment::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Page<CommentView> output = new Page<>(page, pageSize, result.getTotal());
        output.setRecords(result.getRecords().stream().map(row -> new CommentView(row.getId(), row.getParentId(), row.getContent(), row.getUserId(), authors.getOrDefault(row.getUserId(), "社区邻居"), row.getCreateTime())).toList());
        return Result.success(output);
    }
    @PostMapping("/posts/{id}/comments")
    @Transactional
    public Result<Long> comment(@PathVariable Long id, @RequestBody CommentInput input) {
        SysUser user = current(); SocialPost post = readablePost(id, user.getId()); published(post);
        SocialComment comment = new SocialComment(); comment.setContent(text(input.content(), 1000, "评论"));
        if (input.parentId() != null) {
            SocialComment parent = comments.getById(input.parentId());
            if (parent == null || !id.equals(parent.getPostId()) || !Integer.valueOf(1).equals(parent.getStatus())) throw new BusinessException(400, "回复目标不存在或不属于此帖");
            comment.setParentId(parent.getId());
        }
        comment.setPostId(id); comment.setUserId(user.getId()); comment.setStatus(1); comment.setCreateTime(LocalDateTime.now()); comment.setCreateBy(user.getId()); comment.setDeleted(0);
        comments.save(comment);
        if (!posts.update(new LambdaUpdateWrapper<SocialPost>().eq(SocialPost::getId, id).eq(SocialPost::getStatus, 1).setSql("comment_count = COALESCE(comment_count, 0) + 1"))) throw new BusinessException(409, "帖子已不可评论");
        return Result.success(comment.getId());
    }
    @DeleteMapping("/comments/{id}")
    @Transactional
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = current().getId(); SocialComment comment = comments.getById(id);
        if (comment == null) throw new BusinessException(404, "评论不存在");
        if (!userId.equals(comment.getUserId())) throw new BusinessException(403, "只能删除自己的评论");
        if (comments.remove(new LambdaQueryWrapper<SocialComment>().eq(SocialComment::getId, id).eq(SocialComment::getUserId, userId)))
            posts.update(new LambdaUpdateWrapper<SocialPost>().eq(SocialPost::getId, comment.getPostId()).setSql("comment_count = GREATEST(COALESCE(comment_count, 0) - 1, 0)"));
        return Result.success();
    }
    public record ItemInput(String title, String description, Integer category, BigDecimal price, BigDecimal originPrice,
                            Integer conditionLevel, List<String> images, String contactName, String contactPhone, Integer tradeMethod) {}
    public record ItemView(Long id, String title, String description, Integer category, BigDecimal price, BigDecimal originPrice,
                           Integer conditionLevel, String images, String contactName, String contactPhone, Integer tradeMethod,
                           Long userId, String userName, Integer status, LocalDateTime createTime) {}
    private ItemView view(SocialSecondhand item, String name, boolean contact) {
        return new ItemView(item.getId(), item.getTitle(), item.getDescription(), item.getCategory(), item.getPrice(), item.getOriginPrice(), item.getConditionLevel(), item.getImages(),
                contact ? item.getContactName() : null, contact ? item.getContactPhone() : null, item.getTradeMethod(), item.getUserId(), name, item.getStatus(), item.getCreateTime());
    }
    @GetMapping("/items")
    public Result<Page<ItemView>> items(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "false") boolean mine, @RequestParam(required = false) Integer category, @RequestParam(defaultValue = "") String keyword) {
        SysUser user = current(); paging(page, pageSize); if (category != null) category(category);
        if (keyword.length() > 100) throw new BusinessException(400, "搜索词过长");
        Page<SocialSecondhand> result = items.page(new Page<>(page, pageSize), new LambdaQueryWrapper<SocialSecondhand>()
                .eq(mine, SocialSecondhand::getUserId, user.getId()).eq(!mine, SocialSecondhand::getStatus, 0)
                .eq(category != null, SocialSecondhand::getCategory, category).like(!keyword.isBlank(), SocialSecondhand::getTitle, keyword.trim()).orderByDesc(SocialSecondhand::getId));
        Map<Long, String> authors = names(result.getRecords().stream().map(SocialSecondhand::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Page<ItemView> output = new Page<>(page, pageSize, result.getTotal());
        output.setRecords(result.getRecords().stream().map(item -> view(item, authors.getOrDefault(item.getUserId(), "社区邻居"), false)).toList());
        return Result.success(output);
    }
    private SocialSecondhand readableItem(Long id, Long userId) {
        SocialSecondhand item = items.getById(id);
        if (item == null || (!Integer.valueOf(0).equals(item.getStatus()) && !userId.equals(item.getUserId()))) throw new BusinessException(404, "物品不存在或已下架");
        return item;
    }
    @GetMapping("/items/{id}")
    public Result<ItemView> item(@PathVariable Long id) {
        SocialSecondhand item = readableItem(id, current().getId());
        return Result.success(view(item, names(Set.of(item.getUserId())).getOrDefault(item.getUserId(), "社区邻居"), true));
    }
    private SocialSecondhand itemInput(ItemInput input) {
        category(input.category());
        if (input.price() == null || input.price().signum() < 0 || input.price().compareTo(new BigDecimal("99999999.99")) > 0 || input.price().scale() > 2
                || input.originPrice() != null && (input.originPrice().signum() < 0 || input.originPrice().compareTo(new BigDecimal("99999999.99")) > 0 || input.originPrice().scale() > 2)
                || input.conditionLevel() == null || input.conditionLevel() < 1 || input.conditionLevel() > 4
                || input.tradeMethod() == null || input.tradeMethod() < 1 || input.tradeMethod() > 2
                || input.contactPhone() == null || !input.contactPhone().matches("^1[3-9]\\d{9}$")) throw new BusinessException(400, "价格、成色、交易方式或联系电话不正确");
        SocialSecondhand item = new SocialSecondhand(); item.setTitle(text(input.title(), 200, "标题")); item.setDescription(text(input.description(), 5000, "物品描述"));
        item.setContactName(text(input.contactName(), 50, "联系人")); item.setContactPhone(input.contactPhone()); item.setCategory(input.category());
        item.setPrice(input.price()); item.setOriginPrice(input.originPrice()); item.setConditionLevel(input.conditionLevel()); item.setTradeMethod(input.tradeMethod()); item.setImages(images(input.images()));
        return item;
    }
    @PostMapping("/items")
    public Result<Long> createItem(@RequestBody ItemInput input) {
        SysUser user = current(); SocialSecondhand item = itemInput(input);
        item.setItemNo("ES" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)); item.setUserId(user.getId()); item.setStatus(0); item.setViewCount(0); item.setFavoriteCount(0);
        items.save(item); return Result.success(item.getId());
    }
    @PutMapping("/items/{id}")
    public Result<Void> editItem(@PathVariable Long id, @RequestBody ItemInput input) {
        Long userId = current().getId(); SocialSecondhand existing = readableItem(id, userId);
        if (!userId.equals(existing.getUserId())) throw new BusinessException(403, "只能编辑自己的物品");
        if (!Integer.valueOf(0).equals(existing.getStatus())) throw new BusinessException(400, "仅在售物品可编辑");
        SocialSecondhand item = itemInput(input);
        item.setOriginPrice(null);
        boolean changed = items.update(item, new LambdaUpdateWrapper<SocialSecondhand>().eq(SocialSecondhand::getId, id).eq(SocialSecondhand::getUserId, userId).eq(SocialSecondhand::getStatus, 0)
                .set(SocialSecondhand::getOriginPrice, input.originPrice()));
        if (!changed) throw new BusinessException(409, "物品状态已变化，请刷新");
        return Result.success();
    }
    public record ItemState(Integer status) {}
    @PutMapping("/items/{id}/status")
    public Result<Void> itemStatus(@PathVariable Long id, @RequestBody ItemState input) {
        Long userId = current().getId();
        if (input.status() == null || !Set.of(1, 2).contains(input.status())) throw new BusinessException(400, "仅可标记售出或下架");
        if (!items.update(new LambdaUpdateWrapper<SocialSecondhand>().eq(SocialSecondhand::getId, id).eq(SocialSecondhand::getUserId, userId).eq(SocialSecondhand::getStatus, 0).set(SocialSecondhand::getStatus, input.status())))
            throw new BusinessException(403, "只能操作本人在售物品");
        return Result.success();
    }
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        Long userId = current().getId(); SocialSecondhand item = readableItem(id, userId);
        if (!userId.equals(item.getUserId())) throw new BusinessException(403, "只能删除自己的物品");
        items.remove(new LambdaQueryWrapper<SocialSecondhand>().eq(SocialSecondhand::getId, id).eq(SocialSecondhand::getUserId, userId));
        return Result.success();
    }
    @PostMapping("/images")
    public Result<Map<String, String>> image(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        current();
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024 || !Set.of("image/jpeg", "image/png").contains(Objects.toString(file.getContentType(), ""))) throw new BusinessException(400, "请上传2MB以内的 JPG 或 PNG 图片");
        try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new BusinessException(400, "图片格式无效");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096 || !Set.of("JPEG", "PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT))) throw new BusinessException(400, "图片边长不能超过4096像素");
            } finally { reader.dispose(); }
        }
        String filename = UUID.randomUUID() + ("image/png".equals(file.getContentType()) ? ".png" : ".jpg");
        Path directory = Path.of(uploadDir, "community").toAbsolutePath().normalize(); Files.createDirectories(directory);
        try (var stream = file.getInputStream()) { Files.copy(stream, directory.resolve(filename)); }
        // shortcut: unreferenced uploads are retained; add periodic cleanup when storage usage warrants it.
        return Result.success(Map.of("url", "/uploads/community/" + filename));
    }
}
