<script setup>
import { ref, onMounted } from 'vue'
import { getGoodsList, createGoods, deleteGoods, getStock } from '../api/index.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const goodsList = ref([])
const stockMap = ref({})
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const form = ref({
  name: '',
  description: '',
  price: '',
  imageUrl: '',
  stock: ''
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }]
}

const formRef = ref(null)

onMounted(() => {
  fetchAll()
})

const fetchAll = async () => {
  loading.value = true
  try {
    const res = await getGoodsList({ page: 1, size: 100 })
    if (res.code === 200) {
      goodsList.value = res.data.list
      for (const g of goodsList.value) {
        try {
          const stockRes = await getStock(g.id)
          if (stockRes.code === 200) {
            stockMap.value[g.id] = stockRes.data
          }
        } catch (e) {}
      }
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', description: '', price: '', imageUrl: '', stock: '' }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品 "${row.name}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteGoods(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchAll()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  const payload = {
    name: form.value.name,
    description: form.value.description || '',
    price: Number(form.value.price),
    imageUrl: form.value.imageUrl || '',
    stock: Number(form.value.stock)
  }
  try {
    const res = await createGoods(payload)
    if (res.code === 200) {
      ElMessage.success('添加商品成功')
      dialogVisible.value = false
      fetchAll()
    } else {
      ElMessage.error(res.message || '添加商品失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

const getAvailableStock = (goodsId) => {
  const s = stockMap.value[goodsId]
  if (!s) return '加载中...'
  return s.totalStock - s.lockedStock - s.soldCount
}
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h2 class="page-title">后台管理</h2>
      <el-button type="primary" @click="handleAdd" :icon="'Plus'">添加商品</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品，请添加" />

      <el-table v-else :data="goodsList" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="商品名称" min-width="150" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="总库存" width="90">
          <template #default="{ row }">
            {{ stockMap[row.id]?.totalStock ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="已锁定" width="80">
          <template #default="{ row }">
            {{ stockMap[row.id]?.lockedStock ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="已售" width="70">
          <template #default="{ row }">
            {{ stockMap[row.id]?.soldCount ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="可售库存" width="90">
          <template #default="{ row }">
            <el-tag :type="getAvailableStock(row.id) > 0 ? 'success' : 'danger'" size="small">
              {{ getAvailableStock(row.id) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 19) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="添加商品" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入价格" />
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="请输入图片链接地址（选填）" />
        </el-form-item>
        <el-form-item label="库存数量" prop="stock">
          <el-input v-model="form.stock" placeholder="请输入初始库存数量" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  color: #303133;
  margin: 0;
}
</style>