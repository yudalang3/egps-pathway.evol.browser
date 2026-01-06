package module.multiseq.alignment.view.io;

import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * FASTA Continuous 格式 - 序列不换行
 */
public class SaveFilterFastaContinuous extends FileFilter implements MSAFormatSuffixName {

	public static final String FORMAT_SUFFIX = "fas_continuous";

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		String s = f.getName().toLowerCase();
		return s.endsWith(".fas") || s.endsWith(".fasta");
	}

	@Override
	public String getDescription() {
		return "FASTA Continuous (*.fas) - No line breaks";
	}

	@Override
	public String getDefaultFormatName() {
		return FORMAT_SUFFIX;
	}
}
