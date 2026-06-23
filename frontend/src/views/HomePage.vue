<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGoodsList, createOrder } from '../api/index.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const goodsList = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)
const user = ref(null)

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
  fetchGoods()
})

const fetchGoods = async () => {
  loading.value = true
  try {
    const res = await getGoodsList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value
    })
    if (res.code === 200) {
      goodsList.value = res.data.list
      total.value = res.data.total
    }
  } catch (e) {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchGoods()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchGoods()
}

const goDetail = (id) => {
  router.push(`/home/goods/${id}`)
}

const handleBuy = async (goods) => {
  if (!user.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要抢购 "${goods.name}" 吗？单价: ¥${goods.price}，当前库存: ${goods.availableStock ?? 0}`,
      '确认抢购',
      { confirmButtonText: '立即抢购', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await createOrder({
      goodsId: goods.id,
      userId: user.value.id,
      quantity: 1
    })
    if (res.code === 200) {
      ElMessage.success('下单请求已提交，请在「我的订单」查看订单状态')
      fetchGoods()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '抢购失败')
    }
  }
}

const getStatusType = (stock) => {
  if (stock == null || stock <= 0) return 'danger'
  if (stock <= 10) return 'warning'
  return 'success'
}

const getStatusText = (stock) => {
  if (stock == null || stock <= 0) return '已售罄'
  if (stock <= 10) return '即将售罄'
  return '热卖中'
}
</script>

<template>
  <div class="homepage">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        style="width: 300px"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch" :icon="'Search'">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div v-loading="loading" class="goods-grid">
      <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
      <div
        v-for="item in goodsList"
        :key="item.id"
        class="goods-card"
        @click="goDetail(item.id)"
      >
        <div class="goods-image">
          <img
            :src="item.imageUrl"
            :alt="item.name"
            @error="(e) => { e.target.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%22280%22 height=%22200%22><rect fill=%22%23E5E9F2%22 width=%22280%22 height=%22200%22/><text fill=%22%23909399%22 font-size=%2216%22 font-family=%22sans-serif%22 x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22>No Image</text></svg>') }"
          />
          <el-tag
            :type="getStatusType(item.availableStock)"
            class="stock-tag"
            size="small"
          >
            {{ getStatusText(item.availableStock) }}
          </el-tag>
        </div>
        <div class="goods-info">
          <h3 class="goods-name">{{ item.name }}</h3>
          <p class="goods-desc">{{ item.description }}</p>
          <div class="goods-bottom">
            <span class="goods-price">¥{{ item.price }}</span>
            <span class="goods-stock">库存: {{ item.availableStock ?? 0 }}</span>
          </div>
          <el-button
            type="danger"
            class="buy-btn"
            :disabled="!item.availableStock || item.availableStock <= 0"
            @click.stop="handleBuy(item)"
          >
            {{ item.availableStock && item.availableStock > 0 ? '立即抢购' : '已售罄' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.homepage {
  max-width: 1200px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 20px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}

.goods-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.goods-image {
  position: relative;
  height: 180px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.stock-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.goods-info {
  padding: 12px 16px 16px;
}

.goods-name {
  font-size: 16px;
  color: #303133;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.goods-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.goods-stock {
  font-size: 12px;
  color: #909399;
}

.buy-btn {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 1000px) {
  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>