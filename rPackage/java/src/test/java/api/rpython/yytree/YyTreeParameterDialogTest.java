package api.rpython.yytree;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Verifies that an error dialog never blocks the sequential R protocol. */
public final class YyTreeParameterDialogTest {
    public static void main(String[] args) throws Exception {
        YyTreeBridge bridge=new YyTreeBridge();
        String message="Invalid leaf_labels() parameter 'gap_length'.\nReceived: -3\nExpected: One finite nonnegative number.\nExample: leaf_labels(align = 'left', gap_length = 6)";
        try {
            bridge.showParameterError(message);
            if(GraphicsEnvironment.isHeadless()) {
                require(Arrays.stream(Window.getWindows()).noneMatch(Window::isDisplayable),"Headless error opened a window");
                return;
            }
            bridge.showParameterError(message);
            bridge.probeRuntime();
            AtomicReference<JDialog> error=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->{
                var dialogs=Arrays.stream(Window.getWindows()).filter(w->w instanceof JDialog
                    && w.isShowing() && ((JDialog)w).getTitle().contains("Check plotting parameters")).toList();
                require(dialogs.size()==1,"Repeated errors accumulated dialogs");
                JDialog dialog=(JDialog)dialogs.getFirst();error.set(dialog);
                require(!dialog.isModal(),"Parameter dialog blocks the protocol");
                require(containsMessage(dialog,message),"Dialog lost parameter explanation");
                try {
                    File output=new File("temp/yytree-qa/parameter-error-dialog.png");
                    output.getParentFile().mkdirs();
                    BufferedImage image=new BufferedImage(dialog.getWidth(),dialog.getHeight(),BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();dialog.printAll(graphics);graphics.dispose();
                    javax.imageio.ImageIO.write(image,"png",output);
                } catch(java.io.IOException e) { throw new IllegalStateException(e); }
            });
            require(!egps2.UnifiedAccessPoint.isGULaunched(),"Error dialog launched the eGPS desktop");
            bridge.closeAll();
            SwingUtilities.invokeAndWait(()->{
                require(!error.get().isDisplayable(),"Closing yytree left its dialog alive");
                require(Arrays.stream(Window.getWindows()).noneMatch(Window::isDisplayable),"Closing yytree leaked an owner window");
            });
        } finally { bridge.closeAll(); }
    }
    private static boolean containsMessage(Component component,String message) {
        if(component instanceof JTextArea text && text.getText().equals(message))return true;
        if(component instanceof Container container)
            for(Component child:container.getComponents())if(containsMessage(child,message))return true;
        return false;
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
