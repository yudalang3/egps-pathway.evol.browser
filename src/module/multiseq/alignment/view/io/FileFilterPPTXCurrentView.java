package module.multiseq.alignment.view.io;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * PPTX 文件过滤器 - 仅导出当前可见区域
 */
public class FileFilterPPTXCurrentView extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		String name = f.getName().toLowerCase();
		return name.endsWith(".pptx");
	}

	@Override
	public String getDescription() {
		return "PowerPoint (*.pptx) - Current View Only";
	}
}
