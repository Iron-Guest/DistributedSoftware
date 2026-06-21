-- =====================================================
-- MySQL 主从复制配置脚本
-- 在从库容器中执行: docker exec -it seckill-mysql-slave mysql -uroot -pabc123
-- =====================================================

-- 1. 在主库创建复制用户 (在主库执行)
-- CREATE USER 'repl'@'%' IDENTIFIED BY 'repl_pass';
-- GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
-- FLUSH PRIVILEGES;

-- 2. 查看主库状态 (在主库执行)
-- SHOW MASTER STATUS;
-- 记录 File 和 Position 的值

-- 3. 配置从库复制 (在从库执行，替换 MASTER_LOG_FILE 和 MASTER_LOG_POS)
-- CHANGE MASTER TO
--     MASTER_HOST='mysql-master',
--     MASTER_PORT=3306,
--     MASTER_USER='repl',
--     MASTER_PASSWORD='repl_pass',
--     MASTER_LOG_FILE='mysql-bin.000003',
--     MASTER_LOG_POS=157;

-- 4. 启动从库复制
-- START SLAVE;

-- 5. 检查复制状态
-- SHOW SLAVE STATUS\G
-- 确认 Slave_IO_Running=Yes 和 Slave_SQL_Running=Yes