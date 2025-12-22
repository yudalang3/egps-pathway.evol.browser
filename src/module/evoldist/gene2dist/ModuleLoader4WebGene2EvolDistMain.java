package module.evoldist.gene2dist;

import egps2.EGPSProperties;
import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;

import java.io.InputStream;

public class ModuleLoader4WebGene2EvolDistMain implements IModuleLoader {

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(

				ModuleClassification.BYFUNCTIONALITY_PROFESSIONAL_COMPUTATION_INDEX,
				ModuleClassification.BYAPPLICATION_EVOLUTION_INDEX, ModuleClassification.BYCOMPLEXITY_LEVEL_2_INDEX,
				ModuleClassification.BYDEPENDENCY_CROSS_MODULE_REFERENCED);
		return ret;
	}

	@Override
	public String getShortDescription() {
		return "Obtain the MSA from the Ensembel RESTful service and compute the evolutionary distance from the MSA.  (inheritance from eGPS 1)";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/gene2dist.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public ModuleFace getFace() {
		return new DistMatrixViewMainForGene2Dist(this);
	}

	@Override
	public String getTabName() {
		return "Gene to evolutionary distance";
	}

    @Override
    public ModuleVersion getVersion() {
        return EGPSProperties.MAINFRAME_CORE_VERSION;
    }


}
