package module.evolview.genebrowser;

import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.SwingDialog;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.beans.RequiredGeneData;
import module.evolview.gfamily.work.beans.TrackMultipleDomainBean;
import module.evolview.gfamily.work.gui.browser.BrowserPanel;
import module.evolview.gfamily.work.gui.browser.DrawIntermediatedProperties;
import module.evolview.gfamily.work.gui.browser.GeneDrawingLengthCursor;
import module.evolview.gfamily.work.gui.browser.TrackAlignment;
import module.evolview.gfamily.work.gui.browser.TrackGeneStructure;
import module.evolview.gfamily.work.gui.browser.TrackKeyDomains;
import module.evolview.gfamily.work.gui.browser.TrackPrimers;
import module.evolview.gfamily.work.gui.browser.draw.AlignmentWithDerivedStatistics;
import module.evolview.gfamily.work.gui.browser.draw.DrawingPropertyPrimerSet;
import module.evolview.model.AlignmentGetSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.io.IOException;
import java.util.Optional;

@SuppressWarnings("serial")
public class GBBrowserPanel extends BrowserPanel {

	private static final Logger log = LoggerFactory.getLogger(GBBrowserPanel.class);

	public GBBrowserPanel(GeneFamilyController controller, RequiredGeneData geneData) {
		super(controller, geneData);
	}

	protected void intitialTracks() {
		try {
			geneStructureInfo = geneData.getGeneStructComputerStruct();
		} catch (IOException e) {
			log.error("Failed to build gene structure model.", e);
			SwingDialog.showErrorMSGDialog("Load error", "Failed to build gene structure model.\n" + e.getMessage());
			return;
		}

		Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
		geneDrawingLengthCursor = new GeneDrawingLengthCursor(defaultFont);
		geneDrawingLengthCursor.setDrawStart(1);
		int geneLength = geneStructureInfo.geneLength;
		geneDrawingLengthCursor.setDrawEnd(geneLength + 1);
		geneDrawingLengthCursor.setMinValue(1);
		geneDrawingLengthCursor.setMaxValue(geneLength + 1);

		this.calculatorDataForViewer = new DrawIntermediatedProperties(geneDrawingLengthCursor);

		this.virusGeomeStructure = new TrackGeneStructure(this, geneStructureInfo);
		tracks.add(virusGeomeStructure);

		Optional<AlignmentGetSequence> alignmentSequence = geneData.getAlignmentSequence();
		if (alignmentSequence.isPresent()) {
			AlignmentWithDerivedStatistics alignmentStati = new AlignmentWithDerivedStatistics();
			alignmentStati.setSequence(alignmentSequence.get());

			TrackAlignment genomerAlignment = new TrackAlignment(this, alignmentStati);
			tracks.add(genomerAlignment);
		}

		Optional<TrackMultipleDomainBean> keyDomains = geneData.getKeyDomains();
		if (keyDomains.isPresent()) {
			TrackMultipleDomainBean drawingPropertyElementModelItem = keyDomains.get();
			TrackKeyDomains elementStructure = new TrackKeyDomains(this, drawingPropertyElementModelItem);
			tracks.add(elementStructure);
		}

		Optional<DrawingPropertyPrimerSet> primerSets = geneData.getPrimerSets();
		if (primerSets.isPresent() && alignmentSequence.isPresent()) {
			TrackPrimers genomePrimers = new TrackPrimers(this, primerSets.get(), alignmentSequence.get());
			tracks.add(genomePrimers);
		}

		initializeTrackGUI();
	}

}
