"""逻辑回归（嵌入 logistic_regression_demo）"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import importlib.util

_spec = importlib.util.spec_from_file_location("logistic_demo", ROOT / "logistic_regression_demo.py")
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)
