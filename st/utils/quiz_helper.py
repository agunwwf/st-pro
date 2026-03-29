import streamlit as st
import requests
import json

# 动态读取环境变量，如果没有配置就默认 localhost
try:
    BASE_URL = st.secrets["API_BASE_URL"]
except:
    BASE_URL = "http://localhost:8080"

def get_real_attempts(token: str, module_id: str) -> int:
    """ 向后端查询数据库里真实的答题次数"""
    url = f"{BASE_URL}/api/score/attempts?moduleId={module_id}"
    headers = {"Authorization": f"Bearer {token}"}
    try:
        response = requests.get(url, headers=headers, timeout=3)
        if response.status_code == 200:
            result_data = response.json()
            # 假设你的 Result.success() 把数据放在 "data" 字段里
            return result_data.get("data", 0) 
    except Exception as e:
        print(f"获取次数失败: {e}")
    return 0 # 如果网络报错，默认按 0 次算，保证业务不中断

def submit_score_to_backend(token: str, module_id: str, score: int, answers_detail: dict) -> tuple[bool, str]:
    """把成绩和错题详情发送给后端"""
    url = f"{BASE_URL}/api/score/save" 
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    payload = {
        "moduleId": module_id,
        "score": score,
        "answersDetail": json.dumps(answers_detail, ensure_ascii=False)
    }
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=5)
        if response.status_code == 200:
            result_data = response.json()
            if str(result_data.get("code")) in ["200", "0"]: 
                return True, "成绩已安全同步至云端！"
            return False, result_data.get("msg", "后端拒绝接收")
        elif response.status_code == 401:
            return False, "登录已过期，请重新登录"
        return False, f"服务器请求失败 ({response.status_code})"
    except Exception as e:
        return False, f"网络异常: {str(e)}"

def render_quiz_component(module_key: str, title: str, description: str, quiz_data: list):
    """支持云端强一致性与 A/B/C/D 优化的测验组件"""
    st.header(title)
    st.markdown(description)

    token = st.session_state.get("global_token")
    if not token:
        st.info("💡 离线演示模式，成绩不会保存至云端。")

    # 状态 Key 管理
    state_sub = f"{module_key}_submitted"
    state_ans = f"{module_key}_answers"
    state_msg = f"{module_key}_msg"
    state_attempts = f"{module_key}_attempts"
    state_synced = f"{module_key}_attempts_synced" # 标记是否已经和后端同步过次数

    # 初始化状态
    if state_sub not in st.session_state: st.session_state[state_sub] = False
    if state_ans not in st.session_state: st.session_state[state_ans] = {}
    if state_msg not in st.session_state: st.session_state[state_msg] = None
    if state_synced not in st.session_state: st.session_state[state_synced] = False

    # 如果还没和后端同步过，赶紧去查一下真实次数
    if token and not st.session_state[state_synced]:
        real_attempts = get_real_attempts(token, module_key)
        st.session_state[state_attempts] = real_attempts
        st.session_state[state_synced] = True
    elif not token and state_attempts not in st.session_state:
        st.session_state[state_attempts] = 0 # 离线模式默认 0 次

    # 如果达到 3 次，立刻封锁界面，F5 刷新也没用
    if st.session_state[state_attempts] >= 3:
        st.error("🛑 您已用完本模块的 3 次答题机会，无法再次测试。")
        st.info("📊 答题记录已锁定，请前往个人中心查看历史成绩及错题分析。")
        return 

    st.caption(f"🛡️ 当前模块剩余答题机会：**{3 - st.session_state[state_attempts]} / 3**")

    if not st.session_state[state_sub]:
        with st.form(f"{module_key}_quiz_form"):
            answers = {}
            for i, item in enumerate(quiz_data):
                st.markdown(f"**{item['question']}**")
                answers[i] = st.radio("请选择:", item["options"], key=f"{module_key}_q_{i}", index=None, label_visibility="collapsed")
                st.write("---")
            
            if st.form_submit_button("🚀 提交试卷并评分", type="primary"):
                if None in answers.values():
                    st.warning("⚠️ 请完成所有题目后再提交！")
                else:
                    correct_count = sum(1 for i, item in enumerate(quiz_data) if answers[i] == item["answer"])
                    final_score = int((correct_count / len(quiz_data)) * 100)

                    
                    # 例如：{"Q1": "A", "Q2": "C"}
                    abc_map = {0: "A", 1: "B", 2: "C", 3: "D", 4: "E"}
                    answers_detail = {}
                    for i, item in enumerate(quiz_data):
                        # 找到用户选的答案在选项列表里的索引 (0, 1, 2...)
                        selected_index = item["options"].index(answers[i])
                        # 转换为 A/B/C
                        answers_detail[f"Q{i+1}"] = abc_map.get(selected_index, "Unknown")

                    if token:
                        with st.spinner('正在同步成绩与错题本...'):
                            success, msg = submit_score_to_backend(token, module_key, final_score, answers_detail)
                            st.session_state[state_msg] = {"success": success, "msg": msg}
                            if success:
                                st.session_state[state_attempts] += 1
                    else:
                        st.session_state[state_attempts] += 1
                    
                    st.session_state[state_ans] = answers
                    st.session_state[state_sub] = True
                    st.rerun()
    else:
        st.subheader("📊 测验成绩报告")
        correct_count = sum(1 for i, item in enumerate(quiz_data) if st.session_state[state_ans][i] == item["answer"])
        final_score = int((correct_count / len(quiz_data)) * 100)
        
        col1, col2 = st.columns(2)
        col1.metric("最终得分", f"{final_score} 分")
        col2.metric("正确题数", f"{correct_count} / {len(quiz_data)}")

        if st.session_state[state_msg]:
            info = st.session_state[state_msg]
            st.success(f"☁️ {info['msg']}") if info["success"] else st.error(f"☁️ {info['msg']}")

        st.divider()
        st.markdown("### 📝 错题与解析")
        for i, item in enumerate(quiz_data):
            user_ans = st.session_state[state_ans][i]
            if user_ans == item["answer"]:
                st.success(f"✅ {item['question']} \n\n你的答案：{user_ans}")
            else:
                st.error(f"❌ {item['question']} \n\n你的答案：{user_ans} | 🎯 正确答案：{item['answer']}")
            st.info(f"💡 解析：{item['explanation']}")
            st.write("---")

        if st.session_state[state_attempts] < 3:
            if st.button("🔄 消耗 1 次机会重新测试", key=f"{module_key}_reset"):
                st.session_state[state_sub] = False
                st.session_state[state_ans] = {}
                st.session_state[state_msg] = None
                st.rerun()
        else:
            st.warning("您已用完所有 3 次测试机会。")