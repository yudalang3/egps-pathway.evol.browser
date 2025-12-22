package module.pill.images;

import javax.swing.*;
import java.net.URL;

public class ImageUtils {

	
	public static Icon getIcon(String str) {
		URL resource = ImageUtils.class.getResource(str);
		Icon icon = new ImageIcon(resource);
		return icon;
	}
	
	public static URL getResource(String str) {
		URL resource = ImageUtils.class.getResource(str);
		return resource;
	}
}
