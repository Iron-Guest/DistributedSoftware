#!/bin/bash
set -e

MASTER_HOST="${MYSQL_MASTER_HOST:-mysql-master}"
SLAVE_HOST="${MYSQL_SLAVE_HOST:-mysql-slave}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-abc123}"
REPL_USER="repl"
REPL_PASSWORD="repl_pass_2024"

echo "=== Waiting for master to be ready ==="
until mysql -h "$MASTER_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" &>/dev/null; do
  echo "  Master not ready, retrying..."
  sleep 3
done
echo "  Master is ready."

echo "=== Waiting for slave to be ready ==="
until mysql -h "$SLAVE_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" &>/dev/null; do
  echo "  Slave not ready, retrying..."
  sleep 3
done
echo "  Slave is ready."

echo "=== Creating replication user on master ==="
mysql -h "$MASTER_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "
  CREATE USER IF NOT EXISTS '${REPL_USER}'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
  GRANT REPLICATION SLAVE ON *.* TO '${REPL_USER}'@'%';
  FLUSH PRIVILEGES;
" 2>/dev/null || echo "  Replication user may already exist, continuing..."

echo "=== Getting master binlog position ==="
MASTER_STATUS=$(mysql -h "$MASTER_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW MASTER STATUS\G")
echo "$MASTER_STATUS"

MASTER_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

if [ -z "$MASTER_FILE" ] || [ -z "$MASTER_POS" ]; then
  echo "ERROR: Could not get master binlog position"
  exit 1
fi

echo "  Master File: $MASTER_FILE"
echo "  Master Position: $MASTER_POS"

echo "=== Configuring slave replication ==="
mysql -h "$SLAVE_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "
  STOP SLAVE;
  CHANGE MASTER TO
    MASTER_HOST='${MASTER_HOST}',
    MASTER_PORT=3306,
    MASTER_USER='${REPL_USER}',
    MASTER_PASSWORD='${REPL_PASSWORD}',
    MASTER_LOG_FILE='${MASTER_FILE}',
    MASTER_LOG_POS=${MASTER_POS},
    MASTER_CONNECT_RETRY=10;
  START SLAVE;
" 2>/dev/null || echo "  Slave replication config applied."

echo "=== Checking slave replication status ==="
sleep 2
SLAVE_STATUS=$(mysql -h "$SLAVE_HOST" -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW SLAVE STATUS\G")
echo "$SLAVE_STATUS"

IO_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SQL_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')

if [ "$IO_RUNNING" = "Yes" ] && [ "$SQL_RUNNING" = "Yes" ]; then
  echo "=== MySQL replication setup SUCCESSFUL ==="
  echo "  Slave_IO_Running: $IO_RUNNING"
  echo "  Slave_SQL_Running: $SQL_RUNNING"
else
  echo "=== WARNING: Replication may not be fully running ==="
  echo "  Slave_IO_Running: $IO_RUNNING"
  echo "  Slave_SQL_Running: $SQL_RUNNING"
  echo "  Check 'SHOW SLAVE STATUS\\G' on the slave for details."
fi