package module.evolview.pathwaybrowser.io;

import module.evolview.moderntreeviewer.io.MTVImportInforBean;

import java.util.List;

public class ImporterBean4PathwayFamilyBrowser extends MTVImportInforBean {

	String pathwayComponentCountPath;
	String speciesInfoPath;
	String speciesTraitPath;
	List<String> pathwayGalleryPaths;
	String geneNameSeparater;
	String componentsInfoPath;
	String geneColumnName;
	String categoryColumnName;
	String pathwayDetailsFigure;
	String pathwayStatisticsFigure;
	String evolutionarySelectionFigurePath;


    public ImporterBean4PathwayFamilyBrowser(MTVImportInforBean object) {

        this.setInput_nwk_path(object.getInput_nwk_path());
        this.setShowLeafLabel(object.isShowLeafLabel());
        this.setShouldLeafNameRightAlign(object.isShouldLeafNameRightAlign());
        this.setNeedReverseAxisBar(object.isNeedReverseAxisBar());
        this.setWhetherHeightScaleOnMouseWheel(object.isWhetherHeightScaleOnMouseWheel());
        this.setWhetherWidthScaleOnMouseWheel(object.isWhetherWidthScaleOnMouseWheel());

        this.blank_space = object.getBlank_space();
        this.titleString = object.getTitleString();
        this.setRemoveWhitespace(object.isRemoveWhitespace());

        branchLengthUnit = object.getBranchLengthUnit();

        this.defaultFont = object.getDefaultFont();
        this.defaultTitleFont = object.getDefaultTitleFont();


    }


	public String getGeneNameSeparator() {
		return geneNameSeparater;
	}

	public String getComponentsInfoPath() {
		return componentsInfoPath;
	}

	public String getGeneColumnName() {
		return geneColumnName;
	}

	public String getCategoryColumnName() {
		return categoryColumnName;
	}

	public String getPathwayDetailsFigure() {
		return pathwayDetailsFigure;
	}

	public String getPathwayStatisticsFigure() {
		return pathwayStatisticsFigure;
	}

	public String getEvolutionarySelectionFigurePath() {
		return evolutionarySelectionFigurePath;
	}

	public String getSpeciesInfoPath() {
		return speciesInfoPath;
	}

	public List<String> getPathwayGalleryPaths() {
		return pathwayGalleryPaths;
	}

	public String getPathwayComponentCountPath() {
		return pathwayComponentCountPath;
	}

	public String getSpeciesTraitPath() {
		return speciesTraitPath;
	}
}
