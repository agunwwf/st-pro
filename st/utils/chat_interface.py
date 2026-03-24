import streamlit as st
from utils.api_deepseek import ask_ai_assistant

# 工具函数：显示聊天界面 button_list和question_list为列表,带4个值
def display_chat_interface(context="", button_list=[], question_list=[]):
    """显示聊天界面（不保存历史记录）"""
    st.sidebar.markdown("---")
    st.sidebar.subheader("💬 AI助教已就绪")

    # 预设问题快捷按钮
    st.sidebar.markdown("**快捷问题:**")
    col1, col2 = st.sidebar.columns(2)

    with col1:
        btn1 = st.button(button_list[0])
        btn2 = st.button(button_list[1])

    with col2:
        btn3 = st.button(button_list[2])
        btn4 = st.button(button_list[3])

    # 处理快捷问题
    question = ""
    if btn1:
        question = question_list[0]
    elif btn2:
        question = question_list[1]
    elif btn3:
        question = question_list[2]
    elif btn4:
        question = question_list[3]

    # 提问输入框

    # user_input = st.sidebar.text_input("输入你的问题:", key="question_input")
    # if user_input:
    #     question = user_input
    st.sidebar.markdown("**或输入自定义问题:**")
    with st.sidebar.form(key="question_input", clear_on_submit=True):
        user_input = st.text_input("", placeholder="在此输入问题...")
        submitted = st.form_submit_button("提问",use_container_width = False)

    if submitted and user_input:
        question = user_input


    # 处理提问
    if question:
        # 显示当前问题
        st.sidebar.markdown(f"**你:** {question}")

        # 获取回答
        with st.sidebar:
            with st.spinner("助教思考中..."):
                answer = ask_ai_assistant(question, context)

        # 显示当前回答
        st.sidebar.markdown(f"**助教:** {answer}")
        st.sidebar.markdown("---")