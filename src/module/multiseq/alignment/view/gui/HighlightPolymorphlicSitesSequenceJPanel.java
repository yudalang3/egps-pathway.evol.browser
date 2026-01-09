package module.multiseq.alignment.view.gui;

import module.multiseq.alignment.view.model.SequenceBaseColor;
import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceComponentRatio;
import msaoperator.alignment.sequence.SequenceI;

import java.awt.*;
import java.util.List;

public class HighlightPolymorphlicSitesSequenceJPanel extends AbstractSequenceColor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1842801759966677760L;

	@Override
	public void drawSequence(Graphics2D g2, SequenceI sequence, BasicSequenceData sequenceData, int xOffset,
			int yOffset, int charWidth, int charHeight) {

		char[] seq = sequence.getSeq();

		if (seq == null) {
			return; // fix for racecondition
		}

		// 保存原始颜色
		Color originalColor = g2.getColor();

		List<SequenceComponentRatio> ratios = sequenceData.getRatio();

		for (int i = 0; i < seq.length; i++) {
			String base = String.valueOf(seq[i]);
			int stringLen = (int) g2.getFontMetrics().getStringBounds(base, g2).getWidth();
			int start = charWidth / 2 - stringLen / 2;
			SequenceComponentRatio sequenceRatio = ratios.get(i);

			int XPos = i * charWidth + xOffset;

			int percentage = sequenceRatio.getPercentage();

			if (percentage != 100) {
				String maxString = base.toUpperCase();
				Color bgColor = SequenceBaseColor.getBaseColor(maxString);
				g2.setColor(bgColor);
				g2.fillRect(XPos, yOffset - charHeight, charWidth, charHeight);
				g2.setColor(originalColor);
			}

			g2.drawString(base, start + XPos, calcTextY(g2, yOffset, charHeight));
		}

	}
	
	@Override
	public String toString() {
		return "Highlight polymorphic sites";
	}

}
