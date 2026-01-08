package test.module.evoltreio;

import evoltree.phylogeny.DefaultPhyNode;
import evoltree.phylogeny.PhyloTreeEncoderDecoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TestPhyloTreeEncoderDecoder {

    static void main() {
        String path = "C:\\Users\\yudal\\Documents\\project\\EvolutionsKnowledge\\human_evol_timeline_20260108\\humanEvolutionaryTimeLine241231.nwk";

        PhyloTreeEncoderDecoder phyloTreeEncoderDecoder = new PhyloTreeEncoderDecoder();

        try {
            String treeContents = Files.readString(Path.of(path));
            DefaultPhyNode tree = phyloTreeEncoderDecoder.decode(treeContents);
            String line = phyloTreeEncoderDecoder.encode(tree);
            System.out.println(line);
            System.out.println(treeContents);
            System.out.println(Objects.equals(line, treeContents));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
