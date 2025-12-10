package module.multiseq.alignment.view.io;

import java.util.Map;

import egps2.EGPSProperties;
import msaoperator.io.seqFormat.MSA_DATA_FORMAT;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;

public class AligViewAbstractParamsAssignerAndParser extends AbstractParamsAssignerAndParser4VOICE {

	public AligViewAbstractParamsAssignerAndParser() {
		super();

		addKeyValueEntryBean("file.path",
				EGPSProperties.PROPERTIES_DIR + "/bioData/gfamily/aligned.6.sequences.refined.fas",
				"The import path for the Multiple sequence alignment (MSA)");
		addKeyValueEntryBean("file.format", MSA_DATA_FORMAT.ALIGNED_FASTA.getName(), "The format of MSA, see the help for the supported file format.");
	}


	// New API using OrganizedParameterGetter
	public AlignmentImportBean getImportBean(OrganizedParameterGetter o) {
		AlignmentImportBean ret = new AlignmentImportBean();

		String filePath = o.getSimplifiedStringWithDefault("file.path");
		if (filePath != null && !filePath.isEmpty()) {
			ret.setFilePath(filePath);
		}

		String fileFormat = o.getSimplifiedStringWithDefault("file.format");
		if (fileFormat != null && !fileFormat.isEmpty()) {
			ret.setFileFormat(fileFormat);
		}

		return ret;
	}

	// Legacy API for backward compatibility (deprecated)
	@Deprecated
	public AlignmentImportBean getImportBean(String str) {
		AlignmentImportBean ret = new AlignmentImportBean();
		Map<String, String> keyValueStringMap = getKeyValueStringMap(str);
		String string = keyValueStringMap.get("$file.path");
		if (string != null) {
			ret.setFilePath(string);
		}

		string = keyValueStringMap.get("$file.format");
		if (string != null) {
			ret.setFileFormat(string);
		}

		return ret;
	}
}
