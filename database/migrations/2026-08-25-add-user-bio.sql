-- 个人寄语允许为空；默认展示文案由前端统一维护，避免把展示默认值复制到每个用户记录。
ALTER TABLE `user`
    ADD COLUMN `bio` VARCHAR(200) NULL COMMENT '个人寄语' AFTER `avatar`;
