<template>
  <main class="not-found-page">
    <span class="page-eyebrow">404 · LOST RECIPE</span>
    <strong>这道页面暂时不在菜单上</strong>
    <p>地址可能已经变化，回到食光知味工作台继续处理今天的经营事项吧。</p>
    <RouterLink class="primary-button" to="/dashboard">返回工作台</RouterLink>
  </main>
</template>

<script setup>
import { RouterLink } from 'vue-router';
</script>
