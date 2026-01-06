package module.multiseq.alignment.view.gui;

import module.multiseq.alignment.view.model.SequenceBaseColor4AAProperties;
import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceI;

import java.awt.*;


public class HighlightAllSitesSequenceJPanel4AAProperties extends AbstractSequenceColor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6990283181965688379L;

	@Override
	public void drawSequence(Graphics2D g2, SequenceI sequence, BasicSequenceData sequenceData, int xOffset,
			int yOffset, int charWidth, int charHeight) {

		char[] seq = sequence.getSeq();

		if (seq == null) {
			return; // fix for racecondition
		}

		// 保存原始颜色
		Color originalColor = g2.getColor();

		for (int i = 0; i < seq.length; i++) {
			String base = String.valueOf(seq[i]);
			int stringLen = (int) g2.getFontMetrics().getStringBounds(base, g2).getWidth();
			int start = charWidth / 2 - stringLen / 2;
			int XPos = i * charWidth;
			String maxString = base.toUpperCase();

			// 使用 fillRect 代替 setBackground + clearRect
			Color bgColor = SequenceBaseColor4AAProperties.getBaseColor(maxString);
			g2.setColor(bgColor);
			g2.fillRect(XPos + xOffset, yOffset - charHeight, charWidth, charHeight);

			// 恢复前景色绘制文字
			g2.setColor(originalColor);
			g2.drawString(base, start + XPos + xOffset, (int)(yOffset-charHeight*0.15));
		}
	}
	
	@Override
	public String toString() {
		return "Highlight according to amino acid properties";
	}

}
