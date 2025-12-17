# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The file or dir in the .gitignore file is used to exclude files and directories from the project. Ignore them

## Project Overview

eGPS is a Java-based desktop application with a modular architecture. It's a Swing-based GUI application for genomics/bioinformatics purposes, built with a plugin-based system that allows modules to be dynamically loaded.
This is a concrete module for the eGPS project. The core dependent JARs are in `dependency-egps/egps-*`.

## Build and Development Commands

### Compilation
The project uses IntelliJ IDEA with JDK 25. The `.iml` file indicates the build output goes to `./out/production/egps-pathway.evol.browser`.

To compile manually:
```bash
javac -d ./out/production/egps-pathway.evol.browser -cp "dependency-egps/*" $(find src -name "*.java")
```

### Running the Application

**Development mode:**
```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher4Dev
```

**Production mode:**
```bash
java -cp "./out/production/egps-pathway.evol.browser:dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher
```

You can also launch specific modules directly by passing the fully qualified class name:
```bash
java -cp "./out/production/egps-pathway.evol.browser:dependency-egps/*" egps2.Launcher com.package.ModuleClassName
```

## Architecture

### Module System

The application uses a plugin-based module architecture centered around the `IModuleLoader` interface.

**Key interfaces:**
- `IModuleLoader` (egps2/modulei/IModuleLoader.java) - Core interface all modules must implement
- `ModuleFace` (egps2/frame/ModuleFace.java) - Abstract base class for module UI panels
- `IModuleFace` - Interface defining the visual module contract

**Module structure:**
Each module implements `IModuleLoader` and provides:
- `getTabName()` - Display name in the tab
- `getShortDescription()` - Module description
- `getFace()` - Returns the `ModuleFace` panel containing the module's UI
- `getCategory()` - Module classification by functionality, application, complexity, and dependency
- `getVersion()` - Returns ModuleVersion object (REQUIRED since v2.2) - See Module Versioning below
- Optional `getIcon()` - Returns IconBean for the module icon

**Example module location:**
`src/egps2/builtin/modules/filemanager/IndependentModuleLoader.java`

### Module Versioning (Since v2.2)

**All modules MUST implement `getVersion()` method** to provide version information following Semantic Versioning 2.0.0.

**For Mainframe core modules:**
Use the shared version from `EGPSProperties.MAINFRAME_CORE_VERSION`:
```java
import egps2.EGPSProperties;
import egps2.modulei.ModuleVersion;

@Override
public ModuleVersion getVersion() {
    return EGPSProperties.MAINFRAME_CORE_VERSION; // Currently 2.2.0
}
```

**For custom/plugin modules:**
Create your own version instance:
```java
import egps2.modulei.ModuleVersion;

@Override
public ModuleVersion getVersion() {
    return new ModuleVersion(1, 0, 0);
    //                       ^  ^  ^
    //                       |  |  +-- PATCH: Bug fixes
    //                       |  +-- MINOR: New features (backward compatible)
    //                       +-- MAJOR: Breaking changes
}
```

**Version comparison:**
```java
ModuleVersion v1 = new ModuleVersion(1, 5, 3);
ModuleVersion v2 = new ModuleVersion(2, 0, 0);

v2.isNewerThan(v1);        // true
v1.isCompatibleWith(v2);   // false (different major versions)
```

**See:** `docs/module&pluginSystem/MODULE_VERSIONING_SYSTEM.md` for complete documentation.

### Application Entry Points

**Main launcher:** `egps2.Launcher`
- Sets up locale, Look & Feel, fonts, and UI properties
- Initializes configuration in `~/.egps/` (EGPSProperties.PROPERTIES_DIR)
- Creates the main frame via `UnifiedAccessPoint.getInstanceFrame()`
- Processes command-line arguments to auto-load modules

**Development launcher:** `egps2.Launcher4Dev`
- Simply sets `Launcher.isDev = true` and delegates to `Launcher.main()`

**Unified access point:** `egps2.UnifiedAccessPoint`
- Central registry for the singleton `MyFrame` instance
- Manages post-initialization actions via `registerActionAfterMainFrame()`
- Provides `loadTheModuleFromIModuleLoader()` to dynamically load modules

### Directory Structure

**Source organization:**
- `src/egps2/` - Main application code
  - `builtin/modules/` - Built-in modules (CLI, filemanager, gallerymod, itoolmanager, largetextedi, lowtextedi, voice, bonus/modules)
  - `frame/` - Main frame and UI infrastructure
  - `modulei/` - Module interfaces and contracts
  - `panels/` - Reusable UI panels and dialogs
  - `plugin/` - Plugin loading system (fastmodtem, manager)
  - `utils/` - Utilities (common math, file filters, I/O, graphics)
- `src/egps/lnf/` - Custom Look & Feel implementations for various Swing components
- `src/com/raven/` - Alternative UI framework components (appears to be a dashboard/admin template)
- `dependency-egps/` - External JAR dependencies

**Key dependencies:**
- Swing/SwingX for UI
- Apache Commons (IO, Math3, Collections4, Compress, Lang3, CLI, Codec, Logging)
- iText for PDF generation
- MigLayout for layouts
- JIDE Common Layer
- Guava, FastJSON
- SLF4J for logging
- POI for Office file handling
- HTSJdk for genomics file formats
- Reflections (0.10.2) for module discovery and classpath scanning


### Configuration

User configuration stored in `~/.egps/`:
- `EGPSProperties.PROPERTIES_DIR` - Main config directory
- First-time launch detected when this directory doesn't exist
- `AutoConfigThePropertiesAction` handles initial setup

## Development Patterns

### Logging

**IMPORTANT: Always use SLF4J Logger for logging, NEVER use `System.out.println()` or `System.err.println()`.**

The project uses SLF4J for logging. All classes should use proper logging instead of console output.

**Correct usage:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger log = LoggerFactory.getLogger(MyClass.class);

    public void myMethod() {
        log.info("Informational message: {}", value);
        log.warn("Warning message: {}", issue);
        log.error("Error occurred: {}", e.getMessage());
        log.debug("Debug info: {}", debugData);
    }
}
```

**WRONG - Never do this:**
```java
System.out.println("Some message");  // ❌ WRONG
System.err.println("Error");         // ❌ WRONG
e.printStackTrace();                 // ❌ WRONG - use log.error() instead
```

**Why:**
- Logger output can be configured (level, format, destination)
- Console output cannot be controlled or filtered
- Logger supports parameterized messages (better performance)
- Professional applications use proper logging frameworks

### Creating a New Module

1. Implement `IModuleLoader` interface
2. Extend `ModuleFace` for the UI panel
3. Override required methods: `getTabName()`, `getShortDescription()`, `getFace()`, `getCategory()`
4. Optionally provide icon via `getIcon()`
5. Place in `src/egps2/builtin/modules/yourmodule/` or external JAR
6. Name the loader class `IndependentModuleLoader` by convention

### HTML Files for Help/Documentation

**IMPORTANT:** Swing's `JEditorPane` only supports **HTML 3.2** with limited CSS 1.0 support.

**Restrictions:**
- **NO HTML5 features** (semantic tags like `<section>`, `<article>`, `<nav>`, etc.)
- **NO modern CSS** (no `border-radius`, `box-shadow`, `flexbox`, `grid`, etc.)
- **NO CSS3 selectors** (no `:before`, `:after`, complex pseudo-classes)
- **Limited CSS properties:** Only basic `color`, `font-family`, `font-size`, `font-weight`, `text-align`, `background-color`, `border`, `margin`, `padding`
- **Use HTML 3.2 tables for layout** (not CSS positioning)
- **Inline styles or `<style>` tags** in `<head>` work, but limited to CSS 1.0 properties

**Allowed HTML 3.2 tags:**
```html
<html>, <head>, <title>, <body>, <h1>-<h6>, <p>, <br>, <hr>
<b>, <i>, <u>, <strong>, <em>, <font>, <blockquote>, <pre>, <code>
<table>, <tr>, <td>, <th>, <ul>, <ol>, <li>, <dl>, <dt>, <dd>
<a href>, <img src>
```

**Example of compliant HTML for JEditorPane:**
```html
<!DOCTYPE html>
<html>
<head>
<title>My Document</title>
</head>
<body>
<h2><font color="#2c3e50">Title</font></h2>
<table border="1" cellpadding="5" bgcolor="#f0f0f0">
<tr><td><b>Item</b></td><td>Description</td></tr>
</table>
<p><font color="#333333">Regular paragraph text.</font></p>
</body>
</html>
```

**When creating HTML files for Help menu or documentation:**
1. Test in JEditorPane, not in a web browser
2. Use tables for layout instead of CSS
3. Use `<font>` tag for colors and sizes
4. Keep styling minimal and inline
5. Avoid external CSS files

### Threading

- Main UI operations use `SwingUtilities.invokeLater()` for EDT safety
- Background tasks should not block EDT
- Post-initialization actions registered via `UnifiedAccessPoint.registerActionAfterMainFrame()`

**CRITICAL RULE: Never perform time-consuming operations on the EDT thread.**

The Event Dispatch Thread (EDT) is responsible for handling all GUI events and rendering. Blocking the EDT causes the UI to freeze and creates a poor user experience.

**What must run on EDT:**
- All GUI component creation and modification (setText, setEnabled, addComponent, etc.)
- Showing dialogs (JOptionPane, JDialog, etc.)
- Updating table models, list models, or any Swing model
- Repainting and revalidating components
- Any Swing component method calls

**What must NOT run on EDT:**
- File I/O operations (reading/writing files)
- Network operations (HTTP requests, socket operations)
- Database queries
- Complex computations or data processing
- Module scanning, class loading, or reflection operations
- Any operation that takes more than ~100ms

**Checking if you're on EDT:**
```java
if (SwingUtilities.isEventDispatchThread()) {
    // Safe to update GUI directly
    label.setText("Updated");
} else {
    // Must dispatch to EDT
    SwingUtilities.invokeLater(() -> label.setText("Updated"));
}
```

## Important Notes

- The codebase contains both English and Chinese comments
- Locale is forced to English on startup: `Locale.setDefault(Locale.ENGLISH)`
- The project is not set up with Maven or Gradle - it uses manual dependency management via JAR files
- UTF-8 encoding is enforced: `System.setProperty("file.encoding", "UTF-8")`
- **Test files must be placed in `src/test/` directory**, not in the main source tree (`src/egps2/` or `src/egps/`)
- No formal unit testing framework (like JUnit); tests are standalone Java classes with main methods
