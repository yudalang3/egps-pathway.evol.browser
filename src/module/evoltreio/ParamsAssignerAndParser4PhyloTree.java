package module.evoltreio;

import egps2.EGPSProperties;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evoltreio.exception.NewickFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParamsAssignerAndParser4PhyloTree extends AbstractParamsAssignerAndParser4VOICE {

	private static final Logger log = LoggerFactory.getLogger(ParamsAssignerAndParser4PhyloTree.class);

	public ParamsAssignerAndParser4PhyloTree() {
		addKeyValueEntryBean("input.nwk.string", "", "Way1: direct input the nwk string content. Highest priority.");
		addKeyValueEntryBean("input.nwk.path",
				EGPSProperties.PROPERTIES_DIR + "/bioData/example/treeBarplot/SpeciesTree.nwk",
				"Way2: Input the nwk file path. Medium priority");
		addKeyValueEntryBean("nwk.format", String.valueOf(NewickFormatConfig.DEFAULT_FORMAT),
				"Newick format type (0-9). Format 0: support:dist for internal. Format 1: name:dist for internal. See help for all formats.");
		addKeyValueEntryBean("nwk.remove.whitespace", "F",
				"Whether remove whitespace (spaces, tabs, carriage returns, and linefeed) for the nwk file. Default False.");
		addKeyValueEntryBean("input.tableLike.path", "", "Way3: Input the table-like tree file path, see help for details. Last priority but most human readable.");
	}


	/**
	 * 根据键值对生成进化树解析所需信息。
	 *
	 * @return 生成的树结构信息对象
	 */
	public EvolTreeImportInfoBean generateTreeFromKeyValue(OrganizedParameterGetter o) {
		EvolTreeImportInfoBean ret = new EvolTreeImportInfoBean();
		assignValues(o, ret);
		return ret;
	}

	public void assignValues(OrganizedParameterGetter o, EvolTreeImportInfoBean ret) {
		// 解析的时候不要忘记把 $ 符号加进去
		String string = o.getSimplifiedStringWithDefault("input.nwk.string");
		if (!string.isEmpty()) {
			ret.setInput_nwk_string(string);
		}

		string = o.getSimplifiedStringWithDefault("input.nwk.path");
		if (!string.isEmpty()) {
			ret.setInput_nwk_path(string);
		}

		string = o.getSimplifiedStringWithDefault("input.tableLike.path");
		if (!string.isEmpty()) {
			ret.setInput_tableLike_path(string);
		}

		// Parse nwk.format with validation
		string = o.getSimplifiedStringWithDefault("nwk.format");
		if (!string.isEmpty()) {
			try {
				int format = Integer.parseInt(string.trim());
				if (NewickFormatConfig.isValidFormat(format)) {
					ret.setNwkFormat(format);
				} else {
					throw new NewickFormatException(format, NewickFormatConfig.MIN_FORMAT, NewickFormatConfig.MAX_FORMAT);
				}
			} catch (NumberFormatException e) {
				log.error("Invalid nwk.format value '{}': not a valid integer. Using default format {}",
						string, NewickFormatConfig.DEFAULT_FORMAT);
				ret.setNwkFormat(NewickFormatConfig.DEFAULT_FORMAT);
			} catch (NewickFormatException e) {
				log.error("Invalid nwk.format: {}", e.getMessage());
				ret.setNwkFormat(NewickFormatConfig.DEFAULT_FORMAT);
			}
		}

		if (o.isSimplifiedValueExist("nwk.remove.whitespace")) {
			boolean simplifiedBool = o.getSimplifiedBool("nwk.remove.whitespace");
			ret.setRemoveWhitespace(simplifiedBool);
		}


	}

}
