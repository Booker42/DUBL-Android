# Architecture — 0.1.3

The native branch deliberately keeps the first migration small and inspectable.

- `model/` contains pure DUBL domain data and derived formulas. It has no Android dependencies.
- `data/` contains local Android persistence.
- `state/` is the application state/controller layer used by Compose.
- `ui/` contains Compose screens, reusable components, and theme.

Persistence is currently a compact JSON snapshot stored in private SharedPreferences. This is adequate for the current small local model and avoids pulling database complexity into the migration. If inventory, spells, and the full skill catalog make the model relational or large, migrate persistence to Room without moving rule formulas into the database layer.

The UI must consume calculated values from the model rather than duplicating DUBL formulas in composables.
