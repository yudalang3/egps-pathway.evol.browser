package module.evolview.phylotree.visualization.layout;

import java.awt.Container;

import module.evolview.model.tree.ScaleBarProperty;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties4LinageType;

/**
 * Host interface for tree layouts.
 * Implemented by UI panels (e.g., TreeLayoutHost) so the layout engine
 * does not depend on a concrete application module.
 */
public interface TreeLayoutHost {
	ScaleBarProperty getScaleBarProperty();
	AnnotationsProperties getMainAnnotationsProperties();
	AnnotationsProperties4LinageType getLinageTypeAnnotationsProperties();
	Container getHostComponent();
}
