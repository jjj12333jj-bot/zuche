-- =====================================================================
-- S4 补丁：逾期风控与后台信用管理
--   1) credit_rule 增加第三类规则 PARAM（风控参数），不新增表
--   2) 新增 APPEAL_RESTORE 事件（申诉通过补分）
--   3) 违约金单价、预警/拉黑阈值全部参数化，Java 里不写死任何数值
--
-- 执行方式（可重复执行，已存在的规则会被更新值而不是报错）：
--   mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 zuche < sql/04-s4-risk-control.sql
-- =====================================================================

USE zuche;

-- ---------------------------------------------------------------------
-- 1. 扩展 rule_type 语义：LEVEL 等级 / ACTION 事件 / PARAM 参数
--    仅改注释，字段类型不变
-- ---------------------------------------------------------------------
ALTER TABLE credit_rule
  MODIFY COLUMN rule_type VARCHAR(20) NOT NULL
  COMMENT '规则类型 LEVEL等级规则/ACTION行为规则/PARAM风控参数';

-- ---------------------------------------------------------------------
-- 2. 风控参数（PARAM）
--    score_value = 参数值，max_times = 该参数的封顶次数（如违约金最多计 30 天）
--    这些值原来散落在 Java 常量里，S4 统一收口到配置表，后台可直接改
-- ---------------------------------------------------------------------
INSERT INTO credit_rule
  (rule_type, rule_code, rule_name, min_score, max_score, deposit_coefficient,
   score_value, max_times, color, description, sort_no, status)
VALUES
  ('PARAM', 'LATE_FEE_DAILY',        '逾期违约金单价', NULL, NULL, NULL, 200, 30, NULL,
   '逾期每天从押金中扣除的违约金（元/天），累计不超过押金；max_times 为最多计费天数', 31, 1),
  ('PARAM', 'WARN_OVERDUE_DAYS',     '逾期预警天数',   NULL, NULL, NULL, 3,   1, NULL,
   '逾期达到该天数写入风控预警名单（WARNING），只提示不限制下单', 32, 1),
  ('PARAM', 'BLACKLIST_OVERDUE_DAYS','严重逾期天数',   NULL, NULL, NULL, 7,   1, NULL,
   '逾期达到该天数直接列入黑名单（BLACKLIST），限制下单', 33, 1),
  ('PARAM', 'BLACKLIST_SCORE',       '低分拉黑阈值',   NULL, NULL, NULL, 60,  1, NULL,
   '信用分低于该值时列入黑名单，0 表示关闭该规则', 34, 1),
  ('PARAM', 'BLACKLIST_EXPIRE_DAYS', '黑名单有效期',   NULL, NULL, NULL, 90,  1, NULL,
   '系统自动列入黑名单的默认有效天数，到期后由定时任务自动解除', 35, 1)
ON DUPLICATE KEY UPDATE
  rule_name   = VALUES(rule_name),
  score_value = VALUES(score_value),
  max_times   = VALUES(max_times),
  description = VALUES(description),
  sort_no     = VALUES(sort_no),
  status      = 1;

-- ---------------------------------------------------------------------
-- 3. 申诉通过补分事件
--    规则分值为 0：实际补多少由申诉单的 restore_score 决定，
--    走 CreditEvent.scoreChange 覆盖规则分值，但仍然落 credit_log
-- ---------------------------------------------------------------------
INSERT INTO credit_rule
  (rule_type, rule_code, rule_name, min_score, max_score, deposit_coefficient,
   score_value, max_times, color, description, sort_no, status)
VALUES
  ('ACTION', 'APPEAL_RESTORE', '申诉复核补分', NULL, NULL, NULL, 0, 1, NULL,
   '信用申诉通过后补回的分值，取自申诉单 restore_score', 17, 1)
ON DUPLICATE KEY UPDATE
  rule_name   = VALUES(rule_name),
  score_value = VALUES(score_value),
  description = VALUES(description),
  status      = 1;

-- ---------------------------------------------------------------------
-- 4. 历史数据归一化
--    种子数据里的申诉单 3 是"已通过"，但当年没有 credit_log 概念，
--    补分并未真正落日志。S4 起申诉补分必须与 credit_log 一一对应，
--    这里把它标成"已在当期校准中恢复"，避免出现"通过但查不到补分记录"的悬空数据。
-- ---------------------------------------------------------------------
UPDATE credit_appeal
   SET restore_score = 0,
       handle_remark = '申诉通过，扣分已在当期信用校准中恢复，不重复补分'
 WHERE id = 3 AND status = 'APPROVED' AND restore_score > 0;

-- ---------------------------------------------------------------------
-- 5. 自检
-- ---------------------------------------------------------------------
SELECT 'PARAM 参数条数（应为 5）' AS 检查项, COUNT(*) AS 数量
FROM credit_rule WHERE rule_type = 'PARAM' AND deleted = 0;

SELECT rule_code, rule_name, score_value, max_times
FROM credit_rule WHERE rule_type = 'PARAM' AND deleted = 0 ORDER BY sort_no;

SELECT '申诉补分规则' AS 检查项, COUNT(*) AS 数量
FROM credit_rule WHERE rule_type = 'ACTION' AND rule_code = 'APPEAL_RESTORE' AND deleted = 0;
