from langchain_community.chat_models.tongyi import ChatTongyi
from langchain_core.messages import HumanMessage, SystemMessage
import os
import re
import streamlit as st


# 从环境变量中获取密钥：os.getenv("变量名")，变量名建议大写，比如DEEPSEEK_API_KEY
# sk-dfcdca01541d412cad20383052c6e554
# DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY")

# 判空：如果没获取到环境变量，提示错误（避免运行时崩溃）
# if not DASHSCOPE_API_KEY:
#     raise ValueError("请先设置 DASHSCOPE_API_KEY 环境变量！")

llm=ChatTongyi(
    model="qwen-turbo",
    api_key="sk-dfcdca01541d412cad20383052c6e554",
    temperature=0.3
    )


def _invoke_llm_with_stream(messages, stream_placeholder=None) -> str:
    """
    Use streaming when available and optionally render incremental text in UI.
    Falls back to invoke() if stream() is not available or fails.
    """
    chunks = []
    try:
        for chunk in llm.stream(messages):
            text = getattr(chunk, "content", "")
            if not text:
                continue
            chunks.append(text)
            if stream_placeholder is not None:
                stream_placeholder.markdown("".join(chunks))
        final_text = "".join(chunks).strip()
        if final_text:
            if stream_placeholder is not None:
                stream_placeholder.empty()
            return final_text
    except Exception:
        # fallback to non-streaming invoke
        pass

    response = llm.invoke(messages)
    final_text = response.content if getattr(response, "content", None) else ""
    if stream_placeholder is not None:
        stream_placeholder.empty()
    return final_text


def analyze_code(step_num: int, user_code: str, error_msg: str, reference_code: str = "") -> str:
    """
    分析学生代码问题，返回详细的分析和修改建议
    """
    reference_hint = ""
    if reference_code:
        reference_hint = f"""
    参考答案（标准写法，优先以此为准）：
    {reference_code}

    如果你给出修正代码，请尽量贴近参考答案的结构和命名，避免引入本步骤未涉及的新写法。
    """

    has_unfilled_blank = bool(re.search(r"_{4,}", user_code or ""))
    blank_scene_hint = ""
    if has_unfilled_blank:
        blank_scene_hint = """
    当前场景补充（非常重要）：
    - 学生很可能还在思考、还没来得及填完整代码。
    - 你的职责是“温和引导完成本步骤”，不是强调模板符号本身。
    - 禁止出现这些表述：横线、占位符、下划线、空格子、________。
    - 请改用自然表达，例如：先把关键参数补全、先完成这两行核心代码、先把数据集划分与标准化写完整。
    - 语气要像助教：先共情，再给可执行修正方案。
    """

    system_prompt = f"""
    你是一个Python机器学习教学助手，现在学生在第{step_num}步运行代码出错了。
    你只负责当前步骤的“错误分析 + 修正引导”，目标是让学生快速理解并完成本步骤。

    输出要求（非常重要）：
    1) 先用几句中文解释错误原因（不少于三句）
       - 先给一句自然、鼓励式开场（例如：这一步思路已经接近正确了，我们把关键代码补全即可）
       - 不要指责，不要机械报错，不要复述编译器原文
    2) 然后给出【一段】可直接运行的完整修正代码，放在一个 ```python 代码块``` 中
       - 代码中不要包含任何未填写模板符号
       - 尽量贴近参考答案(reference)的结构/变量名/导入
       - 不要引入本步骤未涉及的新库/新写法
    3) 最后给出 3 条要点提示（用列表即可），例如常见坑/下一步该怎么改

    注意：只针对错误本身分析，不要额外拓展无关内容，语气友好耐心、像真人助教。
    {reference_hint}
    {blank_scene_hint}
    """

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=f"学生代码：\n{user_code}\n错误信息：{error_msg}")
    ]

    stream_placeholder = st.empty()
    return _invoke_llm_with_stream(messages, stream_placeholder=stream_placeholder)

# 代码成功时的通用提问解答函数
def chat_about_success(user_code: str, user_question: str) -> str:
    system_prompt = f"""
    你是Python机器学习教学助手，学生成功运行了代码，现在针对这段代码提问。
    请结合学生的代码，用大白话清晰、耐心地解答，适合初学者理解。
    只围绕当前代码和步骤内容解答，不要拓展无关内容。
    """
    context = f"""
    学生的代码：\n{user_code}
    学生的问题：{user_question}
    """
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=context)
    ]
    return _invoke_llm_with_stream(messages)

def chat_with_llm(question: str, history_code: str,history_error: str,history_analysis: str) -> str:
    """
    学生继续追问，与LLM进行对话提问，返回解答
    """
    system_prompt = f"""
    你是一个Python机器学习教学助手，学生针对之前的代码错误继续提问。
    你需要结合之前的代码、错误信息和分析，解答学生的问题，保持耐心，用初学者能听懂的话解释，不要敷衍。
    """

    context = f"""
               学生代码：\n{history_code}\n
               错误信息：{history_error}\n
               历史分析：{history_analysis}
               学生的新问题：{question}
    """

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=context)
    ]

    return _invoke_llm_with_stream(messages)


def _step_ai_keys(module_id: str, step_num: int) -> dict:
    prefix = f"{module_id}_step{step_num}_ai"
    return {
        "error": f"{prefix}_error",
        "user_code": f"{prefix}_user_code",
        "analysis": f"{prefix}_analysis",
        "chat_history": f"{prefix}_chat_history",
        "chat_input": f"{prefix}_chat_input",
    }


def _split_analysis_code_tips(content: str) -> tuple[str, str, str]:
    """
    Split LLM response into (explanation, code, tips).

    Rules:
    - Take the first ```python ... ``` code block as "code".
    - Text before the block is "explanation".
    - Text after the block is "tips" (may be empty).
    """
    s = content or ""
    pattern = re.compile(r"```(?:python)?\s*\n?([\s\S]*?)```", re.IGNORECASE)
    match = pattern.search(s)
    if not match:
        return s.strip(), "", ""

    code = match.group(1).strip()
    explanation = (s[: match.start()] or "").strip()
    tips = (s[match.end() :] or "").strip()
    return explanation, code, tips


def render_ai_error_analysis(content: str) -> None:
    """
    Render AI error analysis in a blue "feedback card".
    """
    explanation, code, tips = _split_analysis_code_tips(content)

    with st.container(border=True):
        st.subheader("AI错误分析与修正建议")

        if explanation:
            st.info(explanation)

        if code:
            st.markdown("**修正代码（复制后可直接运行）**")
            st.code(code, language="python")

        if tips:
            st.markdown("**要点提示（3条）**")
            st.info(tips)


def save_step_error_context(module_id: str, step_num: int, user_code: str, error_msg: str, ai_analysis: str) -> None:
    keys = _step_ai_keys(module_id, step_num)
    st.session_state[keys["error"]] = error_msg
    st.session_state[keys["user_code"]] = user_code
    st.session_state[keys["analysis"]] = ai_analysis
    if keys["chat_history"] not in st.session_state:
        st.session_state[keys["chat_history"]] = []


def clear_step_error_context(module_id: str, step_num: int, keep_chat_history: bool = True) -> None:
    keys = _step_ai_keys(module_id, step_num)
    for key in [keys["error"], keys["analysis"]]:
        if key in st.session_state:
            del st.session_state[key]
    if not keep_chat_history and keys["chat_history"] in st.session_state:
        del st.session_state[keys["chat_history"]]


def render_step_qa_panel(module_id: str, step_num: int, current_user_code: str) -> None:
    keys = _step_ai_keys(module_id, step_num)
    if keys["chat_history"] not in st.session_state:
        st.session_state[keys["chat_history"]] = []
    if current_user_code:
        st.session_state[keys["user_code"]] = current_user_code

    has_error_context = keys["analysis"] in st.session_state and keys["error"] in st.session_state
    if has_error_context:
        render_ai_error_analysis(st.session_state[keys["analysis"]])

    st.divider()
    st.subheader("关于本步骤的提问")

    for msg in st.session_state[keys["chat_history"]]:
        st.chat_message(msg["role"]).write(msg["content"])

    if question := st.chat_input("有不懂的地方可以直接问我...", key=keys["chat_input"]):
        st.chat_message("user").write(question)
        st.session_state[keys["chat_history"]].append({"role": "user", "content": question})

        with st.spinner("正在解答..."):
            if has_error_context:
                ai_reply = chat_with_llm(
                    question=question,
                    history_code=st.session_state.get(keys["user_code"], ""),
                    history_error=st.session_state.get(keys["error"], ""),
                    history_analysis=st.session_state.get(keys["analysis"], "")
                )
            else:
                ai_reply = chat_about_success(
                    user_code=st.session_state.get(keys["user_code"], current_user_code),
                    user_question=question
                )

        st.chat_message("assistant").write(ai_reply)
        st.session_state[keys["chat_history"]].append({"role": "assistant", "content": ai_reply})
