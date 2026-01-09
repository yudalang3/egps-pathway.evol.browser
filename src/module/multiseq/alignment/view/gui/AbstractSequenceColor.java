package module.multiseq.alignment.view.gui;

import msaoperator.alignment.sequence.BasicSequenceData;
import msaoperator.alignment.sequence.SequenceI;

import java.awt.*;

public abstract class AbstractSequenceColor {

	/**
	 * 计算文字绘制的 Y 坐标
	 * <p>
	 * Swing 的 {@link Graphics2D#drawString(String, int, int)} 的 y 参数是 baseline（基线），
	 * 这里按“单元格顶部 + ascent”的方式计算，使序列名称与序列内容在同一基线对齐，并避免依赖魔法系数。
	 * @param yOffset 基准 Y 坐标
	 * @param charHeight 字符高度
	 * @return 调整后的 Y 坐标
	 */
	public static int calcTextY(Graphics2D g2, int yOffset, int charHeight) {
		FontMetrics fm = g2.getFontMetrics();
		return yOffset - charHeight + fm.getAscent();
	}

	public abstract void drawSequence(Graphics2D g2, SequenceI sequence, BasicSequenceData sequenceData, int xOffset,
			int yOffset, int charWidth, int charHeight);
}
