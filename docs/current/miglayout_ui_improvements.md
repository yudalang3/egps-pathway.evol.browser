# MigLayout UI Improvements

## Goal
Use MigLayout, borders, and separators to make parameter-heavy panels easier to scan and less cramped.

## Applied Pattern

- Add `TitledBorder` sections for configuration and input groups.
- Add `JSeparator` between logical blocks.
- Use `fill`, `grow`, and `pushy` so empty space expands naturally instead of leaving the active panel stuck in the middle.
- Standardize the main action button text so the UI reads consistently.

## Useful Constraints

| Constraint | Purpose |
|---|---|
| `fill` | Let the panel consume the available space |
| `insets 0` | Remove extra outer padding |
| `grow, fill` | Make columns expand cleanly |
| `grow, pushy` | Let a section absorb extra height |
| `growx, h 40!` | Make the main action button prominent |

## Takeaway
This layout pattern works well for module panels that combine file pickers, radio groups, and a single run action.
