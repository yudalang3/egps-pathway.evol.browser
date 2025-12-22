package module;

import egps2.EGPSProperties;
import egps2.Launcher;
import egps2.Launcher4Dev;
import egps2.frame.MainFrameProperties;
import egps2.panels.dialog.SwingDialog;
import module.evolview.moderntreeviewer.IndependentModuleLoader;

import javax.swing.*;

/**
 * 这是一个沟通R的JAVA类
 * 
 * @author yudal
 *
 */
public class RlangInterfaceEGPS {

	/**
	 * 启动
	 * 
	 * @return
	 * @throws Exception
	 */
	public String launch() throws Exception {
		Launcher4Dev.main(new String[]{});
		Launcher.isLaunchFromR = true;
		return "Hello this is eGPS desktop, version: ".concat(EGPSProperties.EGPS_VERSION);
	}

	/**
	 * 用来进行测试
	 * 
	 * @param jsonStr
	 * @return
	 */
	public String callTest(String jsonStr) {
		SwingUtilities.invokeLater(() -> {
			SwingDialog.showInfoMSGDialog("Info", jsonStr);
		});
		return String.valueOf(jsonStr.length());
	}

	/**
	 * 载入进化树模块
	 * 
	 * @param jsonStr 通过JSON把R的变量/对象 传进来
	 */
	public void modernTreeView(String jsonStr) {
		/**
		 * 这里可以写 jsonStr字符串的处理方法，生成JAVA对象
		 */
		SwingUtilities.invokeLater(() -> {
			IndependentModuleLoader loader = new IndependentModuleLoader();
			MainFrameProperties.loadTheModuleFromIModuleLoader(loader);
		});
	}

}
