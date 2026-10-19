package module.evolview.gfamily.work.gui;

import egps2.frame.gui.comp.toggle.toggle.ToggleAdapter;
import egps2.frame.gui.comp.toggle.toggle.ToggleButton;
import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import module.evolview.gfamily.work.gui.tree.control.Turn2ThisLayoutPanel;
import module.evolview.model.enums.TreeLayoutEnum;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

@SuppressWarnings("serial")
public class GUITreeZoomContainerInTopFixPanel extends BaseCtrlPanel implements Turn2ThisLayoutPanel {
	private PhylogeneticTreePanel observedTree;
	private boolean synchronizing;
	private final PropertyChangeListener dimensionListener = e -> reInitializeGUIAccording2treeLayoutProperties();
	private JSpinner spinnerVerticalOrWholeZoom;
	private JSpinner spinnerHorizontalChange;

	private ChangeListener changeListenerSpinnerVerticalZoom;
	private ChangeListener changeListenerSpinnerHorizontal;

	private JLabel lblHorizontalZoom;
	private JLabel labelVerticalZoom;

	private final int defaultInitializeValue = 100;

	private JCheckBox chckbxLockAspectRatio;
	private ToggleButton height1ScaleTogbtn;
	private ToggleButton width1ScaleTogbtn;

	public GUITreeZoomContainerInTopFixPanel() {
		GridBagLayout gbl_zoomAndRotate_1 = new GridBagLayout();
		gbl_zoomAndRotate_1.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_zoomAndRotate_1.rowHeights = new int[] { 0, 0, 23, 23, 0, 12, 0 };
		gbl_zoomAndRotate_1.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
		gbl_zoomAndRotate_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gbl_zoomAndRotate_1);

		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 2;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 1;
		add(separator, gbc_separator);

		labelVerticalZoom = new JLabel("Height scale");
		labelVerticalZoom.setToolTipText("Zoom in or zoom out!");
		labelVerticalZoom.setFont(globalFont);
		GridBagConstraints gbc_labelVerticalZoom = new GridBagConstraints();
		gbc_labelVerticalZoom.anchor = GridBagConstraints.WEST;
		gbc_labelVerticalZoom.insets = new Insets(0, 0, 5, 5);
		gbc_labelVerticalZoom.gridx = 0;
		gbc_labelVerticalZoom.gridy = 2;
		add(labelVerticalZoom, gbc_labelVerticalZoom);

		spinnerVerticalOrWholeZoom = new JSpinner();
		spinnerVerticalOrWholeZoom
				.setModel(new SpinnerNumberModel(defaultInitializeValue, defaultInitializeValue, 100000, 1));
		spinnerVerticalOrWholeZoom.setFont(globalFont);
		GridBagConstraints gbc_spinnerVerticalZoom = new GridBagConstraints();
		gbc_spinnerVerticalZoom.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerVerticalZoom.gridx = 1;
		gbc_spinnerVerticalZoom.gridy = 2;
		add(spinnerVerticalOrWholeZoom, gbc_spinnerVerticalZoom);

		JLabel lblNewLabel = new JLabel("%");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 2;
		gbc_lblNewLabel.gridy = 2;
		add(lblNewLabel, gbc_lblNewLabel);

		Dimension dimension = new Dimension(35, 25);
		height1ScaleTogbtn = new ToggleButton();
		java.awt.Color fg = new java.awt.Color(40, 139, 236);
		height1ScaleTogbtn.setForeground(fg);
		height1ScaleTogbtn.setSelected(true);
		height1ScaleTogbtn.setPreferredSize(dimension);
		height1ScaleTogbtn.setToolTipText("Control whether the mouse wheel action take effects on height scale.");
		height1ScaleTogbtn.addEventToggleSelected(new ToggleAdapter() {
			@Override
			public void onSelected(boolean selected) {
				if (controller != null && !synchronizing && controller.getActiveTreeLayoutProperties() != null) {
					controller.getActiveTreeLayoutProperties().setWhetherHeightScaleOnMouseWheel(selected);
					controller.getSelectedPhylogeneticTreePanel().notifyTreeViewStateChanged();
				}
			}
			
		});

		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridx = 3;
		gbc_btnNewButton.gridy = 2;
		add(height1ScaleTogbtn, gbc_btnNewButton);

		lblHorizontalZoom = new JLabel("Width scale");
		lblHorizontalZoom.setToolTipText("Zoom in or zoom out!");
		lblHorizontalZoom.setFont(globalFont);
		GridBagConstraints gbc_lblHorizontalZoom = new GridBagConstraints();
		gbc_lblHorizontalZoom.anchor = GridBagConstraints.WEST;
		gbc_lblHorizontalZoom.insets = new Insets(0, 0, 5, 5);
		gbc_lblHorizontalZoom.gridx = 0;
		gbc_lblHorizontalZoom.gridy = 3;
		add(lblHorizontalZoom, gbc_lblHorizontalZoom);

		spinnerHorizontalChange = new JSpinner();
		spinnerHorizontalChange
				.setModel(new SpinnerNumberModel(defaultInitializeValue, defaultInitializeValue, 100000, 1));
		spinnerHorizontalChange.setFont(globalFont);
		GridBagConstraints gbc_spinnerHorizontalChange = new GridBagConstraints();
		gbc_spinnerHorizontalChange.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerHorizontalChange.anchor = GridBagConstraints.WEST;
		gbc_spinnerHorizontalChange.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerHorizontalChange.gridx = 1;
		gbc_spinnerHorizontalChange.gridy = 3;
		add(spinnerHorizontalChange, gbc_spinnerHorizontalChange);

		JLabel lblNewLabel_1 = new JLabel("%");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 2;
		gbc_lblNewLabel_1.gridy = 3;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		width1ScaleTogbtn = new ToggleButton();
		width1ScaleTogbtn.setPreferredSize(dimension);
		width1ScaleTogbtn.setSelected(true);
		width1ScaleTogbtn.setForeground(fg);
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_1.gridx = 3;
		gbc_btnNewButton_1.gridy = 3;
		add(width1ScaleTogbtn, gbc_btnNewButton_1);
		width1ScaleTogbtn.setToolTipText("Control whether the mouse wheel action take effects on width scale.");
		width1ScaleTogbtn.addEventToggleSelected(new ToggleAdapter() {
			@Override
			public void onSelected(boolean selected) {
				if (controller != null && !synchronizing && controller.getActiveTreeLayoutProperties() != null) {
					controller.getActiveTreeLayoutProperties().setWhetherWidthScaleOnMouseWheel(selected);
					controller.getSelectedPhylogeneticTreePanel().notifyTreeViewStateChanged();
				}
			}
			
		});

		chckbxLockAspectRatio = new JCheckBox("Lock aspect ratio");
		chckbxLockAspectRatio.setFocusable(false);
		chckbxLockAspectRatio.addItemListener(event -> {
			if (!synchronizing && controller != null && controller.getSelectedPhylogeneticTreePanel() != null) {
				controller.getActiveTreeLayoutProperties().setLockAspectRatio(chckbxLockAspectRatio.isSelected());
				controller.getSelectedPhylogeneticTreePanel().notifyTreeViewStateChanged();
			}
		});
		chckbxLockAspectRatio.setFont(globalFont);
		chckbxLockAspectRatio.setToolTipText("<html>This only influence the manually height and width scale in spinner.<br>The mouse wheel zoom in and out will not be affected.");;
		GridBagConstraints gbc_chckbxLockAspectRatio = new GridBagConstraints();
		gbc_chckbxLockAspectRatio.fill = GridBagConstraints.HORIZONTAL;
		gbc_chckbxLockAspectRatio.gridwidth = 2;
		gbc_chckbxLockAspectRatio.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxLockAspectRatio.gridx = 0;
		gbc_chckbxLockAspectRatio.gridy = 4;
		add(chckbxLockAspectRatio, gbc_chckbxLockAspectRatio);

		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.insets = new Insets(0, 0, 0, 5);
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.anchor = GridBagConstraints.CENTER;
		gbc_separator_1.gridwidth = 4;
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 5;
		add(separator_1, gbc_separator_1);

		changeListenerSpinnerHorizontal = e -> applyScale(false);
		changeListenerSpinnerVerticalZoom = e -> applyScale(true);
		actionsTurn2ThisLayout(TreeLayoutEnum.RECTANGULAR_LAYOUT);
	}

	private void applyScale(boolean heightChanged) {
		if (synchronizing || controller == null) return;
		PhylogeneticTreePanel tree = controller.getSelectedPhylogeneticTreePanel();
		if (tree == null) return;
		Dimension old = tree.getTreePanelScale();
		int height = (int) spinnerVerticalOrWholeZoom.getValue();
		int width = (int) spinnerHorizontalChange.getValue();
		if (tree.isUniformScaleLayout()) {
			if (heightChanged) width = height; else height = width;
		} else if (chckbxLockAspectRatio.isSelected()) {
			double factor = heightChanged ? height / (double) old.height : width / (double) old.width;
			factor = Math.max(factor, Math.max(100.0 / old.width, 100.0 / old.height));
			factor = Math.min(factor, Math.min(100000.0 / old.width, 100000.0 / old.height));
			height = (int) Math.round(old.height * factor);
			width = (int) Math.round(old.width * factor);
		}
		tree.setTreePanelScale(height, width);
		reInitializeGUIAccording2treeLayoutProperties();
	}

	@Override
	public void actionsTurn2ThisLayout(TreeLayoutEnum layout) {
		spinnerVerticalOrWholeZoom.removeChangeListener(changeListenerSpinnerVerticalZoom);
		spinnerHorizontalChange.removeChangeListener(changeListenerSpinnerHorizontal);
		spinnerVerticalOrWholeZoom.addChangeListener(changeListenerSpinnerVerticalZoom);
		spinnerHorizontalChange.addChangeListener(changeListenerSpinnerHorizontal);
		reInitializeGUIAccording2treeLayoutProperties();
		if (controller != null) controller.fireLayoutChanged();
	}

	public void resetZoomInAndOut2zero() {
		if (controller != null && controller.getSelectedPhylogeneticTreePanel() != null)
			controller.getSelectedPhylogeneticTreePanel().setTreePanelScale(100, 100);
		reInitializeGUIAccording2treeLayoutProperties();
	}

	/** Synchronize display only; dimensions are owned by the tree panel. */
	public void resetZoomLayoutPanel(int newHeight, int newWidth) {
		reInitializeGUIAccording2treeLayoutProperties();
	}

	@Override
	public void reInitializeGUIAccording2treeLayoutProperties() {
		if (controller == null) return;
		var properties = controller.getActiveTreeLayoutProperties();
		PhylogeneticTreePanel tree = properties == null ? null : controller.getSelectedPhylogeneticTreePanel();
		if (tree != observedTree) {
			if (observedTree != null) observedTree.removePropertyChangeListener("treePanelDimension", dimensionListener);
			observedTree = tree;
			if (tree != null) tree.addPropertyChangeListener("treePanelDimension", dimensionListener);
		}
		synchronizing = true;
		try {
			Dimension scale = tree == null ? new Dimension(100, 100) : tree.getTreePanelScale();
			spinnerVerticalOrWholeZoom.setValue(Math.min(100000, scale.height));
			spinnerHorizontalChange.setValue(Math.min(100000, scale.width));
			spinnerVerticalOrWholeZoom.setEnabled(tree != null);
			spinnerHorizontalChange.setEnabled(tree != null);
			boolean whole = tree != null && tree.isUniformScaleLayout();
			chckbxLockAspectRatio.setEnabled(!whole && tree != null);
			chckbxLockAspectRatio.setSelected(whole || properties != null && properties.isLockAspectRatio());
			height1ScaleTogbtn.setEnabled(tree != null);
			width1ScaleTogbtn.setEnabled(tree != null);
			if (properties != null) {
				height1ScaleTogbtn.setSelected(properties.isWhetherHeightScaleOnMouseWheel());
				width1ScaleTogbtn.setSelected(properties.isWhetherWidthScaleOnMouseWheel());
			}
		} finally {
			synchronizing = false;
		}
	}
}
