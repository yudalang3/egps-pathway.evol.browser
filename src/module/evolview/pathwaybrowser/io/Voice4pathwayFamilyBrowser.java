package module.evolview.pathwaybrowser.io;

import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.bean.VoiceValueParameterBean;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.template.AbstractGuiBaseVoiceFeaturedPanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.moderntreeviewer.io.TreePropertiesAssigner;
import module.evolview.pathwaybrowser.PathwayFamilyMainFace;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

import java.util.LinkedHashMap;
import java.util.Optional;

public class Voice4pathwayFamilyBrowser extends AbstractGuiBaseVoiceFeaturedPanel {
	private int exampleIndex;
	private final java.util.concurrent.atomic.AtomicBoolean preparing = new java.util.concurrent.atomic.AtomicBoolean();
	private final java.util.concurrent.atomic.AtomicInteger importVersion = new java.util.concurrent.atomic.AtomicInteger();
	@Override protected void actionBeforeVoiceGuiAppear() { exampleIndex = 0; }
	@Override protected int getNumberOfExamples() { return PathwayBrowserExamples.TITLES.size(); }
	@Override public void doUserImportAction() {
		if (!preparing.compareAndSet(false, true)) return;
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			try { PathwayBrowserExamples.install(PathwayBrowserExamples.directory()); }
			catch (java.io.IOException error) { throw new java.util.concurrent.CompletionException(error); }
		}).whenComplete((ignored, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
			preparing.set(false);
			if (error != null) egps2.panels.dialog.SwingDialog.showErrorMSGDialog("Pathway examples", error.getMessage() + "\nYou can still import your own data.");
			super.doUserImportAction();
		}));
	}
	private final PathwayFamilyMainFace pathwayFamilyMainFace;
	private final ParamsAssignerAndParser4pathwayFamBrowser paramsAssignerAndParser4pathwayFamBrowser = new ParamsAssignerAndParser4pathwayFamBrowser();

	public Voice4pathwayFamilyBrowser(PathwayFamilyMainFace pathwayFamilyMainFace) {
		super();
		this.pathwayFamilyMainFace = pathwayFamilyMainFace;
	}

	@Override
	protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {
		LinkedHashMap<String, VoiceValueParameterBean> requiredParams1 = paramsAssignerAndParser4pathwayFamBrowser
				.getRequiredParams();
		mapProducer.addKeyValueEntryBean(requiredParams1);
	}

	@Override
	public void execute(OrganizedParameterGetter o) throws Exception {
		int version = importVersion.incrementAndGet();
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			java.util.concurrent.CompletableFuture.runAsync(() -> {
				try { importData(o, version); }
				catch (Exception error) {
					javax.swing.SwingUtilities.invokeLater(() -> {
						if (version == importVersion.get()) {
							egps2.panels.dialog.SwingDialog.showErrorMSGDialog("Pathway import", error.getMessage());
						}
					});
				}
			});
		} else importData(o, version);
	}

	public void cancelPendingImport() {
		importVersion.incrementAndGet();
	}

	private void importData(OrganizedParameterGetter o, int version) throws Exception {

		ImporterBean4PathwayFamilyBrowser importBeanInfo = paramsAssignerAndParser4pathwayFamBrowser
				.getImportBeanInfo(o);
		// Validate optional paths before replacing the current view.
		for (String path : java.util.List.of(importBeanInfo.getSpeciesInfoPath(), importBeanInfo.getPathwayComponentCountPath(), importBeanInfo.getSpeciesTraitPath())) validatePath(path);
		if (importBeanInfo.getPathwayGalleryPaths() != null) for (String path : importBeanInfo.getPathwayGalleryPaths()) validatePath(path);
		for (String path : java.util.List.of(importBeanInfo.getSpeciesInfoPath(), importBeanInfo.getPathwayComponentCountPath(), importBeanInfo.getSpeciesTraitPath())) validateTable(path);
		if (importBeanInfo.getPathwayGalleryPaths() != null) for (String path : importBeanInfo.getPathwayGalleryPaths()) {
			if (disabled(path)) continue;
			try (var stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path)); var slides = new org.apache.poi.xslf.usermodel.XMLSlideShow(stream)) {
				if (slides.getSlides().isEmpty()) throw new IllegalArgumentException("No slides: " + path);
			}
		}

		Optional<GraphicsNode> optRoot = new TreeParser4MTV().parseTree(importBeanInfo);
		if (!optRoot.isPresent()) {
			throw new IllegalArgumentException("The phylogenetic tree has error");
		}

		GraphicsNode convertNode = optRoot.get();
		TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(convertNode);

		TreePropertiesAssigner treePropertiesAssigner = new TreePropertiesAssigner();
		treePropertiesAssigner.assign(treeLayoutProperties, importBeanInfo);
		treePropertiesAssigner.assignGraphicsNodeEffects(convertNode, importBeanInfo);


		javax.swing.SwingUtilities.invokeLater(() -> {
			if (version != importVersion.get()) return;
			pathwayFamilyMainFace.invokeTheFeatureMethod(0);
			pathwayFamilyMainFace.loadingDataInitializeGraphics(importBeanInfo, treeLayoutProperties);
		});

	}

	@Override
	protected String getExampleText() {
		String text = PathwayBrowserExamples.text(exampleIndex, PathwayBrowserExamples.directory());
		exampleIndex = (exampleIndex + 1) % getNumberOfExamples();
		return text;
	}

	private static void validatePath(String path) {
		if (disabled(path)) return;
		if (!java.nio.file.Files.isRegularFile(java.nio.file.Path.of(path))) throw new IllegalArgumentException("File not found: " + path);
	}
	private static boolean disabled(String path) {
		return path == null || path.isBlank() || path.equalsIgnoreCase("false") || path.equalsIgnoreCase("f") || path.equalsIgnoreCase("null");
	}
	static void validateTable(String path) throws Exception {
		if (disabled(path)) return;
		java.util.List<String> headers = new java.util.ArrayList<>();
		String lower = path.toLowerCase(java.util.Locale.ROOT);
		if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
			try (var stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path)); var book = org.apache.poi.ss.usermodel.WorkbookFactory.create(stream)) {
				if (book.getNumberOfSheets() > 0) {
					var row = book.getSheetAt(0).getRow(0);
					if (row != null) for (var cell : row) headers.add(new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(cell));
				}
			}
		} else {
			var lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(path), java.nio.charset.StandardCharsets.UTF_8);
			if (!lines.isEmpty()) headers.addAll(java.util.Arrays.asList(lines.getFirst().replaceFirst("^\uFEFF", "").split("\t", -1)));
		}
		if (!headers.contains("Name")) throw new IllegalArgumentException("Missing Name column: " + path);
	}
}
