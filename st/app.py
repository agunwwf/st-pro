"""Streamlit unified entry for local development.

Usage:
  streamlit run D:\testttttt\st\app.py --server.port 8501
"""

import streamlit as st

from utils.user_context import render_user_sidebar, sync_user_context

import kMeans_demo
import logistic_regression_demo
import neural_network_demo
import linear_regression_demo
import bayes_text_classification_step_by_step



def main() -> None:
    st.set_page_config(page_title="ML Demos", layout="wide")
    sync_user_context()
    # Hide Streamlit built-in multipage sidebar nav (app/pages),
    # keep only the custom "项目导航".
    # 👇 新增：统一拦截并提取 Vue 传来的 Token，存入全局状态
    token = st.query_params.get("st_token")
    if token:
        st.session_state["global_token"] = token
    render_user_sidebar()
    st.sidebar.title("项目导航")
    project_options = [
        ("kmeans", "K-means Teaching Platform"),
        ("logistic", "逻辑回归交互式学习平台"),
        ("neural", "神经网络交互式学习平台"),
        ("linear", "线性回归交互式学习平台"),
        ("text", "文本分析与分类交互式学习平台"),
    ]

    query_project = st.query_params.get("project", "kmeans")
    keys = [item[0] for item in project_options]
    if query_project not in keys:
        query_project = "kmeans"

    default_index = keys.index(query_project)
    labels = [item[1] for item in project_options]
    selected_label = st.sidebar.radio("选择项目", labels, index=default_index)
    selected_key = project_options[labels.index(selected_label)][0]
    st.query_params["project"] = selected_key

    if selected_key == "kmeans":
        kMeans_demo.main()
    elif selected_key == "logistic":
        logistic_regression_demo.main()
    elif selected_key == "neural":
        neural_network_demo.main()
    elif selected_key == "linear":
        linear_regression_demo.main()
    else:
        bayes_text_classification_step_by_step.main()


if __name__ == "__main__":
    main()

