<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>监控设备</span>
              <el-tag size="small" type="info">{{ cameraList.length }} 路</el-tag>
            </div>
          </template>
          <div v-loading="cameraLoading" class="camera-list">
            <div v-if="!cameraList.length" class="empty-tip">暂无监控设备，请先在“监控设备”中添加</div>
            <div
              v-for="cam in cameraList"
              :key="cam.id"
              class="camera-item"
              :class="{ active: selectedCamera?.id === cam.id }"
              @click="selectCamera(cam)"
            >
              <div class="cam-name">{{ cam.cameraName || cam.name || ('摄像头#' + cam.id) }}</div>
              <div class="cam-meta">
                <span>{{ cam.location || '未设置位置' }}</span>
                <el-tag :type="cam.status === 1 ? 'success' : 'danger'" size="small">
                  {{ cam.status === 1 ? '在线' : '离线' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>
                实时画面
                <template v-if="selectedCamera">
                  — {{ selectedCamera.cameraName || selectedCamera.name || selectedCamera.id }}
                </template>
              </span>
              <el-button v-if="selectedCamera" size="small" @click="selectCamera(selectedCamera)">刷新</el-button>
            </div>
          </template>
          <div class="video-container" v-loading="streamLoading">
            <video
              v-if="streamUrl"
              :src="streamUrl"
              controls
              autoplay
              muted
              style="width:100%;max-height:560px;background:#000"
            />
            <div v-else class="stream-placeholder">
              <div class="scanlines"></div>
              <div class="placeholder-content">
                <div class="placeholder-title">
                  {{ selectedCamera ? '暂无可用视频流' : '请选择左侧监控设备' }}
                </div>
                <div v-if="selectedCamera" class="placeholder-desc">
                  设备：{{ selectedCamera.cameraName || selectedCamera.name || selectedCamera.id }}
                  <br />
                  位置：{{ selectedCamera.location || '未设置' }}
                  <br />
                  状态：{{ selectedCamera.status === 1 ? '在线' : '离线' }}
                  <br />
                  <span class="hint">演示环境可在“监控设备”中维护 streamUrl；无流时本页显示占位，避免空白大屏。</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCameraList, getCameraStreamUrl, type CameraItem } from '@/api/guard'

const cameraLoading = ref(false)
const streamLoading = ref(false)
const cameraList = ref<CameraItem[]>([])
const selectedCamera = ref<CameraItem | null>(null)
const streamUrl = ref('')

onMounted(async () => {
  cameraLoading.value = true
  try {
    const res = await getCameraList({ page: 1, pageSize: 100 })
    cameraList.value = res.records || []
    if (cameraList.value.length) {
      await selectCamera(cameraList.value[0])
    }
  } finally {
    cameraLoading.value = false
  }
})

async function selectCamera(cam: CameraItem) {
  selectedCamera.value = cam
  streamUrl.value = ''
  streamLoading.value = true
  try {
    const url = await getCameraStreamUrl(cam.id!)
    streamUrl.value = (url || '').trim()
  } catch {
    streamUrl.value = ''
  } finally {
    streamLoading.value = false
  }
}
</script>

<style scoped>
.camera-list {
  max-height: 620px;
  overflow: auto;
}
.camera-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 8px;
  background: #fafafa;
}
.camera-item:hover {
  background: #f0f7ff;
}
.camera-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.cam-name {
  font-weight: 600;
  margin-bottom: 4px;
}
.cam-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #888;
  font-size: 12px;
}
.empty-tip {
  color: #999;
  text-align: center;
  padding: 24px 8px;
}
.video-container {
  min-height: 420px;
  background: #0b1220;
  border-radius: 8px;
  overflow: hidden;
}
.stream-placeholder {
  position: relative;
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #dbe7ff;
  background:
    radial-gradient(circle at 20% 20%, rgba(64, 158, 255, 0.18), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(103, 194, 58, 0.12), transparent 35%),
    linear-gradient(180deg, #0b1220 0%, #111827 100%);
}
.scanlines {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.03),
    rgba(255, 255, 255, 0.03) 1px,
    transparent 1px,
    transparent 4px
  );
  pointer-events: none;
}
.placeholder-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 24px;
  max-width: 520px;
}
.placeholder-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
}
.placeholder-desc {
  line-height: 1.8;
  color: #b6c7e4;
}
.hint {
  display: inline-block;
  margin-top: 8px;
  color: #8fa4c7;
  font-size: 13px;
}
</style>
