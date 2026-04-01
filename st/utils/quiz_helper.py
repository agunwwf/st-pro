import streamlit as st
import requests
import json

# 读取环境变量，默认 localhost
try:
    BASE_URL = st.secrets["API_BASE_URL"]
except:
    BASE_URL = "http://localhost:8080"

def check_is_completed(token: str, module_id: str) -> bool:
    """ 向后端查询是否已经做过该模块 (防止刷新钻空子) """
    # 之后我们要写这个后端接口，现在先假设它返回状态
    url = f"{BASE_URL}/api/score/check?moduleId={module_id}"
    headers = {"Authorization": f"Bearer {token}"}
    try:
        response = requests.get(url, headers=headers, timeout=3)
        if response.status_code == 200:
            result = response.json()
            return result.get("data", False) # 返回 True 代表做过了
    except:
        pass
    return False 


def get_score_detail(token: str, module_id: str) -> dict | None:
    """读取该模块首提成绩详情（score + answersDetail）"""
    url = f"{BASE_URL}/api/score/detail?moduleId={module_id}"
    headers = {"Authorization": f"Bearer {token}"}
    try:
        response = requests.get(url, headers=headers, timeout=3)
        if response.status_code == 200:
            result = response.json()
            if str(result.get("code")) in ["200", "0"] and result.get("data"):
                return result.get("data")
    except Exception:
        pass
    return None

def submit_detailed_score(token: str, module_id: str, score: int, answers_detail: list) -> tuple[bool, str]:
    """ 把成绩和超级病历发送给后端存库 """
    url = f"{BASE_URL}/api/score/save" 
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    payload = {
        "moduleId": module_id,  # 极其重要！Vue 以后就靠这个区分是哪个算法的错题
        "score": score,
        "answersDetail": json.dumps(answers_detail, ensure_ascii=False)
    }
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=5)
        if response.status_code == 200:
            res_data = response.json()
            if str(res_data.get("code")) in ["200", "0"]: 
                return True, "测验结果已永久存档，请前往 AI 导师查收诊断报告！"
            return False, res_data.get("msg", "存档失败")
        return False, f"服务器状态异常 ({response.status_code})"
    except Exception as e:
        return False, f"网络异常: {str(e)}"


def _render_first_attempt_report(module_key: str, first_score: int | None, first_detail: list, quiz_data: list) -> None:
    """展示首提成绩 + 逐题详情（标准答案/学生答案/解析）"""
    st.error("🛑 本模块测验已提交，正式成绩已锁定。")
    if first_score is not None:
        st.metric("首次提交成绩（锁定）", f"{first_score} 分")
    else:
        st.metric("首次提交成绩（锁定）", "已提交")
    st.caption("说明：成绩已锁定")

    # 若后端/会话有首提病历，用病历展示；否则退回到 quiz_data 的解析展示
    if first_detail:
        st.subheader("📋 首次提交答题详情")
        for i, item in enumerate(first_detail, start=1):
            q = item.get("question", f"第{i}题")
            ua = item.get("userAnswer")
            ca = item.get("correctAnswer")
            ok = bool(item.get("isCorrect"))
            exp = item.get("explanation", "暂无解析")
            if ok:
                st.success(f"{i}. {q}\n\n你的答案：{ua}")
            else:
                st.error(f"{i}. {q}\n\n你的答案：{ua} ｜ 正确答案：{ca}")
            st.info(f"解析：{exp}")
            st.write("---")
    else:
        st.subheader("📋 题目解析")
        for i, item in enumerate(quiz_data, start=1):
            st.markdown(f"**{i}. {item['question']}**")
            st.info(f"标准答案：{item['answer']}")
            st.info(f"解析：{item.get('explanation', '暂无解析')}")
            st.write("---")

def render_quiz_component(module_key: str, title: str, description: str, quiz_data: list):
    """
    首提定档 + 可重做练习组件：
    - 第一次提交后：成绩锁定并用于 AI 分析；
    - 后续可重新答题仅做练习，不改首次成绩。
    """
    st.header(title)
    st.markdown(description)

    token = st.session_state.get("global_token")
    if not token:
        st.info("💡 当前为离线演示模式，登录后即可将成绩存档并开启 AI 诊断。")

    # 状态管理
    state_completed = f"{module_key}_completed"
    state_synced = f"{module_key}_synced"
    state_first_score = f"{module_key}_first_score"
    state_first_detail = f"{module_key}_first_detail"
    state_practice_mode = f"{module_key}_practice_mode"
    state_practice_score = f"{module_key}_practice_score"

    if state_completed not in st.session_state:
        st.session_state[state_completed] = False
    if state_synced not in st.session_state:
        st.session_state[state_synced] = False
    if state_first_score not in st.session_state:
        st.session_state[state_first_score] = None
    if state_first_detail not in st.session_state:
        st.session_state[state_first_detail] = []
    if state_practice_mode not in st.session_state:
        st.session_state[state_practice_mode] = False
    if state_practice_score not in st.session_state:
        st.session_state[state_practice_score] = None

    # 进页面先查一下云端是不是已经做过了
    if token and not st.session_state[state_synced]:
        is_done = check_is_completed(token, module_key)
        if is_done:
            st.session_state[state_completed] = True
            detail = get_score_detail(token, module_key)
            if detail:
                st.session_state[state_first_score] = detail.get("score")
                raw = detail.get("answersDetail")
                try:
                    st.session_state[state_first_detail] = json.loads(raw) if raw else []
                except Exception:
                    st.session_state[state_first_detail] = []
        st.session_state[state_synced] = True

    # ---------------- 已提交：展示锁定成绩 + 详情 + 可重做练习 ----------------
    if st.session_state[state_completed]:
        _render_first_attempt_report(
            module_key=module_key,
            first_score=st.session_state[state_first_score],
            first_detail=st.session_state[state_first_detail],
            quiz_data=quiz_data,
        )

        if st.button("📝 重新答题（练习模式，不影响首次成绩）", key=f"{module_key}_practice_btn"):
            st.session_state[state_practice_mode] = True
            st.session_state[state_practice_score] = None

        if st.session_state[state_practice_mode]:
            st.subheader("练习模式")
            with st.form(f"{module_key}_practice_form"):
                practice_answers = {}
                for i, item in enumerate(quiz_data):
                    st.markdown(f"**{i+1}. {item['question']}**")
                    practice_answers[i] = st.radio(
                        "请选择:",
                        item["options"],
                        key=f"{module_key}_practice_q_{i}",
                        index=None,
                        label_visibility="collapsed",
                    )
                    st.write("---")

                if st.form_submit_button("提交"):
                    if None in practice_answers.values():
                        st.warning("⚠️ 请先完成所有题目。")
                    else:
                        correct = sum(1 for i, item in enumerate(quiz_data) if practice_answers[i] == item["answer"])
                        st.session_state[state_practice_score] = int((correct / len(quiz_data)) * 100)

            if st.session_state[state_practice_score] is not None:
                st.success(f"本次练习得分：{st.session_state[state_practice_score]} 分（不影响首次成绩）")
        return

    # ---------------- 没做过：展示做题界面 ----------------
    st.caption("⚠️ **注意：本测验只有一次提交机会，请谨慎作答！**")
    
    with st.form(f"{module_key}_quiz_form"):
        answers = {}
        for i, item in enumerate(quiz_data):
            st.markdown(f"**{i+1}. {item['question']}**")
            answers[i] = st.radio("请选择:", item["options"], key=f"{module_key}_q_{i}", index=None, label_visibility="collapsed")
            st.write("---")
        
        if st.form_submit_button("🚀 提交试卷并锁定档案", type="primary"):
            if None in answers.values():
                st.warning("⚠️ 必须完成所有题目才能提交哦！")
                return
            
            # 1. 计算得分
            correct_count = sum(1 for i, item in enumerate(quiz_data) if answers[i] == item["answer"])
            final_score = int((correct_count / len(quiz_data)) * 100)

            # 2. 构建超详尽病历 (这就是以后喂给 Vue 端 AI 的神仙数据)
            detailed_answers_list = []
            for i, item in enumerate(quiz_data):
                is_correct = (answers[i] == item["answer"])
                detailed_answers_list.append({
                    "question": item["question"],      
                    "userAnswer": answers[i],          
                    "correctAnswer": item["answer"],   
                    "isCorrect": is_correct,           
                    "explanation": item.get("explanation", "暂无解析"),
                    "topic": module_key # 附带所属模块
                })

            # 3. 如果已登录，存入云端
            if token:
                with st.spinner('正在上传测验档案至云端...'):
                    success, msg = submit_detailed_score(token, module_key, final_score, detailed_answers_list)
                    if success:
                        st.success(f"✅ {msg}")
                        st.session_state[state_first_score] = final_score
                        st.session_state[state_first_detail] = detailed_answers_list
                        st.session_state[state_completed] = True
                        st.rerun()
                    else:
                        # 若后端提示重复提交，也进入锁定页并尝试拉取首次成绩详情
                        if "已提交过" in msg:
                            st.session_state[state_completed] = True
                            detail = get_score_detail(token, module_key)
                            if detail:
                                st.session_state[state_first_score] = detail.get("score")
                                raw = detail.get("answersDetail")
                                try:
                                    st.session_state[state_first_detail] = json.loads(raw) if raw else []
                                except Exception:
                                    st.session_state[state_first_detail] = []
                            st.rerun()
                        else:
                            st.error(f"❌ 存档失败: {msg}")
            else:
                # 离线模式处理
                st.session_state[state_first_score] = final_score
                st.session_state[state_first_detail] = detailed_answers_list
                st.session_state[state_completed] = True
                st.success(f"本地批改完成！首次成绩锁定：{final_score} 分。")
                st.rerun()