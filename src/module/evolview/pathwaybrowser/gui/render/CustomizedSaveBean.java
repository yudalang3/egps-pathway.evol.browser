package module.evolview.pathwaybrowser.gui.render;

import module.evolview.model.enums.ColorScheme;

import java.util.ArrayList;
import java.util.List;

public class CustomizedSaveBean {

	int beforeRenderingIndex = ColorScheme.COUNTRIESREGIONS.getIndex();

	List<CustomizedRenderRecord> records = new ArrayList<>();

	public int getBeforeRenderingIndex() {
		return beforeRenderingIndex;
	}

	public void setBeforeRenderingIndex(int beforeRenderingIndex) {
		this.beforeRenderingIndex = beforeRenderingIndex;
	}

	public List<CustomizedRenderRecord> getRecords() {
		return records;
	}

	public void setRecords(List<CustomizedRenderRecord> records) {
		this.records = records;
	}
}
