<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, createOrder } from '../api/index.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const goods = ref(null)
const loading = ref(false)
const user = ref(null)

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
  fetchDetail()
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getGoodsDetail(route.params.id)
    if (res.code === 200) {
      goods.value = res.data
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('加载商品详情失败')
  } finally {
    loading.value = false
  }
}

const handleBuy = async () => {
  if (!user.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要抢购 "${goods.value.name}" 吗？单价: ¥${goods.value.price}`,
      '确认抢购',
      { confirmButtonText: '立即抢购', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await createOrder({
      goodsId: goods.value.id,
      userId: user.value.id,
      quantity: 1
    })
    if (res.code === 200) {
      ElMessage.success('抢购下单已提交，订单处理中...')
      fetchDetail()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('抢购失败，请稍后重试')
    }
  }
}

const goBack = () => {
  router.push('/home/index')
}
</script>

<template>
  <div class="goods-detail" v-loading="loading">
    <el-button @click="goBack" class="back-btn" :icon="'ArrowLeft'">返回商品列表</el-button>

    <div v-if="goods" class="detail-card">
      <div class="detail-image">
        <img
          :src="goods.imageUrl"
          :alt="goods.name"
          @error="(e) => { e.target.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%22500%22 height=%22400%22><rect fill=%22%23E5E9F2%22 width=%22500%22 height=%22400%22/><text fill=%22%23909399%22 font-size=%2220%22 font-family=%22sans-serif%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22>No Image</text></svg>') }"
        />
      </div>
      <div class="detail-info">
        <h1 class="detail-name">{{ goods.name }}</h1>
        <div class="detail-price-row">
          <span class="detail-price-label">秒杀价</span>
          <span class="detail-price">¥{{ goods.price }}</span>
        </div>
        <div class="detail-stock-row">
          <el-tag
            :type="goods.availableStock && goods.availableStock > 0 ? 'success' : 'danger'"
            size="large"
          >
            {{ goods.availableStock && goods.availableStock > 0 ? `库存: ${goods.availableStock}` : '已售罄' }}
          </el-tag>
        </div>
        <div class="detail-desc">
          <h3>商品描述</h3>
          <p>{{ goods.description || '暂无描述' }}</p>
        </div>
        <el-button
          type="danger"
          size="large"
          class="detail-buy-btn"
          :disabled="!goods.availableStock || goods.availableStock <= 0"
          @click="handleBuy"
        >
          {{ goods.availableStock && goods.availableStock > 0 ? '立即抢购' : '已售罄' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goods-detail {
  max-width: 1000px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 20px;
}

.detail-card {
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.detail-image {
  flex: 0 0 400px;
  height: 400px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.detail-name {
  font-size: 24px;
  color: #303133;
  margin: 0 0 20px;
}

.detail-price-row {
  background: #fef0f0;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.detail-price-label {
  font-size: 14px;
  color: #f56c6c;
  margin-right: 12px;
}

.detail-price {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
}

.detail-stock-row {
  margin-bottom: 20px;
}

.detail-desc {
  flex: 1;
}

.detail-desc h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.detail-desc p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.detail-buy-btn {
  margin-top: 20px;
  width: 200px;
}

@media (max-width: 768px) {
  .detail-card {
    flex-direction: column;
  }
  .detail-image {
    flex: 0 0 250px;
    height: 250px;
  }
}
</style>