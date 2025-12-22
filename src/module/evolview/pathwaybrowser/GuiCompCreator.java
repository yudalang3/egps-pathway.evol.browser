package module.evolview.pathwaybrowser;

import com.google.common.collect.Maps;
import module.evolview.pathwaybrowser.gui.analysis.panel.PathwayDetailsPanel;
import module.evolview.pathwaybrowser.gui.PathwayStatisticsPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.PathwayGalleryPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.SpeciesPanel;
import module.evolview.pathwaybrowser.io.ImporterBean4PathwayFamilyBrowser;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tsv.io.TSVReader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	 * Create and configure the Pathway Details panel with its scroll pane and tab.
	 *
	 * @param geneData Gene family data containing pathway figures and component information
	 * @return
	 */
	public Optional<PathwayDetailsPanel> createPathwayDetailsPanel(
			ImporterBean4PathwayFamilyBrowser geneData) {

		String pathwayDetailsFigure = geneData.getPathwayDetailsFigure();
		File file = new File(pathwayDetailsFigure);
		if (!file.exists()){
			log.warn("Pathway Details Figure not found: {}", pathwayDetailsFigure);
			return Optional.empty();
		}

		PathwayDetailsPanel pathwayDrawDemo = new PathwayDetailsPanel(controller,
				geneData.getPathwayDetailsFigure(),
				geneData.getGeneNameSeparator().charAt(0));
		return Optional.of(pathwayDrawDemo);
	}

	/**
	 * Create and configure the Pathway Statistics panel with its scroll pane and tab.
	 *
	 * @param geneData                        Gene family data containing pathway figures and component information
	 * @param categoryList                    List of category names
	 * @param species2geneCountMap            Map of species names to gene counts
	 * @return
	 */
	public PathwayStatisticsPanel createPathwayStatisticsPanel(
			ImporterBean4PathwayFamilyBrowser geneData,
			List<String> categoryList,
			Map<String, List<Short>> species2geneCountMap) {

		PathwayStatisticsPanel pathwayStageStatistics = new PathwayStatisticsPanel(
				geneData.getPathwayStatisticsFigure(),
				geneData.getCategoryColumnName());
		pathwayStageStatistics.setSpeciesCategory2CompMaps(categoryList, species2geneCountMap);

		return pathwayStageStatistics;
	}


	/**
	 * Parse component information from TSV file and convert to numeric format.
	 *
	 * @param componentsInfoPath Path to the TSV file containing component information
	 * @param geneColumnName Name of the gene column in the TSV file
	 * @param categoryColumnName Name of the category column in the TSV file
	 * @return ComponentData object containing parsed gene list, category list, and species-to-count mapping
	 * @throws IOException if the file cannot be read
	 */
	public static ComponentData parseComponentInfo(
			String componentsInfoPath,
			String geneColumnName,
			String categoryColumnName) throws IOException {

		Map<String, List<String>> tableAsKey2ListMap = TSVReader.readAsKey2ListMap(componentsInfoPath);

		// Extract gene and category lists
		List<String> geneList = tableAsKey2ListMap.remove(geneColumnName);
		List<String> categoryList = tableAsKey2ListMap.remove(categoryColumnName);

		// Convert gene count data to numeric format
		Map<String, List<Short>> species2geneCountMap = Maps.newHashMap();
		for (Entry<String, List<String>> entry : tableAsKey2ListMap.entrySet()) {
			List<String> value = entry.getValue();
			List<Short> array = Lists.newArrayList();
			for (String string : value) {
				array.add(Short.valueOf(string));
			}
			species2geneCountMap.put(entry.getKey(), array);
		}

		return new ComponentData(geneList, categoryList, species2geneCountMap);
	}

	public Optional<SpeciesPanel> createSpeciesInfoPanel(ImporterBean4PathwayFamilyBrowser geneData) {
		String speciesInfoPath = geneData.getSpeciesInfoPath();
		File file = new File(speciesInfoPath);
		if (!file.exists()){
			return Optional.empty();
		}
		
		SpeciesPanel speciesPanel = new SpeciesPanel(controller, file);
		return Optional.of(speciesPanel);
	}

	public Optional<PathwayGalleryPanel> createPathwayGalleryPanel(PathwayBrowserController controller, ImporterBean4PathwayFamilyBrowser geneData) {
		List<String> pathwayGalleryPaths = geneData.getPathwayGalleryPaths();
		if (pathwayGalleryPaths.isEmpty()){
			return Optional.empty();
		}


		PathwayGalleryPanel pathwayGalleryPanel = new PathwayGalleryPanel(controller, pathwayGalleryPaths);
		return Optional.of(pathwayGalleryPanel);

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
