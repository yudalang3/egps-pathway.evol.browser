package module.evolview.moderntreeviewer.io;

import egps2.panels.dialog.EGPSFileChooser;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.util.EGPSPrintUtilities;
import egps2.frame.gui.VectorGraphicsEncoder;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.moderntreeviewer.gui.ModernTreeOperationPanel;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.*;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** MTV owns Newick saving; the host continues to render image formats. */
public class TreeExportHandler {
    private static final Logger log = LoggerFactory.getLogger(TreeExportHandler.class);
    private boolean saving;

    public boolean isSaving() { return saving; }

    public void export(PhylogeneticTreePanel tree, Class<?> owner) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> export(tree, owner));
            return;
        }
        if (tree == null || saving) return;
        saving = true;
        Path target = null;
        try {
            var snapshot = ModernTreeOperationPanel.exportSnapshot(tree.getRootNode(), false);
            target = chooseDestination(owner);
            if (target == null) { saving = false; return; }
            if (Files.exists(target) && !confirmOverwrite(target)) { saving = false; return; }
            if (!target.toString().toLowerCase(Locale.ROOT).endsWith(".nwk")) {
                try { renderImage(target, tree); success(target); }
                finally { saving = false; }
                return;
            }
            Path destination = target;
            CompletableFuture.runAsync(() -> {
                try { write(destination, new PhyloGraphicsTreeEncoderDecoder().encode(snapshot)); }
                catch (IOException error) { throw new java.util.concurrent.CompletionException(error); }
            }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
                saving = false;
                if (error == null) success(destination);
                else failure(destination, error.getCause() == null ? error : error.getCause());
            }));
        } catch (Exception error) {
            saving = false;
            failure(target, error);
        }
    }

    protected Path chooseDestination(Class<?> owner) {
        EGPSFileChooser chooser = new EGPSFileChooser(owner);
        chooser.setAcceptAllFileFilterUsed(false);
        for (String extension : new String[]{"nwk", "pdf", "pptx", "jpg", "png", "svg", "eps"})
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(extension.toUpperCase(Locale.ROOT) + " files", extension));
        chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
        if (chooser.showSaveDialog() != JFileChooser.APPROVE_OPTION) return null;
        String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
        return withExtension(chooser.getSelectedFile().toPath(), extension);
    }

    public static Path withExtension(Path path, String extension) {
        return path.toString().toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT))
                ? path : Path.of(path + "." + extension);
    }

    private void renderImage(Path path, PhylogeneticTreePanel tree) throws Exception {
        String extension = path.getFileName().toString();
        extension = extension.substring(extension.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        switch (extension) {
            case "jpg", "png" -> EGPSPrintUtilities.saveAsBitGraphics(extension, tree, path.toFile());
            case "pdf" -> EGPSPrintUtilities.saveAsPDF(tree, path.toFile());
            case "pptx" -> EGPSPrintUtilities.saveAsPptx(tree, path.toFile());
            case "svg" -> VectorGraphicsEncoder.saveVectorGraphic(tree, path.toString(), VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
            case "eps" -> VectorGraphicsEncoder.saveVectorGraphic(tree, path.toString(), VectorGraphicsEncoder.VectorGraphicsFormat.EPS);
            default -> throw new IllegalArgumentException("Unsupported export format: " + extension);
        }
    }

    protected boolean confirmOverwrite(Path path) {
        Object[] options = {"Cancel", "Overwrite"};
        return JOptionPane.showOptionDialog(null, "File already exists:\n" + path + "\nOverwrite this file?",
                "Export tree", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]) == 1;
    }

    protected void write(Path path, String text) throws IOException {
        Path target = path.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), "mtv-export-", ".tmp");
        try {
            Files.writeString(temporary, text);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    protected void success(Path path) { SwingDialog.showInfoMSGDialog("Export tree", "Saved successfully:\n" + path); }

    protected void failure(Path path, Throwable error) {
        log.error("Unable to export tree to {}", path, error);
        SwingDialog.showErrorMSGDialog("Export tree", "Unable to save" + (path == null ? "" : " to " + path) + ":\n" + error.getMessage());
    }
}
