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
	private volatile boolean moduleClosed = false;

	public void setModuleClosed(boolean closed) {
		this.moduleClosed = closed;
	}

	public boolean isModuleClosed() {
		return moduleClosed;
	}

	@Override protected void actionBeforeVoiceGuiAppear() { exampleIndex = 0; }
	@Override protected int getNumberOfExamples() { return PathwayBrowserExamples.TITLES.size(); }
	@Override public void doUserImportAction() {
		if (moduleClosed || !preparing.compareAndSet(false, true)) return;
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			try { PathwayBrowserExamples.install(PathwayBrowserExamples.directory()); }
			catch (java.io.IOException error) { throw new java.util.concurrent.CompletionException(error); }
		}).whenComplete((ignored, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
			preparing.set(false);
			if (moduleClosed) return;
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
		for (String path : java.util.List.of(importBeanInfo.getSpeciesInfoPath(), importBeanInfo.getSpeciesTraitPath())) validateNameColumnTable(path);
		if (!disabled(importBeanInfo.getPathwayComponentCountPath())) {
			validateTable(importBeanInfo.getPathwayComponentCountPath());
			try {
				PathwayComponentCountData.load(new java.io.File(importBeanInfo.getPathwayComponentCountPath()));
			} catch (java.io.IOException e) {
				throw new IllegalArgumentException("Failed to read component count table: " + importBeanInfo.getPathwayComponentCountPath(), e);
			}
		}
		if (importBeanInfo.getPathwayGalleryPaths() != null) for (String path : importBeanInfo.getPathwayGalleryPaths()) {
			try {
				module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.validateGalleryFile(path);
			} catch (java.io.IOException e) {
				throw new IllegalArgumentException("Failed to read pathway gallery file: " + path, e);
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
			if (moduleClosed || version != importVersion.get()) return;
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
	private static void validateNameColumnTable(String path) {
		if (disabled(path)) return;
		PathwayComponentCountData.RawTable raw;
		try {
			raw = PathwayComponentCountData.readRawTable(java.nio.file.Path.of(path).toFile());
		} catch (java.io.IOException e) {
			throw new IllegalArgumentException("Invalid table file: " + path, e);
		}
		if (PathwayComponentCountData.findNameColumnIndex(raw.headers()) < 0) {
			throw new IllegalArgumentException("Missing Name column: " + path);
		}
	}
	static void validateTable(String path) throws Exception {
		if (disabled(path)) return;
		PathwayComponentCountData.RawTable raw;
		try {
			raw = PathwayComponentCountData.readRawTable(java.nio.file.Path.of(path).toFile());
		} catch (java.io.IOException e) {
			throw new IllegalArgumentException("Invalid table file: " + path, e);
		}
		boolean hasIdentifierColumn = false;
		for (String h : raw.headers()) {
			if (h == null) continue;
			String t = h.trim();
			if (t.equalsIgnoreCase("name") || t.equalsIgnoreCase("gene") || t.equalsIgnoreCase("component")) {
				hasIdentifierColumn = true;
				break;
			}
		}
		if (!hasIdentifierColumn) throw new IllegalArgumentException("Missing Name column: " + path);
	}
}
