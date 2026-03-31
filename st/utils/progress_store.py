import json
import os
import re
from typing import Any

import streamlit as st
import numpy as np


_ST_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
PROGRESS_FILE = os.path.join(_ST_DIR, "st_step_progress.json")
_ACTIVE_MODULE_KEY = "_st_active_module_id"


def isolate_module_session(module_id: str) -> None:
    """
    Prevent cross-module coupling in Streamlit session_state.

    Many step-by-step boards historically used the same generic keys such as:
    - step, completed_steps, code_snippets, step{i}_code, X_train, ...
    When switching boards, those keys would leak into the next module and
    incorrectly move the user to the wrong step / show wrong code.

    Strategy: when module changes, clear a superset of shared keys so the next
    module can initialize and restore its own progress cleanly.
    """
    prev = st.session_state.get(_ACTIVE_MODULE_KEY)
    if prev == module_id:
        return

    # Keys that are shared across the five step-by-step boards (superset).
    shared_keys: set[str] = {
        # navigation / code editing
        "step",
        "completed_steps",
        "code_snippets",
        "ai_feedback",
        # common ML data pipeline names across modules
        "data",
        "raw_dataset",
        "feature_names",
        "chinese_feature_names",
        "X",
        "y",
        "true_labels",
        "X_raw",
        "y_raw",
        "X_train",
        "X_test",
        "y_train",
        "y_test",
        "X_scaled",
        "X_train_scaled",
        "X_test_scaled",
        "cluster_labels",
        "X_pca",
        # common results / metrics
        "y_pred",
        "y_pred_linear",
        "y_pred_nn",
        "mse",
        "r2",
        "accuracy",
        "precision",
        "recall",
        "f1",
        "cm",
        "report",
        "silhouette",
        "calinski_harabasz",
        "linear_mse",
        "linear_r2",
        "nn_mse",
        "nn_r2",
        # objects we intentionally do not persist, but may exist in session
        "model",
        "scaler",
        "linear_model",
        "nn_model",
        "tfidf_vectorizer",
        # text classification
        "X_train_text",
        "X_test_text",
        "class_names",
        "X_train_tfidf",
        "X_test_tfidf",
    }
    # step code widget keys
    for i in range(1, 50):
        shared_keys.add(f"step{i}_code")

    # Also clear module-scoped error contexts from previous module
    # (they are stored with prefix "{module_id}_step", so safe to remove by previous id).
    if prev:
        prev_prefix = f"{prev}_step"
        for k in list(st.session_state.keys()):
            if str(k).startswith(prev_prefix):
                try:
                    del st.session_state[k]
                except Exception:
                    pass

    for k in shared_keys:
        if k in st.session_state:
            try:
                del st.session_state[k]
            except Exception:
                # best-effort; widget keys can be protected sometimes
                st.session_state[k] = None

    # 强制该模块在本次进入时重新从磁盘恢复进度：
    # 之前访问过该模块时可能已经把 `<module>_progress_restored` 置为 True，
    # 如果不清掉，它就会跳过 restore_step_progress，导致只保留默认 step0。
    restore_flag = f"{module_id}_progress_restored"
    if restore_flag in st.session_state:
        try:
            del st.session_state[restore_flag]
        except Exception:
            st.session_state[restore_flag] = False

    st.session_state[_ACTIVE_MODULE_KEY] = module_id


def _safe_user_id() -> str:
    # Prefer the token synced by `sync_user_context()` (key: "_st_token").
    # Fallback to "global_token" (set by `st/app.py`), and finally "anonymous".
    token = st.session_state.get("_st_token") or st.session_state.get("global_token") or "anonymous"
    token = str(token) if token else "anonymous"
    return re.sub(r"[^A-Za-z0-9_.-]", "_", token)


def _load_all_progress() -> dict[str, Any]:
    if not os.path.exists(PROGRESS_FILE):
        return {}
    try:
        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


def _save_all_progress(data: dict[str, Any]) -> None:
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def _serialize_value(value: Any) -> Any:
    if isinstance(value, set):
        return {"__type__": "set", "value": sorted(list(value))}
    # numpy 标量类型（如 np.int64、np.float32）
    if isinstance(value, np.generic):
        return value.item()
    # numpy 数组：序列化为普通列表，必要时可在读取时还原
    if isinstance(value, np.ndarray):
        return {"__type__": "ndarray", "value": value.tolist()}
    # 递归处理字典
    if isinstance(value, dict):
        return {k: _serialize_value(v) for k, v in value.items()}
    # 递归处理列表
    if isinstance(value, (list, tuple)):
        return [_serialize_value(item) for item in value]
    return value



def _deserialize_value(value: Any) -> Any:
    if isinstance(value, dict) and value.get("__type__") == "set":
        return set(value.get("value", []))
    if isinstance(value, dict) and value.get("__type__") == "ndarray":
        return np.array(value.get("value", []))
    return value


def restore_step_progress(module_id: str, base_keys: list[str]) -> None:
    restore_flag = f"{module_id}_progress_restored"
    if st.session_state.get(restore_flag):
        return

    all_data = _load_all_progress()
    user_id = _safe_user_id()
    user_data = all_data.get(user_id, {})
    module_data = user_data.get(module_id, {})

    # Fallback: if we can't find progress for the current user,
    # but there is "anonymous" progress, use it.
    # This prevents "refresh = can't restore" when st_token wasn't present at first save.
    if not module_data and user_id != "anonymous":
        anonymous_data = all_data.get("anonymous", {})
        module_data = anonymous_data.get(module_id, {})
    if not isinstance(module_data, dict):
        st.session_state[restore_flag] = True
        return

    for key in base_keys:
        if key in module_data:
            st.session_state[key] = _deserialize_value(module_data[key])

    # 恢复模块级AI上下文（按前缀隔离）
    ai_prefix = f"{module_id}_step"
    for key, value in module_data.items():
        if key.startswith(ai_prefix):
            st.session_state[key] = _deserialize_value(value)

    st.session_state[restore_flag] = True


def persist_step_progress(module_id: str, base_keys: list[str]) -> None:
    user_id = _safe_user_id()
    all_data = _load_all_progress()
    if user_id not in all_data:
        all_data[user_id] = {}

    module_payload = {}
    for key in base_keys:
        if key in st.session_state:
            module_payload[key] = _serialize_value(st.session_state[key])

    # 保存模块级AI上下文（按前缀隔离）
    ai_prefix = f"{module_id}_step"
    for key in st.session_state:
        if str(key).startswith(ai_prefix):
            module_payload[key] = _serialize_value(st.session_state[key])

    all_data[user_id][module_id] = module_payload
    _save_all_progress(all_data)
