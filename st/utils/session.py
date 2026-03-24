import streamlit as st

# 工具函数：初始化会话状态
def init_session_state(defaults={}):
    for key, value in defaults.items():
        if key not in st.session_state:
            st.session_state[key] = value

# 工具函数：清除会话状态但保留重要状态
def clear_session_state(preserve_keys=None):
    """
    清除会话状态，但保留指定的键

    Args:
        preserve_keys: 要保留的键列表，默认保留 ['logged_in', 'name', 'token']
    """
    if preserve_keys is None:
        preserve_keys = ['logged_in', 'name', 'token']

    # 保存需要保留的值
    preserved_values = {}
    for key in preserve_keys:
        if key in st.session_state:
            preserved_values[key] = st.session_state[key]

    # 清除所有状态
    st.session_state.clear()

    # 恢复保留的值
    for key, value in preserved_values.items():
        st.session_state[key] = value

    
    
