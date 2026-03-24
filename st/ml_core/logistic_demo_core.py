# 逻辑回归 Demo 核心逻辑（无 Streamlit，供 API 调用）
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
)


def sigmoid(x):
    """Sigmoid 激活函数。x 可为标量或数组。"""
    x = np.asarray(x)
    return 1.0 / (1.0 + np.exp(-np.clip(x, -500, 500)))


def generate_classification_data(data_type: str, n_samples: int, separation: float):
    """生成二分类数据，与 logistic_regression_demo 一致。"""
    np.random.seed(42)
    n_samples = int(n_samples)
    if data_type == "线性可分":
        n1, n2 = n_samples // 2, (n_samples + 1) // 2
        X1 = np.random.randn(n1, 2) * 0.8 + np.array([separation, separation])
        X2 = np.random.randn(n2, 2) * 0.8 - np.array([separation, separation])
        X = np.vstack((X1, X2))
        y = np.hstack((np.zeros(n1), np.ones(n2)))
    elif data_type == "线性不可分":
        X = np.random.randn(n_samples, 2) * 1.2
        y = (X[:, 0] ** 2 + X[:, 1] ** 2 < 1.5).astype(int)
    elif data_type == "不平衡数据":
        n_majority = int(n_samples * 0.8)
        n_minority = n_samples - n_majority
        X_majority = (
            np.random.randn(n_majority, 2) * 0.8
            - np.array([separation / 2, separation / 2])
        )
        X_minority = (
            np.random.randn(n_minority, 2) * 0.8
            + np.array([separation / 2, separation / 2])
        )
        X = np.vstack((X_majority, X_minority))
        y = np.hstack((np.zeros(n_majority), np.ones(n_minority)))
    else:
        X = np.random.randn(n_samples, 2) * 0.8
        y = np.random.randint(0, 2, size=n_samples)

    indices = np.random.permutation(n_samples)
    X = X[indices]
    y = y[indices]
    return X, y


def logistic_regression_gradient_descent(X, y, learning_rate: float, n_iterations: int):
    """手动逻辑回归梯度下降，返回 weights, bias, costs 及每步的 (weights, bias) 用于动画。"""
    n_samples, n_features = X.shape
    weights = np.zeros(n_features)
    bias = 0.0
    costs = []
    # 每 step 步记录一次状态，用于前端动画
    step = max(1, n_iterations // 30)
    history = []

    for it in range(n_iterations):
        linear = np.dot(X, weights) + bias
        y_pred = sigmoid(linear)
        cost = -np.mean(
            y * np.log(y_pred + 1e-15) + (1 - y) * np.log(1 - y_pred + 1e-15)
        )
        costs.append(float(cost))

        dw = (1.0 / n_samples) * np.dot(X.T, (y_pred - y))
        db = (1.0 / n_samples) * np.sum(y_pred - y)
        weights -= learning_rate * dw
        bias -= learning_rate * db

        if it % step == 0 or it == n_iterations - 1:
            history.append(
                {
                    "iteration": it,
                    "weights": weights.tolist(),
                    "bias": float(bias),
                    "cost": float(cost),
                }
            )

    return {
        "weights": weights.tolist(),
        "bias": float(bias),
        "costs": costs,
        "history": history,
    }


def manual_predict_and_metrics(X, y, weight: float, bias: float, threshold: float):
    """单特征：用 weight * x + bias 经 sigmoid 后与 threshold 比较得到预测，返回准确率与混淆矩阵。"""
    X_single = np.asarray(X)[:, 0]
    z = weight * X_single + bias
    y_prob = sigmoid(z)
    y_pred = (y_prob >= threshold).astype(int)
    acc = float(accuracy_score(y, y_pred))
    cm = confusion_matrix(y, y_pred)
    # 决策边界线：sigmoid(w*x+b)=threshold => x = (log(threshold/(1-threshold)) - b) / w
    try:
        x_boundary = (np.log(threshold / (1 - threshold)) - bias) / weight
    except Exception:
        x_boundary = 0.0
    x_min, x_max = float(X_single.min()) - 1, float(X_single.max()) + 1
    x_range = np.linspace(x_min, x_max, 150)
    curve_y = sigmoid(weight * x_range + bias)
    return {
        "y_pred": y_pred.tolist(),
        "accuracy": acc,
        "cm": cm.tolist(),
        "x_boundary": float(x_boundary),
        "curve_x": x_range.tolist(),
        "curve_y": curve_y.tolist(),
    }


def loss_comparison(y_true: int, y_pred: float):
    """交叉熵与 MSE，以及曲线上的点。"""
    eps = 1e-10
    y_pred = max(eps, min(1 - eps, y_pred))
    ce = -(
        y_true * np.log(y_pred) + (1 - y_true) * np.log(1 - y_pred)
    )
    mse = (y_true - y_pred) ** 2
    x = np.linspace(0.01, 0.99, 100).tolist()
    if y_true == 1:
        ce_curve = (-np.log(np.clip(x, 1e-10, None))).tolist()
        mse_curve = ((1 - np.array(x)) ** 2).tolist()
    else:
        ce_curve = (-np.log(1 - np.array(x) + 1e-10)).tolist()
        mse_curve = (np.array(x) ** 2).tolist()
    return {
        "ce_loss": float(ce),
        "mse_loss": float(mse),
        "x": x,
        "ce_curve": ce_curve,
        "mse_curve": mse_curve,
    }


def get_model_evaluation_data():
    """固定种子的模型评估用数据：重叠高斯、训练 LR、返回测试集与指标。"""
    np.random.seed(42)
    n = 200
    X0 = np.random.multivariate_normal(
        mean=[-1, -1], cov=[[2, 1.5], [1.5, 2]], size=n // 2
    )
    X1 = np.random.multivariate_normal(
        mean=[1, 1], cov=[[2, -1.5], [-1.5, 2]], size=n // 2
    )
    X = np.vstack((X0, X1))
    y = np.hstack((np.zeros(n // 2), np.ones(n // 2)))
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.3, random_state=42
    )
    model = LogisticRegression(random_state=42)
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    cm = confusion_matrix(y_test, y_pred)
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, zero_division=0)
    rec = recall_score(y_test, y_pred, zero_division=0)
    f1 = f1_score(y_test, y_pred, zero_division=0)
    # 决策边界网格
    x_min, x_max = X_test[:, 0].min() - 0.5, X_test[:, 0].max() + 0.5
    y_min, y_max = X_test[:, 1].min() - 0.5, X_test[:, 1].max() + 0.5
    xx = np.arange(x_min, x_max, 0.02)
    yy = np.arange(y_min, y_max, 0.02)
    XX, YY = np.meshgrid(xx, yy)
    Z = model.predict(np.c_[XX.ravel(), YY.ravel()]).reshape(XX.shape)
    misclassified = (y_test != y_pred)
    return {
        "X_test": X_test.tolist(),
        "y_test": y_test.tolist(),
        "y_pred": y_pred.tolist(),
        "misclassified": misclassified.tolist(),
        "cm": cm.tolist(),
        "accuracy": float(acc),
        "precision": float(prec),
        "recall": float(rec),
        "f1": float(f1),
        "grid_x": xx.tolist(),
        "grid_y": yy.tolist(),
        "grid_Z": Z.tolist(),
    }


def load_example_dataset(example_name: str):
    """返回示例数据 X, y 及描述。"""
    np.random.seed(42)
    sig = sigmoid  # 本地用

    if example_name == "信用卡欺诈检测":
        n_samples = 500
        n_fraud = int(n_samples * 0.1)
        normal_amount = np.random.normal(500, 300, n_samples - n_fraud)
        normal_time = np.random.normal(12, 6, n_samples - n_fraud)
        normal_freq = np.random.normal(2, 1, n_samples - n_fraud)
        fraud_amount = np.random.normal(2000, 800, n_fraud)
        fraud_time = np.random.normal(20, 4, n_fraud)
        fraud_freq = np.random.normal(0.5, 0.3, n_fraud)
        X = np.vstack([
            np.column_stack((normal_amount, normal_time, normal_freq)),
            np.column_stack((fraud_amount, fraud_time, fraud_freq)),
        ])
        y = np.hstack([np.zeros(n_samples - n_fraud), np.ones(n_fraud)])
        indices = np.random.permutation(n_samples)
        X, y = X[indices], y[indices]
        desc = "信用卡欺诈检测数据: 包含交易金额、时间和频率特征，预测交易是否为欺诈(1=欺诈)"
        return X, y, desc

    if example_name == "客户流失预测":
        n_samples = 500
        tenure = np.random.normal(30, 20, n_samples)
        monthly_charge = np.random.normal(50, 30, n_samples)
        support_calls = np.random.randint(0, 10, n_samples)
        X = np.column_stack((tenure, monthly_charge, support_calls))
        z = -0.05 * tenure + 0.03 * monthly_charge + 0.3 * support_calls - 2
        prob = sig(z)
        y = np.random.binomial(1, prob)
        desc = "客户流失预测数据: 包含使用时长、月消费和客服联系次数，预测客户是否会流失(1=流失)"
        return X, y, desc

    if example_name == "疾病风险预测":
        n_samples = 500
        age = np.random.normal(50, 15, n_samples)
        bmi = np.random.normal(25, 5, n_samples)
        blood_pressure = np.random.normal(120, 15, n_samples)
        X = np.column_stack((age, bmi, blood_pressure))
        z = 0.04 * age + 0.1 * bmi + 0.03 * blood_pressure - 10
        prob = sig(z)
        y = np.random.binomial(1, prob)
        desc = "疾病风险预测数据: 包含年龄、BMI和血压，预测患病风险(1=患病)"
        return X, y, desc

    return None, None, ""


def analyze_custom_data_array(X: np.ndarray, y: np.ndarray, feature_names: list):
    """对给定 X, y 训练 LR，返回指标、系数、若 2D 则决策边界网格。"""
    if len(X) < 10:
        return {"error": "数据点至少需要10个样本"}
    model = LogisticRegression(random_state=42, max_iter=1000)
    model.fit(X, y)
    y_pred = model.predict(X)
    acc = accuracy_score(y, y_pred)
    report = classification_report(y, y_pred)
    cm = confusion_matrix(y, y_pred)
    coef = model.coef_[0].tolist()
    result = {
        "accuracy": float(acc),
        "report": report,
        "cm": cm.tolist(),
        "feature_names": feature_names,
        "coefficients": coef,
    }
    if X.shape[1] == 2:
        x_min, x_max = X[:, 0].min() - 0.5, X[:, 0].max() + 0.5
        y_min, y_max = X[:, 1].min() - 0.5, X[:, 1].max() + 0.5
        xx = np.arange(x_min, x_max, 0.02)
        yy = np.arange(y_min, y_max, 0.02)
        XX, YY = np.meshgrid(xx, yy)
        Z = model.predict(np.c_[XX.ravel(), YY.ravel()]).reshape(XX.shape)
        result["grid_x"] = xx.tolist()
        result["grid_y"] = yy.tolist()
        result["grid_Z"] = Z.tolist()
        result["X_2d"] = X.tolist()
        result["y_2d"] = y.tolist()
    return result
