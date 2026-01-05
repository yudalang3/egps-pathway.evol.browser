package module.multiseq.alignment.view.io;

import msaoperator.io.seqFormat.MSAFormatSuffixName;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public abstract class SaveFilterPDF extends FileFilter implements MSAFormatSuffixName {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String s = f.getName();

		return s.endsWith(".pdf") || s.endsWith(".PDF");
	}

	@Override
	public String getDefaultFormatName() {
		return "pdf";
	}

	public abstract SaveFilterPDFDescription getDefaultFormatDescription();

}
