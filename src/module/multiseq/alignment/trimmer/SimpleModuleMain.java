package module.multiseq.alignment.trimmer;

import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import module.multiseq.alignment.trimmer.gui.SimpleLeftControlPanel;

import java.awt.*;

@SuppressWarnings("serial")
public class SimpleModuleMain extends ModuleFace{


	private final SimpleModuleController controller ;
	private SimpleLeftControlPanel leftControlPanel;
	
	protected SimpleModuleMain(IModuleLoader moduleLoader) {
		super(moduleLoader);
		controller = new SimpleModuleController(this);
		setLayout(new BorderLayout());
		
		leftControlPanel = new SimpleLeftControlPanel(controller);
		add(leftControlPanel,BorderLayout.CENTER);
	}
	
	public String[] getTeamAndAuthors() {
		return controller.getTeamAndAuthors();	
	}

	public SimpleModuleController getController() {
		return controller;
	}

	public void enableAllGUIComponents(boolean b) {
		leftControlPanel.enableAllGUIComponents(b);
		
	}

	@Override
	public boolean closeTab() {
		return false;
	}

	@Override
	public void changeToThisTab() {
		
	}


	@Override
	public boolean canImport() {
		return false;
	}

	@Override
	public void importData() {
		
	}

	@Override
	public boolean canExport() {
		return false;
	}

	@Override
	public void exportData() {
		
	}

	@Override
	public String[] getFeatureNames() {
		return new String[] { "Trim the alignment" };
	}

	@Override
	protected void initializeGraphics() {
		
	}

}
