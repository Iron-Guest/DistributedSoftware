#!/bin/bash
# =====================================================
# Nacos 配置初始化脚本
# 在 Nacos 启动后执行，推送共享配置
# =====================================================

NACOS_ADDR="${NACOS_ADDR:-localhost:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-}"

BASE_URL="http://${NACOS_ADDR}/nacos/v1/cs/configs"

echo "=== Pushing common.yml to Nacos ==="
curl -s -X POST "${BASE_URL}" \
  -d "dataId=common.yml&group=DEFAULT_GROUP&content=$(cat <<'EOF'
# 共享配置 — 所有服务共用
seckill:
  max-retry: 3
  order-timeout-seconds: 900

spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:localhost:8080}
        port: ${SENTINEL_PORT:8719}
      eager: true
EOF
)&type=yaml" || echo "Failed to push common.yml"

echo ""
echo "=== Pushing shopping-platform.yml to Nacos ==="
curl -s -X POST "${BASE_URL}" \
  -d "dataId=shopping-platform.yml&group=DEFAULT_GROUP&content=$(cat <<'EOF'
# 后端服务专属配置
seckill:
  max-retry: 3
  order-timeout-seconds: 900
EOF
)&type=yaml" || echo "Failed to push shopping-platform.yml"

echo ""
echo "=== Nacos config initialization complete ==="