package module.evolview.pathwaybrowser.io;

import egps2.EGPSProperties;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Offline examples. Installed files belong to the user and are never replaced. */
public final class PathwayBrowserExamples {
    private PathwayBrowserExamples() { }
    public static final List<String> TITLES = List.of("Wnt complete workflow", "Glycolysis: Newick text", "Excel data", "Multiple pathways: table tree", "Partial coverage and zero counts", "64-tip circular tree");
    public static Path directory() { return Path.of(EGPSProperties.BIO_DATA_DIR,"pathwayBrowser","examples"); }
    public static List<String> resources() {
        return List.of(
                "9_model_species_evolution.nwk",
                "components.info.tsv",
                "wnt_species_info.tsv",
                "wnt_species_traits.tsv",
                "wnt_pathway2.pptx",
                "pathway_counts.pptx",
                "demo_selection.pptx",
                "species.nwk",
                "species.tpv",
                "large.nwk",
                "info.tsv",
                "counts.tsv",
                "traits.tsv",
                "metabolic.tsv",
                "info.xlsx",
                "counts.xlsx",
                "traits.xlsx",
                "metabolic.xlsx",
                "partial.tsv",
                "large.tsv",
                "WP428.pptx",
                "WP534.pptx",
                "SOURCES.txt"
        );
    }
    public static void install(Path directory) throws IOException {
        Files.createDirectories(directory);
        for(String file:resources()) {
            Path target=directory.resolve(file);
            if(Files.isRegularFile(target))continue;
            try(InputStream input=PathwayBrowserExamples.class.getResourceAsStream("examples/"+file)) {
                if(input==null)throw new IOException("Missing bundled example: "+file);
                Path temporary=Files.createTempFile(directory,".pathway-example-",".tmp");
                try {
                    Files.copy(input,temporary,StandardCopyOption.REPLACE_EXISTING);
                    try { Files.move(temporary,target); }
                    catch(FileAlreadyExistsException error) { if(!Files.isRegularFile(target))throw error; }
                } finally { Files.deleteIfExists(temporary); }
            }
        }
        module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.warmupAsync(
                directory.resolve("wnt_pathway2.pptx"),
                directory.resolve("components.info.tsv")
        );
    }
    public static String text(int index,Path directory) {
        Map<String,String> active=new LinkedHashMap<>();
        active.put("input.newick.format","1");
        if(index==1)active.put("input.newick.text","((Human:1,Chimp:1)Primates:1,(Mouse:1,Rat:1)Rodents:1)Root;");
        else if(index==3)active.put("input.table.file",directory.resolve("species.tpv").toString());
        else active.put("input.newick.file",directory.resolve(index==5?"large.nwk":index==0?"9_model_species_evolution.nwk":"species.nwk").toString());
        active.put("layout.type",index==5?"Circular phylogram":"Rectangular phylogram");
        active.put("layout.margins.bottom.px","10");
        active.put("display.title.text",TITLES.get(index)+" (teaching tree and values)");
        active.put("labels.show.leaf.names","true");
        active.put("labels.show.internal.node.names","true");
        String extension=index==2?".xlsx":".tsv";
        active.put("pathway.species.info.path",index==5?"False":directory.resolve(index==0?"wnt_species_info.tsv":"info"+extension).toString());
        active.put("species.traits.path",index==5?"False":directory.resolve(index==0?"wnt_species_traits.tsv":"traits"+extension).toString());
        active.put("pathway.component.counts.path",directory.resolve(index==5?"large.tsv":index==4?"partial.tsv":index==1?"metabolic.tsv":index==0?"components.info.tsv":"counts"+extension).toString());
        active.put("pathway.gallery.figure.paths",index==5?"False":index==0?("\n"+directory.resolve("wnt_pathway2.pptx")+"\n"+directory.resolve("pathway_counts.pptx")+"\n"+directory.resolve("demo_selection.pptx")):("\n"+directory.resolve(index==1?"WP534.pptx":"WP428.pptx")+(index==3?"\n"+directory.resolve("WP534.pptx"):"")));
        var parser=new ParamsAssignerAndParser4pathwayFamBrowser();
        active.forEach((k,v)->parser.addKeyValueEntryBean(k,v,""));
        String parameters=parser.getExampleString().lines().map(line->{
            String key=line.stripLeading();int separator=key.indexOf('=');
            return key.startsWith("$")&&separator>0&&!active.containsKey(key.substring(1,separator))?"# "+line:line;
        }).collect(java.util.stream.Collectors.joining("\n"));
        return "# Example "+(index+1)+": "+TITLES.get(index)+"\n# Trees, lengths, counts and traits are teaching data. Gallery images are static WikiPathways references.\n# Source and snapshot checksums: "+directory.resolve("SOURCES.txt")+"\n"+parameters;
    }
}
