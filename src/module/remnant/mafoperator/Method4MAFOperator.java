package module.remnant.mafoperator;

import module.remnant.datapanel.DefaultPanel;

import javax.swing.*;



public class Method4MAFOperator  {


	public void turnToModule() {
		SwingUtilities.invokeLater(() -> {
			DefaultPanel component = new DefaultPanel();
		});

	}

	public String getModuleTabName() {
		return "MAF Operator";
	}

}
