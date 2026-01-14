package module.treenodecoll;

import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import egps2.frame.ModuleFace;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.EGPSUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CLI {

	private static final Logger log = LoggerFactory.getLogger(CLI.class);

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			String cliUtilityName = EGPSUtil.getCLIUtilityName(CLI.class);
			log.error("Usage: {} configFile", cliUtilityName);
			log.error("Warnings: do not use whitespace, or the parameters will be split.");
			return;
		}

		IndependentModuleLoader independentModuleLoader = new IndependentModuleLoader();
		ModuleFace guiMain = independentModuleLoader.getFace();
		VOICM4TreeLeafInfoGainer voicm4TreeLeafInfoGainer = new VOICM4TreeLeafInfoGainer((GuiMain) guiMain);

		String path = args[0];
		log.info("The input path is: {}", path);
		String fileToString = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);

		VoiceParameterParser voiceParameterParser = new VoiceParameterParser();
		OrganizedParameterGetter organizedParameterGetter = voiceParameterParser.getOrganizedParameterGetter(fileToString);

		try {
			voicm4TreeLeafInfoGainer.execute(organizedParameterGetter);
		} catch (Exception e) {
			log.error("Failed to execute the CLI task.", e);
		}

		log.info("Successfully executed.");
	}

}
