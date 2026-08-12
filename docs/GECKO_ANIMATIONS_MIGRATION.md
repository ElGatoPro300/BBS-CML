# Gecko Animations Migration

## Overview

The Model Editor animation pipeline now supports a Gecko animation runtime that can run in parallel with the existing Legacy system and fallback to Legacy when needed.

Main integration points:

- `ActionsConfig` stores both legacy and gecko animation payloads.
- `ProceduralAnimator` builds both legacy and gecko contexts each frame.
- `GeckoAnimationController` validates config, applies gecko routes, and controls fallback.
- `UIModelPanel` sanitizes and validates gecko configuration on save.

## Data Model

Gecko animation data is serialized under:

- `gecko_animations` (`GeckoAnimationModuleConfig.DATA_KEY`)
- `gecko_animations_js` (editor-generated JS payload)

Core config fields:

- `enabled`: enables gecko runtime.
- `fallback_to_legacy`: enables safe runtime fallback.
- `emit_events`: controls animation event dispatch.
- `transition_speed`: state blend factor.
- `state_animations`: action/state to animation key map.
- `limbs`: per-bone animation settings (`GeckoLimbAnimationConfig`).

## Runtime Flow

Per tick in `ProceduralAnimator`:

1. Build `GeckoAnimationContext` from entity movement and posture.
2. Attempt gecko application through `GeckoAnimationController`.
3. If gecko fails or explicit fallback is enabled, run `LegacyAnimationController`.

Controller rules:

- If gecko is disabled, controller returns without applying.
- Validator sanitizes values before apply.
- Validation ensures mapped bones and referenced animation names are valid.
- Runtime exceptions are captured and result in fallback.

## State Coverage

Current gecko state model includes:

- `idle`
- `walk`
- `run`
- `jump`
- `fall`
- `attack`
- `swim`
- `fly`
- `wheel`

Per-limb options include:

- Swinging arm movement behavior.
- Wheel axis and wheel speed conversion.
- Optional per-state animation overrides (`walkAnimation`, `runAnimation`, etc.).

## Migration Guide

1. Keep existing legacy configuration unchanged.
2. Add `gecko_animations.enabled = true`.
3. Add `state_animations` entries for required states.
4. Add `limbs` entries for target bones.
5. Save from Model Editor to trigger validator sanitization.
6. Keep `fallback_to_legacy = true` during rollout.
7. Disable fallback after verifying model/bone mappings and state transitions.

## Usage Example

```json
{
  "gecko_animations": {
    "enabled": true,
    "fallback_to_legacy": true,
    "emit_events": true,
    "transition_speed": 0.2,
    "state_animations": {
      "walk": "walk",
      "run": "run",
      "jump": "jump"
    },
    "limbs": {
      "right_arm": {
        "swinging": true,
        "walk_animation": "walk",
        "attack_animation": "attack"
      },
      "front_left_wheel": {
        "wheel": true,
        "wheel_axis": "x",
        "wheel_speed": 1.8
      }
    }
  }
}
```

## Validation Notes

- Unknown bones are rejected by validator checks.
- Unknown animation ids are rejected by validator checks.
- Invalid wheel axis values are normalized to `x`.
- Negative wheel speeds are clamped to `0`.
- Transition speed is clamped to `[0, 1]`.

## Test Coverage

Added tests cover:

- Gecko config serialization/deserialization.
- Gecko validator error and sanitization behavior.
- Gecko controller apply path and invalid-config skip behavior.
