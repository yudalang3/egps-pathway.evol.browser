package module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/** Percentage controls shared by the gene-family and pathway tree panels. */
public final class CircularInnerRadiusControls extends JPanel {
    private final JSlider slider = new JSlider(20, 100, 20);
    private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(20, 20, 100, 1));
    private final Supplier<TreeLayoutProperties> properties;
    private final Runnable recalculate;
    private boolean synchronizing;

    public CircularInnerRadiusControls(Font font, Supplier<TreeLayoutProperties> properties, Runnable recalculate) {
        super(new MigLayout("insets 0, gap 5, hidemode 3", "[100!][65!]"));
        this.properties = properties;
        this.recalculate = recalculate;
        JLabel title = new JLabel("Inner radius (%)");
        title.setFont(font);
        add(title, "span 2, center, wrap");
        slider.setFocusable(false);
        spinner.setFont(font);
        add(slider, "growx");
        add(spinner, "growx, wrap");
        String help = "<html>Set the inner radius to 20-100% of the outer radius.<br>At 100%, all nodes share one circle and radial branches have zero length.<br>Labels face the center. The drawing area expands when labels need more room.</html>";
        for (JComponent control : new JComponent[]{title, slider, spinner}) {
            control.putClientProperty("tree.parameter", "Circular layout/Inner radius ratio");
            control.setName("Circular layout/Inner radius ratio");
            control.setToolTipText(help);
        }
        slider.addChangeListener(e -> {
            if (!synchronizing && !slider.getValueIsAdjusting()) applyPercentage(slider.getValue());
        });
        spinner.addChangeListener(e -> {
            if (!synchronizing) applyPercentage(((Number) spinner.getValue()).intValue());
        });
        synchronize(null);
    }

    private void applyPercentage(int percent) {
        TreeLayoutProperties current = properties.get();
        if (current == null || current.getMyLayout() != TreeLayout.INNER_CIR_CLADO) return;
        double ratio = percent / 100.0;
        if (java.util.Objects.equals(current.getCircularLayoutPropertiy().getInnerRadiusRatio(), ratio)) return;
        current.getCircularLayoutPropertiy().setInnerRadiusRatio(ratio);
        synchronize(current);
        recalculate.run();
    }

    public void synchronize(TreeLayoutProperties current) {
        synchronizing = true;
        try {
            boolean inward = current != null && current.getMyLayout() == TreeLayout.INNER_CIR_CLADO;
            var circular = current == null ? null : current.getCircularLayoutPropertiy();
            Double ratio = circular == null ? Double.valueOf(0.2) : circular.getInnerRadiusRatio();
            int percent = (int) Math.round(ratio * 100);
            slider.setValue(percent);
            spinner.setValue(percent);
            slider.setEnabled(inward);
            spinner.setEnabled(inward);
        } finally { synchronizing = false; }
    }
}
