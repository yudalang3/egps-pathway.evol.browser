# Repository Guidelines

## Project Overview

- eGPS Pathway Browser is a Swing‑based plugin module for the eGPS genomics platform.
- This repo is built and packaged as a JAR that the mainframe loads; core JARs are in `dependency-egps/egps-*` (not tracked).

## Project Structure & Module Organization

- Java sources live under `src/module/**`, organized by module packages (e.g., `evolview/pathwaybrowser`, `treebuilder/frommsa`).
- Resources (icons, `.properties`, `.json`, `.html`, example `.fas/.fasta`) are co‑located with code inside each module folder.
- Build output goes to `out/production/egps-pathway.evol.browser/`.
- Do not commit generated artifacts or local deps: `out/`, `dependency-egps/`, `backup/`, `.idea/`.

## Build, Run, and Packaging Commands

- `./compile.sh` — cleans `out/`, compiles all `src/**/*.java` with `javac`, then copies resources into the output tree.
- Run in dev mode:
  `java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher4Dev`
- Run in production mode:
  `java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher`
- Run a specific module by loader class:
  `java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" egps2.Launcher module.somepkg.IndependentModuleLoader`
- `./deploy_src2jar.bash` — jars the compiled output; adjust `destination=` for your machine.

## Module & Coding Conventions

- Modules implement `egps2.modulei.IModuleLoader` and provide a `ModuleFace` UI.
- Required methods: `getTabName()`, `getShortDescription()`, `getFace()`, `getCategory()`, and `getVersion()`.
- Since eGPS v2.2, `getVersion()` must return a `ModuleVersion`; for core‑like modules, return `EGPSProperties.MAINFRAME_CORE_VERSION`.
- Follow existing Java style: tab indentation, lowercase packages under `module.*`, PascalCase classes, loader names like `IndependentModuleLoader` or `ModuleLoader4<Feature>`.
- Logging: always use SLF4J (`LoggerFactory.getLogger(...)`); never use `System.out/err` or `printStackTrace()`.
- GUI fonts: prefer `UnifiedAccessPoint.getLaunchProperty().getDefaultFont()` (body) and `UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont()` (titles/headers) so users can adjust fonts via global preferences; avoid hardcoding font families/sizes.

## Testing Guidelines

- No automated test suite in this module today.
- Validate changes with `./compile.sh`, then launch the app/module.
- If adding tests, use `src/test/...` and name files `*Test.java`.

## Commit & Pull Request Guidelines

- Use short, imperative commit messages (e.g., “Add treebuilder”, “Fix tree builder GUI problems”).
- PRs should include a clear description, affected module paths, and screenshots/GIFs for UI changes.
