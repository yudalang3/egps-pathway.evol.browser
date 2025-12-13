package module.multiseq.deversitydescriptor;

import java.io.InputStream;

import egps2.frame.ModuleFace;
import egps2.EGPSProperties;
import egps2.modulei.IModuleLoader;
import egps2.modulei.ModuleVersion;
import egps2.EGPSProperties;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;
import egps2.EGPSProperties;

public class IndependentModuleLoader implements IModuleLoader{

	@Override
	public String getTabName() {
		return "Alignment diversity descriptor";
	}

	@Override
	public String getShortDescription() {
		return "Quick text display the diversity of each alignment. (inheritance from eGPS 1)";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/diversityDescriptor.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public ModuleFace getFace() {
		return new SimpleModuleMain(this);
	}

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(
				ModuleClassification.BYFUNCTIONALITY_SIMPLE_TOOLS_INDEX,
				ModuleClassification.BYAPPLICATION_EVOLUTION_INDEX,
				ModuleClassification.BYCOMPLEXITY_LEVEL_2_INDEX,
				ModuleClassification.BYDEPENDENCY_CROSS_MODULE_REFERENCED
		);
		return ret;
	}

	@Override
	public ModuleVersion getVersion() {
		return EGPSProperties.MAINFRAME_CORE_VERSION;
	}
}
