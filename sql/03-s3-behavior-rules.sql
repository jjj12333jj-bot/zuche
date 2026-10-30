-- =============================================================
-- S3：信用行为模型参数（不新增表，全部落在 credit_rule）
--
-- 设计说明：
--   事件分（ORDER_FINISH / ON_TIME_RETURN / OVERDUE / OVERDUE_RETURN / CANCEL_PAID）
--   解决"单次行为值多少分"；行为分（BEHAVIOR_*）解决"长期行为特征值多少分"。
--   两者都存 credit_rule，Java 里不出现任何分值字面量。
--
-- 行为分模型（只罚不奖，0 分为满分）：
--   统计窗口 = 最近 WINDOW 笔已支付订单（看近期表现，而不是一辈子的平均）
--   onTimeRate = 窗口内按时归还订单数 / 窗口内已完成订单数
--   cancelRate = 窗口内支付后取消订单数 / 窗口内已支付订单数
--   raw = W_ON_TIME × onTimeRate
--       + W_CANCEL  × (1 - cancelRate)
--       - W_OVERDUE × 窗口内逾期订单数
--       - (W_ON_TIME + W_CANCEL)
--   behaviorTarget = clamp(raw, -FLOOR, 0)
--
--   全按时 + 零取消 + 零逾期 → raw = 0（不奖不罚）
--   任何一项变差 → raw 为负，最低 -FLOOR
--
-- 为什么用滚动窗口：用户有 50 单历史时，再取消 1 单只让取消率涨 2%，
-- 折算成 0.4 分，四舍五入就没了——"人变坏了系统没反应"。限定近 10 单后，
-- 1 次取消 = 10% 取消率，直接反映到行为分上，这才叫实时风控。
--
-- 幂等：用 uk_type_code(rule_type, rule_code) 保证重复执行不产生脏数据
-- =============================================================

INSERT INTO credit_rule
  (rule_type, rule_code, rule_name, min_score, max_score, deposit_coefficient,
   score_value, max_times, color, description, sort_no, status)
VALUES
  ('ACTION', 'BEHAVIOR_ON_TIME', '按时归还率权重', NULL, NULL, NULL, 20, 1, NULL,
   '行为模型参数：按时归还率每低 10% 约扣 2 分，权重 20', 21, 1),
  ('ACTION', 'BEHAVIOR_CANCEL',  '取消率权重',     NULL, NULL, NULL, 20, 1, NULL,
   '行为模型参数：支付后取消率每高 10% 约扣 2 分，权重 20', 22, 1),
  ('ACTION', 'BEHAVIOR_OVERDUE', '逾期次数权重',   NULL, NULL, NULL, 3,  1, NULL,
   '行为模型参数：每个逾期订单在行为分里再扣 3 分', 23, 1),
  ('ACTION', 'BEHAVIOR_FLOOR',   '行为分下限',     NULL, NULL, NULL, 20, 1, NULL,
   '行为模型参数：行为分最多扣到 -20 分，避免一次失误把分数打死', 24, 1),
  ('ACTION', 'BEHAVIOR_WINDOW',  '行为统计窗口',   NULL, NULL, NULL, 10, 1, NULL,
   '行为模型参数：只统计最近 10 笔已支付订单，反映近期表现', 25, 1),
  ('ACTION', 'BEHAVIOR_ADJUST',  '行为评分校准',   NULL, NULL, NULL, 0,  1, NULL,
   '系统按行为特征重算后写入的差额，展示在信用明细里', 26, 1)
ON DUPLICATE KEY UPDATE
  rule_name    = VALUES(rule_name),
  score_value  = VALUES(score_value),
  description  = VALUES(description),
  sort_no      = VALUES(sort_no),
  status       = 1;

-- 历史数据对齐：早期的人工调整事件没有订单号，dedup_key 全是空串。
-- MySQL 唯一索引对 NULL 不生效（(NULL,'ADMIN_ADJUST','') 可以重复插入），
-- 这些行在库里是"可区分的独立调整"，只是键值撞在一起，这里补成唯一键。
UPDATE credit_log
   SET dedup_key = CONCAT('legacy-adj-', id)
 WHERE order_id IS NULL
   AND (dedup_key IS NULL OR dedup_key = '');

-- 自检：应输出 6 行
SELECT rule_code, rule_name, score_value
FROM credit_rule
WHERE rule_type = 'ACTION' AND rule_code LIKE 'BEHAVIOR%'
ORDER BY sort_no;
