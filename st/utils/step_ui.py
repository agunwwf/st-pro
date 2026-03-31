from __future__ import annotations

import re

import streamlit as st


def ensure_step_code_defaults(
    *,
    code_snippets_key: str,
    text_area_key: str,
    starter_code: str,
    reference_code: str,
) -> None:
    """
    Make the default visible code be starter (fill-in style),
    while preserving student's work if they've already edited.
    """
    # 规则：如果该步已完成（completed_steps 包含 step_num），优先展示学生“最后一次正确提交”的代码；
    # 只有在没有保存到学生代码时，才退回显示 reference_code。
    step_num = None
    m = re.match(r"^step(\d+)_code$", text_area_key)
    if m:
        try:
            step_num = int(m.group(1))
        except Exception:
            step_num = None

    completed = st.session_state.get("completed_steps") if "completed_steps" in st.session_state else None
    if step_num is not None and completed is not None and isinstance(completed, (set, list, tuple)) and step_num in completed:
        existing_widget = st.session_state.get(text_area_key)
        student_snippet = None
        if "code_snippets" in st.session_state and isinstance(st.session_state.code_snippets, dict):
            student_snippet = st.session_state.code_snippets.get(code_snippets_key)

        # 如果没有任何 widget 值，就优先用学生提交；否则显示 reference_code
        if existing_widget is None:
            st.session_state[text_area_key] = student_snippet if (student_snippet is not None) else reference_code
        # 如果当前仍显示填空模板，但我们有学生的“最终正确代码”，则替换为学生代码
        elif existing_widget == starter_code:
            if student_snippet is not None and student_snippet != starter_code:
                st.session_state[text_area_key] = student_snippet
            elif student_snippet is None:
                st.session_state[text_area_key] = reference_code

        # 同步更新 code_snippets（确保后续持久化/恢复时一致）
        if "code_snippets" in st.session_state and isinstance(st.session_state.code_snippets, dict):
            if student_snippet is None:
                st.session_state.code_snippets[code_snippets_key] = reference_code
        return

    # code_snippets stores per-step code in many modules
    if "code_snippets" in st.session_state and isinstance(st.session_state.code_snippets, dict):
        existing = st.session_state.code_snippets.get(code_snippets_key)
        if existing is None:
            st.session_state.code_snippets[code_snippets_key] = starter_code

    # text_area_key is the actual bound widget state
    existing_widget = st.session_state.get(text_area_key)
    if existing_widget is None:
        st.session_state[text_area_key] = starter_code


def render_reference_answer(reference_code: str, *, title: str = "查看参考答案（可复制）") -> None:
    # Product decision: fill-in teaching flow should not expose reference answers.
    return

