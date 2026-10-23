package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import module.evolview.model.tree.GraphicsNode;
import javax.swing.SwingUtilities;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicReference;

/** Independent CLI regression entry point; no eGPS frame or R callback is needed. */
public final class YyTreeGeometryTest {
    private static final String SCENE = """
        {"schema_version":1,
         "tree":{"labels":["A","B","C"],"root":5,
           "edges":[[5,4,1],[4,1,0.12345678912345],[4,2,2],[5,3,3]]},
         "canvas":{"width":1000,"height":800,"margins":[20,40,80,40]},
         "layers":[{"type":"layout","code":1},
           {"type":"leaf_render","size":6,"fill":"#0000FFFF"},
           {"type":"leaf_render","df":[{"name":"A","fill":"#FF0000FF"}]}]}
        """;

    public static void main(String[] args) throws Exception {
        new YyTreeBridge().probeRuntime();
        for (int code=1; code<=7; code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            AtomicReference<YyTreeBridge.YyTreePanel> reference=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->reference.set(new YyTreeBridge.YyTreePanel(scene)));
            YyTreeBridge.YyTreePanel panel=reference.get();
            panel.prepare();
            require(panel.leaves.size()==3,"Leaf count changed");
            require(panel.leaves.getFirst().getName().equals("A"),"Input order changed");
            GraphicsNode a=panel.named.get("A"),b=panel.named.get("B"),c=panel.named.get("C");
            require(panel.mrca("A","B")==a.getParent(),"Wrong MRCA");
            require(panel.mrca("A","C")==panel.root,"Root MRCA failed");
            require(a.getRealBranchLength()==0.12345678912345,"Branch precision lost");
            require(panel.sizes.get(a)==6,"Partial style reset earlier radius");
            require(panel.fills.get(a).getRed()==255,"Partial style did not override color");
            require(panel.fills.get(b).getBlue()==255,"Local style leaked to another leaf");
            if(code==2)require(panel.tips.get(a).x==panel.tips.get(b).x,"Rectangular tips do not align");
            if(code==4)for(GraphicsNode node:panel.leaves) {
                Point2D point=panel.tips.get(node);
                require(Math.abs(point.distance(panel.centerX,panel.centerY)-panel.outer)<1e-8,"Circular tips do not align");
            }
            if(code==6)for(GraphicsNode node:panel.leaves) {
                require(panel.tips.get(node).distance(panel.arch(panel.outer,node.getDoubleVariable()))<1e-8,"Arch tips do not align");
            }
            JSONObject relative=JSON.parseObject("{\"mode\":\"relative\",\"x\":0.4,\"y\":0.25}");
            JSONObject percent=JSON.parseObject("{\"mode\":\"percent\",\"x\":40,\"y\":25}");
            JSONObject pixel=JSON.parseObject("{\"mode\":\"pixel\",\"x\":400,\"y\":200}");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(percent,"x",true),"Percent mismatch");
            require(panel.coordinate(relative,"y",false)==panel.coordinate(pixel,"y",false),"Pixel mismatch");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(pixel,"x",true),"Relative mismatch");
        }
        require(!egps2.UnifiedAccessPoint.isGULaunched(),"The eGPS desktop was launched");
        System.out.println("Seven-layout geometry, MRCA, style, precision and coordinate checks passed.");
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
