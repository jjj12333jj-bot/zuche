<template>
  <el-sub-menu v-if="item.children && item.children.length" :index="item.path || `menu-${item.id}`">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <SidebarItem v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>

  <el-menu-item v-else :index="item.path || `menu-${item.id}`">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>
</template>

<script setup>
defineOptions({ name: 'SidebarItem' })

defineProps({
  item: {
    type: Object,
    required: true
  }
})
</script>
