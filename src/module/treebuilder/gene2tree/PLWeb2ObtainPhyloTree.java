package module.treebuilder.gene2tree;

import egps2.frame.MainFrameProperties;
import egps2.panels.dialog.SwingDialog;
import module.evoldist.operator.DistanceParameterConfigurer;
import module.evoltrepipline.ConstantNameClass_EvolutionaryDistance;
import module.evoltrepipline.ConstantNameClass_TreeBuildMethod;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.IndependentModuleLoader;
import module.evolview.phylotree.visualization.util.TreeConversionUtil;
import module.multiseq.gene2msa.PipelineWebToAlignment;
import module.remnant.treeoperator.NodeEGPSv1;
import module.remnant.treeoperator.dmMethod.BuilderBootstrapTree4MSA;
import module.remnant.treeoperator.dmMethod.BuilderSinglePhyloTree;
import module.remnant.treeoperator.reconAlgo.NJ;
import module.remnant.treeoperator.reconAlgo.SwiftNJ;
import module.remnant.treeoperator.reconAlgo.TreeReconMethod;
import module.remnant.treeoperator.reconAlgo.Upgma;

import java.util.Map;

/**
  *
 * @author mhl,yudalang
 * @date 2018-7-30
 * @version 1.0
 * @date 2024-04-28
 * @version 2.0
 *
 */
public class PLWeb2ObtainPhyloTree extends PipelineWebToAlignment{

	protected GraphicsNode finalTree;
	
	private boolean ifBootStrap = false;
	private int bootStrapTimes = 0;

	private BuilderBootstrapTree4MSA<?> treeBuild_instance_BS;
	private BuilderSinglePhyloTree treeBuild_instance;
	
	private String dataSource;
	

	/**
	 * yudalang: When you use this constructor, the geneSymbol paramter could be any
	 * arbitrarily string.
	 * 
	 * way 1@param speciesName
	 *            e.g. "homo_sapiens"
	 * way 1@param geneSymbol
	 *            e.g. "Cyhr1"
	 * way 2@param speciesSetGroup
	 *            e.g. "primates"
	 * way2 @param region
	 *            e.g. "2:106040000-106040050"
	 */
	public PLWeb2ObtainPhyloTree(Map<String, String> t, String geneSymbol) {
		super(t, geneSymbol);
		dataSource = geneSymbol;
	}

	/**
	 * For the constructor polymorphism
	 * @param t
	 * @param region
	 * @param hasGenomicRegion
	 */
	public PLWeb2ObtainPhyloTree(Map<String, String> t, String region, boolean hasGenomicRegion) {
		super(t, region, hasGenomicRegion);
		dataSource = region;
	}


	public GraphicsNode getFinalTree() {
		return finalTree;
	}

	@Override
	public int getTotalSteps() {
		return 5 + bootStrapTimes + super.getTotalSteps();
	}

	@Override
	public int processNext() throws Exception {
		switch (processIndex) {
		case 1:
			if (internally_invoke_restWeb2Alignment()) {
			} else {
				processIndex++;
			}
			return restAlignment.getCurrentProgressIndex() + processIndex - 1;
		case 2:
			get_seqs_seqsNames();
			processIndex++;
			return getAlreadyProgressed() + processIndex - 1;
		case 3:
			assignSeqsAndSeqNameToBuilder();
			processIndex++;
			return getAlreadyProgressed() + processIndex - 1;
		case 4:
			if (buildTreeProcess()) {
			} else {
				processIndex++;
			}
			return getAlreadyProgressed() + processIndex - 1;
		default:
			return PROGRESS_FINSHED;
		}
		
	}
	private boolean buildTreeProcess() throws Exception {
		if (ifBootStrap) {
			if (treeBuild_instance_BS.runOnceBSIteration()) {
				return true;
			}else {
				NodeEGPSv1 tempTree = treeBuild_instance_BS.getFinalTree();
				finalTree = transferTheTree(tempTree);
			}
		}else {
			NodeEGPSv1 tempTree = treeBuild_instance.getFinalTree();
			finalTree = transferTheTree(tempTree);
		}
		
		return false;
	}

	private GraphicsNode transferTheTree(NodeEGPSv1 tempTree) {
		GraphicsNode ret = TreeConversionUtil.node4eGPSToGraphicsNode(tempTree);
		return ret;
	}

	private void assignSeqsAndSeqNameToBuilder() throws Exception {
		if (ifBootStrap) {
			treeBuild_instance_BS.setSeqsAndSeqNames(seqs, seqNames);
			treeBuild_instance_BS.initialize();
		} else {
			treeBuild_instance.setSeqsAndSeqNames(seqs, seqNames);
		}

	}

	@Override
	public void actionsBeforeStart() throws Exception {
		// if we need to use bootstrap
		ConstantNameClass_EvolutionaryDistance cc = new ConstantNameClass_EvolutionaryDistance();
		String bootstrapValueInP = settingValues.get(cc.label1_varianEstimateMethod);

		// Create tree reconstruction method based on settings
		ConstantNameClass_TreeBuildMethod ccTree = new ConstantNameClass_TreeBuildMethod();
		String treeBuildMethod = settingValues.get(ccTree.label1_treeBuildMethod);
		TreeReconMethod tReconMethod = null;
		if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(ccTree.treeBuildMethod_value1_NJ)) {
			tReconMethod = new NJ();
		} else if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(ccTree.treeBuildMethod_value2_SNJ)) {
			tReconMethod = new SwiftNJ();
		} else if (treeBuildMethod != null && treeBuildMethod.equalsIgnoreCase(ccTree.treeBuildMethod_value3_UPGMA)) {
			tReconMethod = new Upgma();
		} else {
			tReconMethod = new NJ(); // default
		}

		if (bootstrapValueInP.contains(cc.varianEstimateMethod_value2_bs)) {
			ifBootStrap = true;
			bootStrapTimes = Integer.parseInt(settingValues.get(cc.label2_numOfBSRep_index));

			treeBuild_instance_BS = new BuilderBootstrapTree4MSA<>();
			treeBuild_instance_BS.setBootTimes(bootStrapTimes);
			// Configure distance calculator using DistanceParameterConfigurer
			DistanceParameterConfigurer.configureBootstrapDistCalculator(treeBuild_instance_BS, settingValues);
			// Set tree reconstruction method
			treeBuild_instance_BS.setTreeReconMethod(tReconMethod);
		} else {
			treeBuild_instance = new BuilderSinglePhyloTree();
			// Configure distance calculator using DistanceParameterConfigurer
			DistanceParameterConfigurer.configureDistanceCalculator(treeBuild_instance, settingValues);
			// Set tree reconstruction method
			treeBuild_instance.setTreeReconMethod(tReconMethod);
		}
	}

	@Override
	public void actionsAfterFinished() throws Exception {
		GraphicsNode finalTree2 = getFinalTree();
		
		if (finalTree2 == null) {
			SwingDialog.showErrorMSGDialog("Error", "The tree is null! Please check the input file!");
		}
		
		IndependentModuleLoader independentModuleLoader = new IndependentModuleLoader();
		independentModuleLoader.setHowModuleLaunchedWithData("Module launched by pipline gene to gene tree.", "Data source is ".concat(dataSource));
		/**
		 * treeLayoutProperties 可选，如果输入为null，会自动根据 root生成 treeLayoutProperties
		 * 开发者也可以自己生成 TreeLayoutProperties类示例，进而设置一些参数
		 */
		independentModuleLoader.setModuleData(finalTree2, null);
		MainFrameProperties.loadTheModuleFromIModuleLoader(independentModuleLoader);

	}
	

}