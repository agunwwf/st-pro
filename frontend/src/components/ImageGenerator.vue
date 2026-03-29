<script setup>
import { ref } from 'vue';
import { Upload, ImageIcon, Loader2, Sparkles, Wand2 } from 'lucide-vue-next';
import { useDeepSeek } from '../composables/useDeepSeek.js';

const { isImageLoading, generateImage } = useDeepSeek();
const prompt = ref('');
const referenceImage = ref(null);
const generatedImage = ref(null);

const onFileChange = (e) => {
  const file = e.target.files?.[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = () => referenceImage.value = reader.result;
    reader.readAsDataURL(file);
  }
};

const onGenerate = async () => {
  if (!prompt.value.trim() || isImageLoading.value) return;
  try {
    generatedImage.value = await generateImage(prompt.value, referenceImage.value);
  } catch (e) {
    alert('生成失败，请检查 API 配置');
  }
};
</script>

<template>
  <div class="max-w-6xl mx-auto py-8">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-12">
      <!-- 左侧控制栏 -->
      <div class="lg:col-span-5 space-y-10">
        <div class="space-y-2">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-100 dark:bg-zinc-900 text-[10px] font-bold uppercase tracking-wider text-zinc-500">
            <Sparkles :size="12" /> AI Image Generation
          </div>
          <h2 class="text-4xl font-bold tracking-tight">创意工坊</h2>
          <p class="text-zinc-500 text-sm">将您的文字转化为令人惊叹的视觉艺术。</p>
        </div>

        <div class="space-y-8">
          <!-- 参考图 -->
          <div class="space-y-4">
            <label class="text-xs font-bold uppercase tracking-widest text-zinc-400">参考图像 (可选)</label>
            <label class="group relative flex flex-col items-center justify-center w-full aspect-video border-2 border-dashed border-zinc-200 dark:border-zinc-800 rounded-3xl cursor-pointer hover:bg-zinc-50 dark:hover:bg-zinc-900/50 transition-all overflow-hidden">
              <input type="file" @change="onFileChange" class="hidden" />
              <template v-if="referenceImage">
                <img :src="referenceImage" class="w-full h-full object-cover" />
                <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                  <Upload class="text-white" :size="24" />
                </div>
              </template>
              <template v-else>
                <div class="flex flex-col items-center gap-2 text-zinc-400">
                  <Upload :size="24" />
                  <span class="text-xs font-medium">点击上传或拖拽图片</span>
                </div>
              </template>
            </label>
          </div>

          <!-- 提示词 -->
          <div class="space-y-4">
            <label class="text-xs font-bold uppercase tracking-widest text-zinc-400">描述您的创意</label>
            <div class="relative">
              <textarea
                  v-model="prompt"
                  placeholder="例如：一个在赛博朋克城市屋顶上喝咖啡的宇航员，电影质感，4k..."
                  rows="5"
                  class="w-full p-5 rounded-3xl bg-zinc-50 dark:bg-zinc-900 border-none outline-none resize-none text-[15px] focus:ring-2 ring-zinc-500/10 transition-all"
              />
              <div class="absolute bottom-4 right-4 text-[10px] text-zinc-400 font-mono">
                {{ prompt.length }} / 500
              </div>
            </div>
          </div>

          <button
              @click="onGenerate"
              :disabled="!prompt.trim() || isImageLoading"
              class="w-full py-5 rounded-3xl font-bold flex items-center justify-center gap-3 shadow-xl shadow-zinc-500/10 transition-all active:scale-[0.98]"
              :class="prompt.trim() && !isImageLoading
              ? 'bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900'
              : 'bg-zinc-100 text-zinc-400 cursor-not-allowed dark:bg-zinc-800'"
          >
            <Loader2 v-if="isImageLoading" class="animate-spin" :size="20" />
            <Wand2 v-else :size="20" />
            <span>{{ isImageLoading ? '正在生成中...' : '立即生成' }}</span>
          </button>
        </div>
      </div>

      <!-- 右侧预览区 -->
      <div class="lg:col-span-7">
        <div class="aspect-square rounded-[40px] border border-zinc-100 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/30 flex items-center justify-center overflow-hidden relative group shadow-inner">
          <Transition name="fade" mode="out-in">
            <div v-if="isImageLoading" class="flex flex-col items-center gap-6">
              <div class="relative">
                <Loader2 class="animate-spin text-zinc-300" :size="64" />
                <Sparkles class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-zinc-400" :size="24" />
              </div>
              <div class="text-center space-y-1">
                <p class="text-sm font-semibold">AI 正在绘制您的想象</p>
                <p class="text-xs text-zinc-400">这通常需要几秒钟时间</p>
              </div>
            </div>
            <img v-else-if="generatedImage" :src="generatedImage" class="w-full h-full object-cover" />
            <div v-else class="text-center space-y-4 opacity-20">
              <ImageIcon :size="80" class="mx-auto text-zinc-400" />
              <p class="text-sm font-medium tracking-wide">生成的艺术品将在此处展示</p>
            </div>
          </Transition>

          <!-- 悬浮操作 -->
          <div v-if="generatedImage && !isImageLoading" class="absolute bottom-6 right-6 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="p-3 bg-white/90 dark:bg-zinc-900/90 backdrop-blur shadow-lg rounded-2xl hover:scale-110 transition-transform">
              <Upload :size="20" class="rotate-180" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
