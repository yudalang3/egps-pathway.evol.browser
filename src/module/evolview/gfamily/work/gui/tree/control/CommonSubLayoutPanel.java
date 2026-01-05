package module.evolview.gfamily.work.gui.tree.control;

import module.evolview.gfamily.GeneFamilyController;

import javax.swing.*;

@SuppressWarnings("serial")
public abstract class CommonSubLayoutPanel extends JPanel {
	protected GeneFamilyController controller;


    public CommonSubLayoutPanel(GeneFamilyController controller) {
        this.controller = controller;
    }


    public void setController(GeneFamilyController controller) {
        this.controller = controller;
    }

}
