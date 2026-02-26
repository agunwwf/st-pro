<template>
  <div class="not-found">
    <h1>404</h1>
    <p>The page you are looking for does not exist.</p>
    <el-button type="primary" round @click="$router.push('/')">Go Home</el-button>
  </div>
</template>

<style scoped>
.not-found { height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; }
h1 { font-size: 80px; margin: 0; }
p { color: var(--apple-text-secondary); margin: 20px 0 40px; }
</style>