package module.ambigbse;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import egps2.EGPSProperties;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.fastmodvoice.TabModuleFaceOfVoice;
import egps2.modulei.IconBean;
import egps2.modulei.ModuleClassification;
import egps2.modulei.ModuleVersion;
import fasta.io.FastaReader;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class IndependentModuleLoader extends TabModuleFaceOfVoice {

	@Override
	public String getTabName() {
		return "Ambiguous nucleotide to concrete";
	}

	@Override
	public String getShortDescription() {
		return "A convenient graphic tool to make the nucleotide sequence to concrete sequence.";
	}

	@Override
	public IconBean getIcon() {
		InputStream resourceAsStream = getClass().getResourceAsStream("images/ambigbse.svg");
		IconBean iconBean = new IconBean();
		iconBean.setSVG(true);
		iconBean.setInputStream(resourceAsStream);
		return iconBean;
	}

	@Override
	public int[] getCategory() {
		int[] ret = ModuleClassification.getOneModuleClassification(
				ModuleClassification.BYFUNCTIONALITY_SIMPLE_TOOLS_INDEX,
				ModuleClassification.BYAPPLICATION_GENOMICS_INDEX,
				ModuleClassification.BYCOMPLEXITY_LEVEL_1_INDEX,
				ModuleClassification.BYDEPENDENCY_ONLY_EMPLOY_CONTAINER
		);
		return ret;
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {

		mapProducer.addKeyValueEntryBean("input.ambiguous.sequence", "CTTTGWWS;AGAWAW",
				"Directly input the ambiguous nucleotide sequence, with ; as splitter. If user both set the input.fasta.path and input.ambiguous.sequence, only take this one.");

        mapProducer.addKeyValueEntryBean("input.alternative.fasta.path", "",
                "The file path has the ambiguous nucleotide sequence, in fasta format. Necessary");
    }

	@Override
	protected void execute(OrganizedParameterGetter o) throws Exception {
		String inputFilePath = null;
		String inputAmbuguousSeq = null;

		String string = o.getSimplifiedStringWithDefault("input.alternative.fasta.path");
		if (Strings.isNullOrEmpty(string)) {

		} else {
			inputFilePath = string;
		}
		string = o.getSimplifiedStringWithDefault("input.ambiguous.sequence");
		if (Strings.isNullOrEmpty(string)) {

		} else {
			inputAmbuguousSeq = string;
		}


		LinkedHashMap<String, String> fastaDNASequence = null;
		AmbiguousNuclBase runner = new AmbiguousNuclBase();
		List<String> ret = Lists.newArrayList();
		
		if (!Strings.isNullOrEmpty(inputAmbuguousSeq)) {
			fastaDNASequence = new LinkedHashMap<>();

			String[] splits = inputAmbuguousSeq.split(";");
			int index = 1;
			for (String string2 : splits) {
				fastaDNASequence.put("seq" + index, string2);
				index++;
			}
		} else if (!Strings.isNullOrEmpty(inputFilePath)) {
			fastaDNASequence = FastaReader.readFastaDNASequence(new File(inputFilePath));

		}else {
			throw new IllegalArgumentException("Please input at last one parameter");
		}

		
		for (Entry<String, String> entry : fastaDNASequence.entrySet()) {
			ret.add(entry.getKey());
			runner.guiUsage(entry.getValue());
			List<String> strings4output = runner.getOutputStrs();
			ret.addAll(strings4output);
		}

		this.setText4Console(ret);
	}

	@Override
	public ModuleVersion getVersion() {
		return EGPSProperties.MAINFRAME_CORE_VERSION;
	}
}
