package module.evolview.pathwaybrowser.io;

import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.bean.VoiceValueParameterBean;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class ParamsAssignerAndParser4pathwayFamBrowser extends ParamsAssignerAndParser4ModernTreeView {

	public ParamsAssignerAndParser4pathwayFamBrowser() {
		// 去掉一些输入方式
		requiredParams.remove("input.nwk.string");
		requiredParams.remove("input.tableLike.path");

		// 需要重新排列Entries的顺序
		List<Entry<String, VoiceValueParameterBean>> alreadyMapBean = new LinkedList<>(requiredParams.entrySet());

		requiredParams.clear();
		String propertiesDir = UnifiedAccessPoint.getLaunchProperty().getTestDataDir();
		addKeyValueEntryBean("@", "The Pathway Family Browser", "");

		addKeyValueEntryBean("input.nwk.path",
				propertiesDir + "/bioData/example/9_model_species_evolution.nwk",
				"Input the Tree nwk file path.");

		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\n");
		sBuilder.append(propertiesDir).append("/bioData/pathwayBrowser/wnt_pathway2.pptx");
		sBuilder.append("\n");
		sBuilder.append(propertiesDir).append("/bioData/pathwayBrowser/pathway_counts.pptx");

		addKeyValueEntryBean("pathway.gallery.figure.paths",
				sBuilder.toString(),
				"Input the file path of the pathway details, format is the pptx and multi-files are allowed.  F/False or annotated for none");

		addKeyValueEntryBean("pathway.species.info.path",
				"False",
				"Input the file path of the species information, tsv format. Must has the Name column. F/False or annotated for none");

		sBuilder.setLength(0);
		sBuilder.append("Input the file path of the species components, tsv format. Must has the Name column. F/False or annotated for none");
		sBuilder.append("\n## Species for rows and gene for columns. For examples:");
		sBuilder.append("\n## Name    AXIN2    WNT3A    DVL3");
		sBuilder.append("\n## human    2    18    3");
		addKeyValueEntryBean("pathway.component.counts.path",
				"False", sBuilder.toString());
		addKeyValueEntryBean("species.traits.path",
				"False", "Input the file path of the species components, tsv format. Must has the Name column. F/False or annotated for none");


		addKeyValueEntryBean("^", "", "");
		addKeyValueEntryBean("%1", "The species tree", "");
		addKeyValueEntryBean("blank.space", "20,20,100,20",
				"The blank area of top,left,bottom,right. Note: right will take effects if divider = 0");
		addKeyValueEntryBean("need.reverse.axis.bar", "T",
				"Whether reverse the axis bar. In some cases, the time is years ago.");
		addKeyValueEntryBean("bottom.title.string", "The phylogenetic tree with {0} high-quality sequenced samples.",
				"The statement that displayed in the bottom of the tree view. {0} will be replaced with number of leaf.");

		for (Entry<String, VoiceValueParameterBean> entry : alreadyMapBean) {
			if (requiredParams.get(entry.getKey()) != null) {
				// 这一句一定要有，否则被覆盖
				continue;
			}
			requiredParams.put(entry.getKey(), entry.getValue());
		}

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
