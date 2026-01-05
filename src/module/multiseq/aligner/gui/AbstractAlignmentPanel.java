package module.multiseq.aligner.gui;

import module.multiseq.aligner.BaseMultipleSeqAlignerMain;

import javax.swing.*;

public abstract class AbstractAlignmentPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	protected BaseMultipleSeqAlignerMain alignmentMain;

	public AbstractAlignmentPanel(BaseMultipleSeqAlignerMain alignmentMain) {
		this.alignmentMain = alignmentMain;
	}

}
