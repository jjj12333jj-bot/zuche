-- =====================================================================
--  基于信用风控的微租车平台 —— 演示数据（seed）
--  前置：先执行 01-schema.sql（含角色/菜单/字典/信用规则/网点/3个账号）
--  执行: mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/02-seed.sql
--
--  数据规模：9 品牌 / 24 车型 / 24 台车 / 72 张车图 / 保险 / 保养 / 违章
--            6 位普通用户 / 300 条订单 / 约 400 条明细 / 750 条支付流水 / 押金退款
--            评价、收藏、地址、公告、轮播、调车、申诉、风控名单、操作日志
--
--  初始账号（密码均为 123456）:
--    admin 系统管理员 | store1 网点管理员(中关村) | store2 网点管理员(望京)
--    user1 信用138优秀 | user2 112良好 | user3 92一般 | user4 74待观察 | user5 100新用户 | user6 128优秀
-- =====================================================================

USE zuche;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- 1. 品牌
-- ---------------------------------------------------------------------
INSERT INTO vehicle_brand (id, brand_name, brand_logo, initial, sort_no, status) VALUES
(1, '大众',   '/images/brands/brand-1.svg', 'D', 1, 1),
(2, '丰田',   '/images/brands/brand-2.svg', 'F', 2, 1),
(3, '本田',   '/images/brands/brand-3.svg', 'B', 3, 1),
(4, '日产',   '/images/brands/brand-4.svg', 'R', 4, 1),
(5, '别克',   '/images/brands/brand-5.svg', 'B', 5, 1),
(6, '比亚迪', '/images/brands/brand-6.svg', 'B', 6, 1),
(7, '特斯拉', '/images/brands/brand-7.svg', 'T', 7, 1),
(8, '奥迪',   '/images/brands/brand-8.svg', 'A', 8, 1),
(9, '宝马',   '/images/brands/brand-9.svg', 'B', 9, 1);

-- ---------------------------------------------------------------------
-- 2. 车型（承载参数与标准押金，车辆在其下实例化）
-- ---------------------------------------------------------------------
INSERT INTO vehicle_model (id, brand_id, model_name, category_code, seats, gearbox, fuel_type, displacement, guide_price, deposit, cover_image, description, sort_no, status) VALUES
(1,  1, '朗逸 2023款',      'ECONOMY', 5, 'AUTO', 'GASOLINE', '1.5L',     129.00,  800.00, '/images/cars/car-1.svg',  '省油耐开，通勤代步首选', 1, 1),
(2,  1, '帕萨特 2023款',    'COMFORT', 5, 'AUTO', 'GASOLINE', '2.0T',     229.00, 1500.00, '/images/cars/car-2.svg',  '商务接待常用车型，后排空间大', 2, 1),
(3,  1, '途观L 2022款',     'SUV',     7, 'AUTO', 'GASOLINE', '2.0T',     339.00, 2200.00, '/images/cars/car-3.svg',  '七座 SUV，适合家庭出行', 3, 1),
(4,  1, 'ID.4 2022款',      'EV',      5, 'AUTO', 'ELECTRIC', '84.8kWh',  329.00, 2500.00, '/images/cars/car-4.svg',  '纯电续航 550km，充电便捷', 4, 1),
(5,  2, '卡罗拉 2023款',    'ECONOMY', 5, 'AUTO', 'GASOLINE', '1.2T',     139.00,  900.00, '/images/cars/car-5.svg',  '保值率高，故障率低', 5, 1),
(6,  2, '凯美瑞 2022款',    'COMFORT', 5, 'AUTO', 'GASOLINE', '2.0L',     239.00, 1500.00, '/images/cars/car-6.svg',  '舒适性突出，长途友好', 6, 1),
(7,  2, 'RAV4荣放 2023款',  'SUV',     5, 'AUTO', 'GASOLINE', '2.0L',     319.00, 2000.00, '/images/cars/car-7.svg',  '四驱通过性好', 7, 1),
(8,  2, 'bZ4X 2023款',      'EV',      5, 'AUTO', 'ELECTRIC', '71.4kWh',  349.00, 2600.00, '/images/cars/car-8.svg',  '纯电 SUV，空间充裕', 8, 1),
(9,  3, '雅阁 2023款',      'COMFORT', 5, 'AUTO', 'GASOLINE', '1.5T',     249.00, 1600.00, '/images/cars/car-9.svg',  '动力平顺，隔音优秀', 9, 1),
(10, 3, 'CR-V 2023款',      'SUV',     5, 'AUTO', 'GASOLINE', '1.5T',     299.00, 2000.00, '/images/cars/car-10.svg', '空间魔术师，家用 SUV 标杆', 10, 1),
(11, 4, '轩逸 2022款',      'ECONOMY', 5, 'AUTO', 'GASOLINE', '1.6L',     119.00,  800.00, '/images/cars/car-11.svg', '座椅舒适，油耗低', 11, 1),
(12, 5, '英朗 2022款',      'ECONOMY', 5, 'AUTO', 'GASOLINE', '1.3T',     109.00,  600.00, '/images/cars/car-12.svg', '入门代步，性价比高', 12, 1),
(13, 5, '君威 2021款',      'COMFORT', 5, 'AUTO', 'GASOLINE', '1.5T',     199.00, 1200.00, '/images/cars/car-13.svg', '底盘扎实，高速稳', 13, 1),
(14, 5, '微蓝6 2022款',     'EV',      5, 'AUTO', 'ELECTRIC', '61.1kWh',  219.00, 1600.00, '/images/cars/car-14.svg', '合资纯电，代步经济', 14, 1),
(15, 6, '宋PLUS 2023款',    'SUV',     5, 'AUTO', 'HYBRID',   '1.5T DM-i',279.00, 1800.00, '/images/cars/car-15.svg', '插混省油，市区通勤友好', 15, 1),
(16, 6, '汉EV 2023款',      'EV',      5, 'AUTO', 'ELECTRIC', '85.4kWh',  359.00, 2800.00, '/images/cars/car-16.svg', '中大型纯电轿车，加速快', 16, 1),
(17, 6, '秦PLUS EV 2023款', 'EV',      5, 'AUTO', 'ELECTRIC', '57kWh',    259.00, 2000.00, '/images/cars/car-17.svg', '高性价比纯电家轿', 17, 1),
(18, 6, '海豹 2023款',      'EV',      5, 'AUTO', 'ELECTRIC', '82.5kWh',  339.00, 2600.00, '/images/cars/car-18.svg', '运动取向纯电轿车', 18, 1),
(19, 7, 'Model 3 2023款',   'EV',      5, 'AUTO', 'ELECTRIC', '78.4kWh',  399.00, 3000.00, '/images/cars/car-19.svg', '智能辅助驾驶，超充网络', 19, 1),
(20, 7, 'Model Y 2023款',   'EV',      5, 'AUTO', 'ELECTRIC', '78.4kWh',  459.00, 3500.00, '/images/cars/car-20.svg', '大空间纯电 SUV', 20, 1),
(21, 8, 'A6L 2023款',       'LUXURY',  5, 'AUTO', 'GASOLINE', '2.0T',     699.00, 5000.00, '/images/cars/car-21.svg', '商务旗舰，接待首选', 21, 1),
(22, 8, 'Q5L 2022款',       'LUXURY',  5, 'AUTO', 'GASOLINE', '2.0T',     659.00, 4500.00, '/images/cars/car-22.svg', '豪华中型 SUV', 22, 1),
(23, 9, '5系 2023款',       'LUXURY',  5, 'AUTO', 'GASOLINE', '2.0T',     799.00, 6000.00, '/images/cars/car-23.svg', '行政级豪华轿车', 23, 1),
(24, 9, '3系 2023款',       'LUXURY',  5, 'AUTO', 'GASOLINE', '2.0T',     599.00, 4000.00, '/images/cars/car-24.svg', '操控与舒适兼顾', 24, 1);

-- ---------------------------------------------------------------------
-- 3. 车辆（每个车型实例化 1 台，共 24 台；车牌/VIN 由 id 派生保证唯一）
-- ---------------------------------------------------------------------
INSERT INTO vehicle (vehicle_no, model_id, brand_id, store_id, license_plate, vin, color,
                     current_mileage, daily_price, deposit, discount_rate, register_date,
                     status, rent_count, view_count, description)
SELECT
    CONCAT('V', LPAD(m.id, 4, '0')),
    m.id,
    m.brand_id,
    1 + (m.id % 4),
    CONCAT('京', CHAR(65 + (m.id % 6)), '·', LPAD((m.id * 137) % 10000, 4, '0')),
    CONCAT('LSVZUCHE', LPAD(m.id, 9, '0')),
    ELT(1 + (m.id % 5), '珍珠白', '曜石黑', '星空蓝', '钛银灰', '云杉绿'),
    ROUND(1 + (m.id % 7) * 0.8, 1),
    ROUND(m.guide_price * (0.96 + (m.id % 5) * 0.02), 2),
    m.deposit,
    ELT(1 + (m.id % 5), 1.00, 0.98, 0.95, 1.00, 0.92),
    DATE_SUB(CURDATE(), INTERVAL (m.id * 11 % 700) DAY),
    CASE WHEN m.id % 11 = 0 THEN 2 WHEN m.id % 9 = 0 THEN 3 ELSE 1 END,
    10 + (m.id * 3) % 60,
    300 + m.id * 37,
    '车况良好，手续齐全，支持异地还车。'
FROM vehicle_model m;

-- 车辆图片：每台 3 张（外观/内饰/尾部），全部使用前端本地静态图，离线可演示
INSERT INTO vehicle_image (vehicle_id, image_url, image_type, sort_no)
SELECT id, CONCAT('/images/cars/car-', model_id, '.svg'), 'EXTERIOR', 1 FROM vehicle
UNION ALL
SELECT id, '/images/cars/gallery/interior.svg', 'INTERIOR', 2 FROM vehicle
UNION ALL
SELECT id, '/images/cars/gallery/rear.svg', 'EXTERIOR', 3 FROM vehicle;

-- 保险：每台车交强险 + 商业险，部分即将到期（用于到期提醒演示）
INSERT INTO vehicle_insurance (vehicle_id, insurance_type, insurer, policy_no, start_date, end_date, premium, status)
SELECT id, 'COMPULSORY', '人保财险(模拟)', CONCAT('PICC-C-', LPAD(id, 6, '0')),
       DATE_SUB(CURDATE(), INTERVAL 300 DAY), DATE_ADD(CURDATE(), INTERVAL (30 + id * 3) DAY),
       950.00, 1
FROM vehicle
UNION ALL
SELECT id, 'COMMERCIAL', '平安财险(模拟)', CONCAT('PAIC-B-', LPAD(id, 6, '0')),
       DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_ADD(CURDATE(), INTERVAL (60 + id * 2) DAY),
       3200.00 + id * 50, 1
FROM vehicle;

-- 保养维修记录
INSERT INTO vehicle_maintenance (vehicle_id, maint_type, maint_date, mileage, cost, content, operator_id, next_date, remark) VALUES
(1,  'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 120 DAY), 2.4,  680.00, '更换机油机滤、空调滤芯', 2, DATE_ADD(CURDATE(), INTERVAL 60 DAY),  '常规保养'),
(2,  'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 90 DAY),  3.1,  820.00, '更换机油三滤、四轮定位', 2, DATE_ADD(CURDATE(), INTERVAL 90 DAY),  '常规保养'),
(3,  'REPAIR',   DATE_SUB(CURDATE(), INTERVAL 45 DAY),  4.6, 2300.00, '更换前保险杠、喷漆',     2, NULL, '客户剐蹭，已走保险'),
(4,  'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 30 DAY),  1.2,  450.00, '电池健康检测、轮胎换位', 2, DATE_ADD(CURDATE(), INTERVAL 150 DAY), '新能源专项检查'),
(5,  'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 150 DAY), 5.2,  760.00, '更换机油机滤',           2, DATE_ADD(CURDATE(), INTERVAL 30 DAY),  '常规保养'),
(9,  'REPAIR',   DATE_SUB(CURDATE(), INTERVAL 20 DAY),  2.9, 1600.00, '修复右后门凹陷',         3, NULL, '已完成钣金喷漆'),
(10, 'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 60 DAY),  3.4, 1150.00, '更换机油、刹车片',       3, DATE_ADD(CURDATE(), INTERVAL 120 DAY), '常规保养'),
(13, 'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 210 DAY), 7.8,  980.00, '大保养：变速箱油、火花塞', 2, DATE_ADD(CURDATE(), INTERVAL 30 DAY), '车龄较长，重点关注'),
(15, 'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 10 DAY),  1.6,  520.00, '混动系统检测、机油更换', 3, DATE_ADD(CURDATE(), INTERVAL 170 DAY), '常规保养'),
(17, 'REPAIR',   DATE_SUB(CURDATE(), INTERVAL 75 DAY),  2.2,  890.00, '更换雨刮电机',           2, NULL, '质保内维修'),
(19, 'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 40 DAY),  1.9,  600.00, '轮胎换位、制动液检测',   3, DATE_ADD(CURDATE(), INTERVAL 140 DAY), '常规保养'),
(21, 'MAINTAIN', DATE_SUB(CURDATE(), INTERVAL 25 DAY),  1.1, 1800.00, '全合成机油、空调系统清洗', 2, DATE_ADD(CURDATE(), INTERVAL 155 DAY), '豪华车型专项保养');

-- 违章记录
INSERT INTO vehicle_violation (vehicle_id, order_id, user_id, violation_time, violation_place, violation_type, fine_amount, points, handle_status, handle_time, remark) VALUES
(3,  NULL, NULL, DATE_SUB(NOW(), INTERVAL 40 DAY), '北京市海淀区中关村大街',  '机动车违反禁令标志', 200.00, 3, 'UNHANDLED', NULL, '待责任人处理'),
(9,  NULL, NULL, DATE_SUB(NOW(), INTERVAL 33 DAY), '北京市朝阳区建国路',      '超速 20% 以下',      100.00, 3, 'HANDLED',   DATE_SUB(NOW(), INTERVAL 20 DAY), '已由承租人缴纳'),
(12, NULL, NULL, DATE_SUB(NOW(), INTERVAL 28 DAY), '北京市大兴区荣华南路',    '违法停车',           200.00, 0, 'HANDLED',   DATE_SUB(NOW(), INTERVAL 15 DAY), '已处理'),
(17, NULL, NULL, DATE_SUB(NOW(), INTERVAL 18 DAY), '北京市朝阳区望京街',      '未按导向车道行驶',   100.00, 2, 'UNHANDLED', NULL, '待核实'),
(21, NULL, NULL, DATE_SUB(NOW(), INTERVAL 12 DAY), '北京市东城区建国门内大街', '闯红灯',             200.00, 6, 'APPEALING', NULL, '承租人申诉中'),
(5,  NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 DAY),  '北京市海淀区学院路',      '违法停车',           200.00, 0, 'UNHANDLED', NULL, NULL),
(23, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY),  '北京市朝阳区东三环',      '占用应急车道',       200.00, 6, 'UNHANDLED', NULL, NULL);

-- ---------------------------------------------------------------------
-- 4. 普通用户（含信用校准，走 credit_log 留痕）
-- ---------------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, phone, email, id_card, driver_license, credit_score, credit_level, status) VALUES
(4, 'user1', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '张伟', '13900000001', 'zhangwei@qq.com',  '110101199001011234', '110101199001011234', 100, 'GOOD', 1),
(5, 'user2', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '李娜', '13900000002', 'lina@qq.com',      '110101199203055678', '110101199203055678', 100, 'GOOD', 1),
(6, 'user3', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '王强', '13900000003', 'wangqiang@qq.com', '110101198807129012', '110101198807129012', 100, 'GOOD', 1),
(7, 'user4', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '刘洋', '13900000004', 'liuyang@qq.com',   '110101199512203456', '110101199512203456', 100, 'GOOD', 1),
(8, 'user5', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '陈静', '13900000005', 'chenjing@qq.com',  '110101200001017890', '110101200001017890', 100, 'GOOD', 1),
(9, 'user6', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '赵磊', '13900000006', 'zhaolei@qq.com',   '110101198505052345', '110101198505052345', 100, 'GOOD', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (4,1),(5,1),(6,1),(7,1),(8,1),(9,1);

-- 历史信用校准（模拟老用户历史行为折算）
INSERT INTO credit_log (user_id, action_code, change_type, score_before, score_change, score_after, reason, create_time) VALUES
(4, 'ADMIN_ADJUST', 'ADMIN', 100,  28, 128, '初始化校准：历史长租记录优质，信用分折算上调', DATE_SUB(NOW(), INTERVAL 31 DAY)),
(5, 'ADMIN_ADJUST', 'ADMIN', 100,  12, 112, '初始化校准：历史履约良好，信用分折算上调',     DATE_SUB(NOW(), INTERVAL 31 DAY)),
(6, 'ADMIN_ADJUST', 'ADMIN', 100, -18,  82, '初始化校准：历史 2 次迟到取车记录',             DATE_SUB(NOW(), INTERVAL 31 DAY)),
(7, 'ADMIN_ADJUST', 'ADMIN', 100, -26,  74, '初始化校准：历史 2 次逾期归还，转入待观察',     DATE_SUB(NOW(), INTERVAL 31 DAY)),
(9, 'ADMIN_ADJUST', 'ADMIN', 100,  38, 138, '初始化校准：企业客户，长期高频优质租赁',       DATE_SUB(NOW(), INTERVAL 31 DAY));

UPDATE sys_user SET credit_score = 128, credit_level = 'EXCELLENT' WHERE id = 4;
UPDATE sys_user SET credit_score = 112, credit_level = 'GOOD'      WHERE id = 5;
UPDATE sys_user SET credit_score =  82, credit_level = 'NORMAL'    WHERE id = 6;
UPDATE sys_user SET credit_score =  74, credit_level = 'WATCH'     WHERE id = 7;
UPDATE sys_user SET credit_score = 138, credit_level = 'EXCELLENT' WHERE id = 9;

-- ---------------------------------------------------------------------
-- 5. 地址 / 收藏 / 公告 / 轮播 / 调车申请
-- ---------------------------------------------------------------------
INSERT INTO user_address (user_id, receiver, phone, province, city, district, detail, is_default) VALUES
(4, '张伟', '13900000001', '北京市', '北京市', '海淀区', '中关村大街 27 号 3 号楼 502', 1),
(4, '张伟', '13900000001', '北京市', '北京市', '朝阳区', '望京西路 12 号院 8 号楼 1102', 0),
(5, '李娜', '13900000002', '北京市', '北京市', '西城区', '金融街 33 号 A 座 1801', 1),
(6, '王强', '13900000003', '北京市', '北京市', '丰台区', '南三环西路 16 号院 5 号楼 903', 1);

INSERT INTO favorite (user_id, vehicle_id) VALUES (4, 9), (4, 17), (5, 13), (5, 19), (8, 4), (9, 21);

INSERT INTO announcement (title, content, type, publisher_id, status, view_count, publish_time) VALUES
('信用分上线：押金最低 8 折', '平台已上线信用风控体系。信用分达到 120 分（优秀档）即可享受押金 8 折，良好档 9 折，待观察档押金上浮 20%。信用分由按时归还率、取消率、逾期次数与完成订单数共同决定，每笔变动都可在"我的-信用中心"查看明细，对扣分有异议可在线申诉。', 'RULE', 1, 1, 328, DATE_SUB(NOW(), INTERVAL 12 DAY)),
('新网点开业：亦庄经开区店', '亦庄经开区店正式营业，取还车时间 08:00-19:00，支持跨网点还车。开业首月经济型车型日租金 8 折。', 'ACTIVITY', 1, 1, 156, DATE_SUB(NOW(), INTERVAL 6 DAY)),
('假期取车提醒', '假期期间取车请提前 30 分钟到店，携带身份证与驾驶证原件。逾期归还将按日租金 1.5 倍收取违约金，并影响信用分。', 'NOTICE', 1, 1, 512, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('押金退还时效说明', '网点确认归还后系统自动发起押金退还，当前为模拟支付通道，1 个工作日内完成。可在订单详情-退款记录中查看流水状态。', 'NOTICE', 1, 1, 87, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO banner (title, image_url, link_url, position, sort_no, status) VALUES
('信用越好 押金越低',      '/images/banners/banner-1.svg', '/credit',       'HOME', 1, 1),
('新用户首单立减 50',      '/images/banners/banner-2.svg', '/vehicles',     'HOME', 2, 1),
('新能源专区 低至 219/天',  '/images/banners/banner-3.svg', '/vehicles?category=EV', 'HOME', 3, 1);

INSERT INTO dispatch_apply (apply_no, vehicle_id, from_store_id, to_store_id, apply_user_id, reason, status, audit_user_id, audit_time, audit_remark, create_time) VALUES
('D202610260001', 22, 3, 1, 3, '国贸店奥迪Q5L库存富余，中关村店豪华型已租罄', 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('D202610150001', 21, 2, 4, 3, '亦庄店商务接待需求上升', 'APPROVED', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), '同意调拨，注意里程登记', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- ---------------------------------------------------------------------
-- 6. 历史订单（300 条，覆盖最近 30 天，双峰时段；一单可含 1~2 台车）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_orders;
DELIMITER $$
CREATE PROCEDURE seed_orders(IN total INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE b INT;
  DECLARE j INT;
  DECLARE vc INT;
  DECLARE v_id BIGINT; DECLARE vp DECIMAL(10,2); DECLARE vd DECIMAL(10,2); DECLARE vdisc DECIMAL(3,2);
  DECLARE u_id BIGINT; DECLARE u_score INT;
  DECLARE u_level VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  DECLARE st VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  DECLARE coeff DECIMAL(3,2);
  DECLARE d_off INT; DECLARE days INT; DECLARE h INT; DECLARE mi INT;
  DECLARE s_date DATE; DECLARE e_date DATE;
  DECLARE rent_total DECIMAL(10,2); DECLARE dep_total DECIMAL(10,2);
  DECLARE irit DECIMAL(10,2); DECLARE idep DECIMAL(10,2);
  DECLARE c_at DATETIME; DECLARE o_no VARCHAR(32); DECLARE ord_id BIGINT;
  DECLARE od INT DEFAULT 0; DECLARE late DECIMAL(10,2) DEFAULT 0;
  DECLARE pay_dep_id BIGINT; DECLARE pay_rent_id BIGINT;
  DECLARE vfirst_price DECIMAL(10,2) DEFAULT 0;
  DECLARE sc INT;

  WHILE i <= total DO
    SET b = i % 20;
    SET u_id = 4 + (i % 6);
    SELECT credit_score, credit_level INTO u_score, u_level FROM sys_user WHERE id = u_id;
    SELECT deposit_coefficient INTO coeff FROM credit_rule
      WHERE rule_type = 'LEVEL' AND min_score <= u_score AND max_score >= u_score LIMIT 1;

    -- 状态分布：70% 完成 / 10% 取消 / 10% 在租 / 5% 逾期 / 5% 待取车
    IF b <= 13 THEN SET st = 'COMPLETED';
    ELSEIF b <= 15 THEN SET st = 'CANCELLED';
    ELSEIF b <= 17 THEN SET st = 'IN_USE';
    ELSEIF b = 18 THEN SET st = 'OVERDUE';
    ELSE SET st = 'WAIT_PICKUP';
    END IF;

    SET days = 1 + (i % 4);
    IF st IN ('COMPLETED', 'CANCELLED') THEN
      SET d_off = 1 + (i * 7) % 29;
      IF days > d_off THEN SET days = d_off; END IF;
    ELSEIF st = 'OVERDUE' THEN
      SET d_off = 6; SET days = 2;
    ELSE
      SET d_off = (i % 3); SET days = 2 + (i % 3);
    END IF;

    -- 双峰时段：9-12 点、18-21 点
    IF i % 10 < 5 THEN SET h = 9 + (i % 4);
    ELSEIF i % 10 < 8 THEN SET h = 18 + (i % 4);
    ELSE SET h = (i * 5) % 24;
    END IF;
    SET mi = (i * 13) % 60;

    SET s_date = DATE_SUB(CURDATE(), INTERVAL d_off DAY);
    SET e_date = DATE_ADD(s_date, INTERVAL days DAY);
    SET c_at   = TIMESTAMP(s_date, MAKETIME(h, mi, 0));
    SET o_no   = CONCAT('L', DATE_FORMAT(c_at, '%Y%m%d%H%i%s'), LPAD(i, 4, '0'));
    SET vc     = IF(i % 3 = 0, 2, 1);

    INSERT INTO lease_order
      (order_no, user_id, store_id, return_store_id, start_date, end_date, rent_days, vehicle_count,
       rent_amount, deposit_amount, total_amount,
       credit_score_snapshot, credit_level_snapshot, deposit_coefficient, status,
       pay_time, pickup_time, return_apply_time, return_time, finish_time, cancel_time, cancel_reason,
       overdue_days, late_fee, deposit_refund_status, create_time)
    VALUES
      (o_no, u_id, 1 + (i % 4), 1 + (i % 4), s_date, e_date, days, vc,
       0, 0, 0,
       u_score, u_level, coeff, st,
       IF(st = 'CANCELLED' AND i % 4 <> 0, NULL, DATE_ADD(c_at, INTERVAL 5 MINUTE)),
       IF(st IN ('IN_USE','OVERDUE','RETURNED','COMPLETED'), DATE_ADD(c_at, INTERVAL 2 HOUR), NULL),
       IF(st IN ('RETURNED','COMPLETED'), TIMESTAMP(e_date, '11:00:00'), NULL),
       IF(st IN ('RETURNED','COMPLETED'), TIMESTAMP(e_date, '12:30:00'), NULL),
       IF(st = 'COMPLETED', TIMESTAMP(e_date, '13:00:00'), NULL),
       IF(st = 'CANCELLED', DATE_ADD(c_at, INTERVAL 20 MINUTE), NULL),
       IF(st = 'CANCELLED', '临时行程变更', NULL),
       0, 0,
       CASE st WHEN 'COMPLETED' THEN 'REFUNDED' WHEN 'RETURNED' THEN 'PENDING' ELSE 'NONE' END,
       c_at);

    SET ord_id = LAST_INSERT_ID();
    SET rent_total = 0; SET dep_total = 0; SET j = 1; SET vfirst_price = 0;

    WHILE j <= vc DO
      SET v_id = ((i * 5 + j * 7 + 3) % 24) + 1;
      SELECT daily_price, deposit, discount_rate INTO vp, vd, vdisc FROM vehicle WHERE id = v_id;
      IF j = 1 THEN SET vfirst_price = vp; END IF;
      SET irit = ROUND(vp * days * vdisc, 2);
      SET idep = ROUND(vd * coeff, 2);

      INSERT INTO order_item (order_id, vehicle_id, vehicle_name, license_plate, daily_price,
                              quantity, rent_days, discount_rate, rent_amount, deposit_amount, create_time)
      SELECT ord_id, v_id, CONCAT(br.brand_name, ' ', m.model_name), v.license_plate, vp,
             1, days, vdisc, irit, idep, c_at
        FROM vehicle v
        JOIN vehicle_model m ON m.id = v.model_id
        JOIN vehicle_brand br ON br.id = v.brand_id
       WHERE v.id = v_id;

      SET rent_total = rent_total + irit;
      SET dep_total  = dep_total + idep;
      SET j = j + 1;
    END WHILE;

    SET od = 0; SET late = 0;
    IF st = 'OVERDUE' THEN
      SET od = DATEDIFF(CURDATE(), e_date);
      IF od < 1 THEN SET od = 1; END IF;
      SET late = ROUND(vfirst_price * od * 1.5, 2);
    END IF;

    UPDATE lease_order
       SET rent_amount = rent_total,
           deposit_amount = dep_total,
           total_amount = rent_total + dep_total,
           overdue_days = od,
           late_fee = late
     WHERE id = ord_id;

    -- 支付流水：未取消(或已支付后取消)的订单都有租金+押金两条
    IF st <> 'CANCELLED' OR i % 4 = 0 THEN
      INSERT INTO payment_record (pay_no, order_id, user_id, pay_type, amount, pay_method, status, pay_time, remark, create_time)
      VALUES (CONCAT('PAY', DATE_FORMAT(c_at, '%Y%m%d%H%i%s'), LPAD(i, 4, '0'), 'R'),
              ord_id, u_id, 'RENT', rent_total, 'MOCK_WECHAT', 'SUCCESS', DATE_ADD(c_at, INTERVAL 5 MINUTE), '模拟支付-租金', DATE_ADD(c_at, INTERVAL 5 MINUTE));
      SET pay_rent_id = LAST_INSERT_ID();

      INSERT INTO payment_record (pay_no, order_id, user_id, pay_type, amount, pay_method, status, pay_time, remark, create_time)
      VALUES (CONCAT('PAY', DATE_FORMAT(c_at, '%Y%m%d%H%i%s'), LPAD(i, 4, '0'), 'D'),
              ord_id, u_id, 'DEPOSIT', dep_total, 'MOCK_WECHAT', 'SUCCESS', DATE_ADD(c_at, INTERVAL 5 MINUTE), '模拟支付-押金', DATE_ADD(c_at, INTERVAL 5 MINUTE));
      SET pay_dep_id = LAST_INSERT_ID();

      -- 完成订单：退押金
      IF st = 'COMPLETED' THEN
        INSERT INTO refund_record (refund_no, order_id, payment_id, user_id, refund_type, amount, reason,
                                   status, apply_time, refund_time, channel, remark, create_time)
        VALUES (CONCAT('REF', DATE_FORMAT(c_at, '%Y%m%d%H%i%s'), LPAD(i, 4, '0'), 'D'),
                ord_id, pay_dep_id, u_id, 'DEPOSIT', dep_total, '订单完成，押金原路退还',
                'REFUNDED', TIMESTAMP(e_date, '12:30:00'), TIMESTAMP(e_date, '13:00:00'), 'MOCK_WECHAT', '模拟退还押金',
                TIMESTAMP(e_date, '13:00:00'));
      END IF;

      -- 已支付后取消：全额退款
      IF st = 'CANCELLED' AND i % 4 = 0 THEN
        INSERT INTO refund_record (refund_no, order_id, payment_id, user_id, refund_type, amount, reason,
                                   status, apply_time, refund_time, channel, remark, create_time)
        VALUES (CONCAT('REF', DATE_FORMAT(c_at, '%Y%m%d%H%i%s'), LPAD(i, 4, '0'), 'C'),
                ord_id, pay_rent_id, u_id, 'CANCEL', rent_total + dep_total, '用户取消订单，租金与押金全额退还',
                'REFUNDED', DATE_ADD(c_at, INTERVAL 20 MINUTE), DATE_ADD(c_at, INTERVAL 30 MINUTE), 'MOCK_WECHAT', '模拟取消退款',
                DATE_ADD(c_at, INTERVAL 30 MINUTE));
      END IF;
    END IF;

    -- 信用事件抽样（每 20 单），避免历史数据把分数顶到上限
    IF i % 20 = 0 THEN
      SET sc = (SELECT credit_score FROM sys_user WHERE id = u_id);
      IF st = 'COMPLETED' THEN
        INSERT INTO credit_log (user_id, action_code, change_type, score_before, score_change, score_after, order_id, dedup_key, reason, create_time)
        VALUES (u_id, 'ORDER_FINISH', 'EVENT', sc, 2, LEAST(150, sc + 2), ord_id, '', '订单完成', TIMESTAMP(e_date, '13:00:00'));
        UPDATE sys_user SET credit_score = LEAST(150, sc + 2) WHERE id = u_id;
      ELSEIF st = 'OVERDUE' THEN
        INSERT INTO credit_log (user_id, action_code, change_type, score_before, score_change, score_after, order_id, dedup_key, reason, create_time)
        VALUES (u_id, 'OVERDUE', 'EVENT', sc, -3, GREATEST(0, sc - 3), ord_id, '1', CONCAT('逾期未归还 ', od, ' 天'), TIMESTAMP(e_date, '23:59:00'));
        UPDATE sys_user SET credit_score = GREATEST(0, sc - 3) WHERE id = u_id;
      END IF;
    END IF;

    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

CALL seed_orders(300);
DROP PROCEDURE seed_orders;

-- ---------------------------------------------------------------------
-- 7. 订单评价（取最近 12 条已完成订单，一单一评）
-- ---------------------------------------------------------------------
INSERT INTO order_evaluation (order_id, user_id, vehicle_id, rating, content, reply, reply_time, status, create_time)
SELECT o.id, o.user_id, MIN(oi.vehicle_id),
       ELT(1 + (o.id % 4), 5, 4, 5, 3),
       ELT(1 + (o.id % 4),
           '车况很新，取车流程 5 分钟搞定，信用分抵扣后押金便宜不少。',
           '整体不错，就是内饰有点旧，性价比可以。',
           '还车检查很快，押金当天到账，会再租。',
           '取车时等了一会儿，车本身没问题。'),
       IF(o.id % 3 = 0, '感谢评价，欢迎再次选择本网点。', NULL),
       IF(o.id % 3 = 0, DATE_ADD(o.finish_time, INTERVAL 1 DAY), NULL),
       1, DATE_ADD(o.finish_time, INTERVAL 6 HOUR)
FROM lease_order o
JOIN order_item oi ON oi.order_id = o.id
WHERE o.status = 'COMPLETED'
GROUP BY o.id
ORDER BY o.id DESC
LIMIT 12;

-- ---------------------------------------------------------------------
-- 8. 信用申诉 / 风控名单
-- ---------------------------------------------------------------------
INSERT INTO credit_appeal (appeal_no, user_id, credit_log_id, order_id, appeal_reason, status, handle_user_id, handle_time, handle_remark, restore_score, create_time) VALUES
('A202610200001', 7, NULL, NULL, '逾期是因为车辆故障无法启动，已联系网点救援，不应计入逾期。', 'PENDING',  NULL, NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('A202610180001', 6, NULL, NULL, '取车迟到 20 分钟，原因是地铁延误，希望酌情减免扣分。', 'REJECTED', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), '取车时间已在订单中明确，维持原判', 0, DATE_SUB(NOW(), INTERVAL 7 DAY)),
('A202610120001', 4, NULL, NULL, '订单取消是网点无车导致，非本人原因。', 'APPROVED', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), '经核实网点库存异常，恢复扣分 3 分', 3, DATE_SUB(NOW(), INTERVAL 16 DAY));

INSERT INTO credit_blacklist (user_id, list_type, reason, trigger_rule, effective_time, expire_time, status, operator_id) VALUES
(7, 'WARNING', '近 90 天累计 2 次逾期归还，触发预警', '90天内逾期≥2次', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 70 DAY), 'ACTIVE', NULL),
(6, 'OVERRIDE', '取消率超过 30%，限制豪华车型下单', '取消率>30%', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 50 DAY), 'ACTIVE', 1);

-- ---------------------------------------------------------------------
-- 9. 操作日志
-- ---------------------------------------------------------------------
INSERT INTO operation_log (user_id, username, module, operation, method, request_uri, params, ip, cost_ms, status, create_time) VALUES
(1, 'admin',  '信用风控', '修改信用等级规则', 'PUT',  '/api/admin/credit/rules/1', '{"depositCoefficient":0.8}', '127.0.0.1', 35, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1, 'admin',  '调度管理', '审批通过调车申请', 'POST', '/api/admin/dispatch/2/approve', '{"auditRemark":"同意调拨"}', '127.0.0.1', 48, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 'admin',  '用户管理', '禁用用户',        'PUT',  '/api/admin/users/8/status', '{"status":0}', '127.0.0.1', 22, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'admin',  '信用风控', '手工调整信用分',  'POST', '/api/admin/credit/users/6/adjust', '{"change":-5,"reason":"核实取消率异常"}', '127.0.0.1', 41, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'store1', '网点订单', '确认交付车辆',    'POST', '/api/store/orders/18/deliver', '{}', '127.0.0.1', 30, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'store1', '网点订单', '确认归还并结算',  'POST', '/api/store/orders/21/return-confirm', '{}', '127.0.0.1', 55, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 'store2', '调车申请', '发起调车申请',    'POST', '/api/store/dispatch', '{"vehicleId":22,"toStoreId":1}', '127.0.0.1', 27, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'store1', '车辆资产', '新增保养记录',    'POST', '/api/admin/vehicle/maintenance', '{"vehicleId":1,"cost":680}', '127.0.0.1', 19, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'store2', '车辆资产', '上架车辆',        'PUT',  '/api/store/vehicles/10/status', '{"status":1}', '127.0.0.1', 15, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'admin',  '公告管理', '发布公告',        'POST', '/api/admin/announcements', '{"title":"押金退还时效说明"}', '127.0.0.1', 33, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------
-- 10. 信用分最终校准（保证恒等式成立，且等级与阈值一致）
--     credit_score = clamp(100 + Σ credit_log.score_change, 0, 150)
-- ---------------------------------------------------------------------
UPDATE sys_user u
LEFT JOIN (SELECT user_id, SUM(score_change) AS s FROM credit_log GROUP BY user_id) x ON x.user_id = u.id
SET u.credit_score = LEAST(150, GREATEST(0, 100 + IFNULL(x.s, 0)));

UPDATE sys_user u
JOIN credit_rule r ON r.rule_type = 'LEVEL' AND u.credit_score BETWEEN r.min_score AND r.max_score
SET u.credit_level = r.rule_code;

-- 网点车辆数不再冗余存储：统一通过 COUNT(vehicle) 实时统计

-- ---------------------------------------------------------------------
-- 11. 自检输出
-- ---------------------------------------------------------------------
SELECT '表数量' AS 指标, COUNT(*) AS 值 FROM information_schema.tables WHERE table_schema = 'zuche'
UNION ALL SELECT '用户数',    COUNT(*) FROM sys_user
UNION ALL SELECT '品牌/车型/车辆', COUNT(*) FROM vehicle_brand
UNION ALL SELECT '车辆数',    COUNT(*) FROM vehicle
UNION ALL SELECT '车辆图片',  COUNT(*) FROM vehicle_image
UNION ALL SELECT '订单数',    COUNT(*) FROM lease_order
UNION ALL SELECT '订单明细',  COUNT(*) FROM order_item
UNION ALL SELECT '支付流水',  COUNT(*) FROM payment_record
UNION ALL SELECT '退款流水',  COUNT(*) FROM refund_record
UNION ALL SELECT '信用日志',  COUNT(*) FROM credit_log;

SELECT credit_level, COUNT(*) AS 用户数, MIN(credit_score) AS 最低分, MAX(credit_score) AS 最高分
FROM sys_user GROUP BY credit_level ORDER BY 最高分 DESC;

SELECT credit_level_snapshot AS 等级, COUNT(*) AS 订单量,
       SUM(status = 'COMPLETED') AS 完成单, SUM(overdue_days > 0) AS 逾期单,
       ROUND(SUM(deposit_amount), 2) AS 押金合计
FROM lease_order GROUP BY credit_level_snapshot ORDER BY 订单量 DESC;

SELECT status AS 订单状态, COUNT(*) AS 数量 FROM lease_order GROUP BY status ORDER BY 数量 DESC;
