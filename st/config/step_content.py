from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Optional

from st.config.step_specs import get_step_spec


@dataclass(frozen=True)
class StepContent:
    starter_code: str
    reference_code: str


CONTENT: Dict[str, Dict[int, StepContent]] = {
    # -----------------------------
    # linear_regression (diabetes) — has blanks
    # -----------------------------
    "linear_regression": {
        1: StepContent(
            starter_code="""
# 加载糖尿病数据集
from sklearn.datasets import load_diabetes
import numpy as np

diabetes = load_diabetes()
X_raw = diabetes.data      # 特征数据（numpy数组，形状：(442, 10)）
y_raw = diabetes.target    # 目标变量（疾病进展，形状：(442,)）
feature_names = diabetes.feature_names    # 特征名称（10个生理指标）

# 观察数据（补充代码）
print("数据形状：", X_raw.________)    # 提示：.shape
print("前5名患者的生理特征：\\n", X_raw[________:________])    # 提示：切片取前5行（0:5）
print("特征名称：", feature_names)    # 如['age', 'sex', 'bmi'等]

# 计算统计量
print("目标变量（疾病进展）的均值：", np.mean(________))    # 提示：目标变量是y_raw
print("第一个特征（年龄）的标准差：", np.std(X_raw[:, ________]))    # 提示：[:,0]取第一列
            """.strip(),
            reference_code="""
# 加载糖尿病数据集
from sklearn.datasets import load_diabetes
import numpy as np

diabetes = load_diabetes()
X_raw = diabetes.data      # 特征数据（numpy数组，形状：(442, 10)）
y_raw = diabetes.target    # 目标变量（疾病进展，形状：(442,)）
feature_names = diabetes.feature_names    # 特征名称（10个生理指标）

# 观察数据
print("数据形状：", X_raw.shape)
print("前5名患者的生理特征：\\n", X_raw[0:5])
print("特征名称：", feature_names)

# 计算统计量
print("目标变量（疾病进展）的均值：", np.mean(y_raw))
print("第一个特征（年龄）的标准差：", np.std(X_raw[:, 0]))
            """.strip(),
        ),
        2: StepContent(
            starter_code="""
# 划分特征（X）和目标变量（y）
# 说明：本步骤直接复用步骤1中已得到的 X_raw 与 y_raw，不需要重新加载 diabetes 数据集
X = X_raw    # 特征（10个生理指标）
y = y_raw    # 目标变量（疾病进展）

# 查看形状
print("X形状：", X.shape)    # (442, 10)
print("y形状：", y.shape)    # (442,)
            """.strip(),
            reference_code="""
# 划分特征（X）和目标变量（y）
# 说明：本步骤直接复用步骤1中已得到的 X_raw 与 y_raw，不需要重新加载 diabetes 数据集
X = X_raw
y = y_raw

# 查看形状
print("X形状：", X.shape)
print("y形状：", y.shape)
            """.strip(),
        ),
        3: StepContent(
            starter_code="""
# 划分训练集和测试集
from sklearn.model_selection import train_test_split

# 补充参数（测试集数据占20%，随机数种子为42）
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=________, random_state=________)

# 特征标准化
from sklearn.preprocessing import StandardScaler
scaler = StandardScaler()

X_train_scaled = scaler.________(X_train)   # 提示：训练集用fit_transform
X_test_scaled = scaler.________(X_test)    # 提示：测试集用transform
scaler_mean = scaler.mean_
scaler_scale = scaler.scale_
print("标准化后训练集均值：", scaler_mean)
print("标准化后训练集缩放比例：", scaler_scale)
print("训练集特征形状：", X_train_scaled.shape)
print("测试集特征形状：", X_test_scaled.shape)
            """.strip(),
            reference_code="""
# 划分训练集和测试集
from sklearn.model_selection import train_test_split

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 特征标准化
from sklearn.preprocessing import StandardScaler
scaler = StandardScaler()

X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)
scaler_mean = scaler.mean_
scaler_scale = scaler.scale_
print("标准化后训练集均值：", scaler_mean)
print("标准化后训练集缩放比例：", scaler_scale)
print("训练集特征形状：", X_train_scaled.shape)
print("测试集特征形状：", X_test_scaled.shape)
            """.strip(),
        ),
        4: StepContent(
            starter_code="""
# 导入线性回归模型
from sklearn.linear_model import LinearRegression

# 实例化模型
model = LinearRegression()

# 查看模型参数
print("模型参数：", model._______)
            """.strip(),
            reference_code="""
# 导入线性回归模型
from sklearn.linear_model import LinearRegression

# 实例化模型
model = LinearRegression()

# 查看模型参数
print("模型参数：", model.get_params())
            """.strip(),
        ),
        5: StepContent(
            starter_code="""
# 用训练集训练模型
model.fit(________, ________)    # 提示：参数为X_train_scaled,  y_train

# 查看模型参数（关注特征系数与疾病进展的关系）
print("特征系数（权重）：", model.coef_)
print("截距：", model.intercept_)

# 用测试集预测
y_pred = model.predict(________)    # 提示：参数为X_test_scaled

# 查看前5个预测结果
print("前5个预测值（疾病进展）：", y_pred[:5])
print("前5个实际值：", y_test[:5])
            """.strip(),
            reference_code="""
# 用训练集训练模型
model.fit(X_train_scaled, y_train)

# 查看模型参数（关注特征系数与疾病进展的关系）
print("特征系数（权重）：", model.coef_)
print("截距：", model.intercept_)

# 用测试集预测
y_pred = model.predict(X_test_scaled)

# 查看前5个预测结果
print("前5个预测值（疾病进展）：", y_pred[:5])
print("前5个实际值：", y_test[:5])
            """.strip(),
        ),
        6: StepContent(
            starter_code="""
# 导入评估指标
from sklearn.metrics import mean_squared_error, r2_score

# 计算评估指标（补充参数）
mse = mean_squared_error(________, ________)    # 提示：实际值y_test，预测值y_pred
r2 = r2_score(________, ________)

print(f"均方误差（MSE）：{mse:.2f}")
print(f"决定系数（R²）：{r2:.2f}")
            """.strip(),
            reference_code="""
# 导入评估指标
from sklearn.metrics import mean_squared_error, r2_score

# 计算评估指标
mse = mean_squared_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print(f"均方误差（MSE）：{mse:.2f}")
print(f"决定系数（R²）：{r2:.2f}")
            """.strip(),
        ),
    }
}


def get_starter_code(module_id: str, step_num: int, default: str) -> str:
    step = CONTENT.get(module_id, {}).get(step_num)
    if step:
        return step.starter_code

    spec = get_step_spec(module_id, step_num)
    if not spec:
        return default

    # Auto-generate fill-in starter by blanking required substrings.
    # 约定：凡是在 step_specs 里配置了 rules 的 step，一律按规则挖空。
    starter = default
    for rule in spec.rules:
        for s in rule.substrings:
            if not s:
                continue
            # 把匹配到的关键写法全部替换为________，形成填空题
            if s in starter:
                starter = starter.replace(s, "________")
    return starter


def get_reference_code(module_id: str, step_num: int, default: str) -> str:
    step = CONTENT.get(module_id, {}).get(step_num)
    return step.reference_code if step else default

