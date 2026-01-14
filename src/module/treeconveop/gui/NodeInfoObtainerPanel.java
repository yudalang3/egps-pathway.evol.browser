/**
 * 
 */
package module.treeconveop.gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.DockableTabModuleFaceOfVoice;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.frame.ComputationalModuleFace;

@SuppressWarnings("serial")
public class NodeInfoObtainerPanel extends DockableTabModuleFaceOfVoice {

	public NodeInfoObtainerPanel(ComputationalModuleFace cmf) {
		super(cmf);
		setBorder(new EmptyBorder(15, 15, 15, 15));
		setLayout(new BorderLayout(0, 0));

		JTextArea txtrPleaseReferTo = new JTextArea();
		txtrPleaseReferTo.setText(
				"Please refer to the module \"Tree node info collector\", the normal procedure is:\r\n1. Setting phylogenetic tree with various format.\r\n2. Select parameters.\r\n3. Get the target node info.");

		Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
		txtrPleaseReferTo.setFont(defaultTitleFont);

		add(txtrPleaseReferTo, BorderLayout.CENTER);
	}


	public String getTabName() {
		return new String("Tree node info collector");
	}

	public String getShortDescription() {
		return new String("Import phylogenetic tree and obtain the node information.");
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE abstractParamsAssignerAndParser4VOICE) {

	}

	@Override
	protected void execute(OrganizedParameterGetter organizedParameterGetter) throws Exception {

	}
}
