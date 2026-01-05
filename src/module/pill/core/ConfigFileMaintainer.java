package module.pill.core;

import egps2.EGPSProperties;
import egps2.panels.dialog.SwingDialog;

import javax.swing.*;
import java.io.*;
import java.util.Properties;

public class ConfigFileMaintainer {

	// 配置文件的文件名
	String configFile = EGPSProperties.JSON_DIR + "/config.properties";

	Properties properties = new Properties();

	public static final String VERSION_INFORMATION = "PathwayIlluminator version 0.0.0.1";

	public void loadProperties() {
		try {
			InputStream inputStream = new FileInputStream(configFile);
			properties.load(inputStream);
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

//    	String username = properties.getProperty("username");
//        String password = properties.getProperty("password");
//        
//        System.out.println("Username: " + username);
//        System.out.println("Password: " + password);
	}

	public boolean shouldLoadTheLicense() {
		File file = new File(configFile);
		if (file.exists()) {
			loadProperties();
			String property = properties.getProperty(CONSTANTS.LICENSE_NAME);
			if (property == null) {
				return true;
			}

			return Boolean.parseBoolean(property);
		} else {
			return true;
		}
	}

	public static void main(String[] args) {

	}

	public void putAndSaveValue(String licenseName, String value) {
		File file = new File(configFile);
		if (file.exists()) {

		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		loadProperties();
		properties.setProperty(licenseName, value);

		try {
			properties.store(new FileWriter(file), "The properties files.");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void writeObjectToFile(Object obj, File outputFile, JComponent skeletonMaker) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(outputFile);
			ObjectOutputStream objOut = new ObjectOutputStream(out);
			objOut.writeObject(obj);
			objOut.flush();
			objOut.close();
			
			if (skeletonMaker == null) {
				System.out.println("Success");
			}else {
				SwingDialog.showSuccessMSGDialog("Export success", "Successfully output the content.");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Object readObjectFromFile(File file) {
		Object temp = null;
		FileInputStream in;
		try {
			in = new FileInputStream(file);
			ObjectInputStream objIn = new ObjectInputStream(in);
			temp = objIn.readObject();
			objIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return temp;
	}
}