package module.evolview.pathwaybrowser.io;

import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ParamsAssignerAndParser4pathwayFamBrowser extends ParamsAssignerAndParser4ModernTreeView {

	public ParamsAssignerAndParser4pathwayFamBrowser() {
        super();
        addKeyValueEntryBean("%pathway", "Pathway data (optional)", "");
        addKeyValueEntryBean("pathway.gallery.figure.paths", "False", "PPTX paths, one per continuation line; first slide only. False to disable.");
        addKeyValueEntryBean("pathway.species.info.path", "False", "Species information TSV/XLSX with a Name column.");
        addKeyValueEntryBean("pathway.component.counts.path", "False", "Component counts TSV/XLSX: Name column identifies tree nodes.");
        addKeyValueEntryBean("species.traits.path", "False", "Species traits TSV/XLSX with a Name column.");
    }

	static String resolvePropertiesDir(String configuredDir, String fallbackDir) {
		String directory = configuredDir == null ? null : configuredDir.trim();
		if (directory == null || directory.isEmpty() || directory.equalsIgnoreCase("null")
				|| !new File(directory).isDirectory()) {
			return fallbackDir;
		}
		return directory;
	}

	public ImporterBean4PathwayFamilyBrowser getImportBeanInfo(OrganizedParameterGetter str) {
		// 得到进化树的一些设置属性
		MTVImportInforBean object = this.generateTreeFromKeyValue(str);
		ImporterBean4PathwayFamilyBrowser bean = new ImporterBean4PathwayFamilyBrowser(object);

		Optional<List<String>> complicatedValues = str.getComplicatedValues("pathway.gallery.figure.paths");
		if (complicatedValues.isPresent()) {
			List<String> strings = complicatedValues.get();
			bean.pathwayGalleryPaths = Collections.unmodifiableList(strings.subList(1, strings.size()));
		}

		bean.speciesInfoPath = str.getSimplifiedStringWithDefault("pathway.species.info.path");
		bean.pathwayComponentCountPath = str.getSimplifiedStringWithDefault("pathway.component.counts.path");
		bean.speciesTraitPath = str.getSimplifiedStringWithDefault("species.traits.path");


		return bean;
	}
}
