# TASKS.md — 基于信用风控的微租车平台

> 状态：`[ ]` 未开始 / `[~]` 进行中 / `[x]` 完成
> 交付纪律：每轮只做 1~2 个任务，改完必跑验证命令。

## 阶段 0：环境与骨架（已完成）
- [x] 扫描仓库（`D:/zuche` 初始为空）
- [x] 需求边界、ER 设计、接口清单、目录结构（`docs/`）
- [x] 确认 MySQL 8.0.46 / 3306 / root / utf8mb4 可用
- [x] **数据库重构为 30 张表**（6 大模块），`sql/01-schema.sql` 含建表 + 必要初始数据
- [x] `sql/02-seed.sql` 演示数据：9 品牌 / 24 车型 / 24 车 / 72 图 / 300 单 / 400 明细 / 540 支付 / 210 退款
- [x] 一致性校验：信用分恒等式 0 异常，订单金额与明细 0 异常
- [x] 后端骨架：pom、application.yml、统一 `Result`/`PageResult`、全局异常、JWT、`@RequireRole` 拦截器、Knife4j
- [x] 后端编译打包通过（`mvn -DskipTests package`，BUILD SUCCESS）
- [x] 服务启动验证：`/api/system/health` 200、`/doc.html` 200、`/v3/api-docs` 200、无 token 401
- [x] 前端脚手架：Vite 5 + Vue 3 + Element Plus + Pinia + Router + Axios 封装
- [x] `npm install` 通过（86 包）、`npm run build` 通过（1678 modules）
- [x] `mvnw` / `mvnw.cmd` 生成，规避本机无 Maven 的问题
- [ ] 登录接口人工回归（curl 含凭据，需本人终端执行；命令见 README 第五节）

## 阶段 1：基础域与登录（已完成）
- [x] 注册接口（`POST /api/auth/register`，默认信用分 100 / GOOD / USER，事务内写 user + user_role）
- [x] 菜单接口 `/api/auth/menus`（按角色返回 sys_menu 树，M/C 类型，含 icon/path/component）
- [x] 用户/角色/网点/品牌/车型/车辆/图片 实体与 Mapper
- [x] 车辆列表（关键词/分类/品牌/网点/价格/排序/只看可租 + 分页）
- [x] 车辆详情（多图图集、门店地址电话营业时间、车型参数、浏览量 +1）
- [x] 车辆筛选项 `/api/vehicles/options`（字典分类 + 品牌 + 网点 + 价格区间）
- [x] 前台页面：注册、车辆列表、车辆详情（图片本地化，带加载失败兜底）
- [x] 首页接入真实数据：分类、热门车型、价格区间、车辆总数
- [x] 后台 Layout + 动态菜单 + 路由守卫（/admin 与 /store 按角色隔离，未实现模块进 Placeholder）
- [x] 数据库调整：删除 `store.vehicle_count` 冗余字段；`sys_menu` 前台路由与前端对齐
- [x] **验证**：车辆接口公开可访问；普通用户菜单 6 项；系统管理员菜单含平台管理；网点管理员仅网点管理；前端 build 通过
- [ ] **待你本人回归**：注册接口（含密码的 curl 被本机安全策略拦截，需在终端或浏览器执行）

## 阶段 2：租赁主流程（P0）—— S2 已完成部分
- [x] `OrderStatus` 状态机（枚举 + `OrderStateMachine` 断言，非法流转返回 1001）
- [x] 价格计算：租金/押金/合计（BigDecimal 两位小数，含信用系数快照）
- [x] 试算 `/api/orders/lease-quote`（按 credit_rule 系数算押金减免）
- [x] 下单：校验可租状态与租期、写 `order_item`、写信用快照、状态 `WAIT_PAY`
- [x] 模拟支付：RENT + DEPOSIT 两条 `payment_record`、`WAIT_PAY → WAIT_PICKUP`
- [x] 车辆锁定：`UPDATE ... WHERE status=1` CAS，抢不到整单回滚
- [x] 取消（仅 `WAIT_PAY`，释放车辆，不涉及退款与扣分）
- [x] 前端：下单页（实时试算）、收银台（模拟支付）、我的订单、订单详情
- [ ] 取车/交付、申请归还、确认归还（违约金、信用事件）→ S4
- [ ] 押金退还 → `refund_record` → 订单完成 → S4
- [ ] `@Scheduled` 逾期扫描（置 OVERDUE + 按天扣分，幂等）→ S4
- **验收**：全链路已实测；非法流转均被拦截；订单金额与明细/流水一致

## 阶段 3：信用风控（核心特色）—— S3/S4 已完成
- [ ] `CreditService.changeScore()` 统一入口（写 credit_log + 更新 user，同事务）
- [ ] `credit_rule` 读取（LEVEL/ACTION 两类），等级映射与系数查询
- [ ] 事件接入：完成 +2、按时 +5、逾期 -3/天、逾期归还 -5、取消已支付 -3
- [ ] 信用中心页：分数/等级/系数/距下一档、四项行为统计（按时归还率/取消率/逾期次数/完成单数）
- [ ] 后台：等级规则与行为规则配置（区间连续性校验）、用户信用列表、手工调整（原因必填）
- [ ] 后台：信用申诉受理（通过则补分并写日志）、风控黑名单（自动/手工加入）
- [ ] 风控效果对比图：各等级订单量/逾期率/押金减免金额/平均客单价
- [ ] **验证**：`100 + Σscore_change` 恒等于 `credit_score`；重复触发不重复加分

## 阶段 4：网点管理员端（S6 完成，2026-10-30）
- [x] 网点概览（本网点车辆/订单/待处理/在租/营收）→ `GET /api/store/overview`
- [x] 本网点车辆管理（归属校验）+ 调价 / 上下架 / 维保 → `/store/vehicles`
- [x] 保养维修台账录入与查询 → `GET /api/store/maintenances`、`POST /api/store/vehicles/{id}/maintenances`
- [x] 本网点订单：交付、确认归还、押金处理 → `/store/orders` 两个办理入口
- [x] 发起调车申请、撤回、查看审批结果 → `/store/dispatch` + `/api/store/dispatches`
- [x] 平台侧调车审批（通过即迁移车辆归属）→ `/admin/dispatch`
- [x] **验证**：网点 A 看不到网点 B 数据（返回 404 而非 403，不暴露订单存在性）；
      交付后订单状态 WAIT_PICKUP → IN_USE 且 pickup_time 落库；
      归还后违约金按实际逾期天数重算、押金退款、信用分结算全部写库；
      下架车辆后用户端可租列表实时减少（6 → 5 台），上架后恢复；
      调车审批通过后车辆 store_id 迁移，调出网点列表少一台
- 保险与违章台账未做（表已存在：`vehicle_insurance` / `vehicle_violation`，接口未开放）
- 详见 `docs/05-网点工作台.md`、`docs/06-网点车辆与调车.md`

## 阶段 5：系统管理员端 —— 看板已完成（S5），其余未做
- [ ] 看板 KPI（订单量/营收/在租车辆/逾期订单/信用分布）
- [ ] 热力图（14×24 与 7×24 两种 mode，按 create_time 聚合）
- [ ] 车型偏好（分类/品牌/Top10 占比）
- [ ] 营收时序预测（近 30 日真实 + 未来 7 日，线性回归 + R²/MAE）
- [ ] 调度审批（通过改 vehicle.store_id，事务 + operation_log）
- [ ] 品牌/车型/车辆/用户/订单/评价/公告/轮播/日志管理页
- [ ] `operation_log` AOP 切面
- [ ] **验证**：图表全部来自 SQL 聚合，无硬编码；清空订单后图表为空不报错

## 阶段 6：收尾与答辩（未开始）
- [ ] seed 数据调优（热力图双峰明显、预测有趋势、四等级都有量）
- [ ] Swagger 注解补全、接口示例
- [ ] 全量构建回归（后端 package + 前端 build）
- [ ] 5 分钟演示脚本（下单 → 押金差异 → 归还 → 信用变化 → 看板）

## 已知风险
1. 本机无 Maven，使用项目内置 `mvnw`（已缓存 maven 3.10.0 发行版）。
2. 环境变量 `SERVER__PORT` 会覆盖 `server.port`，需用 `--server.port=8080` 显式指定。
3. 前端全量引入 Element Plus，主 chunk 约 1.2MB；S6 视情况改为按需引入。
4. 演示图片已本地化为 SVG（`frontend/public/images`），不再依赖外网；单图缺失时前端回落占位图。
5. 后台菜单依赖 `sys_menu.path` 前缀（`/admin` 或 `/store`），改菜单时需保持前缀一致。
