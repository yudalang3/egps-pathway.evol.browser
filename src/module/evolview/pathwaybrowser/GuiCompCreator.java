package module.evolview.pathwaybrowser;

import com.google.common.base.Strings;
import module.evolview.pathwaybrowser.gui.analysis.panel.PathwayComponentPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.PathwayGalleryPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.SpeciesInfoPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.SpeciesTraitPanel;
import module.evolview.pathwaybrowser.io.ImporterBean4PathwayFamilyBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for creating GUI components for the Pathway Browser module.
 * 
 * This class extracts the panel creation logic from PathwayFamilyMainFace
 * to improve code organization and readability.
 */
public class GuiCompCreator {

	private static final Logger log = LoggerFactory.getLogger(GuiCompCreator.class);
	private final PathwayBrowserController controller;

	public GuiCompCreator(PathwayBrowserController controller) {
		this.controller = controller;
	}

	/**
	 * Check if the path is provided and valid.
	 *
	 * @param path The path to check
	 * @param parameterName The name of the parameter (for error messages)
	 * @return true if the path is not provided (null, empty, or false-like values), false if path is provided
	 * @throws IllegalArgumentException if path is provided but file does not exist
	 */
	private boolean isPathNotProvided(String path, String parameterName) {
		// Check if path is not provided (null, empty, or false-like values)
		if (Strings.isNullOrEmpty(path) ||
			path.equalsIgnoreCase("false") ||
			path.equalsIgnoreCase("f") ||
			path.equalsIgnoreCase("null")) {
			return true;
		}

		// Path is provided, check if file exists
		File file = new File(path);
		if (!file.exists()) {
			throw new IllegalArgumentException(
				String.format("File not found for %s: %s", parameterName, path));
		}

		return false;
	}

	/**
	 * Validate a list of file paths.
	 *
	 * @param paths The list of paths to validate
	 * @param parameterName The name of the parameter (for error messages)
	 * @return A list of valid file paths (excluding null/empty/false-like values)
	 * @throws IllegalArgumentException if any provided path does not exist
	 */
	private List<String> validatePathList(List<String> paths, String parameterName) {
		if (paths == null || paths.isEmpty()) {
			return List.of();
		}

		List<String> validPaths = new java.util.ArrayList<>();
		for (int i = 0; i < paths.size(); i++) {
			String path = paths.get(i);

			// Skip if not provided
			if (Strings.isNullOrEmpty(path) ||
				path.equalsIgnoreCase("false") ||
				path.equalsIgnoreCase("f") ||
				path.equalsIgnoreCase("null")) {
				continue;
			}

			// Path is provided, check if file exists
			File file = new File(path);
			if (!file.exists()) {
				throw new IllegalArgumentException(
					String.format("File not found for %s[%d]: %s", parameterName, i, path));
			}

			validPaths.add(path);
		}

		return validPaths;
	}

	public Optional<PathwayComponentPanel> createPathwayComponentPanel(
			ImporterBean4PathwayFamilyBrowser geneData) {

		String pathwayDetailsFigure = geneData.getPathwayComponentCountPath();
		if (isPathNotProvided(pathwayDetailsFigure, "pathwayComponentCountPath")) {
			return Optional.empty();
		}

		PathwayComponentPanel pathwayDrawDemo = new PathwayComponentPanel(controller,
				pathwayDetailsFigure);
		return Optional.of(pathwayDrawDemo);
	}


	public Optional<SpeciesInfoPanel> createSpeciesInfoPanel(ImporterBean4PathwayFamilyBrowser geneData) {
		String speciesInfoPath = geneData.getSpeciesInfoPath();
		if (isPathNotProvided(speciesInfoPath, "speciesInfoPath")) {
			return Optional.empty();
		}

		SpeciesInfoPanel speciesPanel = new SpeciesInfoPanel(controller, new File(speciesInfoPath));
		return Optional.of(speciesPanel);
	}

	public Optional<PathwayGalleryPanel> createPathwayGalleryPanel(PathwayBrowserController controller, ImporterBean4PathwayFamilyBrowser geneData) {
		List<String> pathwayGalleryPaths = geneData.getPathwayGalleryPaths();
		List<String> validPaths = validatePathList(pathwayGalleryPaths, "pathwayGalleryPaths");

		if (validPaths.isEmpty()){
			return Optional.empty();
		}

		PathwayGalleryPanel pathwayGalleryPanel = new PathwayGalleryPanel(controller, validPaths);
		return Optional.of(pathwayGalleryPanel);
	}

	public Optional<SpeciesTraitPanel> createSpeciesTraitPanel(ImporterBean4PathwayFamilyBrowser geneData) {
		String speciesTraitPath = geneData.getSpeciesTraitPath();
		if (isPathNotProvided(speciesTraitPath, "speciesTraitPath")) {
			return Optional.empty();
		}

		SpeciesTraitPanel speciesTraitPanel = new SpeciesTraitPanel(controller, speciesTraitPath);
		return Optional.of(speciesTraitPanel);
	}

	/**
	 * Data container for parsed component information.
	 */
	public static class ComponentData {
		public final List<String> geneList;
		public final List<String> categoryList;
		public final Map<String, List<Short>> species2geneCountMap;

		public ComponentData(List<String> geneList, List<String> categoryList,
							 Map<String, List<Short>> species2geneCountMap) {
			this.geneList = geneList;
			this.categoryList = categoryList;
			this.species2geneCountMap = species2geneCountMap;
		}
	}
}
