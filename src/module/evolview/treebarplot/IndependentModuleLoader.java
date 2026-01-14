package module.evolview.treebarplot;

import egps2.EGPSProperties;
import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;

import java.io.InputStream;

public class IndependentModuleLoader implements IModuleLoader{

	@Override
	public String getTabName() {
		return "Tree with barplot";
	}

	@Override
	public String getShortDescription() {
		return "Draw tree with barplot powered by the VOICE input frame and eGPS features.";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/treeBarPlot.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public ModuleVersion getVersion() {
		return EGPSProperties.MAINFRAME_CORE_VERSION;
	}


	@Override
	public ModuleFace getFace() {
		return new GuiMain(this);
	}

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(
				ModuleClassification.BYFUNCTIONALITY_PRIMITIVE_VISUALIZATION_INDEX,
				ModuleClassification.BYAPPLICATION_EVOLUTION_INDEX,
				ModuleClassification.BYCOMPLEXITY_LEVEL_4_INDEX,
				ModuleClassification.BYDEPENDENCY_CROSS_MODULE_REFERENCED
		);
		return ret;
	}
}
