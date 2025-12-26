# Leaf Label Implementation Correctness Report

## Overview

This report analyzes the `$show.leaf.label` parameter implementation in the Modern Tree Viewer module, examining the data flow from parameter parsing to GUI control and layout calculation.

## Implementation Analysis

### 1. Parameter Definition

**File:** `src/module/evolview/moderntreeviewer/io/ParamsAssignerAndParser4ModernTreeView.java`

```java
addKeyValueEntryBean("$show.leaf.label", "false",
    "Whether to show leaf labels. Default: false");
```

**Status:** CORRECT - Parameter is properly defined with default value `false`.

---

### 2. Parameter Parsing (Bean Assignment)

**File:** `src/module/evolview/moderntreeviewer/io/ParamsAssignerAndParser4ModernTreeView.java:89-93`

```java
string = o.getSimplifiedStringWithDefault("$show.leaf.label");
if (!string.isEmpty()) {
    boolean bool = Boolean.parseBoolean(string);
    ret.setShowLeafLabel(bool);
}
```

**Status:** CORRECT - Value is properly parsed and stored in `MTVImportInforBean`.

---

### 3. Property Transfer to TreeLayoutProperties

**File:** `src/module/evolview/moderntreeviewer/io/TreePropertiesAssigner.java:35-49`

```java
ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
showLeafPropertiesInfo.setShowLeafLabel(object.isShowLeafLabel());

if (object.isShowLeafLabel()) {
    showLeafPropertiesInfo.setNeedChange4showLabel(true);
    showLeafPropertiesInfo.setNeedChange4hideLabel(false);

    List<GraphicsNode> leaves = treeLayoutProperties.getLeaves();
    for (GraphicsNode node : leaves) {
        node.getDrawUnit().setDrawName(true);
    }
} else {
    showLeafPropertiesInfo.setNeedChange4showLabel(false);
    showLeafPropertiesInfo.setNeedChange4hideLabel(false);
}
```

**Status:** CORRECT - Properties are properly transferred and leaf nodes' `drawName` is set.

---

### 4. GUI Button Synchronization

**File:** `src/module/evolview/pathwaybrowser/gui/CtrlTreeOperationPanelByMiglayout.java:494-508`

```java
@Override
public void reInitializeGUIAccording2treeLayoutProperties() {
    ActionListener[] actionListeners = showleaveLabelToggleButton.getActionListeners();
    for (ActionListener actionListener : actionListeners) {
        showleaveLabelToggleButton.removeActionListener(actionListener);
    }

    TreeLayoutProperties treeLayoutProperties = controller.getTreeLayoutProperties();
    ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
    showleaveLabelToggleButton.setSelected(showLeafPropertiesInfo.isShowLeafLabel());

    for (ActionListener actionListener : actionListeners) {
        showleaveLabelToggleButton.addActionListener(actionListener);
    }
}
```

**Status:** CORRECT - GUI button state is properly synchronized with properties.

---

### 5. GUI Button Action Handler

**File:** `src/module/evolview/gfamily/GeneFamilyController.java:420-438`

```java
public void showLeaveLabel(boolean selected) {
    ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
    List<GraphicsNode> leaves = treeLayoutProperties.getLeaves();
    for (GraphicsNode graphicsNode : leaves) {
        graphicsNode.getDrawUnit().setDrawName(selected);
    }
    if (selected) {
        showLeafPropertiesInfo.setNeedChange4showLabel(true);
        showLeafPropertiesInfo.setNeedChange4hideLabel(false);
    } else {
        showLeafPropertiesInfo.setNeedChange4showLabel(false);
        showLeafPropertiesInfo.setNeedChange4hideLabel(true);
    }
    showLeafPropertiesInfo.setShowLeafLabel(selected);
}
```

**Status:** CORRECT - Clicking the button properly updates all leaf nodes and triggers layout recalculation.

---

### 6. Layout Calculation (Multiple Layouts)

The `isShowLeafLabel()` is correctly checked in layout calculations:

#### RectPhyloLayout.java:47-56
```java
ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
if (showLeafPropertiesInfo.isNeedChange4hideLabel()) {
    int stringWidth = getMaxLengthLeafNameWidthAccording2CurrentFont();
    blankArea.setRight(blankArea.getRight() - stringWidth);
    showLeafPropertiesInfo.setNeedChange4hideLabel(false);
} else if (showLeafPropertiesInfo.isNeedChange4showLabel()) {
    int stringWidth = getMaxLengthLeafNameWidthAccording2CurrentFont();
    blankArea.setRight(stringWidth + blankArea.getRight());
    showLeafPropertiesInfo.setNeedChange4showLabel(false);
}
```

#### RectangularLayout.java:77-83
```java
if (treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()) {
    int maxLengthLeafNameWidthAccording2CurrentGlobalFont = getMaxLengthLeafNameWidthAccording2CurrentFont();
    rightMostX += maxLengthLeafNameWidthAccording2CurrentGlobalFont;
} else {
    rightMostX += 15;
}
```

**Status:** CORRECT - Layout calculation correctly considers leaf label visibility for proper spacing.

---

## Issues Found

### Issue #1: CRITICAL BUG - Hardcoded `isShowLeafLabel = false` in MTreeViewMainFace

**File:** `src/module/evolview/moderntreeviewer/MTreeViewMainFace.java:349-356`

```java
boolean isShowLeafLabel = false;  // BUG: Hardcoded to false!
if (isShowLeafLabel) {
    showLeafPropertiesInfo.setNeedChange4showLabel(true);
    showLeafPropertiesInfo.setNeedChange4hideLabel(false);
} else {
    showLeafPropertiesInfo.setNeedChange4showLabel(false);
    showLeafPropertiesInfo.setNeedChange4hideLabel(false);
}
```

**Problem:** This code hardcodes `isShowLeafLabel` to `false`, completely ignoring the value from `MTVImportInforBean`. When a user sets `$show.leaf.label=true` in their configuration, it will be ignored during direct module initialization.

**Impact:** When the module is launched directly via `IndependentModuleLoader` (not via VOICE4MTV import flow), the `$show.leaf.label` parameter value is ignored.

**Fix Required:** Should use value from `treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel()`.

---

### Issue #2: Default Font Size Too Small

**File:** `src/module/evolview/moderntreeviewer/io/ParamsAssignerAndParser4ModernTreeView.java:19`

```java
Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(9f);
```

**Problem:** Default leaf label font size is set to 9 points, which is too small for comfortable reading.

**Recommendation:** Change to 12 points as per user request.

---

## Data Flow Summary

```
Parameter Definition (ParamsAssignerAndParser4ModernTreeView)
    |
    v
Parameter Parsing -> MTVImportInforBean.showLeafLabel
    |
    v
TreePropertiesAssigner.assign() -> ShowLeafPropertiesInfo.showLeafLabel
    |                           -> GraphicsNode.drawUnit.drawName (for each leaf)
    v
Layout Calculation (RectPhyloLayout, RectangularLayout, etc.)
    |                           -> Uses isShowLeafLabel() for spacing
    v
GUI Button (CtrlTreeOperationPanelByMiglayout)
    |                           -> Synced via reInitializeGUIAccording2treeLayoutProperties()
    v
User Interaction -> GeneFamilyController.showLeaveLabel()
                    -> Updates ShowLeafPropertiesInfo
                    -> Updates all leaf nodes' drawName
                    -> Triggers layout recalculation
```

---

## Recommendations

1. **Fix MTreeViewMainFace.java bug** - Replace hardcoded `false` with actual value from properties
2. **Increase default font size** - Change from 9f to 12f
3. **Add unit test** - Create test to verify parameter propagation

---

## Files Involved

| File | Role | Status |
|------|------|--------|
| `ParamsAssignerAndParser4ModernTreeView.java` | Parameter definition & parsing | OK (font size needs change) |
| `MTVImportInforBean.java` | Data bean | OK |
| `TreePropertiesAssigner.java` | Property transfer | OK |
| `ShowLeafPropertiesInfo.java` | Property storage | OK |
| `TreeLayoutProperties.java` | Layout properties | OK |
| `MTreeViewMainFace.java` | Module initialization | **BUG** |
| `GeneFamilyController.java` | GUI action handler | OK |
| `CtrlTreeOperationPanelByMiglayout.java` | GUI button | OK |
| `RectPhyloLayout.java` | Layout calculation | OK |
| `RectangularLayout.java` | Layout calculation | OK |
| `RadicalLayout.java` | Layout calculation | OK |

---

## Conclusion

The overall implementation is well-structured with proper separation of concerns. The main issue is a hardcoded value in `MTreeViewMainFace.java` that bypasses the parameter system during direct module initialization. The font size default of 9 points is also too small for comfortable use.
