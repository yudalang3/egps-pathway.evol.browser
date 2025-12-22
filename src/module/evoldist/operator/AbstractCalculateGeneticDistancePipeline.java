/**  
* <p>Title: AbstructBuildTreeCommon.java</p>  
* <p>Description: </p>  
* <p>Copyright: Copyright (c) 2017, PICB</p>  
* <p>Owner: http://www.picb.ac.cn/evolgen/</p>  
* @author yudalang 
* @date 2018骞�8鏈�26鏃�  
* @version 1.0  
*/
package module.evoldist.operator;

import egps2.modulei.RunningTask;
import module.evoldist.operator.pairdist.DistParameterLevel1;
import module.evoldist.operator.pairdist.DistParameterLevel2;
import module.evoldist.operator.pairdist.DistParameterLevel41;
import module.evoldist.operator.pairdist.DistParameterLevel42;
import module.evoltrepipline.ConstantNameClass_EvolutionaryDistance;
import module.evoltrepipline.IPairwiseEvolutionaryDistance;
import module.evoltrepipline.TreeParameterHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Title: The abstract class for calculating genetic distance!
 * </p>
 * <p>
 * Description: Common used methods in pairwise evolutionary or genetic distances !
 * </p>
 * 
 * @author yudalang
 * @date 2019-8-12
 * 
 * @date 2024-04-29 :ydl modification
 */
public abstract class AbstractCalculateGeneticDistancePipeline  implements RunningTask{

	protected DistanceCalculateor distanceCalculator;
	protected BootstrapDistCalculator<?> bootstrap;
	
	protected List<String> seqs;
	protected List<String> seqNames;
	protected String tabName;
	protected String filePathHeader = "debug";
	protected final Map<String, String> settingValue;

	// Following codes used for saving temporary variables
	protected boolean ifBootStrap = false;
	protected int bootStrapTimes = 0;

	
	protected BufferedWriter bfWriter;

	public AbstractCalculateGeneticDistancePipeline() {
		this(new TreeParameterHandler().getBuildTreeParametersMap());
	}
	public AbstractCalculateGeneticDistancePipeline( Map<String, String> settingValue) {
		this.settingValue = settingValue;
//		 if we need to use bootstrap
		ConstantNameClass_EvolutionaryDistance cc = new ConstantNameClass_EvolutionaryDistance();
		String bootstrapValueInP = settingValue.get(cc.label1_varianEstimateMethod);
		if (bootstrapValueInP.contains(cc.varianEstimateMethod_value2_bs)) {
			ifBootStrap = true;
			String bootstrapValueString = settingValue.get(cc.label2_numOfBSRep_index);
			bootStrapTimes = Integer.parseInt(bootstrapValueString);
		}
	}

	/**
	 * This is the tab name of Biomain frame's tab!
	 * 
	 * @author yudalang
	 */
	public void setTabName(String tabName) {
		this.tabName = tabName;
	}

	/**
	 * Don't forget to set seqs and seqNames
	 * 
	 * @author yudalang
	 * @throws Exception 
	 */
	protected void incorporateParametersAndPackageCalculatorInstance() throws Exception {
		if (ifBootStrap) {
			String distName = settingValue.get(new ConstantNameClass_EvolutionaryDistance().label1_modelOrMethod);
			switch (distName) {

			case IPairwiseEvolutionaryDistance.NUM_OF_DIFF:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel1>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.P_DISTANCE:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel1>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.JC69_MODEL:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel1>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.K2P_MODEL:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel2>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.T3P_MODEL:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel2>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.TAM_NEI_MODEL:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel41>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.TAJ_NEI_MODEL:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel42>(seqs, seqNames);
				break;
			case IPairwiseEvolutionaryDistance.RATE_OF_TRANS:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel2>(seqs, seqNames);
				break;
			default:
				bootstrap = new BootstrapDistCalculator<DistParameterLevel2>(seqs, seqNames);
				break;
			}

			DistanceParameterConfigurer.configureBootstrapDistCalculator(bootstrap, settingValue);
			bootstrap.setbWriter(new File(filePathHeader + "/distBootstrap_" + bootStrapTimes + ".txt"));
			bootstrap.initialize();
		} else {
			// Not use bootstrap
			distanceCalculator = new DistanceCalculateor(seqs,seqNames);
			DistanceParameterConfigurer.configureDistanceCalculator(distanceCalculator, settingValue);
		}
	}

	public void setSeq_names(List<String> seq_names) {
		this.seqNames = seq_names;
	}

	public void setSeqs(List<String> seqs) {
		this.seqs = seqs;
	}
}




