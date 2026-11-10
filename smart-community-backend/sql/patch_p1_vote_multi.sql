-- P1 C-12: 投票记录支持多选（同一用户可对多选项各记一票）
-- 若仍使用旧唯一键 uk_vote_user，后端已做兼容（只写一条主记录）
ALTER TABLE `social_vote_record` DROP INDEX `uk_vote_user`;
ALTER TABLE `social_vote_record` ADD UNIQUE KEY `uk_vote_user_option` (`vote_id`, `user_id`, `option_id`);
