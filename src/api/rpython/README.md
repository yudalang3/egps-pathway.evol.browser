# External Language Bridge APIs

## Overview

This directory contains the Java-side bridge classes intended for R or Python-driven workflows.

These classes are the current external-language integration surface for this repository.

---

## Current Classes

| Class | Role |
|------|------|
| `API4R` | Compatibility-oriented R API for tree node extraction |
| `RlangInterfaceEGPS` | R-side desktop bridge for launching eGPS and opening Modern Tree View |
| `ModernTreeViewPyLauncher` | Python-side launcher for starting eGPS and driving MTV import from a config file |
| `EvolTreeManipulator` | Utility API for extracting node names from Newick trees |
| `TestJFrame` | Local helper/test UI class in the same integration area |

---

## Preferred Entry Methods

| Class | Preferred methods |
|------|-------------------|
| `API4R` | `extractNodeNames(...)`, `describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`, `showPayloadAndReturnLength(...)`, `openModernTreeView(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `EvolTreeManipulator` | `extractNodeNames(...)`, `describe()` |
| `TestJFrame` | `showDemoWindow(...)`, `renderDemoImageAsPng(...)` |

Legacy method names are still kept for backward compatibility.

---

## Boundary

Only external-language bridge entry points and support utilities belong here.

Internal visualization classes should remain in their subsystem packages even if their names include `rpython`, unless they are promoted into a real external API.

---

## Maintenance Rules

1. New R/Python-facing Java bridge classes should be added under `src/api/rpython`.
2. Keep this directory focused on entry points and small bridge utilities.
3. If an external workflow depends on a class here, document it in `docs/current/external_language_api.md`.
