package module.multiseq.alignment.view.gui;

import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceI;

import java.awt.*;

public abstract class AbstractSequenceColor {

	public abstract void drawSequence(Graphics2D g2, SequenceI sequence, BasicSequenceData sequenceData, int xOffset,
			int yOffset, int charWidth, int charHeight);
}
