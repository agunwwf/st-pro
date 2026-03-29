# linear_regression_demo.py
# C:\Users\孙冰\Desktop\AI助教\linear_regression
# streamlit run linear_regression_demo.py

# linear_regression_demo.py
import streamlit as st
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import PolynomialFeatures
from sklearn.metrics import mean_squared_error, r2_score
import time

from utils.api_deepseek import client, ask_ai_assistant
from utils.chat_interface import display_chat_interface
from utils.learning_progress import render_demo_teaching_complete
import linear_regression_step_by_step
from utils.quiz_helper import render_quiz_component

# # 设置页面
try:
    st.set_page_config(page_title="线性回归交互式学习平台", layout="wide")
except:
    pass
st.title("📚 线性回归交互式学习平台")

# 设置中文字体支持
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False


# 数据生成函数
def generate_data(data_type, n_samples, noise_level):
    np.random.seed(42)
    X = np.linspace(0, 10, n_samples)

    if data_type == "线性关系":
        y = 2 * X + 1 + np.random.normal(0, noise_level, n_samples)
    elif data_type == "非线性关系":
        y = 0.5 * X**2 + np.random.normal(0, noise_level, n_samples)
    elif data_type == "带有异常值":
        y = 2 * X + 1 + np.random.normal(0, noise_level, n_samples)
        outlier_indices = np.random.choice(n_samples, size=5, replace=False)
        y[outlier_indices] += 10 * noise_level
    elif data_type == "不同噪声水平":
        y = 2 * X + 1 + np.random.normal(0, noise_level*2, n_samples)

    return X, y

# 绘制数据函数
def plot_data(X, y, data_type):
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.scatter(X, y, alpha=0.7)
    ax.set_xlabel('X (自变量)')
    ax.set_ylabel('y (因变量)')
    ax.set_title(f'数据分布: {data_type}')
    ax.grid(True, linestyle='--', alpha=0.7)
    return fig

# 梯度下降模拟函数
def simulate_gradient_descent(X, y, learning_rate, n_iterations):
    slope = 0
    intercept = 0
    n = len(X)

    slopes = [slope]
    intercepts = [intercept]
    losses = [mean_squared_error(y, slope * X + intercept)]

    for i in range(n_iterations):
        y_pred = slope * X + intercept
        slope_gradient = (-2/n) * np.sum(X * (y - y_pred))
        intercept_gradient = (-2/n) * np.sum(y - y_pred)

        slope = slope - learning_rate * slope_gradient
        intercept = intercept - learning_rate * intercept_gradient

        slopes.append(slope)
        intercepts.append(intercept)
        losses.append(mean_squared_error(y, slope * X + intercept))

    return losses, slopes, intercepts

# 绘制梯度下降步骤函数
def plot_gradient_descent_step(X, y, slope, intercept, loss, iteration, losses):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))

    ax1.scatter(X, y, alpha=0.7, label='数据点')
    y_pred = slope * X + intercept
    ax1.plot(X, y_pred, color='red', linewidth=2, label='当前拟合')
    ax1.set_xlabel('X')
    ax1.set_ylabel('y')
    ax1.set_title(f'迭代 {iteration}: 斜率={slope:.2f}, 截距={intercept:.2f}')
    ax1.legend()
    ax1.grid(True, linestyle='--', alpha=0.7)

    ax2.plot(range(iteration+1), losses[:iteration+1], color='blue')
    ax2.set_xlabel('迭代次数')
    ax2.set_ylabel('损失 (MSE)')
    ax2.set_title(f'当前损失: {loss:.2f}')
    ax2.grid(True, linestyle='--', alpha=0.7)

    plt.tight_layout()
    return fig

# 训练模型函数
def train_model(X, y, model_type):
    X_reshaped = X.reshape(-1, 1)

    if model_type == "线性回归":
        model = LinearRegression()
        model.fit(X_reshaped, y)
    elif model_type == "多项式回归(2次)":
        poly = PolynomialFeatures(degree=2)
        X_poly = poly.fit_transform(X_reshaped)
        model = LinearRegression()
        model.fit(X_poly, y)
    elif model_type == "多项式回归(3次)":
        poly = PolynomialFeatures(degree=3)
        X_poly = poly.fit_transform(X_reshaped)
        model = LinearRegression()
        model.fit(X_poly, y)

    return model

# 绘制模型比较图函数
def plot_model_comparison(X, y, y_pred, model_type):
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.scatter(X, y, alpha=0.7, label='数据点')

    sorted_indices = np.argsort(X)
    X_sorted = X[sorted_indices]
    y_pred_sorted = y_pred[sorted_indices]

    ax.plot(X_sorted, y_pred_sorted, color='red', linewidth=2, label='拟合曲线')
    ax.set_xlabel('X')
    ax.set_ylabel('y')
    ax.set_title(f'模型拟合: {model_type}')
    ax.legend()
    ax.grid(True, linestyle='--', alpha=0.7)
    return fig

# 加载示例数据集函数
def load_example_dataset(example_name):
    if example_name == "房价预测":
        np.random.seed(42)
        sizes = np.random.randint(50, 200, 100)
        prices = 5000 * sizes + 100000 + np.random.normal(0, 20000, 100)
        description = "模拟房价数据: 房屋面积(平方米) vs 价格(元)"
        return sizes, prices, description

    elif example_name == "学生成绩预测":
        np.random.seed(42)
        study_hours = np.random.uniform(1, 10, 100)
        grades = 5 * study_hours + 50 + np.random.normal(0, 10, 100)
        description = "模拟学生数据: 学习时间(小时) vs 考试成绩(分)"
        return study_hours, grades, description

    elif example_name == "销售额预测":
        np.random.seed(42)
        ad_budget = np.random.uniform(10, 100, 100)
        sales = 50 * ad_budget + 200 + np.random.normal(0, 100, 100)
        description = "模拟销售数据: 广告预算(千元) vs 销售额(万元)"
        return ad_budget, sales, description

    return None, None, ""

# 数据生成与探索模块
def data_generation_section():
    st.header("📈 数据生成与探索")

    col1, col2 = st.columns(2)

    with col1:
        data_type = st.selectbox("选择数据分布类型",
                               ["线性关系", "非线性关系", "带有异常值", "不同噪声水平"])
        n_samples = st.slider("样本数量", 50, 500, 100)
        noise_level = st.slider("噪声水平", 0.1, 5.0, 1.0, 0.1)

        X, y = generate_data(data_type, n_samples, noise_level)

        st.write(f"数据统计:")
        st.write(f"- X均值$\\bar{{x}}$: {np.mean(X):.2f}, X标准差$s_x$: {np.std(X):.2f}")
        st.write(f"- y均值$\\bar{{y}}$: {np.mean(y):.2f}, y标准差$s_y$: {np.std(y):.2f}")
        st.write(f"- X和y的相关系数R²: {np.corrcoef(X, y)[0, 1]:.6f}")

    with col2:
        fig = plot_data(X, y, data_type)
        st.pyplot(fig)
        st.latex(r"R^2 = 1 - \frac{\sum_{i=1}^{n} (y_i - \hat{y}_i)^2}{\sum_{i=1}^{n} (y_i - \bar{y})^2}")
        st.write("其中，$y_i$是第i个样本的实际值,$\\hat{y}_i$是第i个样本的预测值,$\\bar{y}$是y的均值,$\\hat{y} = f(x)$是模型预测值")
        st.latex(r"s_x = \sqrt{\frac{\sum_{i=1}^{n} (x_i - \bar{x})^2}{n}}")

    st.info("""
    **数据探索要点:**
    - 观察数据的分布模式和关系
    - 注意异常值对整体模式的影响
    - 噪声水平影响数据的分散程度
    - 相关系数衡量X和y之间的线性关系强度
    """)

    return f"数据生成模块: 创建了{data_type}数据，样本数={n_samples}，噪声水平={noise_level}"

# 手动拟合体验模块
def manual_fitting_section():
    st.header("✋ 手动拟合体验")

    X, y = generate_data("线性关系", 100, 1.0)

    col1, col2 = st.columns(2)

    with col1:
        slope = st.slider("斜率", -5.0, 5.0, 1.0, 0.1)
        intercept = st.slider("截距", -10.0, 10.0, 0.0, 0.5)

        y_pred = slope * X + intercept
        mse = mean_squared_error(y, y_pred)
        r2 = r2_score(y, y_pred)

        st.metric("当前均方误差(MSE)", f"{mse:.2f}")
        st.metric("当前决定系数(R²)", f"{r2:.2f}")

        model = LinearRegression()
        model.fit(X.reshape(-1, 1), y)
        st.write(f"最佳拟合参考: 斜率={model.coef_[0]:.2f}, 截距={model.intercept_:.2f}")

    with col2:
        fig, ax = plt.subplots(figsize=(10, 6))
        ax.scatter(X, y, alpha=0.7, label='数据点')
        ax.plot(X, y_pred, color='red', linewidth=2, label='手动拟合')

        y_best = model.predict(X.reshape(-1, 1))
        ax.plot(X, y_best, color='green', linestyle='--', linewidth=2, label='最佳拟合')

        ax.set_title("手动拟合 vs 最佳拟合")
        ax.legend()
        ax.grid(True, linestyle='--', alpha=0.7)
        st.pyplot(fig)

        st.latex("MSE = \\frac{1}{n}\\sum_{i=1}^{n} (y_i - \\hat{y}_i)^2")
        st.write("- 为了方便梯度的求导，我们通常将损失函数乘以1/2，即MSE = $\\frac{1}{2n}\\sum_{i=1}^{n} (y_i - \\hat{y}_i)^2$")
        st.write("- MSE越小，拟合效果越好")
        st.write("- R²越接近1，拟合效果越好")

    if st.button("挑战: 找到最佳拟合参数"):
        st.info("尝试调整斜率和截距，使均方误差最小化，R²分数最大化!")

        with st.expander("需要提示?"):
            st.write("""
            1. 观察数据点的整体趋势
            2. 先调整斜率使直线方向与数据趋势一致
            3. 再调整截距使直线通过数据点的中心区域
            4. 微调两个参数使误差最小化
            """)

    return f"手动拟合模块: 当前斜率={slope:.2f}, 截距={intercept:.2f}, MSE={mse:.2f}, R²={r2:.2f}"

# 梯度下降可视化模块
def gradient_descent_section():
    st.header("📉 梯度下降过程可视化")

    X, y = generate_data("线性关系", 50, 1.0)

    col1, col2 = st.columns(2)

    with col1:
        learning_rate = st.slider("学习率", 0.001, 0.1, 0.01, 0.001,format="%.3f")
        n_iterations = st.slider("迭代次数", 10, 1000, 100)

        if st.button("开始梯度下降演示"):
            losses, slopes, intercepts = simulate_gradient_descent(
                X, y, learning_rate, n_iterations
            )

            placeholder = st.empty()
            for i in range(0, n_iterations + 1, max(1, n_iterations // 20)):
                with placeholder.container():
                    fig = plot_gradient_descent_step(X, y, slopes[i], intercepts[i], losses[i], i, losses)
                    st.pyplot(fig)
                    time.sleep(0.2)

            st.success(f"梯度下降完成! 最终参数: 斜率={slopes[-1]:.2f}, 截距={intercepts[-1]:.2f}")

    with col2:
        st.markdown("""
        **什么是损失函数？**

        损失函数是用来衡量模型预测值与实际值之间的差异的函数。
        损失函数越小，模型的拟合效果越好。
        例如线性回归的损失函数为均方误差(MSE)即：J = MSE = $\\frac{1}{2n}\\sum_{i=1}^{n} (y_i - \\hat{y}_i)^2$。
        我们在将来的算法学习中会学习到更多的损失函数，例如交叉熵损失函数、均方根误差(RMSE)等。

        **梯度下降原理:**

        梯度下降是一种优化算法，用于找到使损失函数最小化的参数值。

        1. 初始化参数(斜率和截距),我们可以选择零初始化即:($\\omega$ = 0, b = 0)
        2. 计算损失函数的梯度(导数)
        $$
            \\frac{\\partial J}{\\partial \\omega} = \\frac{1}{n}\\sum_{i=1}^{n} (y_i - \\hat{y}_i)x_i
        $$
        $$
            \\frac{\\partial J}{\\partial b} = \\frac{1}{n}\\sum_{i=1}^{n} (y_i - \\hat{y}_i)
        $$
        3. 沿梯度反方向更新参数
        4. 重复直到收敛

        **参数更新公式:**
        - 斜率 = 斜率 - 学习率 × ∂损失/∂斜率
        $$
            \\omega = \\omega - \\alpha \\frac{\\partial J}{\\partial \\omega}
        $$
        - 截距 = 截距 - 学习率 × ∂损失/∂截距
        $$
        b = b - \\alpha \\frac{\\partial J}{\\partial b}
        $$

        **学习率的影响:**
        - 太小: 收敛慢
        - 太大: 可能无法收敛，甚至发散
        """)

    return f"梯度下降模块: 学习率={learning_rate}, 迭代次数={n_iterations}"

# 模型比较模块
def model_comparison_section():
    st.header("🔍 模型比较")

    X, y = generate_data("线性关系", 100, 3.0)

    model_type = st.radio("选择模型类型",
                         ["线性回归", "多项式回归(2次)", "多项式回归(3次)"],
                         horizontal=True)

    model = train_model(X, y, model_type)

    X_reshaped = X.reshape(-1, 1)
    if model_type != "线性回归":
        degree = 2 if "2次" in model_type else 3
        poly = PolynomialFeatures(degree=degree)
        X_reshaped = poly.fit_transform(X_reshaped)

    y_pred = model.predict(X_reshaped)

    fig = plot_model_comparison(X, y, y_pred, model_type)
    st.pyplot(fig)

    mse = mean_squared_error(y, y_pred)
    r2 = r2_score(y, y_pred)

    col1, col2 = st.columns(2)
    with col1:
        st.metric("均方误差(MSE)", f"{mse:.2f}")
    with col2:
        st.metric("决定系数(R²)", f"{r2:.2f}")

    st.info("""
    **模型复杂度与过拟合:**
    - 线性回归: 简单模型，可能欠拟合
    - 多项式回归(2次): 中等复杂度，可能适合
    - 多项式回归(3次): 高复杂度，可能过拟合

    **过拟合**指模型过于复杂，过度适应训练数据中的噪声，导致在新数据上表现不佳。
    """)

    return f"模型比较模块: 当前模型={model_type}, MSE={mse:.2f}, R²={r2:.2f}"

# 残差分析模块
def residual_analysis_section():
    st.header("📊 残差分析")

    X, y = generate_data("线性关系", 100, 1.0)

    model = LinearRegression()
    model.fit(X.reshape(-1, 1), y)
    y_pred = model.predict(X.reshape(-1, 1))
    residuals = y - y_pred

    col1, col2 = st.columns(2)

    with col1:
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 8))

        ax1.scatter(y_pred, residuals, alpha=0.7)
        ax1.axhline(y=0, color='r', linestyle='--')
        ax1.set_xlabel("预测值")
        ax1.set_ylabel("残差")
        ax1.set_title("残差 vs 预测值")
        ax1.grid(True, linestyle='--', alpha=0.7)

        ax2.hist(residuals, bins=20, alpha=0.7)
        ax2.set_xlabel("残差")
        ax2.set_ylabel("频数")
        ax2.set_title("残差分布")
        ax2.grid(True, linestyle='--', alpha=0.7)

        plt.tight_layout()
        st.pyplot(fig)

    with col2:
        st.write("残差统计分析:")
        st.write(f"- 均值: {np.mean(residuals):.4f} (应接近0)")
        st.write(f"- 标准差: {np.std(residuals):.4f}")
        st.write(f"- 最小值: {np.min(residuals):.4f}")
        st.write(f"- 最大值: {np.max(residuals):.4f}")

        from scipy import stats
        _, p_value = stats.normaltest(residuals)
        st.write(f"- 正态性检验p值: {p_value:.4f}")
        if p_value > 0.05:
            st.write("  ✅ 残差近似正态分布")
        else:
            st.write("  ⚠️ 残差可能不是正态分布")

        st.info("""
        **残差分析帮助检查模型假设:**

        1. 残差应该随机分布在0附近
        2. 不应该有明显的模式或趋势
        3. 理想情况下应该接近正态分布

        如果残差图显示模式(如曲线形状)，可能意味着线性模型不合适。
        """)

    return f"残差分析模块: 残差均值={np.mean(residuals):.4f}, 标准差={np.std(residuals):.4f}"

# 概念测验模块

def quiz_section():
    # 1. 结构化的高质量题库（带有 A/B/C/D 前缀，并丰富了干扰选项）
    LINEAR_QUIZ_DATA = [
        {
            "question": "1. 线性回归的核心优化目标是什么？",
            "options": [
                "A. 最小化预测值与实际值的绝对差",
                "B. 最小化预测值与实际值的平方差",
                "C. 最大化预测值与实际值的相关系数",
                "D. 最小化数据特征之间的相关性"
            ],
            "answer": "B. 最小化预测值与实际值的平方差",
            "explanation": "线性回归通常使用最小二乘法（OLS），其核心目标是最小化所有数据点的残差平方和（SSE），从而找到一条最佳拟合直线。"
        },
        {
            "question": "2. 在机器学习中，什么是“梯度下降” (Gradient Descent)？",
            "options": [
                "A. 一种用于特征降维的数据处理技术",
                "B. 一种优化算法，用于迭代更新参数以最小化损失函数",
                "C. 一种评估模型预测准确率的指标",
                "D. 一种用来生成新特征的数学变换"
            ],
            "answer": "B. 一种优化算法，用于迭代更新参数以最小化损失函数",
            "explanation": "梯度下降是一种一阶最优化算法，它通过计算损失函数关于参数的梯度（导数），沿着梯度的反方向不断迈步（学习率），最终找到损失函数的极小值点。"
        },
        {
            "question": "3. 模型发生“过拟合” (Overfitting) 是什么意思？",
            "options": [
                "A. 模型过于简单，无法捕捉数据中的潜在模式",
                "B. 模型在训练集和测试集上都表现出极高的准确率",
                "C. 模型过于复杂，过度学习了训练数据中的特征甚至噪声",
                "D. 模型的参数数量太少导致训练时间过长"
            ],
            "answer": "C. 模型过于复杂，过度学习了训练数据中的特征甚至噪声",
            "explanation": "过拟合意味着模型“死记硬背”了训练数据（包括噪声和异常值），导致其泛化能力极差，在新数据（测试集）上表现糟糕。"
        },
        {
            "question": "4. 决定系数 (R² score) 的理论取值范围是什么？",
            "options": [
                "A. 0 到 1 之间",
                "B. -1 到 1 之间",
                "C. -∞ (负无穷) 到 1 之间",
                "D. -∞ 到 +∞ 之间"
            ],
            "answer": "C. -∞ (负无穷) 到 1 之间",
            "explanation": "R² 最大值为 1，表示模型完美预测数据。当模型性能不如直接使用均值预测时，R² 为负数。若模型极其糟糕，R² 理论上可以趋于负无穷。"
        },
        {
            "question": "5. 残差分析 (Residual Analysis) 在线性回归中的主要作用是什么？",
            "options": [
                "A. 检查模型误差是否符合零均值、同方差及正态分布等假设",
                "B. 增加模型的复杂度以提高训练集准确率",
                "C. 减少梯度下降算法收敛所需的计算时间",
                "D. 自动筛选并剔除不重要的特征向量"
            ],
            "answer": "A. 检查模型误差是否符合零均值、同方差及正态分布等假设",
            "explanation": "线性回归的成立依赖于多个假设（如残差应随机分布且无自相关性）。残差分析通过绘制残差图，帮助我们验证这些假设是否被破坏，从而决定是否需要对数据进行非线性转换。"
        }
    ]

    # 2. 调用通用组件渲染

    render_quiz_component(
        module_key="linear",
        title="🎯 线性回归概念与原理测验",
        description="本测验旨在检验你对线性回归原理、优化算法（梯度下降）以及模型评估指标的掌握情况。请答题，成绩及错题分析将同步至云端档案。",
        quiz_data=LINEAR_QUIZ_DATA
    )

    return "概念测验模块: 用户正在进行 线性回归 综合选择题测验"

# 实际应用案例模块
def real_world_example_section():
    st.header("🌍 实际应用案例")

    example = st.selectbox(
        "选择实际应用案例:",
        ["房价预测", "学生成绩预测", "销售额预测", "上传自己的数据"]
    )

    if example == "上传自己的数据":
        uploaded_file = st.file_uploader("上传CSV文件", type="csv")
        if uploaded_file:
            data = pd.read_csv(uploaded_file)
            st.write("数据预览:", data.head())

            col1, col2 = st.columns(2)
            with col1:
                x_col = st.selectbox("选择自变量(X)", data.columns)
            with col2:
                y_col = st.selectbox("选择因变量(y)", data.columns)

            X = data[x_col].values
            y = data[y_col].values

            analyze_custom_data(X, y, x_col, y_col)
            return f"实际应用模块: 上传自定义数据, X={x_col}, y={y_col}"
    else:
        X, y, description = load_example_dataset(example)
        st.write(description)

        analyze_custom_data(X, y, "X", "y")
        return f"实际应用模块: 使用{example}数据集"

# 分析自定义数据函数
def analyze_custom_data(X, y, x_name, y_name):
    if len(X) != len(y):
        st.error("X和y的长度不匹配!")
        return

    if len(X) < 2:
        st.error("数据点太少!")
        return

    fig, ax = plt.subplots(figsize=(10, 6))
    ax.scatter(X, y, alpha=0.7)
    ax.set_xlabel(x_name)
    ax.set_ylabel(y_name)
    ax.set_title(f"{y_name} vs {x_name}")
    ax.grid(True, linestyle='--', alpha=0.7)
    st.pyplot(fig)

    X_reshaped = X.reshape(-1, 1)
    model = LinearRegression()
    model.fit(X_reshaped, y)
    y_pred = model.predict(X_reshaped)

    col1, col2 = st.columns(2)
    with col1:
        st.metric("斜率(系数)", f"{model.coef_[0]:.4f}")
        st.metric("截距", f"{model.intercept_:.4f}")
    with col2:
        mse = mean_squared_error(y, y_pred)
        r2 = r2_score(y, y_pred)
        st.metric("均方误差(MSE)", f"{mse:.4f}")
        st.metric("决定系数(R²)", f"{r2:.4f}")

    fig2, ax2 = plt.subplots(figsize=(10, 6))
    ax2.scatter(X, y, alpha=0.7, label='数据点')

    sorted_indices = np.argsort(X)
    X_sorted = X[sorted_indices]
    y_pred_sorted = y_pred[sorted_indices]

    ax2.plot(X_sorted, y_pred_sorted, color='red', linewidth=2, label='拟合线')
    ax2.set_xlabel(x_name)
    ax2.set_ylabel(y_name)
    ax2.set_title(f"线性回归拟合: {y_name} = {model.coef_[0]:.2f}×{x_name} + {model.intercept_:.2f}")
    ax2.legend()
    ax2.grid(True, linestyle='--', alpha=0.7)
    st.pyplot(fig2)

    st.info(f"""
    **结果解释:**

    线性回归模型为: {y_name} = {model.coef_[0]:.2f}×{x_name} + {model.intercept_:.2f}

    - 斜率({model.coef_[0]:.2f}): {x_name}每增加1个单位，{y_name}平均变化{model.coef_[0]:.2f}个单位
    - 截距({model.intercept_:.2f}): 当{x_name}=0时，{y_name}的预测值
    - R²({r2:.2f}): 模型解释了{y_name}变异性的{r2*100:.1f}%
    """)

# 主程序
def main():
    # 初始化会话状态（新增：确保section变量存在）
    if 'section' not in st.session_state:
        st.session_state.section = "数据生成与探索"

    st.sidebar.title("导航菜单")
    # 用会话状态的section作为当前选中项
    section = st.sidebar.radio("选择学习模块", [
        "数据生成与探索",
        "手动拟合体验",
        "梯度下降可视化",
        "模型比较",
        "残差分析",
        "概念测验",
        "实际应用案例",
        "编程实例（糖尿病数据集）"  # 保留新增选项
    ])

    # 更新会话状态的section
    st.session_state.section = section

    if section != "编程实例（糖尿病数据集）":
        render_demo_teaching_complete("linear")

    context = ""
    if section == "数据生成与探索":
        context = data_generation_section()
    elif section == "手动拟合体验":
        context = manual_fitting_section()
    elif section == "梯度下降可视化":
        context = gradient_descent_section()
    elif section == "模型比较":
        context = model_comparison_section()
    elif section == "残差分析":
        context = residual_analysis_section()
    elif section == "概念测验":
        context = quiz_section()
    elif section == "实际应用案例":
        context = real_world_example_section()
    elif section == "编程实例（糖尿病数据集）":
        # 初始化step变量（如果不存在）
        if 'step' not in st.session_state:
            st.session_state.step = 0
        linear_regression_step_by_step.main()
        context = "编程实例模块: 糖尿病数据集线性回归分步练习"

    # 只有在非编程实例页面时显示聊天界面
    if section != "编程实例（糖尿病数据集）":
        button_list = ["什么是过拟合?", "梯度下降原理", "R²的意义", "残差分析"]
        question_list = ["什么是过拟合?如何识别和避免?", "请用简单易懂的方式解释梯度下降的原理", "决定系数R²的含义是什么?如何解释它的值?", "为什么要进行残差分析?它能告诉我们什么?"]
        display_chat_interface(context, button_list, question_list)

    # 侧边栏信息（保持不变）
    st.sidebar.markdown("---")
    st.sidebar.info("""
    **线性回归交互式学习平台**

    设计用于机器学习教学，帮助学生理解:
    - 线性回归基本原理
    - 模型训练与评估
    - 梯度下降优化
    - 模型诊断与改进
    """)


if __name__ == "__main__":
    main()
