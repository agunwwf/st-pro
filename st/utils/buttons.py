import streamlit as st

# 回到上一步和进入下一步按钮
def back_and_next_buttons(name,steps):
    flex = st.container(horizontal=True)
    if flex.button("回到上一步"):
        if st.session_state[name] > 0:
            setattr(st.session_state, name, st.session_state[name] - 1)
        else:
            st.warning("已经是第一步")
    if flex.button("进入下一步"):
        if st.session_state[name] < len(steps) - 1:
            setattr(st.session_state, name, st.session_state[name] + 1)
        else:
            st.warning("已经是最后一步")