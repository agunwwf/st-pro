import io
import numpy as np
from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from ml_core.logistic_regression_core import (
    load_cancer_dataset,
    compute_feature_stats,
    split_features_and_target,
    split_and_scale,
    build_logistic_model,
    train_and_predict,
    evaluate_classification,
    compute_feature_importance,
    run_full_pipeline,
)
from ml_core.logistic_demo_core import (
    sigmoid,
    generate_classification_data,
    logistic_regression_gradient_descent,
    manual_predict_and_metrics,
    loss_comparison,
    get_model_evaluation_data,
    load_example_dataset,
    analyze_custom_data_array,
)


app = FastAPI(
    title="逻辑回归教学 API",
    description="基于乳腺癌数据集的逻辑回归分步教学后端，供前端网页调用。",
    version="1.0.0",
)


# 允许本地前端（以及以后比赛部署的前端）跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/logistic/overview")
def logistic_overview():
    """
    数据概览：
    - 样本数、特征数
    - 前 10 行样本数据（X_head, y_head）
    - 部分特征的均值和方差
    """
    data = load_cancer_dataset()
    X_raw = data["X_raw"]
    y_raw = data["y_raw"]
    feature_names_cn = data["feature_names_cn"]
    target_names_cn = data["target_names_cn"]

    stats = compute_feature_stats(X_raw)

    return {
        "n_samples": int(X_raw.shape[0]),
        "n_features": int(X_raw.shape[1]),
        "feature_names_cn": feature_names_cn,
        "target_names_cn": target_names_cn,
        "X_head": X_raw[:10].tolist(),
        "y_head": y_raw[:10].tolist(),
        "feature_means": stats["means"].tolist(),
        "feature_vars": stats["vars"].tolist(),
    }


@app.get("/api/logistic/train-evaluate")
def logistic_train_evaluate(test_size: float = 0.2, random_state: int = 42):
    """
    完成：
    - 特征与目标划分
    - 训练/测试划分 + 标准化
    - 逻辑回归训练与预测
    - 计算 Accuracy / Precision / Recall / F1 / 混淆矩阵 / 分类报告
    """
    data = load_cancer_dataset()
    X_raw = data["X_raw"]
    y_raw = data["y_raw"]
    target_names_cn = data["target_names_cn"]

    Xy = split_features_and_target(X_raw, y_raw)
    X, y = Xy["X"], Xy["y"]

    preprocessed = split_and_scale(
        X,
        y,
        test_size=test_size,
        random_state=random_state,
    )

    X_train_scaled = preprocessed["X_train_scaled"]
    X_test_scaled = preprocessed["X_test_scaled"]
    y_train = preprocessed["y_train"]
    y_test = preprocessed["y_test"]

    model = build_logistic_model()
    train_result = train_and_predict(
        model,
        X_train_scaled,
        X_test_scaled,
        y_train,
    )
    y_pred = train_result["y_pred"]

    metrics = evaluate_classification(
        y_test,
        y_pred,
        target_names_cn=target_names_cn,
    )

    return {
        "n_train": int(X_train_scaled.shape[0]),
        "n_test": int(X_test_scaled.shape[0]),
        "test_size": float(test_size),
        "metrics": metrics,
    }


@app.get("/api/logistic/feature-importance")
def logistic_feature_importance(test_size: float = 0.2, random_state: int = 42):
    """
    运行一次完整流水线，并返回特征重要性排序结果。
    前端可以用来绘制条形图和前 5 名特征表格。
    """
    data = load_cancer_dataset()
    X_raw = data["X_raw"]
    y_raw = data["y_raw"]
    feature_names_cn = data["feature_names_cn"]

    Xy = split_features_and_target(X_raw, y_raw)
    X, y = Xy["X"], Xy["y"]

    preprocessed = split_and_scale(
        X,
        y,
        test_size=test_size,
        random_state=random_state,
    )

    X_train_scaled = preprocessed["X_train_scaled"]
    X_test_scaled = preprocessed["X_test_scaled"]
    y_train = preprocessed["y_train"]

    model = build_logistic_model()
    train_result = train_and_predict(
        model,
        X_train_scaled,
        X_test_scaled,
        y_train,
    )
    model_trained = train_result["model"]

    importance = compute_feature_importance(
        model_trained,
        feature_names_cn,
    )

    return {
        "sorted_names": importance["sorted_names"],
        "sorted_coef": importance["sorted_coef"],
    }


@app.get("/api/logistic/full")
def logistic_full(test_size: float = 0.2, random_state: int = 42):
    """
    一次性返回完整流水线的所有关键教学数据：
    - 数据集基本信息 + 样本预览
    - 特征均值/方差
    - 训练/测试划分信息
    - 各类评估指标
    - 特征重要性排序
    适合前端在初始化页面时获取完整上下文。
    """
    result = run_full_pipeline(
        test_size=test_size,
        random_state=random_state,
    )
    return result


# ---------- Demo 交互式学习平台 API ----------


@app.get("/api/demo/classification-data")
def demo_classification_data(
    data_type: str = "线性可分",
    n_samples: int = 200,
    separation: float = 2.0,
):
    """分类数据生成：返回 X, y 及统计，供前端画散点图。"""
    X, y = generate_classification_data(data_type, n_samples, separation)
    n0 = int(np.sum(y == 0))
    n1 = int(np.sum(y == 1))
    return {
        "X": X.tolist(),
        "y": y.tolist(),
        "n0": n0,
        "n1": n1,
        "mean_f1": float(np.mean(X[:, 0])),
        "std_f1": float(np.std(X[:, 0])),
        "mean_f2": float(np.mean(X[:, 1])),
        "std_f2": float(np.std(X[:, 1])),
    }


@app.get("/api/demo/sigmoid-curve")
def demo_sigmoid_curve():
    """Sigmoid 曲线采样点，用于绘图。"""
    x = np.linspace(-10, 10, 500).tolist()
    y = sigmoid(np.linspace(-10, 10, 500)).tolist()
    return {"x": x, "y": y}


@app.get("/api/demo/sigmoid-threshold")
def demo_sigmoid_threshold(z_value: float = 0.0):
    """不同阈值的 sigmoid 示意：阈值 0.3/0.5/0.7 对应的 x 及曲线。"""
    x = np.linspace(-5, 5, 200).tolist()
    y = sigmoid(np.linspace(-5, 5, 200)).tolist()
    thresholds = [0.3, 0.5, 0.7]
    x_thresholds = [
        float(np.log(t / (1 - t))) if 0 < t < 1 else 0 for t in thresholds
    ]
    sig_z = float(sigmoid(z_value))
    return {
        "curve_x": x,
        "curve_y": y,
        "thresholds": thresholds,
        "x_thresholds": x_thresholds,
        "z_value": z_value,
        "sigmoid_z": sig_z,
    }


@app.post("/api/demo/manual-predict")
async def demo_manual_predict(body: dict):
    """参数手动调整：传入 X, y, weight, bias, threshold，返回预测、准确率、混淆矩阵、曲线。"""
    X = np.array(body["X"])
    y = np.array(body["y"])
    weight = float(body["weight"])
    bias = float(body["bias"])
    threshold = float(body["threshold"])
    return manual_predict_and_metrics(X, y, weight, bias, threshold)


@app.post("/api/demo/gradient-descent")
async def demo_gradient_descent(body: dict):
    """梯度下降：传入 X, y, learning_rate, n_iterations，返回 weights, bias, costs, history。"""
    from sklearn.preprocessing import StandardScaler
    X = np.array(body["X"])
    y = np.array(body["y"])
    lr = float(body["learning_rate"])
    n_iter = int(body["n_iterations"])
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    out = logistic_regression_gradient_descent(X_scaled, y, lr, n_iter)
    out["scaler_mean"] = scaler.mean_.tolist()
    out["scaler_scale"] = scaler.scale_.tolist()
    out["X_original"] = X.tolist()
    out["y_original"] = y.tolist()
    return out


@app.get("/api/demo/loss-comparison")
def demo_loss_comparison(y_true: int = 0, y_pred: float = 0.3):
    """损失函数对比：交叉熵 vs MSE，及曲线数据。"""
    return loss_comparison(y_true, y_pred)


@app.get("/api/demo/model-evaluation")
def demo_model_evaluation():
    """模型评估：固定数据集与训练结果，决策边界网格、混淆矩阵、指标。"""
    return get_model_evaluation_data()


@app.get("/api/demo/example-dataset")
def demo_example_dataset(name: str = "信用卡欺诈检测"):
    """示例数据集：信用卡欺诈/客户流失/疾病风险。返回 X, y, description。"""
    X, y, desc = load_example_dataset(name)
    if X is None:
        return {"error": "未知数据集", "X": [], "y": [], "description": ""}
    return {"X": X.tolist(), "y": y.tolist(), "description": desc}


@app.get("/api/demo/analyze-example")
def demo_analyze_example(name: str = "信用卡欺诈检测"):
    """对内置示例数据集做 LR 分析，返回与 analyze-custom 相同结构。"""
    X, y, _ = load_example_dataset(name)
    if X is None:
        return {"error": "未知数据集"}
    n_features = X.shape[1]
    feature_names = [f"特征{i+1}" for i in range(n_features)]
    return analyze_custom_data_array(X, y, feature_names)


@app.post("/api/demo/analyze-custom")
async def demo_analyze_custom(file: UploadFile = File(...)):
    """上传 CSV，解析后训练 LR，返回指标、系数、2D 时决策边界。"""
    if not file.filename or not file.filename.lower().endswith(".csv"):
        return {"error": "请上传 CSV 文件"}
    try:
        raw = await file.read()
        import pandas as pd
        df = pd.read_csv(io.BytesIO(raw))
    except Exception as e:
        return {"error": f"解析 CSV 失败: {str(e)}"}
    categorical = df.select_dtypes(include=["object"]).columns.tolist()
    if categorical:
        df = df.select_dtypes(exclude=["object"])
    if len(df.columns) < 2:
        return {"error": "至少需要一列特征和一列目标"}
    # 让前端指定目标列名，或这里取最后一列为目标
    target_col = df.columns[-1]
    feature_cols = [c for c in df.columns if c != target_col]
    if not feature_cols:
        return {"error": "没有特征列"}
    X = df[feature_cols].values
    y = df[target_col].values
    unique = np.unique(y)
    if len(unique) != 2:
        return {"error": "目标列必须是二分类(仅两个取值)"}
    # 映射为 0/1
    y = (y == unique[1]).astype(int)
    result = analyze_custom_data_array(X, y, feature_cols.tolist())
    return result


@app.get("/")
def root():
    return {
        "message": "逻辑回归教学 API 就绪。文档见 /docs 或 /redoc。",
    }

