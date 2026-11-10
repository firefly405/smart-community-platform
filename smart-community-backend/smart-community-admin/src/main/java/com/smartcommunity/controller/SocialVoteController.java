package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SocialVote;
import com.smartcommunity.entity.SocialVoteOption;
import com.smartcommunity.entity.SocialVoteRecord;
import com.smartcommunity.service.SocialVoteOptionService;
import com.smartcommunity.service.SocialVoteRecordService;
import com.smartcommunity.service.SocialVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Tag(name = "社区互动-投票")
@RestController
@RequestMapping("/api/v1/vote")
@RequiredArgsConstructor
public class SocialVoteController {

    private final SocialVoteService voteService;
    private final SocialVoteRecordService recordService;
    private final SocialVoteOptionService optionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/list")
    public Result<Page<SocialVote>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title) {
        LambdaQueryWrapper<SocialVote> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(SocialVote::getTitle, title);
        qw.orderByDesc(SocialVote::getId);
        Page<SocialVote> result = voteService.page(new Page<>(page, pageSize), qw);
        for (SocialVote vote : result.getRecords()) {
            fillCompat(vote);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<SocialVote> getById(@PathVariable Long id) {
        SocialVote vote = voteService.getById(id);
        if (vote != null) {
            fillCompat(vote);
            vote.setOptionList(listOptions(id));
        }
        return Result.success(vote);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> create(@RequestBody SocialVote entity) {
        normalizeVote(entity);
        if (entity.getVoteNo() == null || entity.getVoteNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
            entity.setVoteNo("VT" + ts + rnd);
        }
        if (entity.getUserId() == null) {
            entity.setUserId(1L);
        }
        if (entity.getTotalVotes() == null) {
            entity.setTotalVotes(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getStartTime() == null) {
            entity.setStartTime(LocalDateTime.now());
        }
        if (entity.getEndTime() == null) {
            entity.setEndTime(LocalDateTime.now().plusDays(7));
        }
        voteService.save(entity);
        saveOptions(entity.getId(), entity.getOptions(), entity.getOptionList());
        return Result.success();
    }

    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@RequestBody SocialVote entity) {
        normalizeVote(entity);
        voteService.updateById(entity);
        if (entity.getId() != null && (entity.getOptions() != null || entity.getOptionList() != null)) {
            optionService.remove(new LambdaQueryWrapper<SocialVoteOption>().eq(SocialVoteOption::getVoteId, entity.getId()));
            saveOptions(entity.getId(), entity.getOptions(), entity.getOptionList());
        }
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SocialVote vote = voteService.getById(id);
        if (vote == null) {
            throw new BusinessException(404, "投票不存在");
        }
        if (vote.getStatus() != null && (vote.getStatus() == 0 || vote.getStatus() == 2)) {
            throw new BusinessException("投票已结束或未开始");
        }
        List<Long> optionIds = new ArrayList<>();
        if (body != null && body.get("optionIds") instanceof List<?> rawList) {
            for (Object o : rawList) {
                if (o instanceof Number n) {
                    optionIds.add(n.longValue());
                } else if (o != null) {
                    optionIds.add(Long.parseLong(String.valueOf(o)));
                }
            }
        }
        if (optionIds.isEmpty()) {
            throw new BusinessException("请选择投票选项");
        }
        boolean multi = vote.getVoteType() != null && vote.getVoteType() == 2;
        if (!multi && optionIds.size() > 1) {
            throw new BusinessException("单选投票只能选一项");
        }
        Long userId = 1L;
        String userName = "演示用户";
        if (body != null) {
            if (body.get("userId") instanceof Number n) {
                userId = n.longValue();
            } else if (body.get("userId") != null) {
                userId = Long.parseLong(String.valueOf(body.get("userId")));
            }
            if (body.get("userName") != null && !String.valueOf(body.get("userName")).isBlank()) {
                userName = String.valueOf(body.get("userName"));
            }
        }
        long voted = recordService.count(new LambdaQueryWrapper<SocialVoteRecord>()
                .eq(SocialVoteRecord::getVoteId, id)
                .eq(SocialVoteRecord::getUserId, userId));
        if (voted > 0) {
            throw new BusinessException("您已参与过该投票");
        }
        // multi-select: only one record due to uk_vote_user; primary option stored, all options count++
        Long primaryOptionId = optionIds.get(0);
        String optionRemark = optionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        for (Long optionId : optionIds) {
            SocialVoteOption option = optionService.getById(optionId);
            if (option == null || option.getVoteId() == null || !option.getVoteId().equals(id)) {
                throw new BusinessException("无效选项: " + optionId);
            }
            option.setVoteCount((option.getVoteCount() == null ? 0 : option.getVoteCount()) + 1);
            optionService.updateById(option);
        }
        SocialVoteRecord record = new SocialVoteRecord();
        record.setVoteId(id);
        record.setOptionId(primaryOptionId);
        record.setUserId(userId);
        record.setUserName(userName);
        record.setVoteTime(LocalDateTime.now());
        if (optionIds.size() > 1) {
            record.setRemark("options:" + optionRemark);
        }
        recordService.save(record);
        vote.setTotalVotes((vote.getTotalVotes() == null ? 0 : vote.getTotalVotes()) + 1);
        voteService.updateById(vote);
        return Result.success();
    }

    @GetMapping("/{id}/result")
    public Result<SocialVote> result(@PathVariable Long id) {
        SocialVote vote = voteService.getById(id);
        if (vote == null) {
            throw new BusinessException(404, "投票不存在");
        }
        List<SocialVoteOption> options = listOptions(id);
        for (SocialVoteOption opt : options) {
            if (opt.getVoteCount() == null) {
                long c = recordService.count(new LambdaQueryWrapper<SocialVoteRecord>()
                        .eq(SocialVoteRecord::getVoteId, id)
                        .eq(SocialVoteRecord::getOptionId, opt.getId()));
                opt.setVoteCount((int) c);
            }
        }
        fillCompat(vote);
        vote.setOptionList(options);
        return Result.success(vote);
    }

    @GetMapping("/{id}/records")
    public Result<Page<SocialVoteRecord>> records(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SocialVoteRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialVoteRecord::getVoteId, id);
        qw.orderByDesc(SocialVoteRecord::getId);
        return Result.success(recordService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}/options")
    public Result<List<SocialVoteOption>> options(@PathVariable Long id) {
        return Result.success(listOptions(id));
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        optionService.remove(new LambdaQueryWrapper<SocialVoteOption>().eq(SocialVoteOption::getVoteId, id));
        recordService.remove(new LambdaQueryWrapper<SocialVoteRecord>().eq(SocialVoteRecord::getVoteId, id));
        voteService.removeById(id);
        return Result.success();
    }

    private void normalizeVote(SocialVote entity) {
        if (entity.getIsMulti() != null) {
            entity.setVoteType(entity.getIsMulti() == 1 ? 2 : 1);
        }
        if (entity.getVoteType() == null) {
            entity.setVoteType(1);
        }
        if (entity.getIsAnonymous() == null) {
            entity.setIsAnonymous(0);
        }
    }

    private void fillCompat(SocialVote vote) {
        if (vote.getVoteType() != null) {
            vote.setIsMulti(vote.getVoteType() == 2 ? 1 : 0);
        } else {
            vote.setIsMulti(0);
        }
    }

    private List<SocialVoteOption> listOptions(Long voteId) {
        return optionService.list(new LambdaQueryWrapper<SocialVoteOption>()
                .eq(SocialVoteOption::getVoteId, voteId)
                .orderByAsc(SocialVoteOption::getSort)
                .orderByAsc(SocialVoteOption::getId));
    }

    private void saveOptions(Long voteId, String optionsText, List<SocialVoteOption> optionList) {
        List<String> texts = new ArrayList<>();
        if (optionList != null && !optionList.isEmpty()) {
            for (SocialVoteOption o : optionList) {
                if (o.getOptionText() != null && !o.getOptionText().isBlank()) {
                    texts.add(o.getOptionText().trim());
                }
            }
        }
        if (texts.isEmpty() && optionsText != null && !optionsText.isBlank()) {
            String raw = optionsText.trim();
            try {
                if (raw.startsWith("[")) {
                    List<Object> arr = objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
                    for (Object item : arr) {
                        if (item instanceof Map) {
                            Object t = ((Map<?, ?>) item).get("text");
                            if (t == null) t = ((Map<?, ?>) item).get("optionText");
                            if (t == null) t = ((Map<?, ?>) item).get("label");
                            if (t != null) texts.add(String.valueOf(t));
                        } else {
                            texts.add(String.valueOf(item));
                        }
                    }
                } else {
                    texts = Arrays.stream(raw.split("[\r\n,]+"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                texts = Arrays.stream(raw.split("[\r\n,]+"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        }
        int sort = 1;
        for (String text : texts) {
            SocialVoteOption opt = new SocialVoteOption();
            opt.setVoteId(voteId);
            opt.setOptionText(text);
            opt.setVoteCount(0);
            opt.setSort(sort++);
            opt.setCreateTime(LocalDateTime.now());
            optionService.save(opt);
        }
    }
}
