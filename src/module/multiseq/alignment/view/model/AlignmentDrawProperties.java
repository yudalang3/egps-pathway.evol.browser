package module.multiseq.alignment.view.model;

import module.multiseq.alignment.view.gui.AbstractSequenceColor;
import module.multiseq.alignment.view.gui.NoColorSequenceJPanel;

public class AlignmentDrawProperties {

	private String myLayout = SequenceLayout.CONTINUOUS;
	private AbstractSequenceColor sequenceBackgroundColor = new NoColorSequenceJPanel();

	public String getMyLayout() {
		return myLayout;
	}

	public void setMyLayout(String myLayout) {
		this.myLayout = myLayout;
	}

	public void setSequenceBackgroundColor(AbstractSequenceColor sequenceBackgroundColor) {

		this.sequenceBackgroundColor = sequenceBackgroundColor;
	}

	public AbstractSequenceColor getSequenceBackgroundColor() {
		return sequenceBackgroundColor;
	}
}
