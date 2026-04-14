"""
Centralized step requirements & error messages.

This is the single source of truth for:
- what we consider "pass" for each (module_id, step)
- the specific beginner-friendly error messages shown in UI

Note:
- These checks are intentionally lightweight (substring based) to match
  the current project style and keep student freedom.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Optional


@dataclass(frozen=True)
class MustContainRule:
    substrings: List[str]
    message: str
    remove_spaces: bool = False


@dataclass(frozen=True)
class StepSpec:
    rules: List[MustContainRule]
    success_message: str = "✅ 通过！"


SPECS: Dict[str, Dict[int, StepSpec]] = {
    # -----------------------------
    # Linear Regression (diabetes)
    # -----------------------------
    "linear_regression": {
        1: StepSpec(
            success_message="✅ 步骤1通过！成功观察数据",
            rules=[
                MustContainRule(["X_raw.shape"], "❌ 数据形状应使用X_raw.shape（提示：.shape）", remove_spaces=True),
                MustContainRule(["X_raw[0:5]"], "❌ 前5行特征应使用X_raw[0:5]（提示：切片0:5）", remove_spaces=True),
                MustContainRule(["np.mean(y_raw)"], "❌ 目标变量均值应使用np.mean(y_raw)（提示：参数是y_raw）", remove_spaces=True),
                MustContainRule(["X_raw[:,0]"], "❌ 第一个特征标准差应使用X_raw[:,0]（提示：[:,0]取第一列）", remove_spaces=True),
            ],
        ),
        2: StepSpec(
            success_message="✅ 步骤2通过！特征与目标划分正确",
            rules=[
                MustContainRule(
                    ["X = X_raw"],
                    "❌ 本步骤请基于步骤1结果，使用 `X = X_raw` 完成特征赋值（不要重新加载数据集）",
                ),
                MustContainRule(
                    ["y = y_raw"],
                    "❌ 本步骤请基于步骤1结果，使用 `y = y_raw` 完成目标变量赋值（不要改用 `diabetes.target`）",
                ),
                MustContainRule(
                    ["X.shape"],
                    "❌ 建议输出 `X.shape` 以确认特征矩阵维度",
                ),
                MustContainRule(
                    ["y.shape"],
                    "❌ 建议输出 `y.shape` 以确认目标变量维度",
                ),
            ],
        ),
        3: StepSpec(
            success_message="✅ 步骤3通过！数据预处理完成",
            rules=[
                MustContainRule(["train_test_split"], "❌ 请用train_test_split划分训练集和测试集"),
                MustContainRule(["test_size=0.2"], "❌ train_test_split的test_size参数应为0.2"),
                MustContainRule(["random_state=42"], "❌ train_test_split的random_state参数应为42"),
                MustContainRule(["scaler.fit_transform(X_train)"], "❌ 训练集标准化应使用scaler.fit_transform(X_train)"),
                MustContainRule(["scaler.transform(X_test)"], "❌ 测试集标准化应使用scaler.transform(X_test)"),
            ],
        ),
        4: StepSpec(
            success_message="✅ 步骤4通过！模型构建正确",
            rules=[
                MustContainRule(["LinearRegression()"], "❌ 请实例化LinearRegression模型（如model = LinearRegression()）"),
                MustContainRule(["model = LinearRegression()"], "❌ 模型实例化应为model = LinearRegression()"),
            ],
        ),
        5: StepSpec(
            success_message="✅ 步骤5通过！训练和预测完成",
            rules=[
                MustContainRule(["model.fit(X_train_scaled, y_train)"], "❌ 训练模型应为model.fit(X_train_scaled, y_train)"),
                MustContainRule(["model.predict(X_test_scaled)"], "❌ 预测应为model.predict(X_test_scaled)"),
            ],
        ),
        6: StepSpec(
            success_message="✅ 步骤6通过！模型评估完成",
            rules=[
                MustContainRule(["mean_squared_error(y_test, y_pred)"], "❌ MSE计算应为mean_squared_error(y_test, y_pred)"),
                MustContainRule(["r2_score(y_test, y_pred)"], "❌ R²计算应为r2_score(y_test, y_pred)"),
            ],
        ),
    },
    # -----------------------------
    # Logistic Regression (cancer)
    # -----------------------------
    "logistic_regression": {
        1: StepSpec(
            success_message="✅ 步骤1通过！",
            rules=[
                MustContainRule(["load_breast_cancer"], "❌ 请加载乳腺癌数据集（使用load_breast_cancer）"),
                MustContainRule(["X_raw.shape"], "❌ 数据形状应使用X_raw.shape（提示：.shape）"),
                MustContainRule(["X_raw[:3]"], "❌ 前3行特征应使用X_raw[:3]（提示：切片[:3]）"),
                MustContainRule(["np.mean(X_raw, axis=0)"], "❌ 特征均值应使用np.mean(X_raw, axis=0)（提示：计算列均值）"),
                MustContainRule(["np.var(X_raw, axis=0)"], "❌ 特征方差应使用np.var(X_raw, axis=0)（提示：计算列方差）"),
            ],
        ),
        2: StepSpec(
            success_message="✅ 步骤2通过！",
            rules=[
                MustContainRule(["X = X_raw"], "❌ 请用X = X_raw和y = y_raw划分特征与目标"),
                MustContainRule(["y = y_raw"], "❌ 请用X = X_raw和y = y_raw划分特征与目标"),
            ],
        ),
        3: StepSpec(
            success_message="✅ 步骤3通过！",
            rules=[
                MustContainRule(["train_test_split"], "❌ 请用train_test_split划分训练集和测试集"),
                MustContainRule(["test_size=0."], "❌ 请设置参数test_size 和 random_state"),
                MustContainRule(["random_state="], "❌ 请设置参数test_size 和 random_state"),
                MustContainRule(["scaler.fit_transform(X_train)"], "❌ 训练集用fit_transform，测试集用transform"),
                MustContainRule(["scaler.transform(X_test)"], "❌ 训练集用fit_transform，测试集用transform"),
            ],
        ),
        4: StepSpec(
            success_message="✅ 步骤4通过！",
            rules=[
                MustContainRule(["LogisticRegression()"], "❌ 请实例化逻辑回归模型（model = LogisticRegression()）"),
                MustContainRule(["model = LogisticRegression()"], "❌ 请实例化逻辑回归模型（model = LogisticRegression()）"),
            ],
        ),
        5: StepSpec(
            success_message="✅ 步骤5通过！",
            rules=[
                MustContainRule(["model.fit(X_train_scaled, y_train)"], "❌ 训练模型应为model.fit(X_train_scaled, y_train)"),
                MustContainRule(["model.predict(X_test_scaled)"], "❌ 预测应为model.predict(X_test_scaled)"),
            ],
        ),
        6: StepSpec(
            success_message="✅ 步骤6通过！",
            rules=[
                MustContainRule(["accuracy_score"], "❌ 请用accuracy_score计算准确率"),
                MustContainRule(["precision_score"], "❌ 请用precision_score计算精确率"),
                MustContainRule(["recall_score"], "❌ 请用recall_score计算召回率"),
                MustContainRule(["f1_score"], "❌ 请用f1_score计算F1分数"),
                MustContainRule(["confusion_matrix"], "❌ 请用confusion_matrix生成混淆矩阵"),
                MustContainRule(["classification_report"], "❌ 请用classification_report生成详细分类报告"),
            ],
        ),
        7: StepSpec(
            success_message="✅ 步骤7通过！",
            rules=[
                MustContainRule(["model.coef_"], "❌ 请从模型中获取特征系数（提示：model.coef_[0]）"),
                MustContainRule(["np.argsort"], "❌ 请用np.argsort(np.abs(...))对系数绝对值排序"),
                MustContainRule(["np.abs"], "❌ 请用np.argsort(np.abs(...))对系数绝对值排序"),
                MustContainRule(["plt.barh"], "❌ 请使用plt.barh绘制水平条形图展示特征影响力"),
                MustContainRule(["sorted_indices"], "❌ 请生成sorted_indices（排序后的索引）用于同步特征名与系数"),
                MustContainRule(["feature_names"], "❌ 请使用feature_names（中文特征名）来展示图表y轴"),
            ],
        ),
    },
    # -----------------------------
    # KMeans (wine)
    # -----------------------------
    "kmeans": {
        1: StepSpec(
            success_message="✅ 步骤1通过！",
            rules=[
                MustContainRule(["load_wine"], "❌ 请加载葡萄酒数据集（使用load_wine）"),
                MustContainRule(["X_raw.shape"], "❌ 数据形状应使用X_raw.shape（提示：.shape）"),
                MustContainRule(["X_raw[:3]"], "❌ 前3行特征应使用X_raw[:3]（提示：切片[:3]）"),
                MustContainRule(["np.mean(X_raw, axis=0)"], "❌ 特征均值应使用np.mean(X_raw, axis=0)"),
                MustContainRule(["np.var(X_raw, axis=0)"], "❌ 特征方差应使用np.var(X_raw, axis=0)"),
            ],
        ),
        2: StepSpec(
            success_message="✅ 步骤2通过！",
            rules=[
                MustContainRule(["X = X_raw"], "❌ 请用X = X_raw定义特征数据"),
            ],
        ),
        3: StepSpec(
            success_message="✅ 步骤3通过！",
            rules=[
                MustContainRule(["StandardScaler"], "❌ 请用StandardScaler进行特征标准化"),
                MustContainRule(["scaler.fit_transform(X)"], "❌ 应使用scaler.fit_transform(X)标准化特征"),
            ],
        ),
        4: StepSpec(
            success_message="✅ 步骤4通过！",
            rules=[
                MustContainRule(["KMeans"], "❌ 请实例化KMeans模型（model = KMeans()）"),
                MustContainRule(["model = KMeans"], "❌ 请实例化KMeans模型（model = KMeans()）"),
                MustContainRule(["n_clusters=3"], "❌ 请设置n_clusters=3（葡萄酒数据集原始有3类）"),
                MustContainRule(["random_state=42"], "❌ 请设置random_state=42保证结果可复现"),
            ],
        ),
        5: StepSpec(
            success_message="✅ 步骤5通过！",
            rules=[
                MustContainRule(["model.fit"], "❌ 训练模型应为model.fit(X_scaled)"),
                MustContainRule(["X_scaled"], "❌ 训练模型应为model.fit(X_scaled)"),
                MustContainRule(["fit_predict"], "❌ 请使用model.predict或fit_predict获取聚类标签"),
            ],
        ),
        6: StepSpec(
            success_message="✅ 步骤6通过！",
            rules=[
                MustContainRule(["silhouette_score"], "❌ 请用silhouette_score计算轮廓系数"),
                MustContainRule(["calinski_harabasz_score"], "❌ 请用calinski_harabasz_score计算CH指数"),
                MustContainRule(["PCA"], "❌ 请用PCA进行降维可视化"),
            ],
        ),
    },
    # -----------------------------
    # Neural Network vs Linear (housing)
    # -----------------------------
    "neural_network": {
        1: StepSpec(
            success_message="✅ 步骤1通过！",
            rules=[
                MustContainRule(["fetch_california_housing"], "❌ 请加载加州房价数据集（使用fetch_california_housing）"),
                MustContainRule(["shape"], "❌ 请查看数据形状（使用.shape）"),
                MustContainRule(["mean"], "❌ 请查看数据统计信息（均值、标准差等）"),
                MustContainRule(["std"], "❌ 请查看数据统计信息（均值、标准差等）"),
                MustContainRule(["corrcoef"], "❌ 请计算特征相关性（使用np.corrcoef）"),
            ],
        ),
        2: StepSpec(
            success_message="✅ 步骤2通过！",
            rules=[
                MustContainRule(["train_test_split"], "❌ 请使用train_test_split划分训练集和测试集"),
                MustContainRule(["test_size=0.2"], "❌ 请设置test_size=0.2"),
                MustContainRule(["random_state=42"], "❌ 请设置random_state=42保证结果可复现"),
            ],
        ),
        3: StepSpec(
            success_message="✅ 步骤3通过！",
            rules=[
                MustContainRule(["StandardScaler"], "❌ 请用StandardScaler进行特征标准化"),
                MustContainRule(["fit_transform"], "❌ 应使用fit_transform处理训练集，transform处理测试集"),
                MustContainRule(["transform"], "❌ 应使用fit_transform处理训练集，transform处理测试集"),
            ],
        ),
        4: StepSpec(
            success_message="✅ 步骤4通过！",
            rules=[
                MustContainRule(["LinearRegression"], "❌ 请实例化线性回归模型（linear_model = LinearRegression()）"),
                MustContainRule(["linear_model = LinearRegression()"], "❌ 请实例化线性回归模型（linear_model = LinearRegression()）"),
                MustContainRule(["fit"], "❌ 请训练模型（使用.fit()方法）"),
                MustContainRule(["predict"], "❌ 请预测测试集结果（使用.predict()方法）"),
            ],
        ),
        5: StepSpec(
            success_message="✅ 步骤5通过！",
            rules=[
                MustContainRule(["MLPRegressor"], "❌ 请实例化神经网络回归模型（nn_model = MLPRegressor()）"),
                MustContainRule(["nn_model = MLPRegressor"], "❌ 请实例化神经网络回归模型（nn_model = MLPRegressor()）"),
                MustContainRule(["hidden_layer_sizes"], "❌ 请设置hidden_layer_sizes参数"),
                MustContainRule(["max_iter=200"], "❌ 请设置max_iter=200"),
                MustContainRule(["random_state=42"], "❌ 请设置random_state=42"),
            ],
        ),
        6: StepSpec(
            success_message="✅ 步骤6通过！",
            rules=[
                MustContainRule(["mean_squared_error"], "❌ 请用mean_squared_error计算均方误差"),
                MustContainRule(["r2_score"], "❌ 请用r2_score计算R²分数"),
            ],
        ),
    },
    # -----------------------------
    # Bayes Text Classification
    # -----------------------------
    "bayes_text": {
        1: StepSpec(
            success_message="✅ 步骤1通过！",
            rules=[
                MustContainRule(["fetch_20newsgroups"], "❌ 请使用fetch_20newsgroups加载新闻数据集"),
                MustContainRule(["subset='train'"], "❌ 请同时加载训练集(subset='train')和测试集(subset='test')"),
                MustContainRule(["subset='test'"], "❌ 请同时加载训练集(subset='train')和测试集(subset='test')"),
                MustContainRule(["remove=("], "❌ 请移除邮件头、签名和引用(remove=('headers', 'footers', 'quotes'))"),
            ],
        ),
        2: StepSpec(
            success_message="✅ 步骤2通过！",
            rules=[
                MustContainRule(["X_train_text ="], "❌ 请正确提取训练集和测试集文本数据"),
                MustContainRule(["X_test_text ="], "❌ 请正确提取训练集和测试集文本数据"),
                MustContainRule(["y_train ="], "❌ 请正确提取训练集和测试集标签"),
                MustContainRule(["y_test ="], "❌ 请正确提取训练集和测试集标签"),
                MustContainRule(["Counter"], "❌ 请使用Counter统计类别分布"),
            ],
        ),
        3: StepSpec(
            success_message="✅ 步骤3通过！",
            rules=[
                MustContainRule(["TfidfVectorizer"], "❌ 请使用TfidfVectorizer进行文本特征提取"),
                MustContainRule(["fit_transform"], "❌ 训练集应使用fit_transform进行转换"),
                MustContainRule(["transform"], "❌ 测试集应使用transform进行转换"),
            ],
        ),
        4: StepSpec(
            success_message="✅ 步骤4通过！",
            rules=[
                MustContainRule(["MultinomialNB"], "❌ 请导入并使用MultinomialNB模型"),
                MustContainRule(["model = MultinomialNB"], "❌ 请正确实例化MultinomialNB模型"),
            ],
        ),
        5: StepSpec(
            success_message="✅ 步骤5通过！",
            rules=[
                MustContainRule(["model.fit"], "❌ 请使用model.fit训练模型"),
                MustContainRule(["X_train_tfidf"], "❌ 训练时应使用X_train_tfidf和y_train"),
                MustContainRule(["y_train"], "❌ 训练时应使用X_train_tfidf和y_train"),
            ],
        ),
        6: StepSpec(
            success_message="✅ 步骤6通过！",
            rules=[
                MustContainRule(["model.predict"], "❌ 请使用model.predict进行预测"),
                MustContainRule(["accuracy_score"], "❌ 请计算准确率(accuracy_score)"),
                MustContainRule(["classification_report"], "❌ 请生成分类报告(classification_report)"),
            ],
        ),
    },
}


def get_step_spec(module_id: str, step_num: int) -> Optional[StepSpec]:
    return SPECS.get(module_id, {}).get(step_num)

