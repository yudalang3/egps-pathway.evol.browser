package module.evoldist.operator;

import module.evoltrepipline.ConstantNameClass_EvolutionaryDistance;
import module.evoltrepipline.ConstantNameClass_GeneticCode;

import java.util.Map;

/**
 * Configuration utility for distance calculators.
 *
 * Moved from evoltrepipline.ParameterAssigner to break circular dependency
 * (evoldist ↔ evoltrepipline).
 *
 * @author yudalang
 * @date 2019-07-03
 * @date 2025-12-11 - Moved to evoldist package to break circular dependency
 */
public class DistanceParameterConfigurer {

	/**
	 * Configure the DistanceCalculateor instance by incorporating the parameters.
	 *
	 * @author yudalang
	 * @date 2019-07-03
	 */
	public static DistanceCalculateor configureDistanceCalculator(
			DistanceCalculateor distanceCalculateor,
			Map<String, String> settingValue) {

		ConstantNameClass_EvolutionaryDistance cc = new ConstantNameClass_EvolutionaryDistance();
		String gaps_missDataTreatment = settingValue.get(cc.label1_treatment);

		/**
		 * Set "Subsitution model"
		 * "p-distance", "Kimura 2-parameter model", "No. of differences", "Jukes-cantor model",
		 * "Tajima-Nei model", "Tamura 3-parameter model", "Tamura-Nei model"
		 */
		String subsitutionModel = settingValue.get(cc.label1_modelOrMethod);
		ConstantNameClass_GeneticCode c2 = new ConstantNameClass_GeneticCode();
		String ambiguousBaseTreat = settingValue.get(c2.label2_geneticNucleotide);

		if (ambiguousBaseTreat != null && ambiguousBaseTreat.equalsIgnoreCase(c2.geneticNucleotide_value2)) {
			distanceCalculateor.initDistance(subsitutionModel, true);
			distanceCalculateor.setIfSupportFuzzyBase(true);
		} else {
			distanceCalculateor.initDistance(subsitutionModel, false);
		}

		/**
		 * Set "Subsitutions to include"
		 */
		if (gaps_missDataTreatment != null && gaps_missDataTreatment.equalsIgnoreCase(cc.treatment_value1_compD)) {
			distanceCalculateor.setGap_miss_dataTreatment(1);
		} else if (gaps_missDataTreatment != null && gaps_missDataTreatment.contains(cc.treatment_value2_pairD)) {
			distanceCalculateor.setGap_miss_dataTreatment(2);
		} else {
			distanceCalculateor.setGap_miss_dataTreatment(3);
			String siteCoverageCutoff = settingValue.get(cc.label2_siteCoverageCutoff_index);
			if (siteCoverageCutoff != null) {
				distanceCalculateor.setPartialDeleteRatio(Double.parseDouble(siteCoverageCutoff) / 100d);
			}
		}

		return distanceCalculateor;
	}

	/**
	 * Configure bootstrap distance calculator parameters.
	 *
	 * @author yudalang
	 * @date 2019-08-30
	 */
	public static BootstrapDistCalculator configureBootstrapDistCalculator(
			BootstrapDistCalculator distanceCalculateor,
			Map<String, String> settingValue) {

		configureDistanceCalculator(distanceCalculateor, settingValue);

		ConstantNameClass_EvolutionaryDistance cc = new ConstantNameClass_EvolutionaryDistance();
		String bootstrapValueInP = settingValue.get(cc.label1_varianEstimateMethod);
		if (bootstrapValueInP != null && bootstrapValueInP.contains(cc.varianEstimateMethod_value2_bs)) {
			int bootTimes = Integer.parseInt(settingValue.get(cc.label2_numOfBSRep_index));
			distanceCalculateor.setBootTimes(bootTimes);
		}

		return distanceCalculateor;
	}
}
