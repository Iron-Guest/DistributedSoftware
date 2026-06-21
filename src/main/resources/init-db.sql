-- =====================================================
-- 电商秒杀平台 - 数据库初始化脚本
-- =====================================================
-- 用法: 使用 MySQL 客户端执行本脚本
--   mysql -u root -p < init-db.sql
-- 或在 MySQL 命令行中:
--   source E:/Codes/ShoppingPlatform/src/main/resources/init-db.sql;
-- =====================================================

-- 确保客户端使用 UTF-8 编码，防止中文乱码
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =====================================================
-- 1. 创建数据库
-- =====================================================
DROP DATABASE IF EXISTS shopping_platform;
CREATE DATABASE shopping_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE shopping_platform;

-- =====================================================
-- 2. 用户表
-- =====================================================
CREATE TABLE t_user (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY   COMMENT '用户ID',
    username   VARCHAR(50)  NOT NULL UNIQUE              COMMENT '用户名',
    password   VARCHAR(255) NOT NULL                     COMMENT '密码（BCrypt 加密）',
    phone      VARCHAR(20)                               COMMENT '手机号',
    email      VARCHAR(100)                              COMMENT '邮箱',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 3. 商品表
-- =====================================================
CREATE TABLE t_goods (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name        VARCHAR(200)   NOT NULL                   COMMENT '商品名称',
    description TEXT                                     COMMENT '商品描述',
    price       DECIMAL(10,2)  NOT NULL DEFAULT 0.00      COMMENT '商品价格',
    image_url   VARCHAR(500)                              COMMENT '商品图片URL',
    status      TINYINT        DEFAULT 1                  COMMENT '状态: 1=上架, 0=下架',
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at  DATETIME       DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =====================================================
-- 4. 库存表
--     使用 version 字段实现乐观锁，防止超卖
-- =====================================================
CREATE TABLE t_stock (
    id           BIGINT   AUTO_INCREMENT PRIMARY KEY  COMMENT '库存ID',
    goods_id     BIGINT   NOT NULL UNIQUE             COMMENT '商品ID',
    total_stock  INT      NOT NULL DEFAULT 0          COMMENT '总库存',
    locked_stock INT      NOT NULL DEFAULT 0          COMMENT '已锁定库存（已下单未支付）',
    sold_count   INT      NOT NULL DEFAULT 0          COMMENT '已售数量',
    version      INT      NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_stock_goods FOREIGN KEY (goods_id)
        REFERENCES t_goods(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- =====================================================
-- 5. 订单表
--     存储商品名称和价格冗余字段，避免查询历史订单时
--     因商品信息变更导致数据不一致
-- =====================================================
CREATE TABLE t_order (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no     VARCHAR(64)    NOT NULL UNIQUE            COMMENT '订单号',
    user_id      BIGINT         NOT NULL                   COMMENT '用户ID',
    goods_id     BIGINT         NOT NULL                   COMMENT '商品ID',
    goods_name   VARCHAR(200)                               COMMENT '商品名称（冗余）',
    goods_price  DECIMAL(10,2)                              COMMENT '商品单价（冗余）',
    quantity     INT            NOT NULL DEFAULT 1          COMMENT '购买数量',
    total_amount DECIMAL(10,2)                              COMMENT '订单总金额',
    status       TINYINT        DEFAULT 1                   COMMENT '状态: 0=已取消, 1=待支付, 2=已支付',
    created_at   DATETIME       DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    updated_at   DATETIME       DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_order_user  FOREIGN KEY (user_id)  REFERENCES t_user(id),
    CONSTRAINT fk_order_goods FOREIGN KEY (goods_id) REFERENCES t_goods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================================
-- 6. 索引
-- =====================================================
CREATE INDEX idx_order_user_id  ON t_order(user_id);
CREATE INDEX idx_order_goods_id ON t_order(goods_id);
CREATE INDEX idx_order_status   ON t_order(status);
CREATE INDEX idx_order_created  ON t_order(created_at);
CREATE UNIQUE INDEX idx_order_user_goods ON t_order(user_id, goods_id);
CREATE INDEX idx_stock_goods_id ON t_stock(goods_id);

-- =====================================================
-- 7. 种子数据: 用户
-- =====================================================
INSERT INTO t_user (username, password, phone, email, created_at, updated_at) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYQ', '13800000000', 'admin@test.com', NOW(), NOW());

-- =====================================================
-- 8. 种子数据: 商品
-- =====================================================
INSERT INTO t_goods (id, name, description, price, image_url, status) VALUES
(1, 'iPhone 15 Pro Max 256GB',
 'Apple iPhone 15 Pro Max，A17 Pro 芯片，256GB 存储，钛金属原色',
 8999.00, 'https://picsum.photos/seed/iphone15/400/300', 1),

(2, '华为 Mate 60 Pro',
 '华为 Mate 60 Pro 昆仑玻璃版，麒麟 9000S 芯片，卫星通话，12GB+512GB',
 6999.00, 'https://picsum.photos/seed/mate60/400/300', 1),

(3, '戴森 V15 Detect 无绳吸尘器',
 '戴森 Dyson V15 Detect 无绳吸尘器，激光探测微尘，智能灰尘感应',
 4990.00, 'https://picsum.photos/seed/dysonv15/400/300', 1),

(4, '飞天茅台 53度 500ml',
 '贵州茅台酒 飞天茅台 53%vol 酱香型白酒 500ml 单瓶装',
 1499.00, 'https://picsum.photos/seed/maotai/400/300', 1),

(5, '索尼 PS5 光驱版',
 '索尼 PlayStation 5 光驱版 国行正品，825GB SSD，支持 4K 120Hz 输出',
 3899.00, 'https://picsum.photos/seed/ps5/400/300', 1),

(6, 'AirPods Pro 第二代',
 'Apple AirPods Pro (第二代) USB-C 充电盒，自适应降噪，个性化空间音频',
 1899.00, 'https://picsum.photos/seed/airpods/400/300', 1),

(7, '联想拯救者 Y9000P',
 '联想拯救者 Y9000P 2024 款，i9-14900HX，RTX 4060，16GB DDR5，1TB SSD',
 9999.00, 'https://picsum.photos/seed/legion/400/300', 1),

(8, '大疆 DJI Mini 4 Pro',
 '大疆 DJI Mini 4 Pro 无人机，249g 轻小机身，全向避障，4K/100fps',
 4788.00, 'https://picsum.photos/seed/dji/400/300', 1),

(9, 'Switch OLED 马力欧红蓝',
 '任天堂 Nintendo Switch OLED 续航版 马力欧红蓝配色，7寸OLED屏幕',
 2599.00, 'https://picsum.photos/seed/switch/400/300', 1),

(10, 'SK-II 神仙水 230ml',
 'SK-II 护肤精华露（神仙水）230ml，日本进口，补水保湿控油提亮肤色',
 1370.00, 'https://picsum.photos/seed/skii/400/300', 1);

-- =====================================================
-- 9. 种子数据: 库存（与商品一一对应）
-- =====================================================
INSERT INTO t_stock (goods_id, total_stock, locked_stock, sold_count, version) VALUES
(1,  100, 0, 0, 0),
(2,   80, 0, 0, 0),
(3,   50, 0, 0, 0),
(4,   30, 0, 0, 0),
(5,   60, 0, 0, 0),
(6,  150, 0, 0, 0),
(7,   40, 0, 0, 0),
(8,   35, 0, 0, 0),
(9,   70, 0, 0, 0),
(10, 120, 0, 0, 0);