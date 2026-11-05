package module.evolview.pathwaybrowser.io;

import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.moderntreeviewer.io.TreePropertiesAssigner;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import java.nio.file.*;

public final class PathwayBrowserExamplesTest {
    static void require(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path data=Files.createTempDirectory(Path.of("temp"),"pathway-examples-");
        PathwayBrowserExamples.install(data);
        Files.writeString(data.resolve("info.tsv"),"user data");
        Files.delete(data.resolve("traits.tsv"));
        PathwayBrowserExamples.install(data);
        require(Files.readString(data.resolve("info.tsv")).equals("user data"),"Preserve user edits");
        require(Files.isRegularFile(data.resolve("traits.tsv")),"Restore missing resource");
        Files.delete(data.resolve("info.tsv"));PathwayBrowserExamples.install(data);
        var parser=new ParamsAssignerAndParser4pathwayFamBrowser();
        for(int i=0;i<6;i++) {
            String text=PathwayBrowserExamples.text(i,data);
            var input=new VoiceParameterParser().getOrganizedParameterGetter(text);
            var bean=parser.getImportBeanInfo(input);
            var root=new TreeParser4MTV().parseTree(bean).orElseThrow();
            var properties=new TreeLayoutProperties(root);
            new TreePropertiesAssigner().assign(properties,bean);
            require(properties.getLeaves().size()==(i==5?64:i==1?4:9),"Leaf count example "+i);
            require(bean.getPathwayGalleryPaths()!=null || i==5,"Gallery parsed");
            if(i==3)require(bean.getPathwayGalleryPaths().size()==2,"Both gallery paths retained");
            for(String file:bean.getPathwayGalleryPaths()==null?java.util.List.<String>of():bean.getPathwayGalleryPaths()) {
                if(file.equalsIgnoreCase("false"))continue;
                try(var stream=Files.newInputStream(Path.of(file));var ppt=new org.apache.poi.xslf.usermodel.XMLSlideShow(stream)) {
                    require(ppt.getSlides().size()==1,"Single-slide gallery");
                    var size=ppt.getPageSize();
                    var image=new java.awt.image.BufferedImage(size.width,size.height,java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();
                    try { ppt.getSlides().getFirst().draw(graphics); } finally { graphics.dispose(); }
                    javax.imageio.ImageIO.write(image,"png",data.resolve("preview-"+i+"-"+Path.of(file).getFileName()+".png").toFile());
                }
            }
            String extension=i==2?"xlsx":"tsv";
            require(Files.isRegularFile(Path.of(bean.getPathwayComponentCountPath())),"Counts exist "+extension);
            Voice4pathwayFamilyBrowser.validateTable(bean.getPathwayComponentCountPath());
            if(i==2) for(String stem:java.util.List.of("info","counts","traits")) {
                var lines=Files.readAllLines(data.resolve(stem+".tsv"));
                try(var stream=Files.newInputStream(data.resolve(stem+".xlsx"));var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook(stream)) {
                    var sheet=workbook.getSheetAt(0);
                    for(int r=0;r<lines.size();r++) {
                        String[] cells=lines.get(r).split("\t",-1);
                        for(int c=0;c<cells.length;c++)require(cells[c].equals(sheet.getRow(r).getCell(c).getStringCellValue()),"TSV/XLSX cell mismatch");
                    }
                }
            }
        }
        var input=new VoiceParameterParser().getOrganizedParameterGetter("$input.nwk.path=old.nwk\n$input.newick.file=new.nwk\n");
        require(parser.getImportBeanInfo(input).getInput_nwk_path().equals("new.nwk"),"Canonical input wins");
        var voice=new Voice4pathwayFamilyBrowser(null);
        String first=voice.getExampleText();
        for(int i=1;i<6;i++)require(voice.getExampleText().contains("# Example "+(i+1)+":"),"Stable sequence");
        require(first.equals(voice.getExampleText()),"Cycle wraps");
        voice.actionBeforeVoiceGuiAppear();require(first.equals(voice.getExampleText()),"Reopen resets");
        if(!java.awt.GraphicsEnvironment.isHeadless()) for(int opening=0;opening<4;opening++) {
            javax.swing.JPanel[] panel={null};
            javax.swing.SwingUtilities.invokeAndWait(()->panel[0]=voice.generateImportDataDialogGUI());
            javax.swing.SwingUtilities.invokeAndWait(()->{
                var tree=findTree(panel[0]);
                var root=(javax.swing.tree.DefaultMutableTreeNode)tree.getModel().getRoot();
                var nodes=root.preorderEnumeration();int index=0;
                while(nodes.hasMoreElements()) {
                    var node=(javax.swing.tree.DefaultMutableTreeNode)nodes.nextElement();
                    if(node.getUserObject() instanceof egps2.builtin.modules.voice.BookMarkNode bookmark && bookmark.isExample()) {
                        require(bookmark.getContent().orElseThrow().equals(PathwayBrowserExamples.text(index,PathwayBrowserExamples.directory())),"Bookmark mapping "+index);
                        index++;
                    }
                }
                require(index==6,"Six dialog bookmarks");
            });
        }
        org.slf4j.LoggerFactory.getLogger(PathwayBrowserExamplesTest.class).info("Six examples passed; rendered gallery previews: {}",data);
    }
    private static javax.swing.JTree findTree(java.awt.Container parent) {
        for(var child:parent.getComponents()) {
            if(child instanceof javax.swing.JTree tree)return tree;
            if(child instanceof java.awt.Container container) { var tree=findTree(container);if(tree!=null)return tree; }
        }
        return null;
    }
}
