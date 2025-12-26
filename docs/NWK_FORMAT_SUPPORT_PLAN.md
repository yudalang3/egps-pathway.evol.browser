# Newick Format Support Plan

## Overview

This document outlines the implementation plan for the `nwk.format` parameter in the Modern Tree Viewer module. The design follows ETE3/ETE4's Newick format specification.

## Reference: ETE3/ETE4 Format Types

| Format | Leaf Node | Internal Node | Description |
|--------|-----------|---------------|-------------|
| **0** | name:dist | support:dist | Flexible format with support values (default) |
| **1** | name:dist | name:dist | Internal node names instead of support |
| **2** | name:dist (strict) | support:dist (strict) | Strict format with support values |
| **3** | name:dist (strict) | name:dist (strict) | Strict format with node names |
| **4** | name:dist | - (none) | Leaf only, no internal node data |
| **5** | name:dist | :dist only | Internal nodes have only distance |
| **6** | name:dist | name only | Internal nodes have only name |
| **7** | name only | name only | Names only, no distances |
| **8** | name only | - (none) | Leaf names only |
| **9** | - (none) | - (none) | Pure topology |

## Support Value Convention

**IMPORTANT:** Bootstrap/support values should be in the **0-100 range** (e.g., 90, 95, 100), not decimal format (0.0-1.0).

| Correct | Incorrect | Meaning |
|---------|-----------|---------|
| `90` | `0.9` | 90% bootstrap support |
| `95` | `0.95` | 95% bootstrap support |
| `100` | `1.0` | 100% bootstrap support |

Example Newick strings:
- ✅ Correct: `(A:0.1,(B:0.2,C:0.3)90:0.4);`
- ❌ Incorrect: `(A:0.1,(B:0.2,C:0.3)0.9:0.4);`

This convention is documented in:
- Manual files (manual_en.html, manual_zh.html)
- Code documentation (NodeParseStrategyFactory.java, AbstractNodeParseStrategy.java)

## Implementation Plan

### Phase 1: Parameter Definition

#### 1.1 Add parameter to `ParamsAssignerAndParser4PhyloTree.java`

```java
// Add in constructor
addKeyValueEntryBean("nwk.format", "0",
    "Newick format type (0-9). See help for details. Default: 0 (flexible with support values).");
```

#### 1.2 Add field to `EvolTreeImportInfoBean.java`

```java
private int nwkFormat = 0;

public int getNwkFormat() {
    return nwkFormat;
}

public void setNwkFormat(int nwkFormat) {
    this.nwkFormat = nwkFormat;
}
```

#### 1.3 Update `assignValues()` method

```java
string = o.getSimplifiedStringWithDefault("nwk.format");
if (!string.isEmpty()) {
    int format = Integer.parseInt(string);
    if (format >= 0 && format <= 9) {
        ret.setNwkFormat(format);
    }
}
```

### Phase 2: Parser Modification

#### 2.1 Create Format Configuration Class

Location: `src/module/evoltreio/NewickFormatConfig.java`

```java
package module.evoltreio;

public class NewickFormatConfig {

    public enum NodeField {
        NAME,       // Node name
        DISTANCE,   // Branch length
        SUPPORT,    // Bootstrap/support value
        NONE        // Not present
    }

    private final NodeField leafField1;
    private final NodeField leafField2;
    private final NodeField internalField1;
    private final NodeField internalField2;
    private final boolean strict;

    public static NewickFormatConfig getFormat(int formatNumber) {
        return switch (formatNumber) {
            case 0 -> new NewickFormatConfig(NAME, DISTANCE, SUPPORT, DISTANCE, false);
            case 1 -> new NewickFormatConfig(NAME, DISTANCE, NAME, DISTANCE, false);
            case 2 -> new NewickFormatConfig(NAME, DISTANCE, SUPPORT, DISTANCE, true);
            case 3 -> new NewickFormatConfig(NAME, DISTANCE, NAME, DISTANCE, true);
            case 4 -> new NewickFormatConfig(NAME, DISTANCE, NONE, NONE, false);
            case 5 -> new NewickFormatConfig(NAME, DISTANCE, NONE, DISTANCE, false);
            case 6 -> new NewickFormatConfig(NAME, DISTANCE, NAME, NONE, false);
            case 7 -> new NewickFormatConfig(NAME, NONE, NAME, NONE, false);
            case 8 -> new NewickFormatConfig(NAME, NONE, NONE, NONE, false);
            case 9 -> new NewickFormatConfig(NONE, NONE, NONE, NONE, false);
            default -> getFormat(0); // Default to format 0
        };
    }

    // Constructor and getters...
}
```

#### 2.2 Modify Tree Parser

Update `TreeParser4Evoltree.java` to accept format configuration:

```java
private DefaultPhyNode getTree(String nwkStr, boolean removeBlank, int format) throws Exception {
    NewickFormatConfig config = NewickFormatConfig.getFormat(format);

    // Create appropriate decoders based on config
    NWKLeafCoderDecoder<DefaultPhyNode> leafDecoder = createLeafDecoder(config);
    NWKInternalCoderDecoder<DefaultPhyNode> internalDecoder = createInternalDecoder(config);

    PrimaryNodeTreeDecoder<DefaultPhyNode> treeDecoder =
        new PrimaryNodeTreeDecoder<>(leafDecoder, internalDecoder);

    return treeDecoder.decode(nwkStr);
}
```

### Phase 3: Decoder Modifications

#### 3.1 Evaluate Current Decoder Classes

Files to modify (in dependency JAR or source):
- `NWKLeafCoderDecoder.java`
- `NWKInternalCoderDecoder.java`
- `PrimaryNodeTreeDecoder.java`

#### 3.2 Create Format-Aware Decoders

Option A: Extend existing decoders with format awareness
Option B: Create new decoder implementations for each format

Recommended: Option A with strategy pattern

```java
public interface NodeParseStrategy {
    void parseNodeString(String nodeStr, DefaultPhyNode node);
}

public class Format0LeafStrategy implements NodeParseStrategy {
    @Override
    public void parseNodeString(String nodeStr, DefaultPhyNode node) {
        // Parse as "name:distance"
        String[] parts = nodeStr.split(":");
        node.setName(parts[0]);
        if (parts.length > 1) {
            node.setLength(Double.parseDouble(parts[1]));
        }
    }
}

public class Format0InternalStrategy implements NodeParseStrategy {
    @Override
    public void parseNodeString(String nodeStr, DefaultPhyNode node) {
        // Parse as "support:distance"
        String[] parts = nodeStr.split(":");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            node.setSupport(Double.parseDouble(parts[0]));
        }
        if (parts.length > 1) {
            node.setLength(Double.parseDouble(parts[1]));
        }
    }
}
```

### Phase 4: Integration Points

#### 4.1 Update Method Signatures

```java
// TreeParser4Evoltree.java
public Optional<DefaultPhyNode> getTree(EvolTreeImportInfoBean object) throws Exception {
    int format = object.getNwkFormat();
    boolean removeWhitespace = object.isRemoveWhitespace();
    // ... use format in parsing
}
```

#### 4.2 Update Calling Code

Files that call `TreeParser4Evoltree.getTree()`:
- `TreeParser4MTV.java`
- Other tree-related parsers

### Phase 5: Testing

#### 5.1 Test Data Files

Create test files for each format in `~/.egps/bioData/example/newick_formats/`:

```
format0_example.nwk  # (A:0.1,(B:0.2,C:0.3)90:0.4);
format1_example.nwk  # (A:0.1,(B:0.2,C:0.3)NodeX:0.4);
format2_example.nwk  # (A:0.1,(B:0.2,C:0.3)90:0.4);
...
```

#### 5.2 Unit Tests

Location: `src/test/module/evoltreio/NewickFormatTest.java`

```java
public class NewickFormatTest {
    public static void main(String[] args) {
        testFormat0();
        testFormat1();
        // ... test all formats
    }

    private static void testFormat0() {
        String nwk = "(A:0.1,(B:0.2,C:0.3)90:0.4);";
        // Parse with format 0, verify support value is 90
    }
}
```

### Phase 6: Documentation

- [x] Update `manual_en.html` with format table
- [x] Update `manual_zh.html` with format table
- [ ] Add example files to distribution
- [ ] Add tooltips in GUI

## Implementation Status (Updated)

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1: Parameter Definition | ✅ Complete | `nwk.format` parameter added |
| Phase 2: Parser Modification | ✅ Complete | `FormatAwareNewickParser` created |
| Phase 3: Decoder Modifications | ✅ Complete | Strategy pattern with 10 format implementations |
| Phase 4: Integration | ✅ Complete | Integrated into `TreeParser4Evoltree` |
| Phase 5: Testing | ✅ Complete | 53 tests passing |
| Phase 6: Documentation | ✅ Mostly | Manual updated, example files pending |

### Files Created/Modified

**New files (parser package):**
- `src/module/evoltreio/parser/NodeParseStrategy.java` - Strategy interface
- `src/module/evoltreio/parser/AbstractNodeParseStrategy.java` - Abstract base
- `src/module/evoltreio/parser/NodeParseStrategyFactory.java` - Factory with 10 format implementations
- `src/module/evoltreio/parser/FormatAwareNewickParser.java` - Main parser

**New files (exception package):**
- `src/module/evoltreio/exception/NewickParseException.java` - Base exception
- `src/module/evoltreio/exception/NewickFormatException.java` - Invalid format
- `src/module/evoltreio/exception/NewickSyntaxException.java` - Syntax errors
- `src/module/evoltreio/exception/NewickNodeFormatException.java` - Node format errors
- `src/module/evoltreio/exception/NewickNumericException.java` - Numeric parsing errors

**Modified files:**
- `src/module/evoltreio/NewickFormatConfig.java` - Format configuration
- `src/module/evoltreio/EvolTreeImportInfoBean.java` - Added `nwkFormat` field
- `src/module/evoltreio/ParamsAssignerAndParser4PhyloTree.java` - Added parameter
- `src/module/evoltreio/TreeParser4Evoltree.java` - Uses FormatAwareNewickParser

**Test files:**
- `src/test/module/evoltreio/NewickFormatExceptionTest.java` - Exception tests (45 pass)
- `src/test/module/evoltreio/FormatAwareNewickParserTest.java` - Parser tests (53 pass)

## Implementation Priority

1. **High Priority**: Formats 0, 1 (most commonly used)
2. **Medium Priority**: Formats 4, 5, 8 (common simplified formats)
3. **Low Priority**: Formats 2, 3, 6, 7, 9 (less common)

## Dependencies

- `evoltree.phylogeny.NWKLeafCoderDecoder`
- `evoltree.phylogeny.NWKInternalCoderDecoder`
- `evoltree.struct.io.PrimaryNodeTreeDecoder`

These classes are in `dependency-egps/` JAR files. May need to:
1. Check if they already support format configuration
2. If not, create wrapper classes or modify source

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing parsing | High | Add format parameter with default value 0 |
| Complex format edge cases | Medium | Implement only commonly used formats first |
| Performance overhead | Low | Format config is lightweight |

## Timeline Estimate

- Phase 1 (Parameter Definition): Straightforward
- Phase 2 (Parser Modification): Depends on existing decoder flexibility
- Phase 3 (Decoder Modifications): Most complex, depends on dependency JAR access
- Phase 4 (Integration): Straightforward
- Phase 5 (Testing): Important but can be incremental
- Phase 6 (Documentation): Already started

## References

- [ETE3 Newick Parser Source](https://github.com/etetoolkit/ete/blob/ete3/ete3/parser/newick.py)
- [ETE Toolkit Tutorial](https://etetoolkit.org/docs/latest/tutorial/tutorial_trees.html)
- [NHX Format Specification](http://www.phylosoft.org/NHX/)
- [Newick Format Wikipedia](https://en.wikipedia.org/wiki/Newick_format)
