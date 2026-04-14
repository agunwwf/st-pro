"""
教学完成记录：Streamlit 调用 Java 后端，把「演示完成 / 分步完成」写入 MySQL。

数据流（建议按这个顺序读代码）：
1. 主站 Vue 在 iframe 里打开 Streamlit，URL 带 st_token（JWT）。
2. app.py 里 sync_user_context() 把 token 放进 st.session_state["_st_token"]。
3. 学生在侧栏点「我已完成…演示教学」或在 step7 点「我已完成…分步练习」→ 本文件 POST /api/learning/complete。
4. 后端 JwtInterceptor 校验 token，取出 userId，插入 sys_learning_completion（唯一键防重复）。
5. 学生回到 Dashboard → 请求 GET /api/learning/summary → 卡片显示 totalCount/maxCount。

module_id: kmeans | logistic | neural | linear | text（与 app.py 里 project 参数一致）
kind: demo（演示教学） | step（分步练习）
"""

from __future__ import annotations

import os

import requests
import streamlit as st

# 与 user_context.py 一样，部署时可用环境变量指到真实后端地址（如 Docker 里用 host.docker.internal）
BACKEND_API_URL = os.getenv("BACKEND_API_URL", "http://localhost:8080").rstrip("/")

# 与 Java LearningCompletionServiceImpl.MODULES 保持一致，多写一层校验避免拼错 moduleId
ALLOWED_MODULES = frozenset({"kmeans", "logistic", "neural", "linear", "text"})


def submit_learning_complete(module_id: str, kind: str) -> tuple[bool, str]:
    """
    发 HTTP POST 写库。返回 (是否成功, 给人看的提示文案)。
    不在此函数里 st.rerun：按钮点击后 Streamlit 会自动重跑脚本，success/error 会显示在同一轮。
    """
    mid = (module_id or "").strip().lower()
    k = (kind or "").strip().lower()
    if mid not in ALLOWED_MODULES or k not in ("demo", "step"):
        return False, "内部错误：无效的模块或类型"
    # 登录态来自 iframe 的 query：无 token 则无法关联到具体用户，后端会 401 或你不应调用
    token = st.session_state.get("_st_token")
    if not token:
        return False, "未同步登录：请从主站登录后打开本实验，或检查链接是否带 st_token"
    try:
        r = requests.post(
            f"{BACKEND_API_URL}/api/learning/complete",
            # Spring 的 JwtInterceptor 认 header "token"，与 Vue 的 axios 拦截器一致
            headers={"token": token, "Content-Type": "application/json"},
            json={"moduleId": mid, "kind": k},
            timeout=12,
        )
        data = r.json()
        if data.get("code") == 200:
            return True, "已同步到学习档案"
        return False, data.get("msg") or f"记录失败（HTTP {r.status_code}）"
    except Exception as e:
        return False, f"无法连接后端：{e}"


def render_demo_teaching_complete(api_module_id: str) -> None:
    """
    演示教学完成改为在概念测验提交后自动同步（见 quiz_helper.py）。
    """
    return


def render_step_teaching_complete(api_module_id: str) -> None:
    """
    插在分步教程最后一页（step7）：用户完成编码闯关后点一次，记 kind=step。
    用主内容区 st.button（不是侧栏），因为分步模式侧栏已被「步骤进度」占满。
    """
    st.divider()
    st.caption("学习进度同步")
    if st.button(
        "我已完成本分步练习教学",
        key=f"learning_step_done_{api_module_id}",
        type="primary",
    ):
        ok, msg = submit_learning_complete(api_module_id, "step")
        if ok:
            st.success(msg)
        else:
            st.error(msg)
