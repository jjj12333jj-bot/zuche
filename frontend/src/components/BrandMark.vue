<template>
  <span
    class="brand-mark"
    :class="`brand-mark--${variant}`"
    :style="{ '--mark-size': `${size}px` }"
    aria-hidden="true"
  >
    <span class="brand-mark__glyph">微</span>
  </span>
</template>

<script setup>
/**
 * 品牌标识：以「微」字为主体
 * 蓝色渐变圆角方块 + 左上高光 + 底部暗弧（暗示车身与路面），白色「微」居中
 *
 * variant:
 *   solid — 蓝底白字，用于浅色背景与图片背景
 *   light — 白底蓝字，用于深色侧栏、深蓝品牌栏
 */
defineProps({
  size: { type: Number, default: 34 },
  variant: {
    type: String,
    default: 'solid',
    validator: (v) => ['solid', 'light'].includes(v)
  }
})
</script>

<style scoped>
.brand-mark {
  position: relative;
  display: grid;
  place-items: center;
  width: var(--mark-size);
  height: var(--mark-size);
  flex-shrink: 0;
  overflow: hidden;
  border-radius: calc(var(--mark-size) * 0.29);
  background:
    radial-gradient(120% 95% at 26% 6%, rgba(255, 255, 255, 0.3), transparent 60%),
    linear-gradient(148deg, #3a8df0 0%, #1a63d4 62%, #14479c 100%);
  box-shadow:
    0 5px 14px rgba(20, 80, 170, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

/* 底部暗弧：压出"地面"的层次，让白字更跳，不抢字 */
.brand-mark::after {
  content: '';
  position: absolute;
  left: -22%;
  right: -22%;
  bottom: -52%;
  height: 72%;
  border-radius: 50%;
  background: rgba(9, 46, 104, 0.26);
}

.brand-mark__glyph {
  position: relative;
  z-index: 1;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  font-size: calc(var(--mark-size) * 0.55);
  font-weight: 600;
  line-height: 1;
  color: #fff;
  text-shadow: 0 1px 1px rgba(6, 32, 72, 0.45);
  transform: translateY(-2%);
}

/* 深色背景上的浅色变体：白底蓝字，避免蓝块融进深色栏 */
.brand-mark--light {
  background:
    radial-gradient(120% 95% at 26% 6%, #ffffff 0%, rgba(255, 255, 255, 0) 62%),
    linear-gradient(148deg, #ffffff 0%, #e8f1ff 100%);
  box-shadow:
    0 5px 14px rgba(4, 18, 38, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.brand-mark--light::after {
  background: rgba(26, 99, 212, 0.09);
}

.brand-mark--light .brand-mark__glyph {
  color: #1a63d4;
  text-shadow: none;
}
</style>
