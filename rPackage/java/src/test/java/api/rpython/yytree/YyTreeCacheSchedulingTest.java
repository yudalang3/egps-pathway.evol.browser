package api.rpython.yytree;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import javax.swing.*;
import com.alibaba.fastjson.JSONObject;

/** Review-only probes: no top-level windows and no repository source changes. */
public class YyTreeCacheSchedulingTest {
    static Object field(Object target,String name) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    static void field(Object target,String name,Object value) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    static YyTreePreview preview() throws Exception {
        Method scene=YyTreeHoverTest.class.getDeclaredMethod("scene",int.class);scene.setAccessible(true);
        Method make=YyTreeHoverTest.class.getDeclaredMethod("preview",JSONObject.class);make.setAccessible(true);
        YyTreePreview p=(YyTreePreview)make.invoke(null,scene.invoke(null,1));
        SwingUtilities.invokeAndWait(()->wheel(p,-8));YyTreeBridge.awaitRendering(p);return p;
    }
    static void wheel(YyTreePreview p,int rotation) {
        Point v=p.scroll.getViewport().getViewPosition();
        p.navigation.mouseWheelMoved(new MouseWheelEvent(p.canvas,MouseEvent.MOUSE_WHEEL,0,0,v.x+200,v.y+180,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,rotation));
    }
    public static void main(String[] args) throws Exception {
        if(!GraphicsEnvironment.isHeadless())throw new AssertionError("Must run headless");
        YyTreePreview p=preview();
        try {
            SwingUtilities.invokeAndWait(()->{
                try {
                    // Simulate a renderer failure for a known non-current viewport.
                    p.scroll.getViewport().setViewPosition(new Point(60,40));
                    Method key=p.cache.getClass().getDeclaredMethod("key");key.setAccessible(true);
                    Object failedKey=key.invoke(p.cache);
                    field(p.cache,"failed",failedKey);
                    field(p.cache,"failedCause",new IllegalStateException("injected renderer failure"));
                    p.cache.ready.completeExceptionally(new IllegalStateException("injected renderer failure"));
                    p.scroll.getViewport().setViewPosition(new Point(80,60));
                    p.scroll.getViewport().setViewPosition(new Point(60,40));
                    p.cache.render();
                    if(!p.cache.ready.isCompletedExceptionally()) throw new AssertionError("Failed view left pending wait");
                    ((Timer)field(p.cache,"settle")).stop();
                }catch(Exception e){throw new RuntimeException(e);}
            });
        }finally{SwingUtilities.invokeAndWait(p::disposeInteraction);}
        YyTreePreview q=preview();
        try {
            SwingUtilities.invokeAndWait(()->{
                try {
                    Point view=q.scroll.getViewport().getViewPosition();
                    q.navigation.mousePressed(new MouseEvent(q.canvas,MouseEvent.MOUSE_PRESSED,0,InputEvent.BUTTON1_DOWN_MASK,view.x+200,view.y+180,1,false,MouseEvent.BUTTON1));
                    q.scroll.getViewport().setViewPosition(new Point(view.x+40,view.y+30));
                    double before=q.canvas.zoom;
                    wheel(q,-1);
                    Object worker=field(q.cache,"worker");
                    if(worker!=null)throw new AssertionError("Wheel submitted intermediate drag view");

                }catch(Exception e){throw new RuntimeException(e);}
            });
            YyTreeBridge.awaitRendering(q);
        }finally{SwingUtilities.invokeAndWait(q::disposeInteraction);}
        System.out.println("Cache failure wait and compound gesture scheduling passed.");
    }
}
