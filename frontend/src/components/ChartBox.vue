<template>
  <div ref="box" class="chart-box" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const box = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  // notMerge=true：切换时间范围时旧 series 必须清掉，否则会叠加
  chart.setOption(props.option, true)
}

onMounted(() => {
  chart = echarts.init(box.value)
  render()
  // 侧边栏折叠、窗口缩放都会改变容器宽度，靠 ResizeObserver 比监听 window 更准
  observer = new ResizeObserver(() => chart && chart.resize())
  observer.observe(box.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  if (chart) chart.dispose()
  chart = null
})
</script>

<style scoped>
.chart-box {
  width: 100%;
}
</style>
