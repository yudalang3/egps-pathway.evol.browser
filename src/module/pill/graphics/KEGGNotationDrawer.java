package module.pill.graphics;

import egps2.UnifiedAccessPoint;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

/**
 * Regular map Notation <br>
 * https://www.genome.jp/kegg/document/help_pathway.html
 * 
 * 
 */
public class KEGGNotationDrawer {

	private Image image;
	private Font titleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();

	public KEGGNotationDrawer() {
	}

	public void drawNotation(Graphics2D g2d) {

		g2d.setFont(titleFont);
		g2d.drawString("The KEGG Regular map Notation", 300, 30);

		if (image == null) {
			try {
				image = ImageIO.read(getClass().getResourceAsStream("symbols.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		g2d.drawImage(image, 50, 50, null);

	}



}
