package module.evolview.model.tree;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 这里的 displayDoubles应该是 work space 中的 长度，减少可视化的计算
 * @author yudal
 *
 */
public class BottomScaleBarDrawProperty {

	private List<String> displayedStrings = Lists.newArrayList();
	private List<Double> displayeDoubles = Lists.newArrayList();
    private final List<Double> tickValues = Lists.newArrayList();

    public List<Double> getTickValues() { return tickValues; }

	/**
	 * @return the displayedStrings
	 */
	public List<String> getDisplayedStrings() {
		return displayedStrings;
	}

	/**
	 * @return the displayeDoubles
	 */
	public List<Double> getDisplayeDoubles() {
		return displayeDoubles;
	}
	
    public void addElement(String label, double logicalValue, double pixelOffset) {
        displayedStrings.add(label);
        tickValues.add(logicalValue);
        displayeDoubles.add(pixelOffset);
    }

	public void clear() {
		displayedStrings.clear();
		displayeDoubles.clear();
        tickValues.clear();
	}
	
	public void addElement(String str, double value) {
		addElement(str, Double.NaN, value);
	}
	
}
