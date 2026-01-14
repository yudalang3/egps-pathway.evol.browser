package module.multiseq.alignment.view.io;

import egps2.EGPSProperties;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.template.AbstractGuiBaseVoiceFeaturedPanel;
import egps2.panels.dialog.SwingDialog;
import module.evoltrepipline.alignment.SequenceDataForAViewer;
import module.multiseq.alignment.view.VOICE4AlignmentView;
import msaoperator.io.seqFormat.MSA_DATA_FORMAT;

import javax.swing.*;
import java.awt.Component;
import java.awt.Window;
import java.io.File;

public class VOICE4AlignmentViewIO extends AbstractGuiBaseVoiceFeaturedPanel {

	private VOICE4AlignmentView voicmAlignViewEGPS2;

	public VOICE4AlignmentViewIO() {

	}

	public VOICE4AlignmentViewIO(VOICE4AlignmentView voicmAlignViewEGPS2) {
		this.voicmAlignViewEGPS2 = voicmAlignViewEGPS2;
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {
		mapProducer.addKeyValueEntryBean(
				"input.file.path",
				EGPSProperties.PROPERTIES_DIR + "/bioData/gfamily/aligned.6.sequences.refined.fas",
				"Input MSA file path (Multiple Sequence Alignment file path)"
		);

		String[] allSupportedNames = MSA_DATA_FORMAT.getAllSupportedNames();
		StringBuilder help = new StringBuilder();
		help.append("Input MSA format (same as \"Text input -> Data type\"), supports: ");
		for (String name : allSupportedNames) {
			help.append(name).append(", ");
		}

		mapProducer.addKeyValueEntryBean(
				"input.data.type",
				MSA_DATA_FORMAT.ALIGNED_FASTA.getName(),
				help.toString()
		);
	}

	@Override
	protected void execute(OrganizedParameterGetter o) throws Exception {
		if (voicmAlignViewEGPS2 == null) {
			SwingDialog.showErrorMSGDialog("Internal error", "VOICE4AlignmentViewIO is not bound to an AlignmentView instance.");
			return;
		}

		String filePath = o.getSimplifiedStringWithDefault("input.file.path");
		if (filePath == null || filePath.isBlank()) {
			filePath = o.getSimplifiedStringWithDefault("file.path"); // backward compatible
		}

		String dataType = o.getSimplifiedStringWithDefault("input.data.type");
		if (dataType == null || dataType.isBlank()) {
			dataType = o.getSimplifiedStringWithDefault("file.format"); // backward compatible
		}

		if (filePath == null || filePath.isBlank()) {
			SwingDialog.showErrorMSGDialog("Parameter error", "Please provide parameter: input.file.path");
			return;
		}
		if (dataType == null || dataType.isBlank()) {
			SwingDialog.showErrorMSGDialog("Parameter error", "Please provide parameter: input.data.type");
			return;
		}

		MSA_DATA_FORMAT dataFormat = MSA_DATA_FORMAT.getFormatAccording2name(dataType);
		if (dataFormat == null) {
			SwingDialog.showErrorMSGDialog("Parameter error", "Unsupported MSA format: " + dataType);
			return;
		}

		File inputFile = new File(filePath);
		if (!inputFile.isFile()) {
			SwingDialog.showErrorMSGDialog("File error", "Input file not found: " + filePath);
			return;
		}

		SequenceDataForAViewer parseData = voicmAlignViewEGPS2.parseData(inputFile, dataFormat);
		if (parseData == null) {
			return;
		}
		voicmAlignViewEGPS2.loadOneAlignmentViewTab(parseData,
				"Data directly imported from file ".concat(inputFile.getName()).concat(" (VOICE input)."));

		Component root = SwingUtilities.getRoot(voicmAlignViewEGPS2);
		if (root instanceof Window) {
			((Window) root).dispose();
		}
	}

}
