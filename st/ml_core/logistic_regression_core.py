# 逻辑回归分步教学核心逻辑（乳腺癌数据集，无 Streamlit）
import numpy as np
from sklearn.datasets import load_breast_cancer
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
)


def load_cancer_dataset():
    """加载乳腺癌数据集，返回原始特征/标签及中英文特征名。"""
    cancer = load_breast_cancer()
    X_raw = cancer.data
    y_raw = cancer.target
    feature_names_en = list(cancer.feature_names)
    feature_names_cn = [
        "平均半径", "平均纹理", "平均周长", "平均面积", "平均平滑度",
        "平均紧凑度", "平均凹度", "平均凹点", "平均对称性", "平均分形维数",
        "半径标准差", "纹理标准差", "周长标准差", "面积标准差", "平滑度标准差",
        "紧凑度标准差", "凹度标准差", "凹点标准差", "对称性标准差", "分形维数标准差",
        "最大半径", "最大纹理", "最大周长", "最大面积", "最大平滑度",
        "最大紧凑度", "最大凹度", "最大凹点", "最大对称性", "最大分形维数",
    ]
    target_names_cn = ["恶性", "良性"]
    return {
        "X_raw": X_raw,
        "y_raw": y_raw,
        "feature_names_en": feature_names_en,
        "feature_names_cn": feature_names_cn,
        "target_names_cn": target_names_cn,
    }


def compute_feature_stats(X_raw: np.ndarray):
    """计算每个特征的均值和方差。"""
    feature_means = np.mean(X_raw, axis=0)
    feature_vars = np.var(X_raw, axis=0)
    return {"means": feature_means, "vars": feature_vars}


def split_features_and_target(X_raw: np.ndarray, y_raw: np.ndarray):
    """将原始数据拆分为特征 X 和目标 y。"""
    return {"X": X_raw, "y": y_raw}


def split_and_scale(
    X: np.ndarray,
    y: np.ndarray,
    test_size: float = 0.2,
    random_state: int = 42,
):
    """划分训练/测试集并对特征标准化。"""
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=random_state
    )
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    return {
        "X_train_scaled": X_train_scaled,
        "X_test_scaled": X_test_scaled,
        "y_train": y_train,
        "y_test": y_test,
        "scaler": scaler,
    }


def build_logistic_model(**kwargs) -> LogisticRegression:
    """构建逻辑回归模型实例。"""
    return LogisticRegression(**kwargs)


def train_and_predict(
    model: LogisticRegression,
    X_train_scaled: np.ndarray,
    X_test_scaled: np.ndarray,
    y_train: np.ndarray,
):
    """使用标准化训练集训练，在测试集上预测。"""
    model.fit(X_train_scaled, y_train)
    y_pred = model.predict(X_test_scaled)
    return {"model": model, "y_pred": y_pred}


def evaluate_classification(
    y_test: np.ndarray,
    y_pred: np.ndarray,
    target_names_cn=None,
):
    """计算分类评估指标及混淆矩阵、分类报告。"""
    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    cm = confusion_matrix(y_test, y_pred)
    report = classification_report(
        y_test,
        y_pred,
        target_names=target_names_cn if target_names_cn else None,
    )
    return {
        "accuracy": float(accuracy),
        "precision": float(precision),
        "recall": float(recall),
        "f1": float(f1),
        "cm": cm.tolist(),
        "report": report,
    }


def compute_feature_importance(
    model: LogisticRegression,
    feature_names_cn,
):
    """按系数绝对值排序得到特征重要性。"""
    coef = model.coef_[0]
    sorted_indices = np.argsort(np.abs(coef))[::-1]
    sorted_names = [feature_names_cn[i] for i in sorted_indices]
    sorted_coef = [float(coef[i]) for i in sorted_indices]
    return {"sorted_names": sorted_names, "sorted_coef": sorted_coef}


def run_full_pipeline(test_size: float = 0.2, random_state: int = 42):
    """一键运行完整逻辑回归流水线，返回所有教学用数据。"""
    data = load_cancer_dataset()
    X_raw = data["X_raw"]
    y_raw = data["y_raw"]
    feature_names_cn = data["feature_names_cn"]
    target_names_cn = data["target_names_cn"]

    Xy = split_features_and_target(X_raw, y_raw)
    X, y = Xy["X"], Xy["y"]

    preprocessed = split_and_scale(
        X, y, test_size=test_size, random_state=random_state
    )
    X_train_scaled = preprocessed["X_train_scaled"]
    X_test_scaled = preprocessed["X_test_scaled"]
    y_train = preprocessed["y_train"]
    y_test = preprocessed["y_test"]

    model = build_logistic_model()
    train_result = train_and_predict(
        model, X_train_scaled, X_test_scaled, y_train
    )
    model_trained = train_result["model"]
    y_pred = train_result["y_pred"]

    metrics = evaluate_classification(
        y_test, y_pred, target_names_cn=target_names_cn
    )
    importance = compute_feature_importance(
        model_trained, feature_names_cn
    )
    feature_stats = compute_feature_stats(X_raw)

    return {
        "dataset": {
            "n_samples": int(X_raw.shape[0]),
            "n_features": int(X_raw.shape[1]),
            "feature_names_cn": feature_names_cn,
            "target_names_cn": target_names_cn,
        },
        "sample_preview": {
            "X_head": X_raw[:10].tolist(),
            "y_head": y_raw[:10].tolist(),
        },
        "feature_stats": {
            "means": feature_stats["means"].tolist(),
            "vars": feature_stats["vars"].tolist(),
        },
        "splits": {
            "n_train": int(X_train_scaled.shape[0]),
            "n_test": int(X_test_scaled.shape[0]),
            "test_size": float(test_size),
        },
        "metrics": metrics,
        "importance": importance,
    }
