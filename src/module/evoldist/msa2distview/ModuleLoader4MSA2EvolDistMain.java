package module.evoldist.msa2distview;

import egps2.EGPSProperties;
import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;

import java.io.InputStream;

public class ModuleLoader4MSA2EvolDistMain implements IModuleLoader{

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(
				
				ModuleClassification.BYFUNCTIONALITY_PROFESSIONAL_COMPUTATION_INDEX,
				ModuleClassification.BYAPPLICATION_EVOLUTION_INDEX,
				ModuleClassification.BYCOMPLEXITY_LEVEL_1_INDEX,
				ModuleClassification.BYDEPENDENCY_CROSS_MODULE_REFERENCED
		);
		return ret;
	}

	@Override
	public String getShortDescription() {
		return "Compute the evolutionary distance from the MSA.";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/msa2dist.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public ModuleFace getFace() {
		return new MSA2DistanceMatrixViewerMain(this);
	}

	@Override
	public String getTabName() {
		return "Distance calculator : from MSA";
	}

    @Override
    public ModuleVersion getVersion() {
        return EGPSProperties.MAINFRAME_CORE_VERSION;
    }

}
