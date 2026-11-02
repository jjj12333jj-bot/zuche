-- =====================================================================
-- 08 首页轮播图：加副标题字段，并把占位 SVG 换成实拍旅行图
-- 在 zuche 库上执行一次即可，重复执行不会产生重复数据
-- =====================================================================
USE zuche;

-- 1) 副标题字段：前台大图上展示在大标题下方，选填
SET @has_subtitle := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'banner' AND column_name = 'subtitle'
);
SET @ddl := IF(@has_subtitle = 0,
  'ALTER TABLE banner ADD COLUMN subtitle VARCHAR(120) NULL COMMENT ''副标题/一句话说明'' AFTER title',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 清掉旧的渐变占位图（banner-1.svg 等）
DELETE FROM banner WHERE image_url LIKE '/images/banners/%.svg';

-- 3) 两个旅行主题轮播位（第一屏的信用说明由前端固定渲染，不占用 banner 表）
INSERT INTO banner (title, subtitle, image_url, link_url, position, sort_no, status)
SELECT '山海之间，一路向南', 'SUV 自驾 · 海岸线随停随走 · 泉州两店取还',
       '/images/banners/banner-1.jpg', '/vehicles', 'HOME', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url = '/images/banners/banner-1.jpg');

INSERT INTO banner (title, subtitle, image_url, link_url, position, sort_no, status)
SELECT '周末短途，说走就走', '精选车型 · 信用免押 · 到店即取，两小时上高速',
       '/images/banners/banner-2.jpg', '/vehicles', 'HOME', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE image_url = '/images/banners/banner-2.jpg');

SELECT id, title, subtitle, image_url, link_url, sort_no, status FROM banner ORDER BY sort_no;
