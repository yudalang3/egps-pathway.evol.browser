package module.multiseq.alignment.view.io;

import module.multiseq.alignment.DataFileSuffixNames;
import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SaveFilterMega extends FileFilter implements MSAFormatSuffixName{
	
	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String s = f.getName();

		return s.endsWith(".meg") || s.endsWith(".MEG");
	}

	@Override
	public String getDescription() {
		return "MEGA (*.meg)";
	}

	@Override
	public String getDefaultFormatName() {
		return DataFileSuffixNames.MEGA_DEFAULT_SUFFIX;
	}

}
