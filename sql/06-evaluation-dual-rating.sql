-- =====================================================================
-- 06 · 评价双维度评分迁移
--
-- 背景：原来的 order_evaluation 只有一个 rating，用户只能给一个总印象打分，
--       分不清"车好但门店服务差"还是"车旧但服务好"。
--       现在拆成 vehicle_rating（车辆状况）与 store_rating（门店服务）两项，
--       rating 保留为两者的平均值，继续给榜单、统计、排序用，
--       这样旧接口和旧报表不用改口径。
--
-- 执行：mysql -uroot -p zuche < sql/06-evaluation-dual-rating.sql
-- 幂等：重复执行会报 Duplicate column，属正常，说明已迁移过。
-- =====================================================================

USE zuche;

ALTER TABLE order_evaluation
  ADD COLUMN vehicle_rating TINYINT NOT NULL DEFAULT 5 COMMENT '车辆状况评分 1-5' AFTER rating,
  ADD COLUMN store_rating   TINYINT NOT NULL DEFAULT 5 COMMENT '门店服务评分 1-5' AFTER vehicle_rating;

-- 存量评价只有总评，两个维度都按总评回填，不让老数据出现 0 分
UPDATE order_evaluation
   SET vehicle_rating = rating,
       store_rating   = rating
 WHERE deleted = 0;

-- 演示数据专用：如果 12 条历史评价两个维度永远相等，页面上就看不出
-- "车好服务差"和"车旧服务好"的区别，双维度等于白拆。
-- 这里让奇数 id 的门店服务分比车况低 1 分（最低 3 分），制造差异。
-- 真实业务数据请跳过这一段。
UPDATE order_evaluation
   SET store_rating = GREATEST(vehicle_rating - 1, 3)
 WHERE id % 2 = 1
   AND deleted = 0;

-- 综合分按两个维度重算，保持 rating 与明细自洽
-- FLOOR(x + 0.5) 与 Java 的 Math.round 一致；MySQL 的 ROUND 对浮点是「五成双」，4.5 会变 4
UPDATE order_evaluation
   SET rating = FLOOR((vehicle_rating + store_rating) / 2 + 0.5)
 WHERE deleted = 0;

SELECT '迁移后分布' AS 指标,
       COUNT(*)                                            AS 评价数,
       ROUND(AVG(rating), 2)                               AS 综合均分,
       ROUND(AVG(vehicle_rating), 2)                       AS 车况均分,
       ROUND(AVG(store_rating), 2)                         AS 服务均分
FROM order_evaluation WHERE deleted = 0;
