-- =====================================================================
--  基于信用风控的微租车平台 —— 数据库结构（30 张表）
--  MySQL 8.0+ / InnoDB / utf8mb4_general_ci
--  执行: mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/01-schema.sql
--
--  设计约定（答辩可讲）:
--   1. 全部表 utf8mb4，主键 bigint unsigned 自增，金额 decimal(10,2)，时间 datetime。
--   2. 通用审计字段: create_time / update_time；业务实体表另有 deleted（0 正常 1 删除）。
--      流水、明细、关联、日志类表不设 deleted —— 支付流水与信用日志属于不可删除的记账数据，
--      只能通过反向流水（退款/调整）冲正，这一点是账务类系统的硬约束。
--   3. 表间关系用「逻辑外键 + 索引」维护，不建物理外键：项目使用 MyBatis-Plus 逻辑删除，
--      物理外键会与软删除、批量初始化冲突，完整性由 Service 层保证。
--   4. 信用分恒等式: sys_user.credit_score = clamp(100 + Σ credit_log.score_change, 0, 150)，
--      任何分数变动必须先写 credit_log，再由同一事务更新 sys_user。
--   5. 本文件含「必要初始数据」：角色、菜单、角色菜单、字典、信用规则、网点、三个初始账号。
--      演示业务数据（品牌/车型/车辆/订单等）在 02-seed.sql。
--
--  模块划分:
--    一、系统权限与基础配置 6 张     二、车辆资产精细化管理 7 张
--    三、网点与调度管理     3 张     四、租赁订单与交易     6 张
--    五、信用风控           4 张     六、运营与系统审计     4 张
-- =====================================================================

DROP DATABASE IF EXISTS zuche;
CREATE DATABASE zuche DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE zuche;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- =====================================================================
-- 模块一：系统权限与基础配置（6 张）
-- =====================================================================

-- 1. 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username       VARCHAR(50)   NOT NULL                COMMENT '登录账号',
  password       VARCHAR(100)  NOT NULL                COMMENT '登录密码(BCrypt 密文)',
  real_name      VARCHAR(50)   DEFAULT NULL            COMMENT '真实姓名',
  phone          VARCHAR(20)   DEFAULT NULL            COMMENT '手机号(模拟，不做真实性校验)',
  email          VARCHAR(100)  DEFAULT NULL            COMMENT '邮箱',
  avatar         VARCHAR(255)  DEFAULT NULL            COMMENT '头像地址',
  id_card        VARCHAR(32)   DEFAULT NULL            COMMENT '身份证号(模拟，脱敏存储)',
  driver_license VARCHAR(32)   DEFAULT NULL            COMMENT '驾驶证号(模拟)',
  credit_score   INT           NOT NULL DEFAULT 100    COMMENT '信用分 0~150，默认100',
  credit_level   VARCHAR(20)   NOT NULL DEFAULT 'GOOD' COMMENT '信用等级冗余: EXCELLENT/GOOD/NORMAL/WATCH',
  store_id       BIGINT UNSIGNED DEFAULT NULL          COMMENT '所属网点(网点管理员)',
  status         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态 1正常 0禁用',
  last_login_time DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone),
  KEY idx_credit_level (credit_level),
  KEY idx_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2. 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code   VARCHAR(30)  NOT NULL COMMENT '角色编码: USER/STORE_ADMIN/SYS_ADMIN',
  role_name   VARCHAR(30)  NOT NULL COMMENT '角色名称',
  description VARCHAR(200) DEFAULT NULL COMMENT '角色说明',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 菜单/权限表（前端按角色渲染菜单，后端按 perms 生成权限点）
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
  menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
  menu_type   CHAR(1)      NOT NULL DEFAULT 'C' COMMENT '类型 M目录 C菜单 F按钮',
  path        VARCHAR(200) DEFAULT NULL COMMENT '前端路由地址',
  component   VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
  perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识，如 credit:rule:edit',
  icon        VARCHAR(50)  DEFAULT NULL COMMENT '图标',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 4. 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 5. 角色菜单关联表
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  menu_id     BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 6. 数据字典表（订单状态、车型分类、变速箱、能源类型、支付方式等统一收口）
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  dict_type   VARCHAR(50)  NOT NULL COMMENT '字典类型，如 order_status',
  dict_code   VARCHAR(50)  NOT NULL COMMENT '字典编码，如 WAIT_PAY',
  dict_label  VARCHAR(50)  NOT NULL COMMENT '字典标签，如 待支付',
  dict_value  VARCHAR(100) DEFAULT NULL COMMENT '扩展值(颜色/系数等)',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_code (dict_type, dict_code),
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- =====================================================================
-- 模块二：车辆资产精细化管理（7 张）
-- 品牌 → 车型（参数/指导价） → 车辆（具体一台车：车牌/VIN/所属网点）
-- 车辆为最小可租单元，因此一单多车通过 order_item 多行实现
-- =====================================================================

-- 7. 车辆品牌表
DROP TABLE IF EXISTS vehicle_brand;
CREATE TABLE vehicle_brand (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '品牌ID',
  brand_name  VARCHAR(50)  NOT NULL COMMENT '品牌名称',
  brand_logo  VARCHAR(255) DEFAULT NULL COMMENT '品牌LOGO',
  initial     CHAR(1)      DEFAULT NULL COMMENT '首字母，用于列表索引',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_brand_name (brand_name),
  KEY idx_initial (initial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆品牌表';

-- 8. 车辆车型表（同品牌下的具体车型，承载参数与指导价）
DROP TABLE IF EXISTS vehicle_model;
CREATE TABLE vehicle_model (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车型ID',
  brand_id     BIGINT UNSIGNED NOT NULL COMMENT '品牌ID',
  model_name   VARCHAR(80)  NOT NULL COMMENT '车型名称，如 朗逸 2023款',
  category_code VARCHAR(30) NOT NULL COMMENT '车型分类字典码: ECONOMY/COMFORT/SUV/LUXURY/EV',
  seats        TINYINT      NOT NULL DEFAULT 5 COMMENT '座位数',
  gearbox      VARCHAR(20)  NOT NULL DEFAULT 'AUTO' COMMENT '变速箱 AUTO手动/MANUAL自动(字典 gearbox)',
  fuel_type    VARCHAR(20)  NOT NULL DEFAULT 'GASOLINE' COMMENT '能源类型 字典 fuel_type',
  displacement VARCHAR(20)  DEFAULT NULL COMMENT '排量/电池容量',
  guide_price  DECIMAL(10,2) NOT NULL COMMENT '指导日租金',
  deposit      DECIMAL(10,2) NOT NULL COMMENT '标准押金(乘信用系数前)',
  cover_image  VARCHAR(255) DEFAULT NULL COMMENT '车型主图',
  description  TEXT         DEFAULT NULL COMMENT '车型卖点描述',
  sort_no      INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_brand (brand_id),
  KEY idx_category (category_code),
  KEY idx_guide_price (guide_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆车型表';

-- 9. 车辆核心信息表（具体一台可租车辆）
DROP TABLE IF EXISTS vehicle;
CREATE TABLE vehicle (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
  vehicle_no      VARCHAR(30)  NOT NULL COMMENT '资产编号',
  model_id        BIGINT UNSIGNED NOT NULL COMMENT '车型ID',
  brand_id        BIGINT UNSIGNED NOT NULL COMMENT '品牌ID(冗余，避免列表多表join)',
  store_id        BIGINT UNSIGNED NOT NULL COMMENT '所属网点(主驻网点)',
  license_plate   VARCHAR(20)  NOT NULL COMMENT '车牌号(模拟)',
  vin             VARCHAR(30)  DEFAULT NULL COMMENT '车架号VIN(模拟)',
  color           VARCHAR(20)  DEFAULT NULL COMMENT '车身颜色',
  current_mileage DECIMAL(10,1) NOT NULL DEFAULT 0.0 COMMENT '当前里程(万公里)',
  daily_price     DECIMAL(10,2) NOT NULL COMMENT '实际日租金(可覆盖车型指导价)',
  deposit         DECIMAL(10,2) NOT NULL COMMENT '标准押金(乘信用系数前)',
  discount_rate   DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '租金折扣 1.00=无折扣',
  register_date   DATE         DEFAULT NULL COMMENT '上牌日期',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1可租 2已租出 3维修中 0下架',
  rent_count      INT          NOT NULL DEFAULT 0 COMMENT '累计出租次数',
  view_count      INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  description     VARCHAR(500) DEFAULT NULL COMMENT '车辆备注',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_vehicle_no (vehicle_no),
  UNIQUE KEY uk_license_plate (license_plate),
  KEY idx_model (model_id),
  KEY idx_brand (brand_id),
  KEY idx_store (store_id),
  KEY idx_status (status),
  KEY idx_daily_price (daily_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆核心信息表';

-- 10. 车辆图片表
DROP TABLE IF EXISTS vehicle_image;
CREATE TABLE vehicle_image (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  vehicle_id  BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  image_url   VARCHAR(255) NOT NULL COMMENT '图片地址',
  image_type  VARCHAR(20)  NOT NULL DEFAULT 'EXTERIOR' COMMENT '类型 EXTERIOR外观/INTERIOR内饰/LICENSE证件',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号，第1张为主图',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_vehicle (vehicle_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆图片表';

-- 11. 车辆保养维修记录表
DROP TABLE IF EXISTS vehicle_maintenance;
CREATE TABLE vehicle_maintenance (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  vehicle_id   BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  maint_type   VARCHAR(20)  NOT NULL DEFAULT 'MAINTAIN' COMMENT '类型 MAINTAIN保养/REPAIR维修',
  maint_date   DATE         NOT NULL COMMENT '保养维修日期',
  mileage      DECIMAL(10,1) DEFAULT NULL COMMENT '送保里程(万公里)',
  cost         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '费用',
  content      VARCHAR(500) DEFAULT NULL COMMENT '保养维修内容',
  operator_id  BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人(网点管理员)',
  next_date    DATE         DEFAULT NULL COMMENT '下次保养日期，用于到期提醒',
  remark       VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_vehicle (vehicle_id, maint_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆保养维修记录表';

-- 12. 车辆保险记录表（保险到期是车辆能否上架的前置条件）
DROP TABLE IF EXISTS vehicle_insurance;
CREATE TABLE vehicle_insurance (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  vehicle_id     BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  insurance_type VARCHAR(20)  NOT NULL DEFAULT 'COMMERCIAL' COMMENT '类型 COMPULSORY交强险/COMMERCIAL商业险',
  insurer        VARCHAR(80)  DEFAULT NULL COMMENT '保险公司(模拟)',
  policy_no      VARCHAR(50)  DEFAULT NULL COMMENT '保单号(模拟)',
  start_date     DATE         NOT NULL COMMENT '保险起期',
  end_date       DATE         NOT NULL COMMENT '保险止期',
  premium        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '保费',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1有效 0失效',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_vehicle (vehicle_id),
  KEY idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆保险记录表';

-- 13. 车辆违章记录表
DROP TABLE IF EXISTS vehicle_violation;
CREATE TABLE vehicle_violation (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  vehicle_id      BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  order_id        BIGINT UNSIGNED DEFAULT NULL COMMENT '关联订单(定位到具体租车人)',
  user_id         BIGINT UNSIGNED DEFAULT NULL COMMENT '责任用户',
  violation_time  DATETIME     NOT NULL COMMENT '违章时间',
  violation_place VARCHAR(200) DEFAULT NULL COMMENT '违章地点',
  violation_type  VARCHAR(100) DEFAULT NULL COMMENT '违章类型',
  fine_amount     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '罚款金额',
  points          INT          NOT NULL DEFAULT 0 COMMENT '扣分',
  handle_status   VARCHAR(20)  NOT NULL DEFAULT 'UNHANDLED' COMMENT '处理状态 UNHANDLED未处理/HANDLED已处理/APPEALING申诉中',
  handle_time     DATETIME     DEFAULT NULL COMMENT '处理时间',
  remark          VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_vehicle (vehicle_id, violation_time),
  KEY idx_status (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆违章记录表';

-- =====================================================================
-- 模块三：网点与调度管理（3 张）
-- =====================================================================

-- 14. 多网点信息表（不含地图渲染能力：经纬度仅存储，前端不加载任何地图 SDK）
DROP TABLE IF EXISTS store;
CREATE TABLE store (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '网点ID',
  store_code     VARCHAR(20)  NOT NULL COMMENT '网点编码',
  store_name     VARCHAR(80)  NOT NULL COMMENT '网点名称',
  address        VARCHAR(200) NOT NULL COMMENT '网点地址(文本)',
  phone          VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  business_hours VARCHAR(50)  DEFAULT '08:00-20:00' COMMENT '营业时间',
  longitude      DECIMAL(10,6) DEFAULT NULL COMMENT '经度(仅存储，不做地图渲染)',
  latitude       DECIMAL(10,6) DEFAULT NULL COMMENT '纬度(仅存储)',
  manager_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '网点负责人(用户ID)',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1营业 0停业',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_code (store_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网点信息表';

-- 15. 网点员工关联表（一个网点多个管理员/员工，权限按网点隔离）
DROP TABLE IF EXISTS store_employee;
CREATE TABLE store_employee (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  store_id    BIGINT UNSIGNED NOT NULL COMMENT '网点ID',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  position    VARCHAR(50)  DEFAULT '店长' COMMENT '岗位',
  entry_date  DATE         DEFAULT NULL COMMENT '入职日期',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1在职 0离职',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_user (store_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网点员工关联表';

-- 16. 跨网点调车申请表
DROP TABLE IF EXISTS dispatch_apply;
CREATE TABLE dispatch_apply (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  apply_no      VARCHAR(32) NOT NULL COMMENT '申请单号',
  vehicle_id    BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  from_store_id BIGINT UNSIGNED NOT NULL COMMENT '调出网点',
  to_store_id   BIGINT UNSIGNED NOT NULL COMMENT '调入网点',
  apply_user_id BIGINT UNSIGNED NOT NULL COMMENT '申请人(网点管理员)',
  reason        VARCHAR(200) DEFAULT NULL COMMENT '调车原因',
  status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING待审批/APPROVED通过/REJECTED驳回/CANCELLED撤回',
  audit_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人(系统管理员)',
  audit_time    DATETIME DEFAULT NULL COMMENT '审批时间',
  audit_remark  VARCHAR(200) DEFAULT NULL COMMENT '审批意见',
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply_no (apply_no),
  KEY idx_vehicle_status (vehicle_id, status),
  KEY idx_from_store (from_store_id),
  KEY idx_to_store (to_store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨网点调车申请表';

-- =====================================================================
-- 模块四：租赁订单与交易（6 张）
-- 订单主表 + 明细（一单多车）+ 支付流水 + 退款流水 + 评价 + 地址
-- =====================================================================

-- 17. 租赁订单主表（信用快照字段：历史订单押金不随用户当前分数漂移）
DROP TABLE IF EXISTS lease_order;
CREATE TABLE lease_order (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  order_no              VARCHAR(32)  NOT NULL COMMENT '订单号',
  user_id               BIGINT UNSIGNED NOT NULL COMMENT '下单用户',
  store_id              BIGINT UNSIGNED NOT NULL COMMENT '取车网点',
  return_store_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '归还网点(支持异地还车)',
  start_date            DATE         NOT NULL COMMENT '租期开始日期',
  end_date              DATE         NOT NULL COMMENT '租期结束日期',
  rent_days             INT          NOT NULL COMMENT '租期天数',
  vehicle_count         INT          NOT NULL DEFAULT 1 COMMENT '车辆数量(明细条数)',
  rent_amount           DECIMAL(10,2) NOT NULL COMMENT '租金合计=Σ明细租金',
  deposit_amount        DECIMAL(10,2) NOT NULL COMMENT '押金合计=Σ(标准押金×信用系数)',
  total_amount          DECIMAL(10,2) NOT NULL COMMENT '应付合计=租金+押金',
  credit_score_snapshot INT          NOT NULL DEFAULT 100 COMMENT '快照:下单时信用分',
  credit_level_snapshot VARCHAR(20)  NOT NULL DEFAULT 'GOOD' COMMENT '快照:下单时信用等级',
  deposit_coefficient   DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '快照:押金系数',
  status                VARCHAR(20)  NOT NULL COMMENT '状态 WAIT_PAY/WAIT_PICKUP/IN_USE/WAIT_RETURN/RETURNED/COMPLETED/CANCELLED/OVERDUE',
  pay_time              DATETIME     DEFAULT NULL COMMENT '支付时间',
  pickup_time           DATETIME     DEFAULT NULL COMMENT '取车/交付时间',
  return_apply_time     DATETIME     DEFAULT NULL COMMENT '申请归还时间',
  return_time           DATETIME     DEFAULT NULL COMMENT '确认归还时间',
  finish_time           DATETIME     DEFAULT NULL COMMENT '订单完成时间',
  cancel_time           DATETIME     DEFAULT NULL COMMENT '取消时间',
  cancel_reason         VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
  overdue_days          INT          NOT NULL DEFAULT 0 COMMENT '逾期天数',
  late_fee              DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '违约金=日租金×逾期天数×1.5',
  deposit_refund_status VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT '押金退还状态 NONE/PENDING/REFUNDED',
  remark                VARCHAR(255) DEFAULT NULL COMMENT '用户备注',
  create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted               TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_status (user_id, status),
  KEY idx_store_status (store_id, status),
  KEY idx_status_end (status, end_date),
  KEY idx_create_time (create_time),
  KEY idx_level_snapshot (credit_level_snapshot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁订单主表';

-- 18. 订单明细表（一单多车；下单时快照车名/车牌/日租金）
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id       BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  vehicle_id     BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  vehicle_name   VARCHAR(100) NOT NULL COMMENT '快照:车辆名称(品牌+车型)',
  license_plate  VARCHAR(20)  DEFAULT NULL COMMENT '快照:车牌',
  daily_price    DECIMAL(10,2) NOT NULL COMMENT '快照:日租金',
  quantity       INT          NOT NULL DEFAULT 1 COMMENT '数量',
  rent_days      INT          NOT NULL COMMENT '租期天数(快照)',
  discount_rate  DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '快照:折扣',
  rent_amount    DECIMAL(10,2) NOT NULL COMMENT '本车租金=日租金×天数×数量×折扣',
  deposit_amount DECIMAL(10,2) NOT NULL COMMENT '本车押金=标准押金×信用系数',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_order (order_id),
  KEY idx_vehicle (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 19. 支付流水表（模拟支付；营收口径只统计 RENT）
DROP TABLE IF EXISTS payment_record;
CREATE TABLE payment_record (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  pay_no      VARCHAR(32)  NOT NULL COMMENT '支付流水号(模拟)',
  order_id    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '付款人',
  pay_type    VARCHAR(20)  NOT NULL COMMENT '类型 RENT租金/DEPOSIT押金/LATE_FEE违约金',
  amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_method  VARCHAR(20)  NOT NULL DEFAULT 'MOCK_WECHAT' COMMENT '模拟渠道 MOCK_WECHAT/MOCK_ALIPAY',
  status      VARCHAR(20)  NOT NULL COMMENT '状态 PENDING/SUCCESS/FAILED',
  pay_time    DATETIME     DEFAULT NULL COMMENT '支付成功时间',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order (order_id),
  KEY idx_type_status (pay_type, status),
  KEY idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表(模拟)';

-- 20. 退款流水表（押金退还、取消退款、违约金冲正）
DROP TABLE IF EXISTS refund_record;
CREATE TABLE refund_record (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  refund_no   VARCHAR(32)  NOT NULL COMMENT '退款流水号(模拟)',
  order_id    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  payment_id  BIGINT UNSIGNED DEFAULT NULL COMMENT '原支付流水ID(押金退还时指向押金流水)',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '收款用户',
  refund_type VARCHAR(20)  NOT NULL COMMENT '类型 DEPOSIT押金退还/CANCEL取消退款/LATE_FEE违约金冲正',
  amount      DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  reason      VARCHAR(200) DEFAULT NULL COMMENT '退款原因',
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING待退款/REFUNDED已退款/FAILED失败',
  apply_time  DATETIME     DEFAULT NULL COMMENT '申请时间',
  refund_time DATETIME     DEFAULT NULL COMMENT '退款完成时间',
  channel     VARCHAR(20)  NOT NULL DEFAULT 'MOCK_WECHAT' COMMENT '退款渠道(模拟)',
  remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_order (order_id),
  KEY idx_user (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水表(模拟)';

-- 21. 车辆评价表（一单一评，只有已完成订单可评价）
DROP TABLE IF EXISTS order_evaluation;
CREATE TABLE order_evaluation (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '评价人',
  vehicle_id  BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  rating         TINYINT   NOT NULL DEFAULT 5 COMMENT '综合评分 1-5，车辆与门店两项平均后四舍五入',
  vehicle_rating TINYINT   NOT NULL DEFAULT 5 COMMENT '车辆状况评分 1-5',
  store_rating   TINYINT   NOT NULL DEFAULT 5 COMMENT '门店服务评分 1-5',
  content     VARCHAR(500) NOT NULL COMMENT '评价内容',
  images      TEXT         DEFAULT NULL COMMENT '图片 JSON 数组',
  reply       VARCHAR(200) DEFAULT NULL COMMENT '商家（网点）解释性回复',
  reply_time  DATETIME     DEFAULT NULL COMMENT '回复时间',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1显示 0隐藏',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order (order_id),
  KEY idx_vehicle (vehicle_id, status),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆评价表';

-- 22. 用户地址表（用于合同/发票寄送，本系统不接物流）
DROP TABLE IF EXISTS user_address;
CREATE TABLE user_address (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  receiver    VARCHAR(50)  NOT NULL COMMENT '收件人',
  phone       VARCHAR(20)  NOT NULL COMMENT '联系电话',
  province    VARCHAR(50)  NOT NULL COMMENT '省',
  city        VARCHAR(50)  NOT NULL COMMENT '市',
  district    VARCHAR(50)  DEFAULT NULL COMMENT '区县',
  detail      VARCHAR(200) NOT NULL COMMENT '详细地址',
  is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- =====================================================================
-- 模块五：信用风控（4 张，核心特色）
-- =====================================================================

-- 23. 信用规则与押金系数配置表
--     rule_type=LEVEL  → 等级区间与押金系数
--     rule_type=ACTION → 行为加减分（完成/按时归还/逾期/取消）
--     两类配置都是「可后台修改」的业务参数，合并为一张规则表用 rule_type 区分，
--     避免为两张结构相近的配置表各建一套 CRUD。
DROP TABLE IF EXISTS credit_rule;
CREATE TABLE credit_rule (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  rule_type           VARCHAR(20)  NOT NULL COMMENT '规则类型 LEVEL等级规则/ACTION行为规则',
  rule_code           VARCHAR(30)  NOT NULL COMMENT '规则编码: EXCELLENT/GOOD/NORMAL/WATCH 或 ORDER_FINISH/ON_TIME_RETURN/OVERDUE/OVERDUE_RETURN/CANCEL_PAID/ADMIN_ADJUST',
  rule_name           VARCHAR(50)  NOT NULL COMMENT '规则名称',
  min_score           INT          DEFAULT NULL COMMENT '等级规则: 区间下限(含)',
  max_score           INT          DEFAULT NULL COMMENT '等级规则: 区间上限(含)',
  deposit_coefficient DECIMAL(3,2) DEFAULT NULL COMMENT '等级规则: 押金系数 0.80/0.90/1.00/1.20',
  score_value         INT          DEFAULT NULL COMMENT '行为规则: 分值(可负)',
  max_times           INT          DEFAULT 1 COMMENT '行为规则: 单订单最多生效次数',
  color               VARCHAR(20)  DEFAULT NULL COMMENT '等级规则: 前端标签颜色',
  description         VARCHAR(200) DEFAULT NULL COMMENT '规则说明(前端展示)',
  sort_no             INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_code (rule_type, rule_code),
  KEY idx_min_score (min_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用规则与押金系数配置表';

-- 24. 信用分变动日志表（分数可复算；uk_dedup 保证同一订单同一事件只记一次）
DROP TABLE IF EXISTS credit_log;
CREATE TABLE credit_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  action_code  VARCHAR(30)  NOT NULL COMMENT '事件码，对应 credit_rule.rule_code',
  change_type  VARCHAR(20)  NOT NULL DEFAULT 'EVENT' COMMENT '类型 EVENT自动/ADMIN手工/APPEAL申诉',
  score_before INT          NOT NULL COMMENT '变动前分数',
  score_change INT          NOT NULL COMMENT '变动值(可负)',
  score_after  INT          NOT NULL COMMENT '变动后分数',
  order_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '关联订单',
  dedup_key    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '幂等键，无则空串',
  reason       VARCHAR(200) DEFAULT NULL COMMENT '变动原因(手工调整必填)',
  operator_id  BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dedup (order_id, action_code, dedup_key),
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分变动日志表';

-- 25. 信用申诉记录表（用户对扣分有异议时申诉，管理员复核后可调整分数）
DROP TABLE IF EXISTS credit_appeal;
CREATE TABLE credit_appeal (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  appeal_no     VARCHAR(32)  NOT NULL COMMENT '申诉单号',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '申诉用户',
  credit_log_id BIGINT UNSIGNED DEFAULT NULL COMMENT '针对的扣分记录',
  order_id      BIGINT UNSIGNED DEFAULT NULL COMMENT '关联订单',
  appeal_reason VARCHAR(500) NOT NULL COMMENT '申诉理由',
  evidence      TEXT         DEFAULT NULL COMMENT '证据图片 JSON 数组',
  status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING待处理/APPROVED通过/REJECTED驳回',
  handle_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人',
  handle_time   DATETIME     DEFAULT NULL COMMENT '处理时间',
  handle_remark VARCHAR(200) DEFAULT NULL COMMENT '处理意见',
  restore_score INT          NOT NULL DEFAULT 0 COMMENT '申诉通过补回分数',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_appeal_no (appeal_no),
  KEY idx_user (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用申诉记录表';

-- 26. 风控黑名单/预警表（逾期未还、严重违约自动/手工加入，影响下单与押金）
DROP TABLE IF EXISTS credit_blacklist;
CREATE TABLE credit_blacklist (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  list_type      VARCHAR(20)  NOT NULL DEFAULT 'WARNING' COMMENT '类型 WARNING预警/OVERRIDE限制下单/BLACKLIST黑名单',
  reason         VARCHAR(200) NOT NULL COMMENT '加入原因',
  trigger_rule   VARCHAR(100) DEFAULT NULL COMMENT '触发规则(如 连续逾期2次)',
  effective_time DATETIME     NOT NULL COMMENT '生效时间',
  expire_time    DATETIME     DEFAULT NULL COMMENT '失效时间，空表示长期',
  status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE生效/RELEASED解除',
  operator_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人(系统自动则为空)',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_user_status (user_id, status),
  KEY idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控黑名单预警表';

-- =====================================================================
-- 模块六：运营与系统审计（4 张）
-- =====================================================================

-- 27. 系统公告表
DROP TABLE IF EXISTS announcement;
CREATE TABLE announcement (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(100) NOT NULL COMMENT '公告标题',
  content      TEXT         NOT NULL COMMENT '公告正文',
  type         VARCHAR(20)  NOT NULL DEFAULT 'NOTICE' COMMENT '类型 NOTICE公告/ACTIVITY活动/RULE规则',
  publisher_id BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1已发布 0草稿',
  view_count   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  publish_time DATETIME     DEFAULT NULL COMMENT '发布时间',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_status_time (status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 28. 首页轮播图表
DROP TABLE IF EXISTS banner;
CREATE TABLE banner (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(80)  NOT NULL COMMENT '标题',
  image_url   VARCHAR(255) NOT NULL COMMENT '图片地址',
  link_url    VARCHAR(255) DEFAULT NULL COMMENT '跳转地址',
  position    VARCHAR(20)  NOT NULL DEFAULT 'HOME' COMMENT '展示位置 HOME首页/PORTAL列表页',
  sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1显示 0隐藏',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_sort (position, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图表';

-- 29. 用户收藏表
DROP TABLE IF EXISTS favorite;
CREATE TABLE favorite (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  vehicle_id  BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_vehicle (user_id, vehicle_id),
  KEY idx_vehicle (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 30. 系统操作日志表（AOP 切面写入后台写操作）
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
  username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人账号',
  module      VARCHAR(50)  DEFAULT NULL COMMENT '业务模块',
  operation   VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
  method      VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  request_uri VARCHAR(200) DEFAULT NULL COMMENT '请求地址',
  params      TEXT         DEFAULT NULL COMMENT '请求参数',
  ip          VARCHAR(50)  DEFAULT NULL COMMENT '客户端IP',
  cost_ms     BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时(毫秒)',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_create_time (create_time),
  KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';


-- #####################################################################
--  必要初始数据
--  （演示业务数据见 02-seed.sql：品牌/车型/车辆/订单/支付/评价等）
-- #####################################################################

-- 1) 角色
INSERT INTO sys_role (id, role_code, role_name, description, sort_no) VALUES
(1, 'USER',        '普通用户',   '浏览车辆、在线租赁、查看信用分与申诉', 1),
(2, 'STORE_ADMIN', '网点管理员', '管理本网点车辆、订单交付归还、发起调车申请', 2),
(3, 'SYS_ADMIN',   '系统管理员', '平台运营、信用风控、调度审批、基础数据管理', 3);

-- 2) 初始账号（密码均为 123456，BCrypt 10 轮）
INSERT INTO sys_user (id, username, password, real_name, phone, email, credit_score, credit_level, store_id, status) VALUES
(1, 'admin',  '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '系统管理员', '13800000001', 'admin@zuche.com',  100, 'GOOD', NULL, 1),
(2, 'store1', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '王店长',     '13800000002', 'store1@zuche.com', 100, 'GOOD', 1,    1),
(3, 'store2', '$2a$10$tBD/P3b4/p1JzLWpifIqVuScXtBfCKpZ3EhBWs5pwH8.PvvXZQl3i', '李店长',     '13800000003', 'store2@zuche.com', 100, 'GOOD', 2,    1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 3), (2, 2), (3, 2);

-- 3) 菜单（目录 M / 菜单 C / 按钮 F），前端按角色渲染，后端 perms 作为权限点
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_no, visible, status) VALUES
(1,   0,   '首页',       'C', '/',                  'portal/Home',           NULL,                  'HomeFilled', 1, 1, 1),
(2,   0,   '车辆租赁',   'M', NULL,                  NULL,                    NULL,                  'Van',        2, 1, 1),
(21,  2,   '车辆列表',   'C', '/vehicles',           'portal/VehicleList',    'vehicle:list',        'List',       1, 1, 1),
(22,  2,   '车辆详情',   'C', '/vehicles/:id',       'portal/VehicleDetail',  'vehicle:detail',      NULL,         2, 0, 1),
(3,   0,   '我的订单',   'C', '/orders',             'user/OrderList',        'order:my',            'Tickets',    3, 1, 1),
(4,   0,   '信用中心',   'C', '/credit',             'user/CreditCenter',     'credit:self',         'Medal',      4, 1, 1),
(5,   0,   '我的收藏',   'C', '/favorites',          'user/Favorite',         'favorite:list',       'Star',       5, 1, 1),
(6,   0,   '个人中心',   'C', '/profile',            'user/Profile',          'user:profile',        'User',       6, 1, 1),
(10,  0,   '网点管理',   'M', '/store',              NULL,                    NULL,                  'OfficeBuilding', 10, 1, 1),
(101, 10,  '网点概览',   'C', '/store/overview',     'store/Overview',        'store:overview',      'DataLine',   1, 1, 1),
(102, 10,  '网点车辆',   'C', '/store/vehicles',     'store/VehicleManage',   'store:vehicle',       'Van',        2, 1, 1),
(103, 10,  '网点订单',   'C', '/store/orders',       'store/OrderManage',     'store:order',         'Tickets',    3, 1, 1),
(104, 10,  '调车申请',   'C', '/store/dispatch',     'store/DispatchApply',   'dispatch:apply',      'Switch',     4, 1, 1),
(105, 10,  '客户评价',   'C', '/store/evaluations',  'store/EvaluationReply', 'evaluation:reply',    'ChatLineSquare', 5, 1, 1),
(20,  0,   '平台管理',   'M', '/admin',              NULL,                    NULL,                  'Setting',    20, 1, 1),
(201, 20,  '数据看板',   'C', '/admin/dashboard',    'admin/Dashboard',       'dashboard:view',      'DataAnalysis', 1, 1, 1),
(212, 20,  '智能分析',   'C', '/admin/analytics',    'admin/Analytics',       'analytics:view',      'TrendCharts', 2, 1, 1),
(202, 20,  '信用风控',   'M', '/admin/credit',       NULL,                    NULL,                  'Medal',      2, 1, 1),
(2021,202, '信用规则',   'C', '/admin/credit/rules', 'admin/credit/RuleConfig',   'credit:rule:list', 'Setting', 1, 1, 1),
(2022,202, '用户信用',   'C', '/admin/credit/users', 'admin/credit/UserCredit',   'credit:user:list', 'User',    2, 1, 1),
(2023,202, '信用申诉',   'C', '/admin/credit/appeals','admin/credit/AppealList',  'credit:appeal:list','ChatDotSquare', 3, 1, 1),
(2024,202, '风控名单',   'C', '/admin/credit/blacklist','admin/credit/Blacklist', 'credit:blacklist:list','Warning', 4, 1, 1),
(203, 20,  '调度审批',   'C', '/admin/dispatch',     'admin/DispatchAudit',   'dispatch:audit',      'Switch',     3, 1, 1),
(204, 20,  '车辆资产',   'M', '/admin/vehicle',      NULL,                    NULL,                  'Van',        4, 1, 1),
(2041,204, '品牌管理',   'C', '/admin/vehicle/brands','admin/vehicle/BrandList','vehicle:brand:list','Collection', 1, 1, 1),
(2042,204, '车型管理',   'C', '/admin/vehicle/models','admin/vehicle/ModelList','vehicle:model:list','Grid',    2, 1, 1),
(2043,204, '车辆档案',   'C', '/admin/vehicle/list', 'admin/vehicle/VehicleList','vehicle:manage',   'Van',        3, 1, 1),
(2044,204, '保养维修',   'C', '/admin/vehicle/maintenance','admin/vehicle/Maintenance','vehicle:maintenance:list','Tools', 4, 1, 1),
(2045,204, '保险记录',   'C', '/admin/vehicle/insurance','admin/vehicle/Insurance','vehicle:insurance:list','Document', 5, 1, 1),
(2046,204, '违章记录',   'C', '/admin/vehicle/violation','admin/vehicle/Violation','vehicle:violation:list','Warning', 6, 1, 1),
(205, 20,  '订单管理',   'C', '/admin/orders',       'admin/OrderManage',     'order:manage',        'Tickets',    5, 1, 1),
(206, 20,  '用户管理',   'C', '/admin/users',        'admin/UserManage',      'user:manage',         'UserFilled', 6, 1, 1),
(207, 20,  '网点管理',   'C', '/admin/stores',       'admin/StoreManage',     'store:manage',        'OfficeBuilding', 7, 1, 1),
(208, 20,  '评价管理',   'C', '/admin/evaluations',  'admin/EvaluationManage','evaluation:manage',   'ChatLineSquare', 8, 1, 1),
(209, 20,  '公告管理',   'C', '/admin/announcements','admin/AnnouncementManage','announcement:manage','Bell',      9, 1, 1),
(210, 20,  '轮播图管理', 'C', '/admin/banners',      'admin/BannerManage',    'banner:manage',       'Picture',    10, 1, 1),
(211, 20,  '操作日志',   'C', '/admin/logs',         'admin/OperationLog',    'log:list',            'Document',   11, 1, 1),
(20211,2021,'规则编辑',  'F', NULL, NULL,             'credit:rule:edit',    NULL, 1, 0, 1),
(20221,2022,'信用调整',  'F', NULL, NULL,             'credit:user:adjust',  NULL, 1, 0, 1),
(20301,203, '调度审批',  'F', NULL, NULL,             'dispatch:approve',    NULL, 1, 0, 1),
(20501,205, '订单关闭',  'F', NULL, NULL,             'order:close',         NULL, 1, 0, 1);

-- 智能分析插在数据看板之后，原 202~211 整体顺延一位，保证"看板 → 智能分析 → 信用风控"的顺序
UPDATE sys_menu SET sort_no = sort_no + 1 WHERE parent_id = 20 AND id BETWEEN 202 AND 211;

-- 4) 角色菜单授权：管理员全量，用户只看前台，网点管理员看首页+订单+网点管理
--    网点管理员能进「客户评价」只能回复，删除接口在后端限定 SYS_ADMIN
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 3, id FROM sys_menu;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (1, 2, 21, 22, 3, 4, 5, 6);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1, 3, 10, 101, 102, 103, 104, 105);

-- 5) 数据字典：订单状态机、车型分类、变速箱、能源、支付方式、信用等级、调度状态、申诉状态
INSERT INTO sys_dict_data (dict_type, dict_code, dict_label, dict_value, sort_no, remark) VALUES
('order_status', 'WAIT_PAY',    '待支付',   'warning', 1, '下单后未支付'),
('order_status', 'WAIT_PICKUP', '待取车',   'primary', 2, '已支付待网点交付'),
('order_status', 'IN_USE',      '已取车',   'success', 3, '在租中'),
('order_status', 'WAIT_RETURN', '待归还',   'info',    4, '用户已申请归还'),
('order_status', 'RETURNED',    '已归还',   'info',    5, '网点已确认归还，待结算'),
('order_status', 'COMPLETED',   '已完成',   'success', 6, '押金已退还，终态'),
('order_status', 'CANCELLED',   '已取消',   'danger',  7, '终态'),
('order_status', 'OVERDUE',     '已逾期',   'danger',  8, '超过结束日期未归还'),

('vehicle_category', 'ECONOMY', '经济型', '#909399', 1, '日租 100-150'),
('vehicle_category', 'COMFORT', '舒适型', '#409EFF', 2, '日租 200-250'),
('vehicle_category', 'SUV',     'SUV',    '#67C23A', 3, '日租 280-340'),
('vehicle_category', 'LUXURY',  '豪华型', '#E6A23C', 4, '日租 600-800'),
('vehicle_category', 'EV',      '新能源', '#00B894', 5, '纯电/混动'),

('gearbox',   'AUTO',   '自动挡', NULL, 1, NULL),
('gearbox',   'MANUAL', '手动挡', NULL, 2, NULL),

('fuel_type', 'GASOLINE', '汽油', NULL, 1, NULL),
('fuel_type', 'HYBRID',   '混动', NULL, 2, NULL),
('fuel_type', 'ELECTRIC', '纯电', NULL, 3, NULL),

('vehicle_status', '0', '下架',   'info',    1, NULL),
('vehicle_status', '1', '可租',   'success', 2, NULL),
('vehicle_status', '2', '已租出', 'warning', 3, NULL),
('vehicle_status', '3', '维修中', 'danger',  4, NULL),

('pay_method', 'MOCK_WECHAT', '模拟微信支付', NULL, 1, '不接入真实渠道'),
('pay_method', 'MOCK_ALIPAY', '模拟支付宝',   NULL, 2, '不接入真实渠道'),

('credit_level', 'EXCELLENT', '优秀',   '#67C23A', 1, '120-150 分，押金 8 折'),
('credit_level', 'GOOD',      '良好',   '#409EFF', 2, '100-119 分，押金 9 折'),
('credit_level', 'NORMAL',    '一般',   '#E6A23C', 3, '80-99 分，押金原价'),
('credit_level', 'WATCH',     '待观察', '#F56C6C', 4, '0-79 分，押金 1.2 倍'),

('dispatch_status', 'PENDING',   '待审批', 'warning', 1, NULL),
('dispatch_status', 'APPROVED',  '已通过', 'success', 2, NULL),
('dispatch_status', 'REJECTED',  '已驳回', 'danger',  3, NULL),
('dispatch_status', 'CANCELLED', '已撤回', 'info',    4, NULL),

('appeal_status', 'PENDING',  '待处理', 'warning', 1, NULL),
('appeal_status', 'APPROVED', '已通过', 'success', 2, NULL),
('appeal_status', 'REJECTED', '已驳回', 'danger',  3, NULL),

('violation_status', 'UNHANDLED', '未处理', 'danger',  1, NULL),
('violation_status', 'HANDLED',   '已处理', 'success', 2, NULL),
('violation_status', 'APPEALING', '申诉中', 'warning', 3, NULL),

('blacklist_type', 'WARNING',   '预警',     'warning', 1, '限制高价值车型'),
('blacklist_type', 'OVERRIDE',  '限制下单', 'danger',  2, '需人工审核'),
('blacklist_type', 'BLACKLIST', '黑名单',   'danger',  3, '禁止下单');

-- 6) 信用规则：等级 4 条 + 行为 6 条（阈值与系数均可后台修改）
INSERT INTO credit_rule (rule_type, rule_code, rule_name, min_score, max_score, deposit_coefficient, score_value, max_times, color, description, sort_no) VALUES
('LEVEL',  'EXCELLENT',    '优秀',   120, 150, 0.80, NULL, NULL, '#67C23A', '信用分 120-150，押金 8 折', 1),
('LEVEL',  'GOOD',         '良好',   100, 119, 0.90, NULL, NULL, '#409EFF', '信用分 100-119，押金 9 折', 2),
('LEVEL',  'NORMAL',       '一般',    80,  99, 1.00, NULL, NULL, '#E6A23C', '信用分 80-99，押金原价', 3),
('LEVEL',  'WATCH',        '待观察',   0,  79, 1.20, NULL, NULL, '#F56C6C', '信用分 0-79，押金 1.2 倍', 4),
('ACTION', 'ORDER_FINISH', '订单完成', NULL, NULL, NULL, 2,  1, NULL, '订单走完取车-归还-结算全流程，加 2 分', 11),
('ACTION', 'ON_TIME_RETURN','按时归还', NULL, NULL, NULL, 5,  1, NULL, '未逾期归还，加 5 分', 12),
('ACTION', 'OVERDUE',      '逾期未还', NULL, NULL, NULL, -3, 7, NULL, '每逾期 1 天扣 3 分，单订单最多扣 7 天', 13),
('ACTION', 'OVERDUE_RETURN','逾期后归还',NULL, NULL, NULL, -5, 1, NULL, '逾期订单归还时额外扣 5 分', 14),
('ACTION', 'CANCEL_PAID',  '取消已支付订单',NULL,NULL,NULL,-3,1, NULL, '支付后取消占用库存，扣 3 分', 15),
('ACTION', 'ADMIN_ADJUST', '管理员调整', NULL, NULL, NULL, 0,  1, NULL, '人工校准，分值由管理员填写', 16);

-- 7) 网点
INSERT INTO store (id, store_code, store_name, address, phone, business_hours, longitude, latitude, manager_id, status) VALUES
(1, 'S001', '泉州南安店', '福建省泉州市南安市成功街 88 号（南安汽车站旁）', '0595-86261001', '08:00-21:00', 118.386000, 24.960000, 2, 1),
(2, 'S002', '泉州晋江店', '福建省泉州市晋江市青阳街道阳光路 12 号', '0595-85602002', '08:00-20:00', 118.552000, 24.781000, 3, 1);

INSERT INTO store_employee (store_id, user_id, position, entry_date, status) VALUES
(1, 2, '店长', '2024-03-01', 1),
(2, 3, '店长', '2024-05-10', 1);
