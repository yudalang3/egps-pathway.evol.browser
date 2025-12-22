package module.multiseq.alignment.view.io;

import module.multiseq.alignment.DataFileSuffixNames;
import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SaveFilterGCGMSF extends FileFilter implements MSAFormatSuffixName{
	
	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String s = f.getName();

		return s.endsWith(".msf") || s.endsWith(".MSF");
	}

	@Override
	public String getDescription() {
		return "GCG(MSF) (*.msf)";
	}

	@Override
	public String getDefaultFormatName() {
		return DataFileSuffixNames.GCGMSF_DEFAULT_SUFFIX;
	}

}
