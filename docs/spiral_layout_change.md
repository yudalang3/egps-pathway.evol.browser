# Spiral Layout Analysis and Changes

## 1. Overview

Spiral layout places phylogenetic tree nodes along Archimedean spirals. There are two variants:
- **Alpha variant** (`SpiralPhyloWithAlpha`): Fixed beta (growth rate), variable alpha (starting radius)
- **Beta variant** (`SpiralPhyloWithBeta`): Fixed alpha (starting radius), variable beta (growth rate)

### Archimedean Spiral Formula
```
r = alpha + beta * theta
```
Where:
- `r` = radius from center
- `alpha` = initial radius (starting point)
- `beta` = growth rate per radian
- `theta` = angle in radians

---

## 2. Alpha vs Beta Variants Comparison

| Aspect | Alpha Variant | Beta Variant |
|--------|---------------|--------------|
| **Formula** | `r = alpha + beta * theta` | `r = alpha + beta * theta` |
| **Fixed** | beta (growth rate) | alpha (starting radius) |
| **Variable** | alpha (based on branch length) | beta (based on branch length) |
| **Visual Effect** | Leaves at same angle but different radii | Leaves follow different spiral curves |
| **Branch Length Encoding** | Radial distance from center | Spiral curve steepness |

### Alpha Variant Characteristics
- All leaves follow the SAME spiral curve (same beta)
- Branch lengths are encoded in the radial distance (alpha offset)
- Root is at center, leaves spread outward along same spiral
- Better for trees where branch length variations are small

### Beta Variant Characteristics
- Each branch follows a DIFFERENT spiral curve (different beta)
- Branch lengths are encoded in the spiral's growth rate
- Creates a "cone-like" visual effect
- Better for trees with large branch length variations

---

## 3. When to Use Spiral Layouts

### Ideal Use Cases

1. **Large Trees (100-1000+ taxa)**
   - Spiral layouts efficiently use space for very large trees
   - Unlike circular layouts, spirals don't waste the center area
   - Leaves naturally spread outward, reducing overlap

2. **Trees with Hierarchical Time Structure**
   - Evolution over geological time (millions of years)
   - Ancient lineages near center, recent lineages at periphery
   - Creates intuitive "timeline" from center outward

3. **Trees with Continuous Divergence**
   - When taxa diverged gradually over time
   - Smooth spiral progression matches gradual evolution
   - Better visual flow than discrete circular arcs

4. **Artistic/Publication Figures**
   - Spiral creates visually striking images
   - Often used in "tree of life" visualizations
   - Memorable and recognizable layout

### When NOT to Use Spiral Layouts

1. **Small Trees (<30 taxa)**
   - Spiral benefits only appear with larger trees
   - Rectangular or circular layouts are cleaner for small trees

2. **Trees Requiring Precise Comparisons**
   - Branch lengths are harder to compare in spiral form
   - Rectangular layouts better for quantitative analysis

3. **Trees with Many Polytomies**
   - Spiral assumes binary branching pattern
   - Polytomies create visual artifacts

4. **Trees Needing Annotation Bars**
   - Traditional bar annotations don't fit spiral curves
   - Rectangular layouts better for heatmaps, presence/absence data

---

## 4. Issues Found and Fixed

### Issue 1: Center Offset (Fixed)

**Problem**: Center was calculated as `currentWidth/2, currentHeight/2` which ignores margin offsets.

**File**: `SprialLayout.java` (lines 26-28)

**Before**:
```java
centerX = (int) (currentWidth / 2);
centerY = (int) (currentHeight / 2);
```

**After**:
```java
int workHeight = blankArea.getWorkHeight((int) currentHeight);
int workWidth = blankArea.getWorkWidth((int) currentWidth);
centerX = blankArea.getLeft() + workWidth / 2;
centerY = blankArea.getTop() + workHeight / 2;
```

### Issue 2: Debug Lines Still Visible (To Review)

**Location**: `SpiralPhyloWithAlpha.java` and `SpiralPhyloWithBeta.java`

The `specificTreeDrawingProcess()` methods contain commented-out debug drawing code. These should remain commented but could be cleaned up if no longer needed for debugging.

### Issue 3: Magic Number +50

**Location**: `SprialLayout.java` line 44

```java
biggestCircleRadicus = 0.5 * Math.min(workWidth, workHeight) + 50;
```

The `+50` is described as "experiential visual adjustment". This may need to be parameterized or removed after fixing the center offset issue.

---

## 5. Spiral Layout Parameters

| Parameter | Location | Description |
|-----------|----------|-------------|
| `globalStartDegree` | SprialLayoutProperty | Starting angle (default: 0) |
| `globalExtendingDegree` | SprialLayoutProperty | Total angle span (default: 360 * N turns) |
| `gapSize` | SprialLayoutProperty | Gap between spiral turns |
| `betaFactor` | SprialLayoutProperty | Multiplier for beta in Beta variant |

---

## 6. Recommendations

### Short-term
1. Test spiral layouts after center offset fix
2. Verify leaf labels don't extend outside canvas
3. Check axis/scale bar positioning

### Medium-term
1. Consider making the `+50` adjustment configurable
2. Add spiral-specific annotation support (curved text along spiral)
3. Consider adding parameter to control spiral tightness

### Long-term
1. Implement adaptive spiral that adjusts based on tree size
2. Add support for logarithmic spiral (`r = a * e^(b*theta)`) for exponential growth trees
3. Consider hyperbolic spiral for trees with concentrated recent divergence

---

## 7. Visual Comparison

```
CIRCULAR LAYOUT:           SPIRAL LAYOUT:
    ___________               ___________
   /           \             /     ∿∿∿   \
  /   o---o     \           /    ∿∿∿      \
 |    |    \     |         |   ∿∿∿        |
 |    o     o    |         |  ∿∿∿ center  |
 |    |    /     |         |   ∿∿∿        |
  \   o---o     /           \    ∿∿∿      /
   \___________/             \_____∿∿∿___/

- Fixed radius per depth      - Increasing radius as spiral
- Arcs connect nodes          - Spiral curves connect nodes
- Center often empty          - Center is spiral origin
```

---

## 8. Code Structure

```
BaseLayout (abstract)
    ├── CicularLayout (abstract)
    │   ├── CircularPhylo
    │   ├── CircularCladoEqual
    │   └── ...
    └── SprialLayout (abstract)   ← Center offset fixed here
        ├── SpiralPhyloWithAlpha  ← Alpha variant
        ├── SpiralPhyloWithBeta   ← Beta variant
        ├── SprialCladoAlignedWithAlpha
        └── SpiralCladoAlignedWithBeta
```

All spiral layouts inherit from `SprialLayout`, so fixing the center offset there fixes all variants.
