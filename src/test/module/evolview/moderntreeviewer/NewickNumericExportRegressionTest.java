package test.module.evolview.moderntreeviewer;

import java.util.Locale;
import module.evolview.model.tree.DefaultGraphicsPhyloNode;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;

/** Numeric exports must preserve doubles regardless of the desktop locale. */
public class NewickNumericExportRegressionTest {
    public static void main(String[] args) throws Exception {
        Locale original = Locale.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.US, Locale.GERMANY, Locale.CHINA}) {
                Locale.setDefault(locale);
                for (double length : new double[]{1234.56789, 0.000000123456789, 0, 1e100, 1e-100}) {
                    DefaultGraphicsPhyloNode root = new DefaultGraphicsPhyloNode();
                    root.setLength(length);
                    for (String name : new String[]{"A", "B"}) {
                        DefaultGraphicsPhyloNode leaf = new DefaultGraphicsPhyloNode();
                        leaf.setName(name);
                        leaf.setLength(length);
                        root.addChild(leaf);
                    }
                    PhyloGraphicsTreeEncoderDecoder codec = new PhyloGraphicsTreeEncoderDecoder();
                    DefaultGraphicsPhyloNode restored = codec.decode(codec.encode(root));
                    if (restored.getChildCount() != 2 || Double.compare(restored.getLength(), length) != 0
                            || Double.compare(restored.getChildAt(0).getLength(), length) != 0)
                        throw new AssertionError("Numeric round trip failed for " + locale + ": " + length);
                }
            }
        } finally { Locale.setDefault(original); }
    }
}
