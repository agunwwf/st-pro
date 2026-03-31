"""
与主站登录用户对齐：通过 iframe URL 中的 st_token（JWT）拉取资料，并在切换账号时清空 Streamlit session，避免做题进度串号。
"""

from __future__ import annotations

import html
import os
from typing import Any

import requests
import streamlit as st

BACKEND_API_URL = os.getenv("BACKEND_API_URL", "http://localhost:8080").rstrip("/")


def _normalize_token(raw: Any) -> str | None:
    """Normalize query token value to stable str/None."""
    if raw is None:
        return None
    # Streamlit query params may return list-like values in some environments.
    if isinstance(raw, (list, tuple)):
        raw = raw[0] if raw else None
    if raw is None:
        return None
    token = str(raw).strip()
    return token or None


def _fetch_me(token: str) -> dict[str, Any] | None:
    try:
        r = requests.get(
            f"{BACKEND_API_URL}/api/user/me",
            headers={"token": token},
            timeout=12,
        )
        data = r.json()
        if data.get("code") == 200 and data.get("data"):
            return data["data"]
    except Exception:
        pass
    return None


def _normalize_avatar_src(raw: str) -> str | None:
    """仅接受 URL 或 data: 内联图（与前端 Profile 保存格式一致）。"""
    if not raw:
        return None
    s = raw.strip()
    if s.startswith(("http://", "https://", "data:")):
        return s
    return None


def _render_sidebar_avatar(avatar: str, nickname: str) -> None:
    """侧边栏顶部小头像：统一用 HTML img，避免 st.image 对部分 URL/base64 不显示。"""
    src = _normalize_avatar_src(avatar)
    nick = (nickname or "?").strip() or "?"
    letter = html.escape(nick[:1].upper())

    if src:
        safe_src = html.escape(src, quote=True)
        st.sidebar.markdown(
            f'<div style="margin-bottom:6px">'
            f'<img src="{safe_src}" width="44" height="44" alt="" '
            f'style="border-radius:50%;object-fit:cover;display:block;'
            f'border:1px solid rgba(128,128,128,0.35);"/>'
            f"</div>",
            unsafe_allow_html=True,
        )
        return

    st.sidebar.markdown(
        f"""
        <div style="
            width:44px;height:44px;border-radius:50%;
            background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);
            color:#fff;font-size:18px;font-weight:600;
            text-align:center;line-height:44px;margin-bottom:6px;
            font-family:system-ui,sans-serif;
        ">{letter}</div>
        """,
        unsafe_allow_html=True,
    )


def sync_user_context() -> None:
    token_from_query = _normalize_token(st.query_params.get("st_token") or st.query_params.get("token"))
    prev_token = _normalize_token(st.session_state.get("_st_token"))

    # 容错策略：
    # 1) URL 临时丢 token（刷新/重载）时，不要误判成“换账号”，沿用上一次 token；
    # 2) 只有“明确拿到新 token 且与旧 token 不同”才清空 session（防止串号）。
    if token_from_query is None and prev_token is not None:
        token = prev_token
    else:
        token = token_from_query

    if token is not None and prev_token is not None and token != prev_token:
        st.session_state.clear()

    st.session_state["_st_token"] = token

    if not token:
        st.session_state["_user_profile"] = None
        return

    if st.session_state.get("_user_profile_loaded_for") == token:
        return

    st.session_state["_user_profile"] = _fetch_me(token)
    st.session_state["_user_profile_loaded_for"] = token


def render_user_sidebar() -> None:
    token = st.session_state.get("_st_token")
    profile = st.session_state.get("_user_profile")

    st.sidebar.markdown("---")

    if not token:
        st.sidebar.warning(
           "请先登录系统，再使用实验功能。"
        )
        return

    if not profile:
        st.sidebar.warning(
            "无法加载用户信息。请尝试刷新页面或重新登录。"
            "如问题持续存在，请联系管理员获取帮助。"
        )
        return

    nickname = profile.get("nickname") or profile.get("username") or "用户"
    avatar = (profile.get("avatar") or "").strip()

    safe_nick = html.escape(str(nickname))

    _render_sidebar_avatar(avatar, nickname)
    st.sidebar.markdown(f"**{safe_nick}**")
