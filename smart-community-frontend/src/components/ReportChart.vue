<template>
  <div ref="chartRef" class="report-chart" :style="{ height }" role="img" :aria-label="ariaLabel" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(defineProps<{
  option: any
  height?: string
  ariaLabel?: string
}>(), {
  height: '300px',
  ariaLabel: '数据统计图表'
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

function render() {
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption(props.option, true)
}

watch(() => props.option, () => nextTick(render), { deep: true })

onMounted(() => {
  render()
  if (chartRef.value) {
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartRef.value)
  }
})

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.report-chart { width: 100%; min-width: 0; }
</style>
