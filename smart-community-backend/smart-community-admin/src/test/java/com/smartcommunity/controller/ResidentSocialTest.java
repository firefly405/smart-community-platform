package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.*;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.*;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResidentSocialTest {
    private final SocialPostService posts = mock(SocialPostService.class);
    private final SocialCommentService comments = mock(SocialCommentService.class);
    private final SocialSecondhandService items = mock(SocialSecondhandService.class);
    private final SysUserService users = mock(SysUserService.class);
    private final ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ResidentSocialController controller = new ResidentSocialController(posts, comments, items, users, json);
    @BeforeAll static void tableInfo() {
        for (Class<?> type : List.of(SocialPost.class, SocialComment.class, SocialSecondhand.class)) TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), type);
    }
    @BeforeEach void setup() {
        UserContext.setUserId(13L); SysUser user = new SysUser(); user.setId(13L); user.setUserType(1); user.setStatus(1); when(users.getById(13L)).thenReturn(user);
        SocialPost post = new SocialPost(); post.setId(1L); post.setUserId(14L); post.setStatus(1); when(posts.getById(1L)).thenReturn(post);
    }
    @AfterEach void clear() { UserContext.clear(); }
    private ResidentSocialController.ItemInput item() {
        return new ResidentSocialController.ItemInput("Desk", "Good desk", 1, new BigDecimal("80.00"), null, 2, List.of(), "Owner", "13800138000", 1);
    }
    @Test void forgedPostIdentityStatusAndTopAreIgnored() throws Exception {
        controller.createPost(json.readValue("""
                {"title":"Hello","content":"Shared content","category":2,"userId":1,"status":2,"isTop":1,"id":88}
                """, ResidentSocialController.PostInput.class));
        ArgumentCaptor<SocialPost> capture = ArgumentCaptor.forClass(SocialPost.class); verify(posts).save(capture.capture());
        assertEquals(13L, capture.getValue().getUserId()); assertEquals(1, capture.getValue().getStatus()); assertEquals(0, capture.getValue().getIsTop()); assertNull(capture.getValue().getId()); assertNotNull(capture.getValue().getPostNo());
    }
    @Test void otherAuthorsPostsCannotBeEditedOrDeleted() {
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.editPost(1L, new ResidentSocialController.PostInput("A", "B", 1, List.of()))).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.deletePost(1L)).getCode());
        verify(posts, never()).update(any(LambdaUpdateWrapper.class)); verify(posts, never()).remove(any(LambdaQueryWrapper.class));
    }
    @Test void hiddenOwnPostCannotBeRepublishedByEditing() {
        SocialPost hidden = new SocialPost(); hidden.setId(2L); hidden.setUserId(13L); hidden.setStatus(0); when(posts.getById(2L)).thenReturn(hidden);
        assertThrows(BusinessException.class, () -> controller.editPost(2L, new ResidentSocialController.PostInput("A", "B", 1, List.of())));
        verify(posts, never()).update(any(LambdaUpdateWrapper.class));
    }
    @Test void hiddenPostIsNotReadableByOtherResidents() {
        SocialPost hidden = new SocialPost(); hidden.setUserId(14L); hidden.setStatus(0); when(posts.getById(2L)).thenReturn(hidden);
        assertEquals(404, assertThrows(BusinessException.class, () -> controller.post(2L)).getCode());
    }
    @Test void crossPostReplyIsRejected() {
        SocialComment parent = new SocialComment(); parent.setPostId(99L); parent.setStatus(1); when(comments.getById(3L)).thenReturn(parent);
        assertThrows(BusinessException.class, () -> controller.comment(1L, new ResidentSocialController.CommentInput("Reply", 3L)));
        verify(comments, never()).save(any());
    }
    @Test void commentUsesCurrentUserNotForgedAuthor() throws Exception {
        when(posts.update(any(LambdaUpdateWrapper.class))).thenReturn(true);
        controller.comment(1L, json.readValue("""
                {"content":"Nice post","userId":1,"userName":"Administrator","postId":99,"status":0}
                """, ResidentSocialController.CommentInput.class));
        ArgumentCaptor<SocialComment> capture = ArgumentCaptor.forClass(SocialComment.class); verify(comments).save(capture.capture());
        assertEquals(13L, capture.getValue().getUserId()); assertEquals(1L, capture.getValue().getPostId()); assertEquals(1, capture.getValue().getStatus()); assertNull(capture.getValue().getUserName());
    }
    @Test void otherPersonsCommentsCannotBeDeleted() {
        SocialComment comment = new SocialComment(); comment.setUserId(14L); when(comments.getById(2L)).thenReturn(comment);
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.deleteComment(2L)).getCode()); verify(comments, never()).remove(any(LambdaQueryWrapper.class));
    }
    @Test void commentEntityMatchesTableWithoutInventedAuditColumns() {
        var fields = TableInfoHelper.getTableInfo(SocialComment.class).getFieldList().stream().map(field -> field.getColumn()).toList();
        assertFalse(fields.contains("user_name")); assertFalse(fields.contains("update_time")); assertFalse(fields.contains("update_by")); assertTrue(fields.contains("create_time"));
    }
    @Test void newItemStartsForSaleAndUsesCurrentUser() {
        controller.createItem(item()); ArgumentCaptor<SocialSecondhand> capture = ArgumentCaptor.forClass(SocialSecondhand.class); verify(items).save(capture.capture());
        assertEquals(13L, capture.getValue().getUserId()); assertEquals(0, capture.getValue().getStatus()); assertNotNull(capture.getValue().getItemNo());
    }
    @Test void otherPersonsItemCannotBeEditedDeletedOrReadAfterRemoval() {
        SocialSecondhand other = new SocialSecondhand(); other.setUserId(14L); other.setStatus(0); when(items.getById(1L)).thenReturn(other);
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.editItem(1L, item())).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.deleteItem(1L)).getCode());
        other.setStatus(2); assertEquals(404, assertThrows(BusinessException.class, () -> controller.item(1L)).getCode());
    }
    @Test void itemCannotBeRestoredFromSoldOrRemovedStatus() {
        assertThrows(BusinessException.class, () -> controller.itemStatus(1L, new ResidentSocialController.ItemState(0))); verifyNoInteractions(items);
    }
    @Test void publicItemListsHideSellerContactsAndRemovedItems() {
        SocialSecondhand item = new SocialSecondhand(); item.setId(1L); item.setUserId(14L); item.setContactPhone("13800138000");
        Page<SocialSecondhand> page = new Page<>(); page.setRecords(List.of(item)); when(items.page(any(Page.class), any())).thenReturn(page); when(users.listByIds(any())).thenReturn(List.of());
        var result = controller.items(1, 12, false, null, "").getData(); assertNull(result.getRecords().get(0).contactPhone());
        ArgumentCaptor<LambdaQueryWrapper<SocialSecondhand>> capture = ArgumentCaptor.forClass(LambdaQueryWrapper.class); verify(items).page(any(Page.class), capture.capture());
        assertTrue(capture.getValue().getSqlSegment().contains("status =")); assertTrue(capture.getValue().getParamNameValuePairs().containsValue(0));
    }
    @Test void mineFilterUsesCurrentAccount() {
        when(items.page(any(Page.class), any())).thenReturn(new Page<SocialSecondhand>()); controller.items(1, 12, true, null, "");
        ArgumentCaptor<LambdaQueryWrapper<SocialSecondhand>> capture = ArgumentCaptor.forClass(LambdaQueryWrapper.class); verify(items).page(any(Page.class), capture.capture());
        assertTrue(capture.getValue().getSqlSegment().contains("user_id =")); assertTrue(capture.getValue().getParamNameValuePairs().containsValue(13L));
    }
    @Test void invalidImageAndLargePageAreRejected() {
        assertThrows(BusinessException.class, () -> controller.createPost(new ResidentSocialController.PostInput("A", "B", 2, List.of("javascript:alert(1)"))));
        assertThrows(BusinessException.class, () -> controller.posts(1, 1000, false, null, ""));
        assertThrows(BusinessException.class, () -> controller.image(new MockMultipartFile("file", "test.png", "image/png", new byte[] { 1, 2, 3 })));
        verify(posts, never()).save(any());
    }
}
