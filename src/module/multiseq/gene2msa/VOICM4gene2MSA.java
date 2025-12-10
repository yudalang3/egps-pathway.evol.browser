package module.multiseq.gene2msa;

import java.util.Map;

import module.evoltre.pipline.TreeParameterHandler;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.template.AbstractGuiBaseVoiceFeaturedPanel;

public class VOICM4gene2MSA extends AbstractGuiBaseVoiceFeaturedPanel {

	private GuiMain guiMain;

	public VOICM4gene2MSA(GuiMain guiMain) {
		this.guiMain = guiMain;
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {
		// TODO: Define parameters if needed
		// For now, keeping minimal implementation as the original didn't use inputs
	}

	@Override
	protected void execute(OrganizedParameterGetter o) throws Exception {
		Map<String, String> buildTreeParametersMap = new TreeParameterHandler().getBuildTreeParametersMap();
		PLWeb2ObtainAlignment plWeb2ObtainAlignment = new PLWeb2ObtainAlignment(buildTreeParametersMap, "INS");
		guiMain.registerRunningTask(plWeb2ObtainAlignment);

	}

}
