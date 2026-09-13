# Architecture — 0.2

The Android app is a native Kotlin + Jetpack Compose client with rules logic kept outside composables.

- `model/` contains pure DUBL domain data and deterministic formulas. It has no Android dependencies.
- `model/SkillModels.kt` owns the skill catalog, rank XP costs, skill resolution, and skill calculations.
- `model/DevelopmentModels.kt` owns skills/abilities, prerequisites, branch access, ability-point budgeting, and requirement validation.
- `model/MagicEquipmentModels.kt` owns mana, spell-learning, equipment load/capacity, and burden rules.
- `model/RollRules.kt` owns the tested dice engine for normal rolls, advantages/hindrances, doubles, criticals, and follow-up dice.
- `data/` contains local persistence and catalog readers.
- `state/CharacterController.kt` is the UI-facing mutation layer for core character data; character-sheet UI extras remain in their dedicated repository.
- `ui/` contains Compose screens, reusable components, and theme.

Persistence is a compact JSON snapshot stored in private SharedPreferences. Schema 4 contains skills, development, magic, and equipment while remaining backward compatible with older snapshots where those fields are absent.

Large reference catalogs are packaged as local assets so the app works offline. Character saves store only selected/owned character data, not duplicate copies of every catalog entry.

Cross-system formulas belong in the model. Examples include equipment burden affecting Defense/Reflexes/Run, magic state satisfying development prerequisites, and skill calculations using one selected attribute when several alternatives are allowed.

Compose screens must consume model results rather than reproducing rules formulas locally.
