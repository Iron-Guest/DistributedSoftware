# 电商库存秒杀平台 - 系统设计文档

## 一、系统架构草图

```
┌──────────────────────────────────────────────────────────────────┐
│                         前端 (Frontend)                          │
│              Vue 3 + Element Plus + JavaScript                  │
│                    (SPA, 端口: 5173)                             │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP/REST API
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                      后端 (Backend)                              │
│              Spring Boot 4.x + MyBatis (端口: 8080)              │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    控制层 (Controller)                     │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │
│  │  │UserCtrl   │  │GoodsCtrl  │  │OrderCtrl  │  │StockCtrl  │  │   │
│  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  │   │
│  └────────┼───────────────┼───────────────┼───────────────┼──────┘   │
│           │               │               │               │          │
│  ┌────────┼───────────────┼───────────────┼───────────────┼──────┐   │
│  │        ▼               ▼               ▼               ▼      │   │
│  │                    服务层 (Service)                           │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │
│  │  │UserSvc   │  │GoodsSvc   │  │OrderSvc   │  │StockSvc   │  │   │
│  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  │   │
│  └────────┼───────────────┼───────────────┼───────────────┼──────┘   │
│           │               │               │               │          │
│  ┌────────┼───────────────┼───────────────┼───────────────┼──────┐   │
│  │        ▼               ▼               ▼               ▼      │   │
│  │                   持久层 (Mapper / MyBatis)                    │   │
│  └────────┼───────────────┼───────────────┼───────────────┼──────┘   │
└───────────┼───────────────┼───────────────┼───────────────┼──────────┘
            │               │               │               │
            ▼               ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────────┐
│                       数据存储层                                  │
│  ┌─────────────────┐          ┌─────────────────────┐            │
│  │    MySQL 8.x    │          │    Redis (缓存)      │            │
│  │  (持久化存储)    │          │  (秒杀库存预扣减)    │            │
│  └─────────────────┘          └─────────────────────┘            │
└──────────────────────────────────────────────────────────────────┘
```

### 服务拆分（按业务领域划分）

| 服务名称 | 职责说明 | 核心功能 |
|---------|---------|---------|
| **用户服务 (User Service)** | 用户身份管理 | 注册、登录、个人信息查询与修改 |
| **商品服务 (Goods/Product Service)** | 商品信息管理 | 商品CRUD、商品列表查询、商品详情 |
| **订单服务 (Order Service)** | 订单生命周期管理 | 创建订单、查询订单、订单状态流转 |
| **库存服务 (Stock/Inventory Service)** | 库存扣减与恢复 | 库存查询、预扣减、扣减确认、库存回补 |

### 调用关系

```
用户浏览器 ──► 前端 SPA ──► 后端 API Gateway (Controller层)
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
   用户服务                 商品服务                 订单服务
        │                       │                       │
        │                       ▼                       │
        │                   库存服务 ◄──────────────────┘
        │                       │
        ▼                       ▼
      MySQL                 MySQL + Redis
```

---

## 二、API 接口定义 (RESTful)

### 基础路径: `http://localhost:8080/api`

### 2.1 用户服务 `/api/user`

| 方法 | 路径 | 描述 | 请求体 / 参数 |
|------|------|------|--------------|
| `POST` | `/api/user/register` | 用户注册 | `{ username, password, phone?, email? }` |
| `POST` | `/api/user/login` | 用户登录 | `{ username, password }` |
| `GET` | `/api/user/{id}` | 查询用户信息 | 路径参数: `id` |
| `PUT` | `/api/user/{id}` | 更新用户信息 | `{ username, phone, email }` |

**通用响应格式:**
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 2.2 商品服务 `/api/goods`

| 方法 | 路径 | 描述 | 请求体 / 参数 |
|------|------|------|--------------|
| `GET` | `/api/goods` | 商品列表（分页） | `?page=1&size=10&keyword=` |
| `GET` | `/api/goods/{id}` | 商品详情 | 路径参数: `id` |
| `POST` | `/api/goods` | 添加商品（管理） | `{ name, description, price, imageUrl, stock }` |
| `PUT` | `/api/goods/{id}` | 更新商品信息 | `{ name, description, price, imageUrl }` |

### 2.3 库存服务 `/api/stock`

| 方法 | 路径 | 描述 | 请求体 / 参数 |
|------|------|------|--------------|
| `GET` | `/api/stock/{goodsId}` | 查询商品库存 | 路径参数: `goodsId` |
| `POST` | `/api/stock/deduct` | 预扣减库存 | `{ goodsId, quantity, userId }` |
| `POST` | `/api/stock/confirm` | 确认扣减 | `{ goodsId, userId, orderId }` |
| `POST` | `/api/stock/rollback` | 回补库存 | `{ goodsId, quantity, userId }` |

### 2.4 订单服务 `/api/order`

| 方法 | 路径 | 描述 | 请求体 / 参数 |
|------|------|------|--------------|
| `POST` | `/api/order` | 创建秒杀订单 | `{ goodsId, userId, quantity }` |
| `GET` | `/api/order/{id}` | 查询订单详情 | 路径参数: `id` |
| `GET` | `/api/order/user/{userId}` | 用户订单列表 | 路径参数: `userId` |
| `PUT` | `/api/order/{id}/cancel` | 取消订单 | 路径参数: `id` |

---

## 三、数据库 ER 图

```
┌──────────────────────────────────────────────────────────────────────┐
│                        数据库: shopping_platform                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐       ┌──────────────────────────────────────┐
│       t_user         │       │              t_goods                 │
├──────────────────────┤       ├──────────────────────────────────────┤
│ PK  id        BIGINT │       │ PK  id          BIGINT              │
│     username  VARCHAR(50) UNIQUE │ name        VARCHAR(200)        │
│     password  VARCHAR(255) │     description TEXT                  │
│     phone     VARCHAR(20)  │     price       DECIMAL(10,2)        │
│     email     VARCHAR(100) │     image_url   VARCHAR(500)         │
│     created_at DATETIME   │     status      TINYINT DEFAULT 1    │
│     updated_at DATETIME   │     created_at  DATETIME              │
└──────────┬───────────────┘     │ updated_at  DATETIME              │
           │                     └────────────┬─────────────────────────┘
           │ 1                                 │ 1
           │                                  │
           │ N                                │ 1
           ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│                            t_order                                   │
├──────────────────────────────────────────────────────────────────────┤
│ PK  id            BIGINT                                            │
│     order_no      VARCHAR(64) UNIQUE   -- 订单编号(雪花ID)          │
│ FK  user_id       BIGINT               -- 用户ID                   │
│ FK  goods_id      BIGINT               -- 商品ID                   │
│     goods_name    VARCHAR(200)         -- 商品名称(冗余)           │
│     goods_price   DECIMAL(10,2)        -- 成交价格(冗余)           │
│     quantity      INT                  -- 购买数量                  │
│     total_amount  DECIMAL(10,2)        -- 订单总额                  │
│     status        TINYINT              -- 0:已取消 1:待支付 2:已支付│
│     created_at    DATETIME                                         │
│     updated_at    DATETIME                                         │
└──────────┬──────────────────────────────────────────────────────────┘
           │
           │ 1
           │
           │ 1
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           t_stock                                    │
├──────────────────────────────────────────────────────────────────────┤
│ PK  id            BIGINT                                            │
│ FK  goods_id      BIGINT UNIQUE        -- 商品ID (一对一)          │
│     total_stock   INT                  -- 总库存                    │
│     locked_stock  INT DEFAULT 0        -- 锁定库存(预扣减)         │
│     sold_count    INT DEFAULT 0        -- 已售数量                  │
│     version       INT DEFAULT 0        -- 乐观锁版本号              │
│     updated_at    DATETIME                                         │
└──────────────────────────────────────────────────────────────────────┘

关系说明:
  t_user  1 ──── N  t_order    (一个用户可创建多个订单)
  t_goods 1 ──── N  t_order    (一个商品可有多个订单)
  t_goods 1 ──── 1  t_stock    (一个商品对应一条库存记录)
```

---

## 四、技术栈选型说明

### 4.1 编程语言与框架

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| **后端语言** | Java | 17 | LTS 版本，生态成熟，性能优异 |
| **后端框架** | Spring Boot | 4.0.7 | 业界主流微服务框架，自动配置，开箱即用 |
| **ORM 框架** | MyBatis | 4.0.1 (Spring Boot Starter) | SQL 可控性高，适合秒杀场景下复杂查询和性能调优 |
| **安全框架** | Spring Security | 内嵌于 Spring Boot | 提供 BCrypt 密码加密和请求安全过滤 |
| **前端语言** | JavaScript | ES6+ | 前端标准语言 |
| **前端框架** | Vue 3 | 3.5.x | 渐进式框架，Composition API，响应式数据绑定 |
| **UI 组件库** | Element Plus | 最新版 | Vue 3 生态最成熟的组件库 |
| **前端构建** | Vite | 8.x | 新一代前端构建工具，HMR 极速开发体验 |
| **HTTP 客户端** | Axios | 最新版 | Promise 风格的 HTTP 库，支持拦截器 |

### 4.2 中间件初选

| 中间件 | 用途 | 说明 |
|--------|------|------|
| **MySQL** | 持久化存储 | 用户、商品、订单、库存等核心数据落盘存储 |
| **Redis** | 缓存 / 分布式锁 | 秒杀场景下库存预扣减、热点商品缓存、分布式锁防超卖 |
| **RabbitMQ / Kafka** (待定) | 消息队列 | 削峰填谷，异步处理订单创建，降低数据库瞬时压力 |

### 4.3 数据库说明

- **关系型数据库**: MySQL 8.x，用于存储用户、商品、订单、库存等结构化数据
- **缓存数据库**: Redis，用于秒杀场景下的库存预热与原子扣减操作
- **连接池**: HikariCP（Spring Boot 默认连接池）

### 4.4 项目结构

```
ShoppingPlatform/
├── pom.xml                          # Maven 配置文件
├── src/
│   └── main/
│       ├── java/com/whu/shoppingplatform/
│       │   ├── ShoppingPlatformApplication.java   # 启动类
│       │   ├── config/               # 配置类 (Security, CORS, etc.)
│       │   ├── controller/           # REST 控制器
│       │   ├── service/              # 业务逻辑服务
│       │   ├── mapper/               # MyBatis Mapper 接口
│       │   ├── entity/               # 数据库实体类
│       │   └── dto/                  # 数据传输对象
│       └── resources/
│           ├── application.yml       # 主配置
│           ├── application-dev.yml   # 开发环境配置
│           └── mapper/               # MyBatis XML 映射文件
└── frontend/                         # Vue 3 前端项目
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js                   # 入口文件
        ├── App.vue                   # 根组件
        ├── api/                      # API 请求封装
        ├── router/                   # 路由配置
        └── views/                    # 页面组件
```

---

## 五、当前实现进度

### 已完成 ✅

- [x] 项目基础框架搭建（Spring Boot + MyBatis + Vue 3 + Vite）
- [x] 用户注册功能（密码 BCrypt 加密存储）
- [x] 用户登录功能（密码匹配验证）
- [x] 前后端联调（CORS 配置）
- [x] 系统设计文档（本文档）
- [x] 商品管理（商品列表、商品详情、添加商品、删除商品）
- [x] 库存管理（库存查询、乐观锁扣减、取消订单回补）
- [x] 订单管理（秒杀下单、订单查询、取消订单）
- [x] 平台首页（商品网格展示、搜索、分页、抢购按钮）
- [x] 商品详情页（商品信息展示、抢购入口）
- [x] 商品订单页（用户订单列表、取消订单）
- [x] 后台管理页（库存情况展示、添加/删除商品）
- [x] 前端整体布局（侧边栏导航：商品列表、我的订单、后台管理）

### 待开发 🔲

- [ ] Redis 缓存集成（热点商品缓存、库存预热）
- [ ] 分布式锁防超卖（Redis 分布式锁 / Lua 脚本）
- [ ] 消息队列异步处理（削峰填谷）
- [ ] 用户权限管理（普通用户 / 管理员角色分离）

---

## 六、快速启动

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS shopping_platform DEFAULT CHARSET utf8mb4;

USE shopping_platform;

-- 用户表
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE t_goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    image_url VARCHAR(500),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存表
CREATE TABLE t_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL UNIQUE,
    total_stock INT NOT NULL DEFAULT 0,
    locked_stock INT NOT NULL DEFAULT 0,
    sold_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (goods_id) REFERENCES t_goods(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    goods_name VARCHAR(200),
    goods_price DECIMAL(10,2),
    quantity INT NOT NULL DEFAULT 1,
    total_amount DECIMAL(10,2),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (goods_id) REFERENCES t_goods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 启动后端

```bash
# 在项目根目录
mvnw spring-boot:run
# 或使用 IDE 运行 ShoppingPlatformApplication.main()
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问

- 前端地址: http://localhost:5173
- 后端接口: http://localhost:8080/api

---

## 七、Docker 容器化部署

### 架构图

```
                    ┌─────────────┐
                    │   JMeter    │ 压力测试
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Nginx:80  │ 负载均衡 + 前端静态资源
                    └──┬──────┬──┘
                       │      │
              ┌────────▼┐ ┌──▼────────┐
              │ 后端:8081│ │ 后端:8082 │ Spring Boot 集群
              └────┬─────┘ └──┬────────┘
                   │          │
                   └────┬─────┘
                        │
                   ┌────▼────┐
                   │ MySQL:8 │ 数据库
                   └─────────┘
```

### 1. 构建前端

```bash
cd frontend
npm install
npx vite build     # 输出到 frontend/dist/
```

### 2. 启动所有容器

```bash
# 在项目根目录
docker-compose up -d
```

### 3. 查看运行状态

```bash
docker-compose ps
docker-compose logs -f backend1 backend2  # 查看后端日志
```

### 4. 访问（Nginx 统一入口）

- 前端页面: http://localhost
- 后端 API: http://localhost/api/goods

### 5. 停止

```bash
docker-compose down
docker-compose down -v   # 同时删除数据库数据卷
```

---

## 八、负载均衡算法切换

编辑 `nginx/nginx.conf`，取消对应 upstream 块的注释，注释掉其他块：

| 算法 | 配置 | 说明 |
|------|------|------|
| 轮询 (默认) | `upstream backend_pool { server ... }` | 请求依次分发，简单均匀 |
| 最少连接 | `least_conn;` | 优先发给活跃连接最少的服务器 |
| IP 哈希 | `ip_hash;` | 同一客户端 IP 始终访问同一服务器 |
| 随机 | `random;` | 随机选择后端服务器 |
| 加权轮询 | `weight=3` / `weight=1` | 按权重比例分发 |

切换后重启 Nginx:
```bash
docker-compose restart nginx
```

---

## 九、JMeter 压力测试

### 1. 安装 JMeter

- 下载: https://jmeter.apache.org/download_jmeter.cgi
- 解压后进入 `bin/` 目录

### 2. GUI 模式运行

```bash
jmeter -t E:/Codes/ShoppingPlatform/jmeter/seckill-test.jmx
```

### 3. 命令行模式运行（推荐）

```bash
# 运行测试并生成 HTML 报告
jmeter -n -t E:/Codes/ShoppingPlatform/jmeter/seckill-test.jmx -l result.jtl -e -o report/

# 打开 report/index.html 查看报告
```

### 4. 测试场景

| 线程组 | 并发数 | 循环次数 | 目标 |
|--------|--------|----------|------|
| 商品列表查询 | 50 | 10 | 测试读请求吞吐量 |
| 秒杀下单 | 100 | 5 | 测试秒杀核心压力 |
| 登录+商品详情 | 30 | 10 | 测试混合场景 |

### 5. 验证负载均衡

```bash
# 查看 Nginx 日志，观察 upstream 分发
docker-compose logs nginx | grep upstream

# 查看各后端实例日志，统计请求数
docker-compose logs backend1 | grep -c "GET\|POST"
docker-compose logs backend2 | grep -c "GET\|POST"
```

### 6. 调整测试参数

在 JMeter GUI 中修改各线程组的 `num_threads` (并发数) 和 `loops` (循环次数) 后重新运行。

---

## 十、动静分离压测

### 测试计划

`jmeter/static-vs-dynamic-test.jmx` 包含 3 个线程组：

| 线程组 | 目标 | 并发 | 预期响应时间 |
|--------|------|------|-------------|
| 静态资源 (CSS/JS) | Nginx 直接返回，浏览器缓存 | 100×10 | < 5ms |
| 后端 API (商品列表) | 经过 Nginx→后端→MySQL | 100×10 | 10-50ms |
| 缓存命中 (商品详情) | Redis 缓存命中 | 100×10 | < 10ms |

### 运行

```bash
jmeter -n -t jmeter/static-vs-dynamic-test.jmx -l static-dynamic.jtl -e -o report-static-dynamic/
```

### 验证动静分离

```bash
curl -I http://localhost/assets/index-CH4oDg2-.css | grep -i cache
# 应看到: Cache-Control: public, immutable; Expires: (30天后)
# 应看到: X-Static-Cache: HIT

curl -I http://localhost/api/goods | grep -i cache
# 应看到: Cache-Control: no-store; X-Dynamic: true
```

---

## 十一、Redis 缓存 — 三大问题防护

### 缓存穿透防护

- **空值缓存**: 查询不存在的商品 ID 时，缓存 `__NULL__` 标记，TTL 5 分钟
- 下次请求直接返回 null，不再穿透到数据库

### 缓存击穿防护

- **互斥锁**: 热点商品缓存过期时，使用 `SETNX` 获取分布式锁
- 只有获取锁的线程去查 DB 重建缓存，其他线程自旋等待后重读缓存

### 缓存雪崩防护

- **随机 TTL**: 每个商品缓存的过期时间 = 基准 30 分钟 + 随机 0~10 分钟
- 避免大量缓存在同一时刻同时过期

### 缓存预热

- `CacheWarmer` 在应用启动时自动加载所有商品到 Redis

---

## 十二、MySQL 读写分离

### 架构

```
写请求 (@Transactional)  ──► mysql-master (3307)
读请求 (@ReadOnly)       ──► mysql-slave  (3308)
```

### 实现原理

- `AbstractRoutingDataSource` 根据 `ThreadLocal` 上下文切换数据源
- `@ReadOnly` 注解标记的方法自动路由到从库
- `@Transactional(readOnly=true)` 也路由到从库
- `@Transactional` (默认) 路由到主库

### 配置主从复制

```bash
# 1. 在主库创建复制用户
docker exec -it seckill-mysql-master mysql -uroot -pabc123
# 执行 mysql/setup-replication.sql 中的步骤 1-2

# 2. 在从库配置复制
docker exec -it seckill-mysql-slave mysql -uroot -pabc123
# 执行 mysql/setup-replication.sql 中的步骤 3-4

# 3. 验证
SHOW SLAVE STATUS\G
```

### 验证读写分离

```bash
# 启动后端，查看日志中的 DataSourceRouter 输出
docker-compose logs backend1 | grep "当前数据源"
# 应看到: 查询走 slave，写入走 master
```

---

## 十三、Elasticsearch 商品搜索

### 架构

- ES 索引: `goods` (单节点，开发模式)
- 分词器: `ik_max_word` (索引) / `ik_smart` (搜索)
- 搜索字段: `name` + `description`

### 初始化索引

```bash
# 重建 ES 索引 (将 MySQL 商品数据同步到 ES)
curl -X POST http://localhost/api/goods/reindex
```

### 搜索示例

```bash
# 搜索 "iPhone"
curl "http://localhost/api/goods?keyword=iPhone"
# 返回匹配的商品列表 (通过 ES 搜索)
```

### 数据同步

- 新增商品 → 自动索引到 ES
- 删除商品 → 自动从 ES 删除
- 数据不一致时 → 调用 `POST /api/goods/reindex` 重建