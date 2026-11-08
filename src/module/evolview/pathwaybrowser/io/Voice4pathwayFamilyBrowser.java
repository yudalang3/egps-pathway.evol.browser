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
	private volatile javax.swing.SwingWorker<Void, Void> importWorker;

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
		if (moduleClosed) return;
		if (importWorker != null) importWorker.cancel(true);
		int version = importVersion.incrementAndGet();
		progress(version, "Preparing pathway data…");
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			importWorker = new javax.swing.SwingWorker<>() {
				@Override protected Void doInBackground() throws Exception { importData(o, version); return null; }
				@Override protected void done() {
					if (moduleClosed || version != importVersion.get() || isCancelled()) return;
					try { get(); }
					catch (Exception error) {
						Throwable cause = error.getCause() == null ? error : error.getCause();
						egps2.panels.dialog.SwingDialog.showErrorMSGDialog("Pathway import", cause.getMessage());
					} finally { progress(version, null); }
				}
			};
			importWorker.execute();
		} else {
			try { importData(o, version); } finally { progress(version, null); }
		}
	}

	public void cancelPendingImport() {
		importVersion.incrementAndGet();
		if (importWorker != null) importWorker.cancel(true);
	}

	private void progress(int version, String message) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			if (!moduleClosed && version == importVersion.get()) pathwayFamilyMainFace.setImportProgress(message);
		});
	}

	private void importData(OrganizedParameterGetter o, int version) throws Exception {

		ImporterBean4PathwayFamilyBrowser importBeanInfo = paramsAssignerAndParser4pathwayFamBrowser
				.getImportBeanInfo(o);
		// Validate optional paths before replacing the current view.
		for (String path : java.util.List.of(importBeanInfo.getSpeciesInfoPath(), importBeanInfo.getPathwayComponentCountPath(), importBeanInfo.getSpeciesTraitPath())) validatePath(path);
		if (importBeanInfo.getPathwayGalleryPaths() != null) for (String path : importBeanInfo.getPathwayGalleryPaths()) validatePath(path);
		var tables = new java.util.LinkedHashMap<String, PathwayComponentCountData.RawTable>();
		for (String path : java.util.List.of(importBeanInfo.getSpeciesInfoPath(), importBeanInfo.getSpeciesTraitPath(), importBeanInfo.getPathwayComponentCountPath())) {
			if (!disabled(path)) {
				var raw = PathwayComponentCountData.readRawTable(new java.io.File(path));
				if (PathwayComponentCountData.findNameColumnIndex(raw.headers()) < 0 && !path.equals(importBeanInfo.getPathwayComponentCountPath()))
					throw new IllegalArgumentException("Missing Name column: " + path);
				tables.put(path, raw);
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
		PathwayComponentCountData counts = disabled(importBeanInfo.getPathwayComponentCountPath())
				? PathwayComponentCountData.empty() : PathwayComponentCountData.fromRawTable(
					tables.get(importBeanInfo.getPathwayComponentCountPath()).headers(), tables.get(importBeanInfo.getPathwayComponentCountPath()).rows());
		var preparedSlides = new java.util.LinkedHashMap<String, module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide>();
		if (importBeanInfo.getPathwayGalleryPaths() != null) for (String path : importBeanInfo.getPathwayGalleryPaths()) {
			if (moduleClosed || version != importVersion.get()) return;
			if (disabled(path)) continue;
			progress(version, "Preparing pathway " + (preparedSlides.size() + 1) + "/" + importBeanInfo.getPathwayGalleryPaths().size());
			module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide slide;
			try {
				slide = prepareGallerySlide(path, counts,
						() -> moduleClosed || version != importVersion.get());
			} catch (Exception error) {
				throw new IllegalArgumentException("Failed to prepare pathway gallery file: " + path + ": " + error.getMessage(), error);
			}
			if (slide == null) return;
			preparedSlides.put(path, slide);
		}
		PreparedImport prepared = new PreparedImport(counts, preparedSlides, tables);


		javax.swing.SwingUtilities.invokeLater(() -> {
			if (moduleClosed || version != importVersion.get()) return;
			pathwayFamilyMainFace.invokeTheFeatureMethod(0);
			pathwayFamilyMainFace.loadingDataInitializeGraphics(importBeanInfo, treeLayoutProperties, prepared);
		});

	}

	protected module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide prepareGallerySlide(
			String path, PathwayComponentCountData counts, java.util.function.BooleanSupplier cancelled) throws Exception {
		return module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.prepareSlide(path, counts, cancelled);
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
