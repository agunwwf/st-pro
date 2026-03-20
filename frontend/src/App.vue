<template>
    <router-view />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

</script>

<style>

</style>