package module.evolview.pathwaybrowser.io;

import module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide;
import java.util.Map;

/** Tables and fully rendered diagrams, prepared before replacing the current view. */
public record PreparedImport(PathwayComponentCountData counts, Map<String, PreparedSlide> slides,
        Map<String, PathwayComponentCountData.RawTable> tables) {
    public PreparedImport { slides = Map.copyOf(slides); tables = Map.copyOf(tables); }
}
