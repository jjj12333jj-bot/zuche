# 基于信用风控的微租车平台

毕业设计项目：Spring Boot 3 + Vue 3 前后端分离的微租车系统，核心特色是**信用分驱动的动态押金**与**运营分析看板**。

> 支付、短信、地图能力全部为**模拟实现**，不接入任何真实第三方服务；本版本不含在线客服/知识库模块。
>
> **当前进度**：S0 骨架 + S1 基础域已完成（注册、菜单树、车辆列表/详情、前台页面、后台动态菜单框架）。
> 演示图片已全部本地化为 `frontend/public/images/*.svg`，断网也能正常答辩。

---

## 一、技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.2.5、Java 21、MyBatis-Plus 3.5.7、JJWT 0.12.6、Knife4j 4.5.0 |
| 数据库 | MySQL 8.0（utf8mb4） |
| 鉴权 | JWT + 自定义拦截器 + `@RequireRole`（未引入完整 Spring Security，仅用其 BCrypt 实现） |
| 前端 | Vue 3、Vite 5、Element Plus、Pinia、Vue Router 4、Axios、ECharts 5 |

---

## 二、目录结构

```
zuche/
├── TASKS.md                    任务清单（S0-S6）
├── docs/
│   ├── 01-需求边界与实施计划.md
│   ├── 02-数据库设计.md         30 张表 ER + 字段 + 校验口径
│   └── 03-接口清单.md
├── sql/
│   ├── 01-schema.sql           30 张表 + 必要初始数据（角色/菜单/字典/信用规则/网点/账号）
│   └── 02-seed.sql             演示数据（品牌/车型/车辆/300 单/支付/退款/风控）
├── backend/                    Spring Boot 工程（含 mvnw）
└── frontend/                   Vue 3 + Vite 工程
    ├── public/images/          本地演示图（品牌/车型/内饰/轮播/占位/头像）
    └── scripts/                演示图生成脚本（SVG，可重复执行）
```

### S1 主要新增文件

```
backend/src/main/java/com/zuche/
├── module/auth/dto/RegisterDTO.java            注册入参（用户名/手机号/密码校验）
├── module/auth/service/AuthService.java        注册事务 + 登录 + 当前用户
├── module/auth/controller/AuthController.java  /register、/menus、/me
├── module/system/                               SysMenu 实体、Mapper、MenuService、MenuVO
├── module/vehicle/                              Vehicle/Brand/Model/Image 实体、Mapper、Service、Controller
├── module/store/                                Store 实体、Mapper（无 vehicle_count 字段）
└── resources/mapper/vehicle/VehicleMapper.xml   列表/详情/筛选项手写 SQL

frontend/src/
├── api/auth.js、api/vehicle.js
├── utils/image.js、utils/menu.js
├── components/VehicleCard.vue
├── layout/PortalLayout.vue、AdminLayout.vue、SidebarItem.vue
├── views/register/index.vue
├── views/portal/Home.vue、VehicleList.vue、VehicleDetail.vue、ComingSoon.vue
├── views/admin/Dashboard.vue、Placeholder.vue
└── public/images/                               41 个本地 SVG（构建后原样复制到 dist）
```

### S2 主要新增文件

```
backend/src/main/java/com/zuche/
├── module/credit/                                CreditRule 实体/Mapper、CreditService 接口+实现、CreditSnapshot
├── module/order/                                 LeaseOrder/OrderItem 实体、Mapper、DTO、VO
│   ├── service/OrderStateMachine.java            状态流转断言（非法流转返回 1001）
│   └── service/OrderService.java                 试算/下单/支付/取消/查询
├── module/payment/                               PaymentRecord 实体、Mapper、PaymentService（RENT+DEPOSIT）
└── resources/mapper/order/LeaseOrderMapper.xml   订单列表/详情 SQL（首图、车名聚合）

frontend/src/
├── api/order.js
├── utils/format.js                               金额/时间/等级/状态统一格式化
├── views/portal/Checkout.vue                     下单页（实时试算 + 信用减免展示）
├── views/portal/Cashier.vue                      收银台（模拟支付 + 成功流水）
├── views/portal/OrderList.vue                    我的订单
└── views/portal/OrderDetail.vue                  订单详情（明细 + 两条流水 + 信用快照）
```

> **S2 未做（刻意留白）**：信用加减分（`CreditService.recordEvent` 只打日志）、取车/归还/押金退还、
> 逾期定时任务、支付超时释放库存。这些在 S3/S4 接入，此时下单页不承诺"15 分钟内支付"等未实现的约束。

---

## 三、启动步骤

### 1. 初始化数据库

```bash
mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/01-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/02-seed.sql
```

> 必须带 `--default-character-set=utf8mb4`，否则 Windows 下中文会乱码。
> 两个脚本的末尾都会输出自检结果：表数量 30、订单 300、四档信用等级均有人。

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- 首次运行会下载依赖，请保证网络畅通。
- 默认端口 8080，配置见 `backend/src/main/resources/application.yml`（数据库账号 root/123456）。
- 接口文档：<http://localhost:8080/doc.html>

打包运行：

```bash
./mvnw -DskipTests clean package
java -jar target/zuche-backend.jar --server.port=8080
```

> 显式带 `--server.port=8080` 是有意的：如果系统里存在 `SERVER__PORT` 环境变量，
> 它的优先级高于 application.yml，会把服务顶到别的端口上，导致前端代理 502。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev                     # http://localhost:5173
npm run build                   # 产物在 dist/
```

开发期 Vite 已把 `/api` 代理到 `http://localhost:8080`，无需处理跨域。

---

## 四、初始账号（密码均为 `123456`）

| 账号 | 角色 | 说明 |
|---|---|---|
| `admin` | 系统管理员 | 看板、信用风控、调度审批、基础数据 |
| `store1` | 网点管理员 | 中关村旗舰店 |
| `store2` | 网点管理员 | 望京SOHO店 |
| `user1` | 普通用户 | 信用 138（优秀，押金 8 折） |
| `user2` | 普通用户 | 信用 112（良好，9 折） |
| `user3` | 普通用户 | 信用 92（一般，原价） |
| `user4` | 普通用户 | 信用 74（待观察，1.2 倍） |
| `user5` | 普通用户 | 信用 100（新用户） |
| `user6` | 普通用户 | 信用 138（优秀） |

---

## 五、验证清单（S0 + S1）

```bash
# 1. 健康检查（公开）
curl http://localhost:8080/api/system/health
# 期望: {"code":200,...,"status":"UP"}

# 2. 未登录访问受保护接口
curl -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/system/whoami
# 期望: 401

# 3. 登录拿 token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"123456"}'
# 期望: code=200，data.token 为 JWT，roleCode=USER

# 4. 带 token 访问
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/auth/me

# 5. 越权校验（用户 token 访问管理员接口）
curl -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/system/admin/ping
# 期望: 403

# 6. 接口文档
# 浏览器打开 http://localhost:8080/doc.html
```

### S1 新增接口回归

```bash
# 1. 车辆列表（公开）：分类 + 价格升序
curl "http://localhost:8080/api/vehicles?categoryCode=SUV&sortBy=priceAsc&page=1&size=3"

# 2. 筛选项（分类/品牌/网点/价格区间）
curl http://localhost:8080/api/vehicles/options

# 3. 车辆详情（含图集、门店、参数，访问后 view_count +1）
curl http://localhost:8080/api/vehicles/1

# 4. 注册（在本机终端执行，密码换成你自己的）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"<你的密码>","confirmPassword":"<你的密码>","realName":"张三","phone":"13911112222","email":"zhangsan@example.com"}'
# 期望: code=200，返回 token，creditScore=100，creditLevel=GOOD，roleCode=USER

# 5. 菜单树（把 <token> 换成登录/注册返回值）
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/auth/menus
# USER 期望: 首页 / 车辆租赁(车辆列表) / 我的订单 / 信用中心 / 我的收藏 / 个人中心
# SYS_ADMIN 期望: 另含平台管理（看板、信用风控、调度审批、车辆资产、订单、用户、网点、评价、公告、轮播、日志）
```

### S2 新增接口回归（需先登录拿 token）

```bash
TOKEN=<登录返回的 token>
CAR=2   # 挑一台 status=1 的可租车辆

# 1. 试算：user1(138/优秀) 应得到 0.8 系数、押金打 8 折
curl -X POST http://localhost:8080/api/orders/lease-quote \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"vehicleId\":$CAR,\"startDate\":\"2026-11-02\",\"endDate\":\"2026-11-04\",\"quantity\":1}"
# 期望: rate=0.8, depositAmount=标准押金×0.8, depositSaved>0

# 2. 下单（金额以后端重算为准，前端传金额无效）
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"vehicleId\":$CAR,\"startDate\":\"2026-11-02\",\"endDate\":\"2026-11-04\",\"quantity\":1}"
# 期望: status=WAIT_PAY，返回 orderId

# 3. 模拟支付 → 两条流水 + 车辆锁定
curl -X POST http://localhost:8080/api/orders/<orderId>/pay \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"payMethod":"MOCK_WECHAT"}'
# 期望: status=WAIT_PICKUP，payments 含 RENT、DEPOSIT 两条 SUCCESS 流水

# 4. 重复支付应被拒绝
# 期望: code=1001「订单已经是「待取车」状态，请勿重复操作」

# 5. 我的订单 / 详情
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/orders?page=1&size=5"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/orders/<orderId>
```

浏览器页面回归：

| 地址 | 预期 |
|---|---|
| `/` | 首页：信用等级面板、分类入口、4 台热门车型、本地图片 |
| `/vehicles` | 筛选（关键词/分类/品牌/网点/价格/排序/只看可租）、分页、24 台车 |
| `/vehicles/1` | 图集切换、价格/押金、参数表、门店信息、登录提示 |
| `/register` | 表单校验、注册后自动登录并回首页 |
| `/login` | 三个演示账号，登录后按角色跳转 |
| `/admin/dashboard` | 左侧动态菜单、真实 KPI（24 台/9 品牌/4 网点）、占位模块 |
| `/store/overview` | 网点管理员只看到网点管理菜单 |
| `/checkout/1` | 下单页：选租期/归还网点/备注，右侧实时试算与信用减免 |
| `/cashier/<orderId>` | 收银台：金额、支付方式、信用快照，支付后展示两条流水 |
| `/orders` | 我的订单：状态页签、去支付/取消/查看详情 |
| `/orders/<id>` | 订单详情：明细表 + 支付流水表 + 信用快照 |

---

## 六、常见问题

1. **端口被占用 / 启动在奇怪端口**：若环境中存在 `SERVER__PORT` 之类的变量，会覆盖 `application.yml`。
   用 `java -jar target/zuche-backend.jar --server.port=8080` 显式指定（命令行参数优先级最高）。
2. **中文乱码**：执行 SQL 时加 `--default-character-set=utf8mb4`；JVM 加 `-Dfile.encoding=UTF-8`。
3. **首次构建慢**：Maven 需下载 Spring Boot 全家桶，约 2-3 分钟；之后走本地仓库。
4. **图片不显示**：演示图片已全部放在 `frontend/public/images/`（SVG），不依赖外网；若单张图缺失，前端会自动回落到 `placeholder.svg`。重新生成：`cd frontend && node scripts/generate-demo-images.mjs`。
5. **后台菜单为空**：说明当前账号的 `sys_role_menu` 没有授权，或 `sys_menu.path` 未以 `/admin` / `/store` 开头；先查 `sys_role_menu` 与 `sys_menu`。
