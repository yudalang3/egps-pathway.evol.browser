package module.multiseq.alignment.view.gui;

import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceComponentRatio;
import msaoperator.alignment.sequence.SequenceI;

import java.awt.*;
import java.util.List;

public class PercentageIdentitySequenceJPanel extends AbstractSequenceColor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8445330225362713623L;

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
			Character base2 = sequenceRatio.getBase();
			int XPos = i * charWidth + xOffset;

			if (base2 == seq[i]) {
				int percentage = sequenceRatio.getPercentage();
				Color bgColor;
				if (percentage > 80) {
					bgColor = new Color(100, 100, 255);
				} else if (percentage > 60) {
					bgColor = new Color(153, 153, 255);
				} else if (percentage > 40) {
					bgColor = new Color(204, 204, 255);
				} else {
					bgColor = Color.WHITE;
				}

				g2.setColor(bgColor);
				g2.fillRect(XPos, yOffset - charHeight, charWidth, charHeight);
				g2.setColor(originalColor);
			}

			g2.drawString(base, start + XPos, calcTextY(g2, yOffset, charHeight));
		}

	}
	
	@Override
	public String toString() {
		return "By percentage identity";
	}

}
