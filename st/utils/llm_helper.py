from langchain_community.chat_models.tongyi import ChatTongyi
from langchain_core.messages import HumanMessage, SystemMessage
import os
import streamlit as st


# 从环境变量中获取密钥：os.getenv("变量名")，变量名建议大写，比如DEEPSEEK_API_KEY
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY")

# 判空：如果没获取到环境变量，提示错误（避免运行时崩溃）
if not DASHSCOPE_API_KEY:
    raise ValueError("请先设置 DASHSCOPE_API_KEY 环境变量！")

llm=ChatTongyi(
    model="qwen-turbo",
    api_key=DASHSCOPE_API_KEY,
    temperature=0.3
    )


def analyze_code(step_num: int, user_code:str,error_msg:str)-> str:
    """
    分析学生代码问题，返回详细的分析和修改建议
    """
    system_prompt = f"""
    你是一个Python机器学习教学助手，现在学生在第{step_num}步运行代码出错了。
    请你基于学生的代码和错误信息，给出清晰、易懂、适合初学者的分析：
    1.  先说明错误的核心原因，用大白话讲，不要太晦涩
    2.  指出代码里具体哪一行/哪一部分有问题
    3.  给出可直接运行的修正后的完整代码片段
    4.  补充几个相关的避坑小技巧

    注意：只针对错误本身分析，不要额外拓展无关内容，语气友好耐心。
    """

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=f"学生代码：\n{user_code}\n错误信息：{error_msg}")
    ]

    response=llm.invoke(messages)
    return response.content

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
    response = llm.invoke(messages)
    return response.content

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

    response=llm.invoke(messages)
    return response.content


def _step_ai_keys(module_id: str, step_num: int) -> dict:
    prefix = f"{module_id}_step{step_num}_ai"
    return {
        "error": f"{prefix}_error",
        "user_code": f"{prefix}_user_code",
        "analysis": f"{prefix}_analysis",
        "chat_history": f"{prefix}_chat_history",
        "chat_input": f"{prefix}_chat_input",
    }


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
        st.info(f"AI错误分析：\n{st.session_state[keys['analysis']]}")

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
