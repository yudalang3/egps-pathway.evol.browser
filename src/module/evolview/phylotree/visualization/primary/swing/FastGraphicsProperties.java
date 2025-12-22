package module.evolview.phylotree.visualization.primary.swing;

import com.google.common.collect.Maps;
import module.evolview.phylotree.visualization.graphics.struct.CollapseProperty;

import java.util.Map;

public class FastGraphicsProperties {

	public int verticalBlankLength = 40;
	public int leftHorizontalBlankLength = 100;
	public int rightHorizontalBlankLength = 180;

	public Map<String, CollapseProperty> collapsePropertyMaps = Maps.newHashMap();

}
