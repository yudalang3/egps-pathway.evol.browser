/**  
* <p>Title: AbstructBuildTreeCommon.java</p>  
* <p>Description: </p>  
* <p>Copyright: Copyright (c) 2017, PICB</p>  
* <p>Owner: http://www.picb.ac.cn/evolgen/</p>  
* @author yudalang 
* @version 1.0  
*/
package module.remnant.treeoperator;

import java.util.Map;

import module.remnant.treeoperator.dmMethod.BuilderBootstrapTree4MSA;
import module.remnant.treeoperator.dmMethod.BuilderSinglePhyloTree;
import module.remnant.treeoperator.reconAlgo.NJ;
import module.remnant.treeoperator.reconAlgo.SwiftNJ;
import module.remnant.treeoperator.reconAlgo.TreeReconMethod;
import module.remnant.treeoperator.reconAlgo.Upgma;
import module.evoldist.operator.AbstractCalculateGeneticDistancePipeline;
import module.evoldist.operator.DistanceParameterConfigurer;
import module.evolview.model.tree.GraphicsNode;
import module.evoltrepipline.ConstantNameClass_TreeBuildMethod;

/**
 * <p>
 * Title: AbstructBuildTreeCommon
 * </p>
 * <p>
 * Description: Common used methods in phylogenetic tree reconstruction
 * </p>
 * 
 * @author yudalang
 * @date 2018-8-26
 * 
 * @date 2024-04-29 :ydl modification
 */
public abstract class AbstractDistanceBasedTreePipeline extends AbstractCalculateGeneticDistancePipeline {

	protected BuilderBootstrapTree4MSA<?> treeBuild_instance_BS;
	protected BuilderSinglePhyloTree treeBuild_instance;

	public AbstractDistanceBasedTreePipeline() {
		super();
	}
	protected AbstractDistanceBasedTreePipeline(Map<String, String> settingValue) {
		super(settingValue);
	}

	/**
	 * Don't forget to set seqs and seqNames
	 *
	 * Inlined tree reconstruction method selection from ParameterAssigner.parameterFactor()
	 * to break circular dependency (remnant ↔ evoltrepipline)
	 *
	 * @author yudalang
	 */
	protected void incorporateParametersAndPackageBuildPhylogeneticTreeInstance() {
		ConstantNameClass_TreeBuildMethod cc = new ConstantNameClass_TreeBuildMethod();
		String treeBuildMethod = settingValue.get(cc.label1_treeBuildMethod);

		// Create tree reconstruction method based on settings
		TreeReconMethod tReconMethod = null;
		if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(cc.treeBuildMethod_value1_NJ)) {
			tReconMethod = new NJ();
		} else if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(cc.treeBuildMethod_value2_SNJ)) {
			tReconMethod = new SwiftNJ();
		} else if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(cc.treeBuildMethod_value3_UPGMA)) {
			tReconMethod = new Upgma();
		} else {
			tReconMethod = new NJ(); // default
		}

		if (ifBootStrap) {
			treeBuild_instance_BS = new BuilderBootstrapTree4MSA<>();
			treeBuild_instance_BS.setBootTimes(bootStrapTimes);
			// Configure distance calculator using DistanceParameterConfigurer (moved from evoltrepipline to break circular dependency)
			DistanceParameterConfigurer.configureBootstrapDistCalculator(treeBuild_instance_BS, settingValue);
			// Set tree reconstruction method (inlined to avoid importing remnant classes in ParameterAssigner)
			treeBuild_instance_BS.setTreeReconMethod(tReconMethod);
		} else {
			// Not use bootstrap
			treeBuild_instance = new BuilderSinglePhyloTree();
			// Configure distance calculator using DistanceParameterConfigurer (moved from evoltrepipline to break circular dependency)
			DistanceParameterConfigurer.configureDistanceCalculator(treeBuild_instance, settingValue);
			// Set tree reconstruction method (inlined to avoid importing remnant classes in ParameterAssigner)
			treeBuild_instance.setTreeReconMethod(tReconMethod);
		}
	}

	protected NodeEGPSv1 getTreeWithoutBS() throws Exception {
		return treeBuild_instance.getFinalTree();
	}

	/**
	 * Before using this method, you need to use while
	 * (treeBuild_instance.runOneBS_returnIfContinue()) {}
	 * 
	 * @author yudalang
	 * @throws Exception
	 */
	protected NodeEGPSv1 getTreeWithBS() throws Exception {
		return treeBuild_instance_BS.getFinalTree();
	}
	
	
	protected GraphicsNode convertTheTree4visualization(NodeEGPSv1 tempTree) {
		GraphicsNode ret = new TreeUtility().node4eGPSToGraphicsNode(tempTree);
		return ret;
	}
}
