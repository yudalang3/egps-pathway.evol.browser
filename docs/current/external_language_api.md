# External Language API Surface

## Overview

External language bridge classes are now centralized under:

```text
src/api/rpython/
```

This directory is the current home for project-facing Java entry points intended to be called from R or Python-side workflows.

---

## Current Classes

| Class | Role |
|------|------|
| `API4R` | Compatibility-oriented R API for tree node extraction |
| `RlangInterfaceEGPS` | R-side desktop bridge for launching eGPS and opening Modern Tree View |
| `ModernTreeViewPyLauncher` | Python-side launcher that starts eGPS and executes MTV import from a VOICE config file |
| `EvolTreeManipulator` | Utility API for extracting node names from Newick trees |
| `TestJFrame` | Local helper/test UI class in the same integration area |

---

## Preferred Method Surface

| Class | Preferred methods |
|------|-------------------|
| `API4R` | `extractNodeNames(...)`, `describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`, `showPayloadAndReturnLength(...)`, `openModernTreeView(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `EvolTreeManipulator` | `extractNodeNames(...)`, `describe()` |
| `TestJFrame` | `showDemoWindow(...)`, `renderDemoImageAsPng(...)` |

---

## Current Boundary

This directory is intended for external language entry points and bridge utilities.

It does not automatically include every internal class whose name contains `rpython`. Rendering-side placeholders under internal visualization packages remain part of the visualization layer unless they become actual external API entry points.

---

## Current Rules

1. R/Python-facing Java bridge classes should live in `src/api/rpython`.
2. Internal visualization classes should stay with the visualization subsystem unless they are promoted to a real external API.
3. Documentation that references external language entry points should point to `src/api/rpython`.

---

## Current MTV-Related External Entry Points

For Modern Tree View specifically, the current external-language entry points are:

- `src/api/rpython/ModernTreeViewPyLauncher.java`
- `src/api/rpython/RlangInterfaceEGPS.java`

The Python launcher executes the full MTV import path from a config file.

The R bridge currently opens the MTV module shell and provides a stable Java-side entry for future R-driven integration.
