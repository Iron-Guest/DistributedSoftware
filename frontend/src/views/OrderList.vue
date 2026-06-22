<script setup>
import { ref, onMounted } from 'vue'
import { getUserOrders, payOrder, cancelOrder } from '../api/index.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const loading = ref(false)
const user = ref(null)

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
    fetchOrders()
  }
})

const fetchOrders = async () => {
  if (!user.value) return
  loading.value = true
  try {
    const res = await getUserOrders(user.value.id)
    if (res.code === 200) {
      orders.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确定要支付订单 "${order.orderNo}" 吗？金额: ¥${order.totalAmount}`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await payOrder(order.id)
    if (res.code === 200) {
      ElMessage.success('支付成功')
      fetchOrders()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('支付失败')
    }
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单 "${order.orderNo}" 吗？`,
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
    )
    const res = await cancelOrder(order.id)
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      fetchOrders()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const getStatusTag = (status) => {
  switch (status) {
    case 0: return { type: 'info', text: '已取消' }
    case 1: return { type: 'warning', text: '待支付' }
    case 2: return { type: 'success', text: '已支付' }
    default: return { type: 'info', text: '未知' }
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<template>
  <div class="order-list">
    <h2 class="page-title">我的订单</h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号: {{ order.orderNo }}</span>
          <el-tag :type="getStatusTag(order.status).type" size="small">
            {{ getStatusTag(order.status).text }}
          </el-tag>
        </div>
        <div class="order-body">
          <div class="order-goods">
            <span class="order-goods-name">{{ order.goodsName }}</span>
            <span class="order-goods-qty">x{{ order.quantity }}</span>
          </div>
          <div class="order-info">
            <span>单价: ¥{{ order.goodsPrice }}</span>
            <span class="order-total">合计: ¥{{ order.totalAmount }}</span>
          </div>
        </div>
        <div class="order-footer">
          <span class="order-time">{{ formatTime(order.createdAt) }}</span>
          <div class="order-actions">
            <el-button
              v-if="order.status === 1"
              type="success"
              size="small"
              @click="handlePay(order)"
            >
              立即支付
            </el-button>
            <el-button
              v-if="order.status === 1"
              type="danger"
              size="small"
              @click="handleCancel(order)"
            >
              取消订单
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-list {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  color: #303133;
  margin-bottom: 20px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.order-no {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-goods-name {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.order-goods-qty {
  font-size: 14px;
  color: #909399;
  margin-left: 12px;
}

.order-info {
  font-size: 14px;
  color: #606266;
}

.order-total {
  font-weight: bold;
  color: #f56c6c;
  margin-left: 16px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.order-time {
  font-size: 12px;
  color: #c0c4cc;
}

.order-actions {
  display: flex;
  gap: 8px;
}
</style>