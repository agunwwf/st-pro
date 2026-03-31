from __future__ import annotations

from typing import Optional

from st.config.step_specs import MustContainRule, StepSpec, get_step_spec


def _contains_all(code: str, rule: MustContainRule) -> bool:
    target = code
    if rule.remove_spaces:
        target = target.replace(" ", "")
    return all(s.replace(" ", "") in target if rule.remove_spaces else s in target for s in rule.substrings)


def validate_step(module_id: str, step_num: int, user_code: str) -> str:
    spec: Optional[StepSpec] = get_step_spec(module_id, step_num)
    if spec is None:
        return "✅ 通过！"

    errors = []
    for rule in spec.rules:
        if not _contains_all(user_code, rule):
            errors.append(rule.message)

    return spec.success_message if not errors else "\n".join(errors)

