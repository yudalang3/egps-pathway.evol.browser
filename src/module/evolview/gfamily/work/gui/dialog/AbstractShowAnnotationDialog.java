package module.evolview.gfamily.work.gui.dialog;

import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;


@SuppressWarnings("serial")
public abstract class AbstractShowAnnotationDialog extends DefaultMutableTreeNode {
	
	protected PhylogeneticTreePanel controller;
	protected Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	public AbstractShowAnnotationDialog(PhylogeneticTreePanel phyloTreePanel) {
		this.controller = phyloTreePanel;
	}

	/**
	 * Put back the right side of each node <code>JPanel</code>
	 * 
	 * @author mhl
	 * 
	 * @Date Created on: 2019-04-25 09:02
	 * 
	 */
	public abstract JComponent getViewJPanel();


	public abstract void configurateAnnotationProperty2preview();

}
