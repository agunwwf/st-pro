import { ref } from 'vue';

// 从环境变量中获取 API Key
// 在 Vite 中，环境变量通常以 VITE_ 开头
const API_KEY = import.meta.env.VITE_DEEPSEEK_API_KEY || '';
const BASE_URL = 'https://api.deepseek.com/chat/completions';

export function useDeepSeek() {
    const isChatLoading = ref(false);
    const isImageLoading = ref(false);

    // 聊天请求逻辑 (DeepSeek Chat)
    const sendMessage = async (messages) => {
        if (!API_KEY) {
            throw new Error('未配置 DEEPSEEK_API_KEY 环境变量');
        }

        isChatLoading.value = true;
        try {
            const response = await fetch(BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${API_KEY}`
                },
                body: JSON.stringify({
                    model: "deepseek-chat",
                    messages: messages,
                    stream: false
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error?.message || '请求失败');
            }

            const data = await response.json();
            return data.choices[0].message.content;
        } catch (error) {
            console.error('DeepSeek API Error:', error);
            throw error;
        } finally {
            isChatLoading.value = false;
        }
    };

    // 图像生成逻辑 (DeepSeek 目前不支持生图，此处为 UI 演示或预留接口)
    const generateImage = async (prompt, referenceImage) => {
        isImageLoading.value = true;
        try {
            // 模拟延迟
            await new Promise(resolve => setTimeout(resolve, 2000));

            console.log('正在使用提示词生成图像:', prompt);

            // 返回一个占位图作为演示
            return `https://picsum.photos/seed/${encodeURIComponent(prompt)}/1024/1024`;
        } catch (error) {
            console.error('Image Generation Error:', error);
            throw error;
        } finally {
            isImageLoading.value = false;
        }
    };

    return {
        isChatLoading,
        isImageLoading,
        sendMessage,
        generateImage
    };
}
