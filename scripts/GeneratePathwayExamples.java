import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.awt.Rectangle;
import java.awt.Dimension;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Run from repository root with the existing dependency-egps classpath. */
public class GeneratePathwayExamples {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of("src/module/evolview/pathwaybrowser/io/examples");
        Files.createDirectories(dir);
        String[] names = {"Human", "Chimp", "Mouse", "Rat", "Dog", "Cow", "Chicken", "Frog", "Zebrafish"};
        Files.writeString(dir.resolve("species.nwk"), "((((Human:1,Chimp:1)Primates:2,(Mouse:1,Rat:1)Rodents:2)MammalsA:1,(Dog:2,Cow:2)MammalsB:2)Mammals:2,(Chicken:3,(Frog:2,Zebrafish:2)Other:1)VertebratesB:3)Root;\n");
        Files.writeString(dir.resolve("species.tpv"), "Root\tMammals:2,Other:3\nMammals\tHuman:1,Chimp:1,Mouse:1,Rat:1,Dog:1,Cow:1\nOther\tChicken:1,Frog:1,Zebrafish:1\n");
        StringBuilder info = new StringBuilder("Name\tDisplay name\tData kind\n");
        StringBuilder counts = new StringBuilder("Name\tAXIN2\tWNT3A\tDVL3\n");
        StringBuilder metabolic = new StringBuilder("Name\tHK1\tGPI\tGAPDH\n");
        StringBuilder traits = new StringBuilder("Name\tDemo group\tDemo score\n");
        for (int i=0;i<names.length;i++) {
            info.append(names[i]+"\t"+names[i]+"\tTeaching data\n");
            counts.append(names[i]+"\t"+(i%3)+"\t"+(i+1)+"\t2\n");
            metabolic.append(names[i]+"\t1\t"+(i%2)+"\t2\n");
            traits.append(names[i]+"\tGroup "+(i%2+1)+"\t"+(i+1)+"\n");
        }
        for (var entry : java.util.Map.of("info",info,"counts",counts,"traits",traits,"metabolic",metabolic).entrySet()) {
            String data = entry.getValue().toString();
            Files.writeString(dir.resolve(entry.getKey()+".tsv"), data);
            try (var book = new XSSFWorkbook()) {
                var sheet = book.createSheet("Data");
                int r=0;
                for (String line : data.lines().toList()) {
                    var row=sheet.createRow(r++); int c=0;
                    for(String value:line.split("\t",-1)) row.createCell(c++).setCellValue(value);
                }
                try(var out=Files.newOutputStream(dir.resolve(entry.getKey()+".xlsx"))) { book.write(out); }
            }
        }
        Files.writeString(dir.resolve("partial.tsv"), "Name\tAXIN2\tWNT3A\tDVL3\nHuman\t0\t\t2\nMouse\t1\t0\t\nPrimates\t2\t3\t4\n");
        StringBuilder large = new StringBuilder("Name\tAXIN2\tWNT3A\n");
        for(int i=1;i<=64;i++) large.append("Sample"+i+"\t"+(i%4)+"\t"+(i%7)+"\n");
        Files.writeString(dir.resolve("large.tsv"), large);
        Files.writeString(dir.resolve("large.nwk"), balanced(1,64)+";\n");
        StringBuilder sources=new StringBuilder("Pathway Browser examples\nSnapshot retrieved: 2026-11-02\nWikiPathways pathway images: CC0; https://www.wikipathways.org/terms.html\nAll trees, branch lengths, counts and traits are deterministic teaching data, not research measurements.\nGenerator: scripts/GeneratePathwayExamples.java\nPPTX conversion: original PNG fitted proportionally on one slide; source caption added.\n");
        for(String id: new String[]{"WP428","WP534"}) {
            byte[] png=Files.readAllBytes(Path.of("orig_matrials",id+".png"));
            String url="https://www.wikipathways.org/wikipathways-assets/pathways/"+id+"/"+id+".png";
            sources.append(id+" "+url+"\nSHA256: "+java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png))+"\n");
            try(var ppt=new XMLSlideShow()) {
                var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
                double scale=Math.min(1200.0/image.getWidth(),800.0/image.getHeight());
                int w=(int)(image.getWidth()*scale),h=(int)(image.getHeight()*scale);
                ppt.setPageSize(new Dimension(w,h+60));
                var slide=ppt.createSlide();
                var picture=slide.createPicture(ppt.addPicture(png,PictureData.PictureType.PNG));
                picture.setAnchor(new Rectangle(0,0,w,h));
                var caption=slide.createTextBox();caption.setAnchor(new Rectangle(5,h,w-10,55));
                caption.setText(id+" | Homo sapiens | WikiPathways CC0\nStatic reference image; teaching counts do not recolor this diagram.");
                caption.getTextParagraphs().forEach(p->p.getTextRuns().forEach(t->t.setFontSize(12.0)));
                try(var out=Files.newOutputStream(dir.resolve(id+".pptx"))) { ppt.write(out); }
            }
        }
        Files.writeString(dir.resolve("SOURCES.txt"),sources);
    }
    static String balanced(int start,int count) {
        if(count==1)return "Sample"+start+":1";
        return "("+balanced(start,count/2)+","+balanced(start+count/2,count/2)+"):1";
    }
}
