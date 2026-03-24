"""神经网络（嵌入 neural_network_demo）"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import importlib.util

_spec = importlib.util.spec_from_file_location("nn_demo", ROOT / "neural_network_demo.py")
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)
