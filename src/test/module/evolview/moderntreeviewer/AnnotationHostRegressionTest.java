package module.evolview.moderntreeviewer;
import egps2.UnifiedAccessPoint;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import module.evolview.gfamily.work.gui.tree.*;
import module.evolview.gfamily.work.gui.dialog.NodeAnnotationDialogContainer;
import module.evolview.moderntreeviewer.io.*;
import module.evolview.phylotree.visualization.layout.*;
public class AnnotationHostRegressionTest {
 static <T> T find(Container parent,Class<T> type,String text) {
  for(var child:parent.getComponents()) {
   if(type.isInstance(child) && (text==null || child instanceof JButton b && text.equals(b.getText())))return type.cast(child);
   if(child instanceof Container nested) {T found=find(nested,type,text);if(found!=null)return found;}
  }return null;
 }
 public static void main(String[] args) throws Exception {
  var failure=new AtomicReference<Throwable>();
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{failure.compareAndSet(null,e);org.slf4j.LoggerFactory.getLogger(AnnotationHostRegressionTest.class).error("Unhandled host failure",e);});
  var input=new MTVImportInforBean();input.setInput_nwk_string(Files.readString(Path.of("src/module/evolview/moderntreeviewer/examples/11-grouped-64.nwk")));
  var root=new TreeParser4MTV().parseTree(input).orElseThrow();
  var faceRef=new AtomicReference<MTreeViewMainFace>();
  try {
   SwingUtilities.invokeAndWait(()->{
    var host=UnifiedAccessPoint.getInstanceFrame();host.setSize(1409,865);host.setVisible(true);
    var loader=new IndependentModuleLoader();loader.setModuleData(root,null);
    var face=(MTreeViewMainFace)UnifiedAccessPoint.loadTheModuleFromIModuleLoader(loader);
    faceRef.set(face);
   });
   long readyDeadline=System.nanoTime()+10_000_000_000L;
   var ready=new java.util.concurrent.atomic.AtomicBoolean();
   while(!ready.get() && System.nanoTime()<readyDeadline) {
    SwingUtilities.invokeAndWait(()->ready.set(faceRef.get().getSelectedPhylogenticTreePanel()!=null));
    if(!ready.get()) Thread.sleep(50);
   }
   SwingUtilities.invokeAndWait(()->{
    var panel=faceRef.get().getSelectedPhylogenticTreePanel();
    if(!UnifiedAccessPoint.isGULaunched() || panel==null)throw new AssertionError("Real host required");
    var before=panel.getLayoutProperties().getMyLayout();
    var menu=new TreePopupMenu(panel).getInternalNodeRightClickPop(root);
    var handled=new boolean[]{false};
    Timer timer=new Timer(100,null);long deadline=System.nanoTime()+15_000_000_000L;
    timer.addActionListener(e->{
     for(var window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()) {
      if(!(dialog instanceof NodeAnnotationDialogContainer)) {failure.compareAndSet(null,new AssertionError("Unexpected dialog: "+dialog.getTitle()));dialog.dispose();continue;}
      if(handled[0])continue;handled[0]=true;
      var preview=find(dialog,PhylogeneticTreePanel.class,null);
      if(preview.getLayoutProperties()==panel.getLayoutProperties())failure.compareAndSet(null,new AssertionError("Shared preview"));
      find(dialog,JButton.class,"Cancel").doClick();timer.stop();
     }
     if(System.nanoTime()>deadline){failure.compareAndSet(null,new AssertionError("Dialog timeout"));timer.stop();for(var w:Window.getWindows())if(w instanceof JDialog)w.dispose();}
    });timer.start();
    for(var component:menu.getComponents())if(component instanceof JMenuItem item && "Annotation...".equals(item.getText()))item.doClick();
    timer.stop();if(!handled[0] || panel.getLayoutProperties().getMyLayout()!=before)throw new AssertionError("Annotation lifecycle");
   });
   if(failure.get()!=null)throw new AssertionError("Host failure",failure.get());
   org.slf4j.LoggerFactory.getLogger(AnnotationHostRegressionTest.class).info("Real host annotation passed with 64-tip fixture");
  } finally {SwingUtilities.invokeAndWait(()->{for(var w:Window.getWindows())w.dispose();});}
 }
}
