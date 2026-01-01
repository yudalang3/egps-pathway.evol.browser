# eGPS Pathway Evolution Browser

A comprehensive **Pathway evolutionary browser** module suite for evolutionary analysis, multiple sequence alignment (MSA), phylogenetic tree construction, and pathway evolution visualization.

![通路进化浏览器 截图](https://github.com/yudalang3/egps-pathway.evol.browser/blob/main/snapshot/ScreenShot_PEBrowser.png?raw=true)


## Overview

**eGPS Pathway Evolution Browser** is a plugin module for the eGPS2 platform that provides 18 integrated bioinformatics tools. The module suite supports a complete workflow from gene sequence retrieval to phylogenetic tree construction and evolutionary distance analysis.

### Key Features

- **Pathway evolutionary browser**: Interactive visualization with phylogenetic context of biological pathways
- **Phylogenetic workflows**: Build trees from MSA/MAF/distance matrices
- **MSA toolkit**: MAFFT alignment, trimming, and multi-format viewers
- **Evolutionary distance analysis**: Distance matrix computation and heatmap visualization

## System Requirements

- **Java**: JDK 25 or higher
- **Memory**: Minimum 4GB heap (recommended: `-Xmx8g`)
- **Platform**: Windows, macOS, Linux

### Optional Dependencies

- **MAFFT**: Required for multiple sequence alignment operations (external dependency)
  - The system can auto-detect MAFFT installation paths
  - Supports custom MAFFT configuration

## Project Structure

```
egps-pathway.evol.browser/
├── src/module/                    # Source code modules
│   ├── ambigbse/                 # Ambiguous nucleotide tools
│   ├── evoldist/                 # Evolutionary distance (3 modules)
│   ├── evolview/                 # Evolution visualization (3 modules)
│   ├── multiseq/                 # Multiple sequence alignment (6 modules)
│   ├── pill/                     # Pathway illuminator
│   ├── treebuilder/              # Tree construction (4 modules)
│   ├── evoltrepipline/          # Shared pipeline components
│   └── [other utilities]/
├── dependency-egps/              # External JAR dependencies
├── docs/                         # Documentation
├── out/                          # Build output
├── CLAUDE.md                    # Developer guidance
└── compile.sh                   # Build script
```

## Build & Compilation

### Automatic Compilation

```bash
bash compile.sh
```

### Manual Compilation

```bash
javac -d ./out/production/egps-pathway.evol.browser \
  -cp "dependency-egps/*" \
  $(find src -name "*.java")
```

### Build Output

Compiled classes: `./out/production/egps-pathway.evol.browser`

## Running the Application

### Development Mode

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher4Dev
```

### Production Mode

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher
```

### Launch Specific Module

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher com.package.ModuleClassName
```

## Modules

The suite contains 18 modules organized in 9 packages:

### Sequence Tools
- **ambigbse**: Converts IUPAC ambiguous nucleotide codes (R, Y, M, K, S, W, H, B, V, D, N) to concrete sequences and reverse complements

### Evolutionary Distance Analysis (3 modules)
- **evoldist/gene2dist**: Retrieve MSA from Ensembl/eGPS cloud sequences and compute evolutionary distance
- **evoldist/msa2distview**: Distance matrix calculation from MSA files (JC69, K2P, Tamura-Nei)
- **evoldist/view**: Heatmap visualization of evolutionary distance matrices

### Evolutionary Visualization (3 modules)
- **evolview/gfamily**: Interactive gene family browser with phylogenetic trees and sequence structures
- **evolview/moderntreeviewer**: Modern phylogenetic tree viewer with multiple layouts
- **evolview/pathwaybrowser**: Biological pathway visualization with phylogenetic context

### Multiple Sequence Alignment (6 modules)
- **multiseq/aligner**: MAFFT wrapper for fast multiple sequence alignment
- **multiseq/alignerwithref**: Reference-based MSA with MAFFT
- **multiseq/alignment/trimmer**: Trim MSA based on reference sequence
- **multiseq/alignment/view**: Interactive MSA viewer (ClustalW, FASTA, PHYLIP, NEXUS, etc.)
- **multiseq/deversitydescriptor**: Text-based alignment diversity metrics
- **multiseq/gene2msa**: Retrieve gene sequences from Ensembl/eGPS cloud and generate MSA

### Pathway Tools
- **pill**: Pathway diagram drawing and editing tool (Pathway Illuminator)

### Tree Construction (4 modules)
- **treebuilder/gene2tree**: Obtain MSA from Ensembl/UCSC and construct gene phylogenetic trees
- **treebuilder/frommsa**: Construct phylogenetic trees from multiple sequence alignments
- **treebuilder/frommaf**: Construct phylogenetic trees from MAF (Multiple Alignment Format) files
- **treebuilder/fromdist**: Construct phylogenetic trees from evolutionary distance matrices

## Architecture

### Module System

The application follows a modular architecture based on the `IModuleLoader` interface:

- Each module implements `IModuleLoader` interface
- UI panels extend `ModuleFace` abstract class
- Dynamic module loading and discovery

### Module Versioning

All modules implement semantic versioning (Major.Minor.Patch):

```java
@Override
public ModuleVersion getVersion() {
    return new ModuleVersion(1, 0, 0);
}
```

### Dependency Management

The codebase maintains a **Directed Acyclic Graph (DAG)** architecture—no circular dependencies between modules.

## Configuration

User configuration is stored in `~/.egps/`:
- **First-time launch**: Creates configuration directory structure
- **Module discovery**: Automatic classpath scanning and plugin loading
- **MAFFT integration**: Auto-detection of MAFFT installation paths

## Development

### Logging

**Important**: Always use SLF4J Logger instead of `System.out.println()`:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("Message: {}", value);
log.error("Error: {}", exception.getMessage());
```

### UTF-8 Encoding

The system enforces UTF-8 encoding:
```
System.setProperty("file.encoding", "UTF-8")
```

### HTML Documentation

For Swing `JEditorPane` support, use **HTML 3.2** with CSS 1.0:
- No HTML5 semantic tags
- No CSS3 features (flexbox, grid, etc.)
- Use inline styles or `<style>` tags
- Use HTML tables for layout

## Dependencies

**Key libraries:**
- Swing/SwingX - GUI framework
- Apache Commons (IO, Math3, Collections4, etc.)
- iText - PDF generation
- MigLayout - UI layout
- HTSJdk - Genomics file formats
- FastJSON - JSON processing
- SLF4J - Logging framework
- POI - Office file handling

See `dependency-egps/` for complete list of JARs.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.

## Platform Support

- **Windows**: Fully supported
- **macOS**: Fully supported
- **Linux**: Fully supported

## Documentation

For detailed developer information, see:
- [CLAUDE.md](CLAUDE.md) - Developer guidance and architecture details
- [modules_we_have.md](docs/modules_we_have.md) - Detailed module specifications
- [docs/](docs/) - Additional documentation

## Contributing

When developing or extending modules:

1. Implement `IModuleLoader` interface
2. Extend `ModuleFace` for UI
3. Follow semantic versioning in `getVersion()`
4. Maintain DAG architecture (no circular dependencies)
5. Use SLF4J for logging
6. Add HTML 3.2-compliant documentation

## Contributors

- codex
- Claude

## Support

For issues, questions, or contributions, please refer to the project documentation in the [docs/](docs/) directory.

---

**Project**: eGPS2 Pathway Evolution Browser
**Version**: 2.2.0
**Last Updated**: 2026-01-01
