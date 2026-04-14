from __future__ import annotations

import json
import os
from typing import Tuple

import streamlit as st


def _progress_file(module_id: str) -> str:
    safe = "".join(ch if ch.isalnum() or ch in ("_", "-") else "_" for ch in (module_id or "module"))
    return f"user_code_progress_{safe}.json"


def _load_progress(module_id: str) -> dict:
    path = _progress_file(module_id)
    if not os.path.exists(path):
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data if isinstance(data, dict) else {}
    except Exception:
        return {}


def _save_progress(module_id: str) -> None:
    path = _progress_file(module_id)
    data = {k: v for k, v in st.session_state.items() if isinstance(k, str) and k.endswith("_code")}
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def render_code_editor_with_reset(
    *,
    module_id: str,
    text_area_key: str,
    default_code: str,
    height: int,
    run_button_key: str,
    run_button_label: str = "运行代码",
    prompt: str = "请补充代码：",
    reset_button_key: str | None = None,
    code_snippet_key: str | None = None,
) -> Tuple[str, bool]:
    """
    Unified code editor area with:
    1) auto-save to disk on change
    2) reset-to-default button
    3) run button placed next to reset button
    """
    if text_area_key not in st.session_state:
        disk = _load_progress(module_id)
        st.session_state[text_area_key] = disk.get(text_area_key, default_code)

    def _on_change_save() -> None:
        if code_snippet_key and "code_snippets" in st.session_state and isinstance(st.session_state.code_snippets, dict):
            st.session_state.code_snippets[code_snippet_key] = st.session_state.get(text_area_key, "")
        _save_progress(module_id)

    def _on_reset() -> None:
        st.session_state[text_area_key] = default_code
        if code_snippet_key and "code_snippets" in st.session_state and isinstance(st.session_state.code_snippets, dict):
            st.session_state.code_snippets[code_snippet_key] = default_code
        _save_progress(module_id)

    user_code = st.text_area(prompt, height=height, key=text_area_key, on_change=_on_change_save)

    col1, col2 = st.columns([5, 1])
    with col1:
        run_clicked = st.button(run_button_label, key=run_button_key)
    with col2:
        st.button("↺ 还原", key=reset_button_key or f"reset_{text_area_key}", on_click=_on_reset)

    return user_code, run_clicked
