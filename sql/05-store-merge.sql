-- =====================================================================
-- 网点收敛：4 个网点 → 2 个（泉州南安店 / 泉州晋江店）
--
-- 说明：
--   1) 只调整 store 的数据行与引用归属，store 表结构不变，
--      全库仍是 30 张表，不影响「30 张表」的规模要求；
--   2) 原 3 号（国贸CBD店）并入 1 号，原 4 号（亦庄经开区店）并入 2 号；
--   3) 订单、车辆、调车申请的归属同步改写，保证没有悬空 store_id；
--   4) 车牌统一换为泉州号牌（闽C）。
--
-- 执行前已备份：sql/_bak_before_store_merge.sql
-- =====================================================================

START TRANSACTION;

-- 1. 两家网点改为泉州门店（名称 / 地址 / 电话 / 经纬度）
UPDATE store SET
    store_code = 'S001',
    store_name = '泉州南安店',
    address    = '福建省泉州市南安市成功街 88 号（南安汽车站旁）',
    phone      = '0595-86261001',
    business_hours = '08:00-21:00',
    longitude  = 118.386000,
    latitude   = 24.960000
WHERE id = 1;

UPDATE store SET
    store_code = 'S002',
    store_name = '泉州晋江店',
    address    = '福建省泉州市晋江市青阳街道阳光路 12 号',
    phone      = '0595-85602002',
    business_hours = '08:00-20:00',
    longitude  = 118.552000,
    latitude   = 24.781000
WHERE id = 2;

-- 2. 车辆归属：原 3 号 → 1 号，原 4 号 → 2 号
UPDATE vehicle SET store_id = 1 WHERE store_id = 3;
UPDATE vehicle SET store_id = 2 WHERE store_id = 4;

-- 3. 订单取车 / 归还网点同步改写
UPDATE lease_order SET store_id = 1 WHERE store_id = 3;
UPDATE lease_order SET store_id = 2 WHERE store_id = 4;
UPDATE lease_order SET return_store_id = 1 WHERE return_store_id = 3;
UPDATE lease_order SET return_store_id = 2 WHERE return_store_id = 4;

-- 4. 调车申请：改写为两家门店之间的调拨，且与车辆当前归属自洽
--    车 22 归 1 号店，申请调往 2 号店（待审批，归属不变）
UPDATE dispatch_apply SET
    from_store_id = 1,
    to_store_id   = 2,
    reason        = '南安店奥迪 Q5L 库存富余，晋江店豪华型已租罄'
WHERE apply_no = 'D202610260001';

--    车 21 已由 1 号店调入 2 号店（已通过，归属已是 2）
UPDATE dispatch_apply SET
    from_store_id = 1,
    to_store_id   = 2,
    reason        = '晋江店商务接待需求上升，由南安店调入'
WHERE apply_no = 'D202610150001';

-- 5. 车牌换为泉州号牌：京B·0137 → 闽C·0137
UPDATE vehicle
SET license_plate = CONCAT('闽C·', SUBSTRING(license_plate, 4))
WHERE license_plate LIKE '京%';

-- 6. 删除多余网点（store 表保留，仅数据行由 4 条变 2 条）
DELETE FROM store WHERE id IN (3, 4);

COMMIT;

-- 校验
SELECT id, store_code, store_name, address, phone, longitude, latitude FROM store ORDER BY id;
SELECT store_id, COUNT(*) AS vehicle_cnt FROM vehicle GROUP BY store_id;
SELECT store_id, COUNT(*) AS order_cnt FROM lease_order GROUP BY store_id;
SELECT id, apply_no, from_store_id, to_store_id, status FROM dispatch_apply;
