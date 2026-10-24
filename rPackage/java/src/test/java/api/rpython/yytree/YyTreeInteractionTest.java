package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Deterministic view-navigation checks, including events delivered outside the canvas. */
public final class YyTreeInteractionTest {
    private static final String SCENE = """
        {"schema_version":1,"tree":{"labels":["A","B","C"],"root":5,
          "edges":[[5,4,1],[4,1,1],[4,2,2],[5,3,3]]},
         "canvas":{"width":1600,"height":1400,"margins":[20,40,80,40]},"layers":[]}
        """;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            YyTreePreview.initializeTheme();
            var panel=new YyTreeBridge.YyTreePanel(JSON.parseObject(SCENE));
            var preview=new YyTreePreview(panel);
            var viewport=preview.scroll.getViewport();
            viewport.setExtentSize(new Dimension(600,400));
            viewport.setViewSize(panel.getPreferredSize());
            var navigation=preview.navigation;
            Cursor initialCursor=panel.getCursor();

            viewport.setViewPosition(new Point(200,150));
            mouse(preview,MouseEvent.MOUSE_PRESSED,100,100,MouseEvent.BUTTON1);
            mouse(preview,MouseEvent.MOUSE_DRAGGED,60,100,MouseEvent.BUTTON1);
            require(viewport.getViewPosition().equals(new Point(240,150)),"Horizontal drag failed");
            mouse(preview,MouseEvent.MOUSE_DRAGGED,60,70,MouseEvent.BUTTON1);
            require(viewport.getViewPosition().equals(new Point(240,180)),"Continuous vertical drag jumped");
            mouse(preview,MouseEvent.MOUSE_DRAGGED,10000,10000,MouseEvent.BUTTON1);
            require(viewport.getViewPosition().equals(new Point(0,0)),"Top-left pan bound failed");
            mouse(preview,MouseEvent.MOUSE_DRAGGED,-10000,-10000,MouseEvent.BUTTON1);
            require(viewport.getViewPosition().equals(new Point(1000,1000)),"Bottom-right pan bound failed");
            navigation.eventDispatched(new MouseEvent(preview.hint,MouseEvent.MOUSE_RELEASED,0,0,0,0,1,false,MouseEvent.BUTTON1));
            require(!navigation.isDragging() && panel.getCursor().equals(initialCursor),"Outside release did not restore cursor");

            Point before=viewport.getViewPosition();
            mouse(preview,MouseEvent.MOUSE_PRESSED,100,100,MouseEvent.BUTTON3);
            mouse(preview,MouseEvent.MOUSE_DRAGGED,50,50,MouseEvent.BUTTON3);
            require(viewport.getViewPosition().equals(before) && !navigation.isDragging(),"Right button panned the canvas");

            viewport.setViewPosition(new Point(200,150));
            mouse(preview,MouseEvent.MOUSE_PRESSED,120,100,MouseEvent.BUTTON1);
            Point anchor=viewport.getViewPosition();
            double logicalX=(anchor.x+120)/panel.zoom,logicalY=(anchor.y+100)/panel.zoom;
            wheel(preview,120,100,-2);
            require(!navigation.isDragging() && panel.zoom>1,"Wheel did not end drag before zoom");
            Point after=viewport.getViewPosition();
            require(Math.abs((after.x+120)/panel.zoom-logicalX)<1 && Math.abs((after.y+100)/panel.zoom-logicalY)<1,"Wheel lost mouse anchor");
            mouse(preview,MouseEvent.MOUSE_PRESSED,120,100,MouseEvent.BUTTON1);
            mouse(preview,MouseEvent.MOUSE_DRAGGED,90,80,MouseEvent.BUTTON1);
            require(viewport.getViewPosition().equals(new Point(after.x+30,after.y+20)),"Drag after zoom failed");
            mouse(preview,MouseEvent.MOUSE_RELEASED,90,80,MouseEvent.BUTTON1);
            mouse(preview,MouseEvent.MOUSE_MOVED,100,100,MouseEvent.NOBUTTON);
            Point position=viewport.getViewPosition();
            String expected=String.format(java.util.Locale.ROOT,"%.1f",(position.x+100)/panel.zoom);
            require(preview.coordinates.getText().contains(expected),"Hover failed after zoom and drag");
            viewport.setViewPosition(new Point(position.x+25,position.y+10));
            expected=String.format(java.util.Locale.ROOT,"%.1f",(position.x+125)/panel.zoom);
            require(preview.coordinates.getText().contains(expected),"Scrolling did not refresh hover coordinates");

            var other=new YyTreePreview(new YyTreeBridge.YyTreePanel(JSON.parseObject(SCENE)));
            require(other.canvas.zoom==1 && other.scroll.getViewport().getViewPosition().equals(new Point()),"Window view state leaked");
            mouse(preview,MouseEvent.MOUSE_PRESSED,100,100,MouseEvent.BUTTON1);
            preview.disposeInteraction();
            require(!navigation.isDragging() && panel.getMouseWheelListeners().length==0,"Closing/replacing view left listeners active");
            other.disposeInteraction();

            var fitted=new YyTreePreview(new YyTreeBridge.YyTreePanel(JSON.parseObject(SCENE)));
            fitted.scroll.getViewport().setExtentSize(new Dimension(2000,1800));
            mouse(fitted,MouseEvent.MOUSE_PRESSED,100,100,MouseEvent.BUTTON1);
            mouse(fitted,MouseEvent.MOUSE_DRAGGED,50,50,MouseEvent.BUTTON1);
            require(fitted.scroll.getViewport().getViewPosition().equals(new Point()),"Fitted canvas moved out of bounds");
            fitted.disposeInteraction();
        });
        require(!egps2.UnifiedAccessPoint.isGULaunched(),"The eGPS desktop was launched");
        System.out.println("FlatLaf, SwingX, pan bounds, release, zoom anchor, hover and view isolation passed.");
    }

    private static void mouse(YyTreePreview preview,int id,int x,int y,int button) {
        Point view=preview.scroll.getViewport().getViewPosition();
        int mask=(id==MouseEvent.MOUSE_PRESSED || id==MouseEvent.MOUSE_DRAGGED)?
            (button==MouseEvent.BUTTON1?InputEvent.BUTTON1_DOWN_MASK:InputEvent.BUTTON3_DOWN_MASK):0;
        int eventButton=id==MouseEvent.MOUSE_DRAGGED || id==MouseEvent.MOUSE_MOVED?MouseEvent.NOBUTTON:button;
        MouseEvent event=new MouseEvent(preview.canvas,id,0,mask,view.x+x,view.y+y,1,false,eventButton);
        switch(id) {
            case MouseEvent.MOUSE_PRESSED -> preview.navigation.mousePressed(event);
            case MouseEvent.MOUSE_DRAGGED -> preview.navigation.mouseDragged(event);
            case MouseEvent.MOUSE_RELEASED -> preview.navigation.mouseReleased(event);
            case MouseEvent.MOUSE_MOVED -> preview.navigation.mouseMoved(event);
            default -> throw new AssertionError("Unexpected test event");
        }
    }

    private static void wheel(YyTreePreview preview,int x,int y,int rotation) {
        Point view=preview.scroll.getViewport().getViewPosition();
        preview.navigation.mouseWheelMoved(new MouseWheelEvent(preview.canvas,MouseEvent.MOUSE_WHEEL,0,0,
            view.x+x,view.y+y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,rotation));
    }

    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
