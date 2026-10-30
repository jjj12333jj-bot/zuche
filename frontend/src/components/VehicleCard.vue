<template>
  <article class="vehicle-card" @click="$emit('click')">
    <div class="cover">
      <img
        :src="resolveImage(vehicle.coverImage)"
        :alt="`${vehicle.brandName}${vehicle.modelName}`"
        loading="lazy"
        @error="onImageError"
      />
      <span class="status" :class="statusClass">{{ vehicle.statusName }}</span>
      <span v-if="hasDiscount" class="discount">{{ discountText }}</span>
    </div>

    <div class="body">
      <h3 class="title">{{ vehicle.brandName }} {{ vehicle.modelName }}</h3>

      <div class="tags">
        <el-tag
          size="small"
          effect="plain"
          :style="{ color: vehicle.categoryColor, borderColor: vehicle.categoryColor }"
        >
          {{ vehicle.categoryName }}
        </el-tag>
        <span class="spec">{{ vehicle.seats }} 座</span>
        <span class="spec">{{ vehicle.gearboxName }}</span>
        <span class="spec">{{ vehicle.fuelTypeName }}</span>
      </div>

      <div class="meta">
        <el-icon><Location /></el-icon>
        <span>{{ vehicle.storeName }}</span>
        <span class="dot">·</span>
        <span>已租 {{ vehicle.rentCount }} 次</span>
      </div>

      <div class="price-row">
        <div class="price">
          <span class="symbol">¥</span>
          <span class="amount">{{ formatMoney(vehicle.finalPrice) }}</span>
          <span class="unit">/天</span>
          <span v-if="hasDiscount" class="origin">¥{{ formatMoney(vehicle.dailyPrice) }}</span>
        </div>
        <div class="deposit">押金 ¥{{ formatMoney(vehicle.deposit) }}</div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { resolveImage, onImageError } from '@/utils/image'

const props = defineProps({
  vehicle: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

const hasDiscount = computed(() => Number(props.vehicle.discountRate) < 1)

const discountText = computed(() => {
  const rate = Number(props.vehicle.discountRate || 1)
  return `${(rate * 10).toFixed(1)} 折`
})

const statusClass = computed(() => {
  const map = { 1: 'ok', 2: 'busy', 3: 'repair', 0: 'off' }
  return map[props.vehicle.status] || 'off'
})

function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.vehicle-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  display: flex;
  flex-direction: column;
}

.vehicle-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(31, 45, 61, 0.12);
}

.cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #eef2f6;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 9px;
  border-radius: 10px;
  font-size: 12px;
  color: #fff;
  backdrop-filter: blur(4px);
}

.status.ok {
  background: rgba(103, 194, 58, 0.92);
}

.status.busy {
  background: rgba(230, 162, 60, 0.92);
}

.status.repair {
  background: rgba(245, 108, 108, 0.92);
}

.status.off {
  background: rgba(144, 147, 153, 0.92);
}

.discount {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 9px;
  border-radius: 10px;
  background: rgba(31, 58, 95, 0.85);
  color: #ffd591;
  font-size: 12px;
  font-weight: 600;
}

.body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}

.title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.spec {
  font-size: 12px;
  color: #909399;
}

.meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.dot {
  margin: 0 2px;
}

.price-row {
  margin-top: auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  padding-top: 4px;
}

.price {
  color: #f56c6c;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.symbol {
  font-size: 14px;
}

.amount {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.unit {
  font-size: 12px;
  color: #909399;
}

.origin {
  margin-left: 6px;
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.deposit {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
</style>
