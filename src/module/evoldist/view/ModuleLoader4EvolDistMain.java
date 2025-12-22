package module.evoldist.view;

import egps2.EGPSProperties;
import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;
import module.evoldist.view.gui.DistanceMatrixViewerMain;

import java.io.InputStream;

public class ModuleLoader4EvolDistMain implements IModuleLoader{

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(
				
				ModuleClassification.BYFUNCTIONALITY_PRIMITIVE_VISUALIZATION_INDEX,
				ModuleClassification.BYAPPLICATION_EVOLUTION_INDEX,
				ModuleClassification.BYCOMPLEXITY_LEVEL_1_INDEX,
				ModuleClassification.BYDEPENDENCY_ONLY_EMPLOY_CONTAINER
		);
		return ret;
	}

	@Override
	public String getShortDescription() {
		return "Visualize the trigangle distance matirx.";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/evolDistView.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public ModuleFace getFace() {
		return new DistanceMatrixViewerMain(this);
	}

	@Override
	public String getTabName() {
		return "Evolutionary dist view";
	}

    @Override
    public ModuleVersion getVersion() {
        return EGPSProperties.MAINFRAME_CORE_VERSION;
    }

}
