package module.multiseq.alignment.view.io;

import module.multiseq.alignment.DataFileSuffixNames;
import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SaveFilterNEXUS extends FileFilter implements MSAFormatSuffixName{
	

	
	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String s = f.getName();

		return s.endsWith(".phy") || s.endsWith(".PHY");
	}

	@Override
	public String getDescription() {
		return "PHYLIP (*.phy)";
	}

	@Override
	public String getDefaultFormatName() {
		return DataFileSuffixNames.PHYLIP_DEFAULT_SUFFIX;
	}

}
