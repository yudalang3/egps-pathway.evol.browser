# Spiral Layout Reference

## Overview

This document describes the current spiral-layout mechanism used by the phylogenetic tree visualization layer.

The implementation is based on the Archimedean spiral formula:

```text
r = alpha + beta * theta
```

where:

- `r` is the radius from the layout center
- `alpha` controls the starting offset
- `beta` controls spiral growth
- `theta` is the angle in radians

---

## Current Layout Variants

The current codebase keeps two spiral variants.

### Alpha-based spiral

This variant changes `alpha` while keeping `beta` fixed.

**Effect**

- branch length is expressed as radial offset
- spiral arms keep the same growth rate
- the result looks closer to concentric spiral bands

**Typical use**

- trees with relatively small branch-length variation
- layouts where parallel-looking spiral tracks are easier to read

### Beta-based spiral

This variant changes `beta` while keeping `alpha` fixed.

**Effect**

- branch length is expressed as spiral steepness
- different branches grow outward at different rates
- the result looks more divergent and cone-like

**Typical use**

- trees with larger branch-length variation
- layouts where stronger visual separation is useful

---

## Coordinate Calculation

The current spiral location calculation follows this pattern:

```java
double radians = Math.toRadians(angleDeg);
double r = alpha + beta * radians;
double x = centerX + r * Math.cos(radians);
double y = centerY - r * Math.sin(radians);
```

The negative sign on `y` is required because the screen coordinate system grows downward.

---

## Current Drawing Responsibilities

The spiral layout implementation currently handles:

- assigning node coordinates
- generating spiral paths for tree branches
- generating spiral-ring regions for clade highlighting
- drawing bottom-axis reference spirals
- converting tree branch-length space into screen distance

The main shared methods are:

- `produceSpiral(...)`
- `produceSpiralRing(...)`
- layout-specific `assignLocation(...)`
- layout-specific `drawBottomAxis(...)`

---

## Scale and Axis Behavior

Spiral layouts use different width calculations depending on which parameter varies.

### Alpha-based layout

When `alpha` varies, the visible radial difference is effectively constant:

```text
delta_r = delta_alpha
```

This makes scale-bar width easier to map directly from the available alpha range.

### Beta-based layout

When `beta` varies, the visible radial difference depends on the outer angle:

```text
delta_r = delta_beta * theta
```

That is why beta-based layouts must compute scale width against the effective maximum angle instead of treating it as constant.

---

## Current Configuration Surface

The active spiral-layout configuration includes:

| Parameter | Meaning |
|-----------|---------|
| `globalStartDegree` | Start angle |
| `globalExtendingDegree` | Total angular extent |
| `gapSize` | Spiral gap spacing |
| `betaFactor` | Beta multiplier used by beta-mode rendering |
| `rootTipLength` | Minimum visual root-tip distance |

---

## Practical Guidance

Use the alpha-based spiral when you want a more regular spiral structure and easier distance comparison.

Use the beta-based spiral when you want larger branch-length differences to open up visually.

If the tree is small or precise branch-length reading matters more than compact spiral presentation, a non-spiral layout is often clearer.
