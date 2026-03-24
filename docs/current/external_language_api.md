# External Language API Surface

## Overview

The current Java bridge surface for external wrappers is centralized under:

```text
src/api/rpython/
```

## Current Classes

| Class | Role |
|------|------|
| `API4R` | Compatibility-oriented tree utility bridge |
| `EvolTreeManipulator` | Base utility API for extracting node names from Newick trees |
| `RlangInterfaceEGPS` | R-facing desktop bridge for launching eGPS and opening Modern Tree View / Pathway Family Browser |
| `ModernTreeViewPyLauncher` | Python-facing launcher for driving Modern Tree View import from a VOICE config file |
| `PathwayFamilyBrowserPyLauncher` | Python-facing launcher for driving Pathway Family Browser import from a VOICE config file |
| `TestJFrame` | Local helper/test UI bridge |

## Preferred Method Surface

| Class | Preferred methods |
|------|-------------------|
| `API4R` | `extractNodeNames(...)`, `describe()` |
| `EvolTreeManipulator` | `extractNodeNames(...)`, `describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`, `showPayloadAndReturnLength(...)`, `openModernTreeView(...)`, `openPathwayFamilyBrowser(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `PathwayFamilyBrowserPyLauncher` | `launchFromConfigFile(...)` |
| `TestJFrame` | `showDemoWindow(...)`, `renderDemoImageAsPng(...)` |

## Scope

This directory is reserved for wrapper-facing entry points and small bridge helpers. Internal visualization classes remain in their subsystem packages unless they become public external APIs.

## Current GUI Entry Points

The two primary GUI modules intended for wrappers are:

- `ModernTreeViewPyLauncher.launchFromConfigFile(...)`
- `PathwayFamilyBrowserPyLauncher.launchFromConfigFile(...)`
- `RlangInterfaceEGPS.openModernTreeView(...)`
- `RlangInterfaceEGPS.openPathwayFamilyBrowser(...)`
