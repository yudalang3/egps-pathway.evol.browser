package module.multiseq.alignment.view.io;

import java.awt.Dialog;
import java.io.File;

import javax.swing.SwingUtilities;

import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.template.AbstractGuiBaseVoiceFeaturedPanel;
import msaoperator.io.seqFormat.MSA_DATA_FORMAT;
import module.multiseq.alignment.view.VOICE4AlignmentView;
import module.multiseq.alignment.view.model.SequenceDataForAViewer;

public class VOICE4AlignmentViewIO extends AbstractGuiBaseVoiceFeaturedPanel {

	private VOICE4AlignmentView voicmAlignViewEGPS2;

	private AligViewAbstractParamsAssignerAndParser mapProducer = new AligViewAbstractParamsAssignerAndParser();

	public VOICE4AlignmentViewIO() {

	}

	public VOICE4AlignmentViewIO(VOICE4AlignmentView voicmAlignViewEGPS2) {
		this.voicmAlignViewEGPS2 = voicmAlignViewEGPS2;
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {
		// Parameters are already defined in AligViewAbstractParamsAssignerAndParser constructor
		// We could delegate to it, but since it's already set up in the field, we can leave this empty
		// or copy the parameters from the field mapProducer
	}

	@Override
	protected void execute(OrganizedParameterGetter o) throws Exception {
		AlignmentImportBean object = mapProducer.getImportBean(o);

		String upperCase = object.getFileFormat();
		MSA_DATA_FORMAT dataFormat = MSA_DATA_FORMAT.getFormatAccording2name(upperCase);

		File tempFile = new File(object.getFilePath());
		SequenceDataForAViewer parseData = voicmAlignViewEGPS2.parseData(tempFile, dataFormat);
		voicmAlignViewEGPS2.loadOneAlignmentViewTab(parseData, "Import data from the voicm content in file: ".concat(tempFile.getName()));


		Dialog root = (Dialog) SwingUtilities.getRoot(voicmAlignViewEGPS2);
		root.dispose();
	}

}
