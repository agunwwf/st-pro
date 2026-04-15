# cover.py
from openai import OpenAI
import os

# 未配置密钥时不应在 import 阶段抛错，否则 Streamlit 整站无法启动
_client: OpenAI | None = None
_cached_key: str | None = None


def _get_client() -> OpenAI | None:
    global _client, _cached_key
    key = (os.getenv("DEEPSEEK_API_KEY") or "").strip()
    if not key:
        _client = None
        _cached_key = None
        return None
    if _client is not None and _cached_key == key:
        return _client
    _client = OpenAI(api_key=key, base_url="https://api.deepseek.com")
    _cached_key = key
    return _client


# 兼容旧代码 `from utils.api_deepseek import client`（多数文件未实际使用）
client = None


def ask_ai_assistant(question, context=""):
    """向AI助教提问"""
    c = _get_client()
    if c is None:
        return (
            "未配置 DEEPSEEK_API_KEY 环境变量，无法调用 AI 助教。"
            "请在启动 Streamlit 的终端中设置该变量后再试；"
            "不调用 AI 时仍可正常浏览其它演示内容。"
        )
    try:
        system_prompt = f"""
        你是一个友好的机器学习助教，专门教授机器学习知识。
        你正在一个交互式学习平台中帮助学生。
        当前上下文: {context}
        请用中文回答学生的问题，保持专业但友好，且饱含鼓励肯定的语气。解释概念时请尽量使用比喻和直观的例子。
        回答字数控制在500字以内。
        """

        response = c.chat.completions.create(
            model="deepseek-chat",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": question},
            ],
            temperature=0.7,
            max_tokens=500,
        )

        return response.choices[0].message.content
    except Exception as e:
        return f"出错了: {str(e)}。请检查您的API密钥是否正确或网络连接。"
