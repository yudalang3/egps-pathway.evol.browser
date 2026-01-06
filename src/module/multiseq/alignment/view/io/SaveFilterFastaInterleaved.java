package module.multiseq.alignment.view.io;

import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * FASTA Interleaved 格式 - 每100个碱基换行
 */
public class SaveFilterFastaInterleaved extends FileFilter implements MSAFormatSuffixName {

	public static final String FORMAT_SUFFIX = "fas_interleaved";
	public static final int LINE_LENGTH = 100;

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
		return "FASTA Interleaved (*.fas) - 100 bases per line";
	}

	@Override
	public String getDefaultFormatName() {
		return FORMAT_SUFFIX;
	}
}
