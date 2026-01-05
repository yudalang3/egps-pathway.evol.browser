package module.evolview.gfamily.work.gui.tree.control;

import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import module.evolview.phylotree.visualization.graphics.struct.SlopeLayoutProperty;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class SubSlopeLayout extends BaseCtrlPanel {
	private JSpinner spinnerOfLeafLocation;
	private JSlider sliderOfLeafLocation;
	private JLabel lblRotation;
	private JSeparator separator;
	private JSpinner spinnerOfMargin;

	private JToggleButton btnRotate0;
	private JToggleButton btnRotate90;
	private JToggleButton btnRotate180;
	private JToggleButton btnRotate270;

	public SubSlopeLayout() {
//		setBorder(new EmptyBorder(15, 15, 15, 15));

		GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout(gridBagLayout);

		// Label: Tree width (%)
		JLabel lblLeafLocationRatio = new JLabel("Tree width (%)");
		lblLeafLocationRatio.setFont(globalFont);
		lblLeafLocationRatio.setToolTipText("Width of tree area (100% = full width, smaller values add right margin)");
		GridBagConstraints gbc_lblLeafLocationRatio = new GridBagConstraints();
		gbc_lblLeafLocationRatio.anchor = GridBagConstraints.WEST;
		gbc_lblLeafLocationRatio.insets = new Insets(0, 0, 5, 5);
		gbc_lblLeafLocationRatio.gridx = 0;
		gbc_lblLeafLocationRatio.gridy = 0;
		gbc_lblLeafLocationRatio.gridwidth = 2;
		add(lblLeafLocationRatio, gbc_lblLeafLocationRatio);

		sliderOfLeafLocation = new JSlider();
		sliderOfLeafLocation.setFocusable(false);
		sliderOfLeafLocation.setValue(100);
		sliderOfLeafLocation.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int value = sliderOfLeafLocation.getValue();
				spinnerOfLeafLocation.setValue(value);
			}
		});
		sliderOfLeafLocation.setPreferredSize(new Dimension(100, 23));
		GridBagConstraints gbc_sliderOfLeafLocation = new GridBagConstraints();
		gbc_sliderOfLeafLocation.anchor = GridBagConstraints.WEST;
		gbc_sliderOfLeafLocation.insets = new Insets(0, 0, 5, 5);
		gbc_sliderOfLeafLocation.gridx = 0;
		gbc_sliderOfLeafLocation.gridy = 1;
		add(sliderOfLeafLocation, gbc_sliderOfLeafLocation);

		spinnerOfLeafLocation = new JSpinner();
		spinnerOfLeafLocation.setFont(globalFont);
		spinnerOfLeafLocation.setModel(new SpinnerNumberModel(100, 0, 100, 1));
		GridBagConstraints gbc_spinnerOfLeafLocation = new GridBagConstraints();
		gbc_spinnerOfLeafLocation.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerOfLeafLocation.gridx = 1;
		gbc_spinnerOfLeafLocation.gridy = 1;
		add(spinnerOfLeafLocation, gbc_spinnerOfLeafLocation);

		// Label: Left margin (%)
		JLabel lblNewLabel = new JLabel("Left margin (%)");
		lblNewLabel.setFont(globalFont);
		lblNewLabel.setToolTipText("Empty space on the left side of the tree");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 2;
		gbc_lblNewLabel.gridwidth = 2;
		add(lblNewLabel, gbc_lblNewLabel);

		JSlider sliderOfMargin = new JSlider();
		sliderOfMargin.setFont(globalFont);
		sliderOfMargin.setFocusable(false);
		sliderOfMargin.setValue(20);
		sliderOfMargin.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int value = sliderOfMargin.getValue();
				spinnerOfMargin.setValue(value);
			}
		});
		sliderOfMargin.setPreferredSize(new Dimension(100, 23));

		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 3;
		add(sliderOfMargin, gbc_lblNewLabel_1);

		spinnerOfMargin = new JSpinner();
		spinnerOfMargin.setFont(globalFont);
		spinnerOfMargin.setModel(new SpinnerNumberModel(20, 0, 100, 1));
		GridBagConstraints gbc_spinnerOfMargin = new GridBagConstraints();
		gbc_spinnerOfMargin.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerOfMargin.gridx = 1;
		gbc_spinnerOfMargin.gridy = 3;
		add(spinnerOfMargin, gbc_spinnerOfMargin);

		separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.gridwidth = 2;
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 4;
		add(separator, gbc_separator);

		lblRotation = new JLabel("Rotation");
		lblRotation.setFont(globalFont);
		GridBagConstraints gbc_lblRotation = new GridBagConstraints();
		gbc_lblRotation.anchor = GridBagConstraints.WEST;
		gbc_lblRotation.insets = new Insets(0, 0, 5, 5);
		gbc_lblRotation.gridx = 0;
		gbc_lblRotation.gridy = 5;
		gbc_lblRotation.gridwidth = 2;
		add(lblRotation, gbc_lblRotation);

		// Rotation toggle buttons in 2x2 grid using MigLayout
		JPanel rotationPanel = new JPanel(new MigLayout("insets 0, gap 4", "[grow,fill][grow,fill]", "[][]"));
		ButtonGroup rotationGroup = new ButtonGroup();

		btnRotate0 = createRotationButton("0°", 0);
		btnRotate90 = createRotationButton("90°", 90);
		btnRotate180 = createRotationButton("180°", 180);
		btnRotate270 = createRotationButton("270°", 270);

		btnRotate0.setSelected(true);

		rotationGroup.add(btnRotate0);
		rotationGroup.add(btnRotate90);
		rotationGroup.add(btnRotate180);
		rotationGroup.add(btnRotate270);

		rotationPanel.add(btnRotate0);
		rotationPanel.add(btnRotate90, "wrap");
		rotationPanel.add(btnRotate180);
		rotationPanel.add(btnRotate270);

		GridBagConstraints gbc_rotationPanel = new GridBagConstraints();
		gbc_rotationPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_rotationPanel.gridwidth = 2;
		gbc_rotationPanel.gridx = 0;
		gbc_rotationPanel.gridy = 6;
		add(rotationPanel, gbc_rotationPanel);

		spinnerOfLeafLocation.addChangeListener(e -> {
			JSpinner source = (JSpinner) e.getSource();
			int value = (int) source.getValue();
			sliderOfLeafLocation.setValue(value);
			SlopeLayoutProperty slopeLayoutProperety = controller.getTreeLayoutProperties().getSlopeLayoutProperety();
			slopeLayoutProperety.setLeafLocationRightMostRatio(0.01 * value);
			controller.reCalculateTreeLayoutAndRefreshViewPort();
		});
		spinnerOfMargin.addChangeListener(e -> {
			JSpinner source = (JSpinner) e.getSource();
			int value = (int) source.getValue();
			sliderOfMargin.setValue(value);

			SlopeLayoutProperty slopeLayoutProperety = controller.getTreeLayoutProperties().getSlopeLayoutProperety();
			slopeLayoutProperety.setBlankMarginRatio(0.01 * value);
			controller.reCalculateTreeLayoutAndRefreshViewPort();
		});

	}

	private JToggleButton createRotationButton(String text, int degrees) {
		JToggleButton btn = new JToggleButton(text);
		btn.setFont(globalFont);
		btn.setFocusable(false);
		btn.addActionListener(e -> {
			if (btn.isSelected()) {
				controller.getTreeLayoutProperties().getSlopeLayoutProperety().setSlopeLayoutRotationDeg(degrees);
				controller.reCalculateTreeLayoutAndRefreshViewPort();
			}
		});
		return btn;
	}

}
