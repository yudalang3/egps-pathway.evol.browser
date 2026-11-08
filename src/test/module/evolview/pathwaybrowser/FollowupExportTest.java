package module.evolview.pathwaybrowser;
import module.evolview.pathwaybrowser.gui.analysis.panel.*;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

public class FollowupExportTest {
    public static void main(String[] args) throws Exception {
        var dir=Path.of("temp","review-export"); Files.createDirectories(dir);
        var image=new BufferedImage(420,260,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.setColor(Color.RED);g.fillRect(0,0,420,260);g.setColor(Color.BLUE);g.fillRect(414,254,6,6);g.dispose();
        var source=dir.resolve("source.png");ImageIO.write(image,"png",source.toFile());
        var prepared=PptxSlideRenderer.prepareSlide(source.toString(),PathwayComponentCountData.empty(),()->false);
        JPanel[] snapshot=new JPanel[1];
        SwingUtilities.invokeAndWait(()->{var panel=new SubSingleSlidePanel(source.toString());panel.installPrepared(prepared,PathwayComponentCountData.empty());snapshot[0]=panel.createExportSnapshot();});
        var saver=new egps2.utils.common.util.SaveUtil();
        for(String ext:new String[]{"jpg","svg","eps","pdf","pptx"}) {
            Path file=dir.resolve("snapshot."+ext);
            SwingUtilities.invokeAndWait(()->{try{if (ext.equals("pptx")) module.evolview.pathwaybrowser.io.PathwaySnapshotSaveUtil.savePptx(snapshot[0], file.toFile()); else saver.directlyProduceFigureAccording2filePath(file.toString(),snapshot[0]);}catch(Exception e){throw new RuntimeException(e);}});
            System.out.println(ext+" bytes="+Files.size(file));
            if(ext.equals("jpg")){var loaded=ImageIO.read(file.toFile());System.out.println("jpg size="+loaded.getWidth()+"x"+loaded.getHeight());}
            if(ext.equals("pptx")){try(var book=new XMLSlideShow(Files.newInputStream(file))){if(book.getPictureData().size()!=1)throw new AssertionError("PPTX image missing");System.out.println("pptx size="+book.getPageSize()+" pictures="+book.getPictureData().size());var back=new BufferedImage(420,260,BufferedImage.TYPE_INT_ARGB);var graphics=back.createGraphics();book.getSlides().get(0).draw(graphics);graphics.dispose();if(back.getRGB(200,100)!=image.getRGB(200,100))throw new AssertionError("PPTX pixels missing");ImageIO.write(back,"png",dir.resolve("pptx-readback.png").toFile());System.out.println("pptx readback center="+Integer.toHexString(back.getRGB(200,100))+" expected red="+Integer.toHexString(image.getRGB(200,100)));}}
            if(ext.equals("svg")){String text=Files.readString(file);System.out.println("svg image="+text.contains("<image")+" size-prefix="+text.substring(0,Math.min(500,text.length())).replace('\n',' '));}
        }
        System.exit(0);
    }
}
