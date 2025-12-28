# Spiral Layout Architecture Design

## 1. Mathematical Foundation

### Archimedean Spiral Formula

```
r = α + β × θ
```

| Symbol | Name | Description |
|--------|------|-------------|
| `r` | radius | Distance from center point |
| `α` (alpha) | initial radius | Starting radius when θ=0 |
| `β` (beta) | growth rate | Radius increase per radian |
| `θ` (theta) | angle | Angle in radians |

### Coordinate Conversion (Polar → Cartesian)

```java
// GuiCalculator.calculateSpiralLocation(alpha, beta, angleDeg, centerX, centerY)
double radians = Math.toRadians(angleDeg);
double r = alpha + beta * radians;
double x = centerX + r * Math.cos(radians);
double y = centerY - r * Math.sin(radians);  // Y-axis inverted in screen coordinates
```

---

## 2. Two Layout Variants

### Overview

| | Alpha Layout | Beta Layout |
|--|-------------|-------------|
| **Variable** | α (starting radius) | β (growth rate) |
| **Fixed** | β (growth rate) | α (starting radius) |
| **Branch Length Encoding** | Radial offset | Spiral steepness |
| **Visual Effect** | Concentric spirals | Diverging spiral curves |

### Alpha Layout (`SpiralPhyloWithAlpha`)

```
r = currentAlpha + beta × θ
    └── varies      └── fixed
```

**Characteristics:**
- All nodes follow spirals with the **same growth rate (β)**
- Branch length determines **radial offset (α)**
- Produces **concentric-like spiral rings**
- Best for: Trees with small branch length variations

**Key Variables:**
```java
double beta;           // Global fixed growth rate
double maxAlpha;       // Maximum alpha value (outermost spiral)
double rootTipLength;  // Minimum alpha value (innermost spiral)
```

### Beta Layout (`SpiralPhyloWithBeta`)

```
r = alpha + currentBeta × θ
    └── fixed  └── varies
```

**Characteristics:**
- Each node follows a spiral with **different growth rate (β)**
- Branch length determines **spiral steepness**
- Produces **diverging cone-like pattern**
- Best for: Trees with large branch length variations

**Key Variables:**
```java
double alpha;      // Global fixed starting radius
double maxBeta;    // Maximum beta value (steepest spiral)
double minBeta;    // Minimum beta value (flattest spiral)
double betaFactor; // Multiplier for visual adjustment (default: 2.5)
```

---

## 3. Axis Reference Lines (drawBottomAxis)

### Purpose
Draw faint spiral reference lines to help users understand branch length scale.

### Algorithm

#### Alpha Layout
```java
// Alpha varies from rootTipLength to maxAlpha
double totalAvailableAlpha = maxAlpha - rootTipLength;

for (int i = 0; i < count; i++) {
    // Map display value to alpha range
    double currentAlpha = (displayValue - first) / range * totalAvailableAlpha + rootTipLength;

    // Draw spiral with varying alpha, fixed beta
    produceSpiral(globalStartDegree, totalDegree, currentAlpha, beta);
}
```

#### Beta Layout
```java
// Beta varies from minBeta to maxBeta
double totalAvailableBeta = maxBeta - minBeta;

for (int i = 0; i < count; i++) {
    // Map display value to beta range
    double currentBeta = (displayValue - first) / range * totalAvailableBeta + minBeta;

    // Draw spiral with fixed alpha, varying beta
    produceSpiral(globalStartDegree, totalDegree, rootTipLength, currentBeta);
}
```

### Important: Why Beta Layout Needs Special Handling

In Beta layout, larger β values cause rapid radius growth:
- When β is large and θ reaches 630° (~11 radians)
- Radius `r = α + β × 11` can exceed canvas bounds

**Solution**: Beta layout should limit axis lines to reasonable angle ranges or use different visualization strategies.

---

## 4. Class Hierarchy

```
BaseLayout (abstract)
├── blankArea: BlankArea
├── centerX, centerY
├── canvas2logicRatio
│
├── CicularLayout (abstract)
│   └── CircularPhylo
│       └── biggestCircleRadicus
│
└── SprialLayout (abstract)
    ├── biggestCircleRadicus
    ├── globalStartDegree
    ├── increseDeg
    ├── produceSpiral(startDeg, extendDeg, alpha, beta)
    ├── produceSpiralRing(startDeg, extendDeg, minAlpha, minBeta, maxAlpha, maxBeta)
    │
    ├── SpiralPhyloWithAlpha
    │   ├── beta (fixed)
    │   ├── maxAlpha (variable max)
    │   └── drawBottomAxis() → varies alpha
    │
    └── SpiralPhyloWithBeta
        ├── alpha (fixed)
        ├── minBeta, maxBeta (variable range)
        ├── betaFactor
        └── drawBottomAxis() → varies beta
```

---

## 5. Key Methods

### produceSpiral
```java
/**
 * Generate a spiral path from startDeg to startDeg+extendDeg
 *
 * @param startDeg   Starting angle in degrees
 * @param extendDeg  Angular extent in degrees
 * @param alpha      Initial radius (r when θ=0)
 * @param beta       Growth rate (Δr per radian)
 * @return GeneralPath representing the spiral curve
 */
protected GeneralPath produceSpiral(double startDeg, double extendDeg, double alpha, double beta)
```

### produceSpiralRing
```java
/**
 * Generate a ring-shaped region between two spirals
 * Used for clade highlighting/annotation
 *
 * @param startDeg   Starting angle
 * @param extendDeg  Angular extent
 * @param minAlpha   Inner spiral's alpha
 * @param minBeta    Inner spiral's beta
 * @param maxAlpha   Outer spiral's alpha
 * @param maxBeta    Outer spiral's beta
 * @return GeneralPath representing the ring region
 */
protected GeneralPath produceSpiralRing(...)
```

### assignLocation (Alpha)
```java
// r = alpha (varies by branch length) + beta * theta
Double pos = GuiCalculator.calculateSpiralLocation(
    node.getRadicusIfNeeded(),  // alpha - varies
    beta,                        // fixed
    currentNodeAngle,
    centerX, centerY
);
```

### assignLocation (Beta)
```java
// r = alpha + beta (varies by branch length) * theta
Double pos = GuiCalculator.calculateSpiralLocation(
    alpha,                              // fixed
    betaFactor * node.getRadicusIfNeeded(),  // beta - varies
    currentNodeAngle,
    centerX, centerY
);
```

---

## 6. Configuration Parameters

| Parameter | Property Class | Description | Default |
|-----------|---------------|-------------|---------|
| `globalStartDegree` | SprialLayoutProperty | Starting angle | 0 |
| `globalExtendingDegree` | SprialLayoutProperty | Total angle span | 630 |
| `gapSize` | SprialLayoutProperty | Gap size for spacing | 10 |
| `betaFactor` | SprialLayoutProperty | Beta multiplier (Beta layout only) | 2.5 |
| `rootTipLength` | TreeLayoutProperties | Root tip display length | 10-15 |

---

## 7. Debugging

Both layouts include debug visualization blocks:

```java
@Override
protected void specificTreeDrawingProcess(Graphics2D g2d) {
    // Draw the whole region of the spiral, for debug
    if (false) {
        generalPath = produceSpiralRing(0, totolDeg, ...);
        g2d.setColor(Color.lightGray);
        g2d.draw(generalPath);
    }

    // Draw time lines, for debug
    if (false) {
        // Multiple spirals at different alpha/beta values
        generalPath = produceSpiral(0, totolDeg, ...);
        g2d.setColor(Color.red);
        g2d.draw(generalPath);
        // ...
    }

    drawBottomAxis(g2d);
}
```

Change `if (false)` to `if (true)` to enable debug visualization.

---

## 8. When to Use Each Layout

### Use Alpha Layout When:
- Branch length variations are relatively small
- You want visually "parallel" spiral tracks
- Comparing distances between taxa is important

### Use Beta Layout When:
- Branch length variations are large
- You want a "spreading" cone-like effect
- Visual separation of deep vs shallow branches is important

### Use Circular Layout Instead When:
- Tree has < 30 taxa
- Precise branch length comparison is needed
- You need traditional annotation bars

---

## 9. Maintenance Notes

- Debug drawing blocks are intentionally guarded by `if (false)`; temporarily switch to `if (true)` for visualization during development.
