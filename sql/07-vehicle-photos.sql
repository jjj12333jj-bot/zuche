-- ---------------------------------------------------------------------
-- 07 车辆图片切换为真实实拍照片
--   旧：/images/cars/car-{id}.svg + 共用的 gallery/interior.svg、gallery/rear.svg
--   新：/images/cars/car-{id}.jpg、-2.jpg、-3.jpg，每台车 3 个真实拍摄角度
--
--   图片来源：汽车之家车型图片库（car.autohome.com.cn），仅用于教学演示，
--   下载后按 1200x800 重新压缩，随前端静态资源离线分发。
--   对应关系见 frontend/public/images/cars/README.md
--
--   幂等：可重复执行
-- ---------------------------------------------------------------------

USE zuche;

-- 1. 车型封面图
UPDATE vehicle_model
SET cover_image = CONCAT('/images/cars/car-', id, '.jpg')
WHERE id BETWEEN 1 AND 24;

-- 2. 车辆相册：每台车 3 张同车型不同角度实拍
UPDATE vehicle_image
SET image_url = CONCAT('/images/cars/car-', vehicle_id, '.jpg'),
    image_type = 'EXTERIOR'
WHERE sort_no = 1;

UPDATE vehicle_image
SET image_url = CONCAT('/images/cars/car-', vehicle_id, '-2.jpg'),
    image_type = 'EXTERIOR'
WHERE sort_no = 2;

UPDATE vehicle_image
SET image_url = CONCAT('/images/cars/car-', vehicle_id, '-3.jpg'),
    image_type = 'EXTERIOR'
WHERE sort_no = 3;

-- 3. 校验：不应再出现 .svg 引用
SELECT
    SUM(image_url LIKE '%.svg')  AS svg_left_in_vehicle_image,
    COUNT(*)                     AS total_rows
FROM vehicle_image;

SELECT
    SUM(cover_image LIKE '%.svg') AS svg_left_in_vehicle_model,
    COUNT(*)                      AS total_models
FROM vehicle_model;
