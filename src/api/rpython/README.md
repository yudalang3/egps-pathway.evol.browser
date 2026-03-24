# External Language Bridge APIs

## Overview

`src/api/rpython/` contains the Java bridge surface intended to be called from Python or R wrappers.

## Current Classes

| Class | Role |
|------|------|
| `API4R` | Compatibility-oriented tree utility bridge |
| `EvolTreeManipulator` | Base utility API for extracting node names from Newick trees |
| `RlangInterfaceEGPS` | R-facing desktop bridge for launching eGPS and opening the two primary GUI modules |
| `ModernTreeViewPyLauncher` | Python-facing launcher for starting eGPS and importing Modern Tree View from a VOICE config file |
| `PathwayFamilyBrowserPyLauncher` | Python-facing launcher for starting eGPS and importing Pathway Family Browser from a VOICE config file |
| `TestJFrame` | Local helper/test UI bridge |

## Preferred Methods

| Class | Preferred methods |
|------|-------------------|
| `API4R` | `extractNodeNames(...)`, `describe()` |
| `EvolTreeManipulator` | `extractNodeNames(...)`, `describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`, `showPayloadAndReturnLength(...)`, `openModernTreeView(...)`, `openPathwayFamilyBrowser(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `PathwayFamilyBrowserPyLauncher` | `launchFromConfigFile(...)` |
| `TestJFrame` | `showDemoWindow(...)`, `renderDemoImageAsPng(...)` |

## Boundary

Only public bridge entry points and small bridge helpers should live here. Internal visualization implementation classes stay in their subsystem packages unless they are promoted into real external APIs.

## Maintenance Rules

1. New R/Python-facing Java bridge classes belong under `src/api/rpython`.
2. Keep the package focused on entry points and lightweight helpers.
3. If a wrapper depends on a class here, document it in `docs/current/external_language_api.md`.
