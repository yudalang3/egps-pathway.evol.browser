package api.rpython.yytree;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.SwingDialog;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/** Per-preview editing session. Its mutable geometry and UI belong exclusively to the EDT. */
final class FreeShapeEditor implements AWTEventListener {
    static final String[] TYPES={"text","ellipse","rectangle","line","arrow","polygon","bezier","bezier_ring","free_heatmap"};
    final YyTreePreview preview;
    final FreeShapes geometry;
    final List<JSONObject> layers=new ArrayList<>();
    int selected=-1;
    boolean active,space;
    String tool="select";
    JDialog dialog;
    JTextArea code;
    JButton copyButton;
    JLabel message;
    JList<String> objects;
    JPanel properties;
    private final Map<String,JTextField> fields=new LinkedHashMap<>();
    private final Map<String,JToggleButton> tools=new LinkedHashMap<>();
    private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private JSONObject original,draft;
    private Point2D press,pointer,snapped;
    private Handle draggingHandle;
    private State beforeDrag;
    private boolean updating,disposed;
    private long heatmapJob;
    private long codeRevision;
    private SwingWorker<String,Void> codeWorker;
    private final javax.swing.Timer codeTimer=new javax.swing.Timer(150,e->refreshCode());
    record State(List<JSONObject> layers,int selected) { }
    record Handle(Point2D point,String key,int index) { }

    FreeShapeEditor(YyTreePreview preview) {
        this.preview=preview;geometry=preview.canvas.freeShapes;
        for(JSONObject l:preview.canvas.layers)if(l.containsKey("mode"))layers.add(FreeShapes.copy(l));
        codeTimer.setRepeats(false);
        preview.canvas.setFocusable(true);
        key("DELETE",()->delete());key("control Z",()->undo());key("control Y",()->redo());key("control shift Z",()->redo());
        key("ESCAPE",()->cancel());key("ENTER",()->finishPolygon());
        key("pressed SPACE",()->space=true);key("released SPACE",()->space=false);
        preview.canvas.addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){space=false;}});
    }
    private void key(String stroke,Runnable action) {
        String name="free-shape-"+stroke;
        preview.canvas.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(stroke),name);
        preview.canvas.getActionMap().put(name,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){if(active)action.run();}});
    }
    void open() {
        if(disposed)return;
        if(dialog==null)buildDialog();
        preview.navigation.endDrag();active=true;preview.clearHover();
        preview.hint.setText("Drag shapes to edit · Space + drag to pan · Wheel to zoom");
        refresh();dialog.setVisible(true);
    }
    void close() {
        cancelGesture();draft=null;tool="select";selected=-1;active=false;space=false;snapped=null;pointer=null;heatmapJob++;
        if(dialog!=null)dialog.setVisible(false);
        preview.canvas.setCursor(Cursor.getDefaultCursor());
        preview.hint.setText("Left drag to pan  ·  Wheel to zoom");preview.refreshHover();
    }
    private void buildDialog() {
        dialog=new JDialog(SwingUtilities.getWindowAncestor(preview),"Add free shapes",Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){close();}});
        JPanel left=new JPanel(new MigLayout("insets 8, fill, wrap 1","[grow,fill]","[][grow 25,fill][grow 75,fill]"));
        JPanel palette=new JPanel(new MigLayout("insets 0, fillx, wrap 2","[grow,fill][grow,fill]"));ButtonGroup group=new ButtonGroup();
        List<String> names=new ArrayList<>();names.add("select");names.addAll(Arrays.asList(TYPES));
        for(String type:names){JToggleButton button=new JToggleButton(type.equals("select")?"Select":functionName(type));group.add(button);tools.put(type,button);palette.add(button);
            button.setToolTipText("<html>"+toolHelp(type)+"<br>Escape cancels the current operation.</html>");button.addActionListener(e->setTool(type));}
        left.add(palette);
        objects=new JList<>();objects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);objects.putClientProperty("html.disable",true);
        objects.addListSelectionListener(e->{if(!updating&&!e.getValueIsAdjusting()){cancelGesture();selected=objects.getSelectedIndex();refreshProperties();preview.canvas.repaint();}});
        JScrollPane objectScroll=new JScrollPane(objects);objectScroll.setBorder(BorderFactory.createTitledBorder("Objects (back to front)"));objectScroll.setPreferredSize(new Dimension(285,115));left.add(objectScroll);
        properties=new JPanel(new MigLayout("insets 6, fillx, wrap 2","[][grow,fill]"));
        JScrollPane propertyScroll=new JScrollPane(properties);propertyScroll.setBorder(BorderFactory.createTitledBorder("Properties"));propertyScroll.setPreferredSize(new Dimension(285,260));left.add(propertyScroll);
        JPanel right=new JPanel(new MigLayout("insets 8, fill, wrap 1","[grow,fill]","[][][grow,fill][]"));
        right.add(new JLabel("<html><b>Replace the old free_* block.</b> Paste this fragment after your base plot object.<br>Changes stay in this preview; copy the code before printing again.</html>"));
        JPanel actions=new JPanel(new MigLayout("insets 0, gap 4"));
        action(actions,"Undo",this::undo);action(actions,"Redo",this::redo);action(actions,"Delete",this::delete);action(actions,"Backward",()->reorder(-1));action(actions,"Forward",()->reorder(1));right.add(actions);
        code=new JTextArea();code.setEditable(false);code.setLineWrap(true);code.setWrapStyleWord(false);JScrollPane codeScroll=new JScrollPane(code);codeScroll.setPreferredSize(new Dimension(420,400));right.add(codeScroll);
        JPanel footer=new JPanel(new MigLayout("insets 0, fillx, wrap 1","[grow,fill]"));message=new JLabel(" ");footer.add(message);
        copyButton=action(footer,"Copy code",()->{
            try{copyCode(Toolkit.getDefaultToolkit().getSystemClipboard());message.setText("Code copied. Replace your old free_* block.");}
            catch(IllegalStateException error){code.selectAll();code.requestFocusInWindow();message.setText("Clipboard busy or unavailable. Press Ctrl+C or try Copy code again.");}
        });right.add(footer);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);split.setResizeWeight(.4);split.setDividerLocation(320);dialog.setContentPane(split);
        Font font=UnifiedAccessPoint.getLaunchProperty().getDefaultFont();if(font!=null)applyFont(split,font);
        dialog.pack();Rectangle usable=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();dialog.setSize(Math.min(900,usable.width),Math.min(740,usable.height));dialog.setLocationRelativeTo(preview);
    }
    private static void applyFont(Component c,Font font){c.setFont(font);if(c instanceof Container parent)for(Component child:parent.getComponents())applyFont(child,font);}
    private JButton action(JPanel parent,String label,Runnable callback){JButton button=new JButton(label);button.addActionListener(e->{if(active)callback.run();});parent.add(button);return button;}
    static String functionName(String type){return type.equals("free_heatmap")?type:"free_"+type;}
    private static String toolHelp(String type){return switch(type){case "select"->"Select an object and drag it or its handles. Space + left drag or middle drag pans.";case "text"->"Click to place text at its baseline.";case "polygon"->"Click vertices; double-click or Enter finishes (at least three points).";case "bezier","bezier_ring"->"Drag a template, then move its control points.";case "free_heatmap"->"Drag a rectangle, then paste a numeric matrix.";default->"Drag from the first point to the opposite point.";};}
    void setTool(String type){cancelGesture();draft=null;tool=type;snapped=null;refresh();preview.canvas.requestFocusInWindow();}
    boolean wantsPan(MouseEvent e){return SwingUtilities.isMiddleMouseButton(e)||(space&&SwingUtilities.isLeftMouseButton(e));}
    boolean isDragging(){return press!=null;}
    void pointerExited(){if(!isDragging()){pointer=null;snapped=null;preview.canvas.repaint();}}
    Point2D logical(MouseEvent e){return new Point2D.Double(e.getX()/preview.canvas.zoom,e.getY()/preview.canvas.zoom);}
    Point2D snap(Point2D point) {
        snapped=null;if(preview.snap.isSelected()){
            double best=Math.pow(12/preview.canvas.zoom,2);
            for(var node:preview.nodePoints){double distance=point.distanceSq(node.x(),node.y());if(distance<=best){best=distance;snapped=new Point2D.Double(node.x(),node.y());}}
        }
        preview.snappedCoordinates.setText(snapped==null?"Snap: —":String.format(Locale.ROOT,"Snap: Pixel: (%.1f, %.1f)",snapped.getX(),snapped.getY()));
        return snapped==null?point:snapped;
    }
    boolean mousePressed(MouseEvent e) {
        if(!active||wantsPan(e))return false;
        if(!SwingUtilities.isLeftMouseButton(e))return true;
        preview.canvas.requestFocusInWindow();Point2D p=logical(e);pointer=p;
        if(tool.equals("polygon")){
            if(e.getClickCount()>=2){finishPolygon();return true;}
            p=snap(p);if(draft==null){draft=base("polygon");draft.put("points",new JSONArray());}
            draft.getJSONArray("points").add(FreeShapes.pair(p.getX(),p.getY()));preview.canvas.repaint();return true;
        }
        if(tool.equals("text")){
            Point2D anchor=snap(p);String text=JOptionPane.showInputDialog(dialog,"Text:","Add free_text",JOptionPane.PLAIN_MESSAGE);
            if(text!=null){JSONObject l=base("text");l.put("x",anchor.getX());l.put("y",anchor.getY());l.put("text",text);add(l);}return true;
        }
        if(!tool.equals("select")){
            press=snap(p);draft=template(tool,press,press);startReleaseListener();return true;
        }
        draggingHandle=handleAt(p);
        if(draggingHandle==null){selected=hit(p);refresh();}
        if(selected>=0){beforeDrag=state();original=FreeShapes.copy(layers.get(selected));press=p;startReleaseListener();preview.clearHover();}
        return true;
    }
    boolean mouseMoved(MouseEvent e) {
        if(!active)return false;pointer=logical(e);
        if(!tool.equals("select"))snap(pointer);else snapped=null;
        preview.canvas.setCursor(Cursor.getPredefinedCursor(!tool.equals("select")?Cursor.CROSSHAIR_CURSOR:handleAt(pointer)!=null?Cursor.CROSSHAIR_CURSOR:hit(pointer)>=0?Cursor.MOVE_CURSOR:Cursor.DEFAULT_CURSOR));
        preview.canvas.repaint();return true;
    }
    boolean mouseDragged(MouseEvent e) {
        if(!active||press==null)return false;
        if((e.getModifiersEx()&InputEvent.BUTTON1_DOWN_MASK)==0){cancelGesture();return true;}
        Point2D p=logical(e);pointer=p;
        if(draft!=null&&!tool.equals("polygon")){draft=template(tool,press,snap(p));}
        else if(original!=null&&selected>=0){
            JSONObject changed=FreeShapes.copy(original);
            if(draggingHandle==null)geometry.transform(changed,AffineTransform.getTranslateInstance(p.getX()-press.getX(),p.getY()-press.getY()));
            else if(draggingHandle.key().equals("x1")||draggingHandle.key().equals("x2")){String x=draggingHandle.key();geometry.setPoint(changed,x,x.equals("x1")?"y1":"y2",snap(p));}
            else if(!draggingHandle.key().equals("box"))geometry.setPoint(changed,draggingHandle.key(),draggingHandle.index(),snap(p));
            else resize(changed,p,e.isShiftDown());
            layers.set(selected,changed);codeTimer.restart();
        }
        preview.canvas.repaint();return true;
    }
    private void resize(JSONObject changed,Point2D p,boolean keepRatio) {
        Rectangle2D b=geometry.shape(original).getBounds2D();int index=draggingHandle.index();double[] a=BOX[index];
        double fx=b.getX()+b.getWidth()*(1-a[0]),fy=b.getY()+b.getHeight()*(1-a[1]);
        double sx=a[0]==.5||b.getWidth()==0?1:Math.max(.01,(p.getX()-fx)/(draggingHandle.point().getX()-fx));
        double sy=a[1]==.5||b.getHeight()==0?1:Math.max(.01,(p.getY()-fy)/(draggingHandle.point().getY()-fy));
        if("text".equals(original.getString("type"))||keepRatio){double scale=a[0]==.5?sy:a[1]==.5?sx:Math.max(sx,sy);sx=sy=scale;}
        AffineTransform tx=new AffineTransform();tx.translate(fx,fy);tx.scale(sx,sy);tx.translate(-fx,-fy);geometry.transform(changed,tx);
    }
    boolean mouseReleased(MouseEvent e) {
        if(!active||press==null)return false;
        if(!SwingUtilities.isLeftMouseButton(e))return true;
        JSONObject completed=draft;String creatingTool=tool;
        if(original!=null&&beforeDrag!=null&&!original.equals(layers.get(selected)))remember(beforeDrag);
        stopReleaseListener();press=null;original=null;beforeDrag=null;draggingHandle=null;draft=null;snapped=null;
        if(completed!=null){Rectangle2D b=geometry.shape(completed).getBounds2D();boolean segment=creatingTool.equals("line")||creatingTool.equals("arrow");boolean enough=segment?Math.hypot(b.getWidth(),b.getHeight())*preview.canvas.zoom>=2:b.getWidth()*preview.canvas.zoom>=2&&b.getHeight()*preview.canvas.zoom>=2;if(enough){if(creatingTool.equals("free_heatmap"))heatmapDialog(completed,true);else add(completed);}}
        refresh();return true;
    }
    @Override public void eventDispatched(AWTEvent event){
        if(event instanceof MouseEvent e&&e.getID()==MouseEvent.MOUSE_RELEASED&&e.getSource()!=preview.canvas&&press!=null&&SwingUtilities.isLeftMouseButton(e)){
            Point at=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),preview.canvas);
            MouseEvent release=new MouseEvent(preview.canvas,MouseEvent.MOUSE_RELEASED,e.getWhen(),e.getModifiersEx(),at.x,at.y,1,false,MouseEvent.BUTTON1);
            mouseDragged(new MouseEvent(preview.canvas,MouseEvent.MOUSE_DRAGGED,e.getWhen(),InputEvent.BUTTON1_DOWN_MASK,at.x,at.y,1,false,MouseEvent.NOBUTTON));mouseReleased(release);
        }
    }
    private void startReleaseListener(){Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK);}
    private void stopReleaseListener(){Toolkit.getDefaultToolkit().removeAWTEventListener(this);}
    private void cancelGesture(){if(beforeDrag!=null)restore(beforeDrag);stopReleaseListener();press=null;original=null;beforeDrag=null;draggingHandle=null;draft=null;snapped=null;codeTimer.stop();preview.canvas.repaint();}
    void cancel(){cancelGesture();tool="select";refresh();}
    void cancelForViewChange(){if(isDragging()||draft!=null){cancelGesture();refresh();}}
    private JSONObject base(String type){
        JSONObject l=new JSONObject(true);l.put("type",type);l.put("mode","pixel");l.put("fill_color",List.of("rectangle","ellipse","polygon","bezier_ring").contains(type)?"#FFD70040":null);l.put("line_color","#000000FF");l.put("line_thickness",1);l.put("line_type","solid");
        if(type.equals("text")){l.put("text","Text");l.put("fill_color","#000000FF");l.put("line_color",null);l.put("line_thickness",0);l.put("size",18);l.put("locator","left");l.put("gap",0);}
        if(type.equals("free_heatmap")){l.put("line_color",null);l.put("line_thickness",0);}
        if(type.equals("arrow")){l.put("head_style","filled");l.put("line_thickness",2);}
        return l;
    }
    JSONObject template(String type,Point2D a,Point2D b){
        JSONObject l=base(type);double x=Math.min(a.getX(),b.getX()),y=Math.min(a.getY(),b.getY()),w=Math.max(.000001,Math.abs(a.getX()-b.getX())),h=Math.max(.000001,Math.abs(a.getY()-b.getY()));
        if(type.equals("line")||type.equals("arrow")){geometry.setPoint(l,"x1","y1",a);geometry.setPoint(l,"x2","y2",b);}
        else if(type.equals("bezier")||type.equals("bezier_ring")){
            h=Math.max(20,h);JSONArray ps=new JSONArray();ps.add(FreeShapes.pair(x,y+h*.25));ps.add(FreeShapes.pair(x+w/3,y));ps.add(FreeShapes.pair(x+w*2/3,y+h*.5));ps.add(FreeShapes.pair(x+w,y+h*.25));l.put("points",ps);
            if(type.equals("bezier_ring")){JSONArray lower=new JSONArray();for(Object p:ps){JSONArray v=(JSONArray)p;lower.add(FreeShapes.pair(v.getDoubleValue(0),v.getDoubleValue(1)+h*.5));}l.put("points2",lower);}
        } else {l.put("x",type.equals("ellipse")?x+w/2:x);l.put("y",type.equals("ellipse")?y+h/2:y);l.put("width",w);l.put("height",h);if(type.equals("free_heatmap")){l.put("cells",JSONArray.parseArray("[[\"#2166ACFF\",\"#FFFFFFFF\"],[\"#FFFFFFFF\",\"#B2182BFF\"]"));}}
        return l;
    }
    void finishPolygon(){if(draft==null||!tool.equals("polygon"))return;if(draft.getJSONArray("points").size()<3)return;JSONObject completed=draft;draft=null;add(completed);}
    void add(JSONObject l){remember(state());geometry.mode(l,"percent");layers.add(l);selected=layers.size()-1;tool="select";draft=null;refresh();}
    private State state(){return new State(layers.stream().map(FreeShapes::copy).toList(),selected);}
    private void restore(State state){layers.clear();for(JSONObject l:state.layers())layers.add(FreeShapes.copy(l));selected=state.selected();}
    private void remember(State state){undo.push(state);while(undo.size()>100)undo.removeLast();redo.clear();}
    void undo(){cancelGesture();if(!undo.isEmpty()){redo.push(state());restore(undo.pop());refresh();}}
    void redo(){cancelGesture();if(!redo.isEmpty()){undo.push(state());restore(redo.pop());refresh();}}
    void delete(){cancelGesture();if(selected>=0){remember(state());layers.remove(selected);selected=Math.min(selected,layers.size()-1);refresh();}}
    void reorder(int delta){cancelGesture();int target=selected+delta;if(selected>=0&&target>=0&&target<layers.size()){remember(state());Collections.swap(layers,selected,target);selected=target;refresh();}}
    int hit(Point2D p){for(int i=layers.size()-1;i>=0;i--)if(geometry.hit(layers.get(i),p,5/preview.canvas.zoom))return i;return -1;}
    private static final double[][] BOX={{0,0},{.5,0},{1,0},{1,.5},{1,1},{.5,1},{0,1},{0,.5}};
    List<Handle> handles(){
        List<Handle> result=new ArrayList<>();if(selected<0||selected>=layers.size())return result;JSONObject l=layers.get(selected);String type=l.getString("type");
        if(type.equals("line")||type.equals("arrow")){for(int i=1;i<=2;i++)result.add(new Handle(new Point2D.Double(geometry.value(l,"x"+i,true),geometry.value(l,"y"+i,false)),"x"+i,0));return result;}
        for(String key:new String[]{"points","points2"})if(l.containsKey(key))for(int i=0;i<l.getJSONArray(key).size();i++)result.add(new Handle(geometry.point(l,l.getJSONArray(key).getJSONArray(i)),key,i));
        Rectangle2D b=geometry.shape(l).getBounds2D();for(int i=0;i<BOX.length;i++){if(type.equals("text")&&i%2==1)continue;result.add(new Handle(new Point2D.Double(b.getX()+BOX[i][0]*b.getWidth(),b.getY()+BOX[i][1]*b.getHeight()),"box",i));}return result;
    }
    private Handle handleAt(Point2D p){for(Handle h:handles())if(h.point().distance(p)<=7/preview.canvas.zoom)return h;return null;}
    void paintContents(Graphics2D g){for(JSONObject l:layers)geometry.draw(g,l);if(active&&draft!=null)geometry.draw(g,draft);}
    void paintOverlay(Graphics2D originalGraphics){
        if(!active)return;Graphics2D g=(Graphics2D)originalGraphics.create();
        try{
            double z=preview.canvas.zoom;g.clip(new Rectangle2D.Double(0,0,preview.canvas.logicalWidth*z,preview.canvas.logicalHeight*z));g.setColor(new Color(35,110,200));g.setStroke(new BasicStroke(1));
            if(selected>=0){Rectangle2D b=geometry.shape(layers.get(selected)).getBounds2D();g.draw(new Rectangle2D.Double(b.getX()*z,b.getY()*z,b.getWidth()*z,b.getHeight()*z));
                for(Handle h:handles()){Rectangle2D box=new Rectangle2D.Double(h.point().getX()*z-3.5,h.point().getY()*z-3.5,7,7);g.setColor(Color.WHITE);g.fill(box);g.setColor(new Color(35,110,200));g.draw(box);}
                JSONObject l=layers.get(selected);if(l.getString("type").startsWith("bezier")){for(String key:new String[]{"points","points2"})if(l.containsKey(key)){JSONArray ps=l.getJSONArray(key);for(int i:new int[]{0,2}){Point2D a=geometry.point(l,ps.getJSONArray(i)),b1=geometry.point(l,ps.getJSONArray(i+1));g.draw(new Line2D.Double(a.getX()*z,a.getY()*z,b1.getX()*z,b1.getY()*z));}}}
            }
            if(pointer!=null&&!tool.equals("select")&&preview.crosshair.isSelected()){Point2D p=snapped==null?pointer:snapped;g.setColor(new Color(120,140,165,150));g.draw(new Line2D.Double(0,p.getY()*z,preview.canvas.logicalWidth*z,p.getY()*z));g.draw(new Line2D.Double(p.getX()*z,0,p.getX()*z,preview.canvas.logicalHeight*z));}
            if(snapped!=null){g.setColor(new Color(231,76,60));g.draw(new Ellipse2D.Double(snapped.getX()*z-7,snapped.getY()*z-7,14,14));}
            if(draft!=null&&tool.equals("polygon")&&pointer!=null){JSONArray ps=draft.getJSONArray("points");if(!ps.isEmpty()){Point2D last=geometry.point(draft,ps.getJSONArray(ps.size()-1));g.draw(new Line2D.Double(last.getX()*z,last.getY()*z,pointer.getX()*z,pointer.getY()*z));}}
        }finally{g.dispose();}
    }
    private void refresh(){
        if(objects!=null){updating=true;try{DefaultListModel<String> model=new DefaultListModel<>();for(int i=0;i<layers.size();i++)model.addElement((i+1)+"  "+functionName(layers.get(i).getString("type")));objects.setModel(model);objects.setSelectedIndex(selected);for(var entry:tools.entrySet())entry.getValue().setSelected(entry.getKey().equals(tool));}finally{updating=false;}}
        refreshProperties();refreshCode();preview.canvas.repaint();
    }
    private void refreshCode(){
        if(code==null||disposed)return;
        long revision=++codeRevision;copyButton.setEnabled(false);
        if(codeWorker!=null)return;
        List<JSONObject> snapshot=layers.stream().map(FreeShapes::copy).toList();
        codeWorker=new SwingWorker<>(){
            @Override protected String doInBackground(){return FreeShapes.code(snapshot);}
            @Override protected void done(){
                codeWorker=null;if(disposed)return;
                if(revision!=codeRevision){refreshCode();return;}
                try{String text=get();if(!text.equals(code.getText())){int caret=code.getCaretPosition();code.setText(text);code.setCaretPosition(Math.min(caret,text.length()));}copyButton.setEnabled(!snapshot.isEmpty());message.setText(" ");}
                catch(Exception error){Throwable cause=error.getCause()==null?error:error.getCause();code.setText("# "+cause.getMessage());copyButton.setEnabled(false);message.setText("Supply the source matrix before copying.");}
            }
        };codeWorker.execute();
    }
    void copyCode(java.awt.datatransfer.Clipboard clipboard){clipboard.setContents(new StringSelection(code.getText()),null);}
    private void field(String key,Object value){JTextField field=new JTextField(value==null?"NA":value.toString(),16);fields.put(key,field);properties.add(new JLabel(key));properties.add(field);field.addActionListener(e->applyProperties());}
    private void refreshProperties(){
        if(properties==null)return;properties.removeAll();fields.clear();
        if(selected<0){properties.add(new JLabel("Select an object to edit its properties."),"span 2");properties.revalidate();properties.repaint();return;}
        JSONObject l=layers.get(selected);String type=l.getString("type");
        JComboBox<String> modes=new JComboBox<>(new String[]{"relative","percent","pixel"});modes.setSelectedItem(l.getString("mode"));properties.add(new JLabel("mode"));properties.add(modes);
        modes.addActionListener(e->{if(selected>=0){remember(state());JSONObject changed=FreeShapes.copy(layers.get(selected));geometry.mode(changed,(String)modes.getSelectedItem());layers.set(selected,changed);refresh();}});
        for(String key:new String[]{"x","y","width","height","x1","y1","x2","y2","text","size","locator","gap"})if(l.containsKey(key))field(key,l.get(key));
        if(type.equals("text"))field("color",l.get("fill_color"));
        else if(!type.equals("free_heatmap")){
            if(!List.of("line","arrow","bezier").contains(type))field("fill_color",l.get("fill_color"));field("line_color",l.get("line_color"));field("line_thickness",FreeShapes.n(l,"line_thickness",1));field("line_type",l.getString("line_type"));
            if(type.equals("arrow")){field("head_style",l.getOrDefault("head_style","open"));field("head_length",l.getOrDefault("head_length","auto"));field("head_width",l.getOrDefault("head_width","auto"));}
        }
        JButton apply=new JButton("Apply properties");apply.addActionListener(e->applyProperties());properties.add(apply,"span 2, growx");
        if(!type.equals("free_heatmap")){
            for(String colorKey:new String[]{"color","line_color","fill_color"})if(fields.containsKey(colorKey)){
                JButton color=new JButton(colorKey.equals("color")?"Choose text color…":colorKey.equals("fill_color")?"Choose fill color…":"Choose line color…");
                color.addActionListener(e->{
                    Color previous;try{previous=YyTreeBridge.color(parseColor(fields.get(colorKey).getText()));}catch(IllegalArgumentException invalid){previous=Color.BLACK;}
                    Color chosen=JColorChooser.showDialog(dialog,"Choose "+colorKey,previous==null?Color.BLACK:previous);
                    if(chosen!=null){fields.get(colorKey).setText(String.format(Locale.ROOT,"#%02X%02X%02X%02X",chosen.getRed(),chosen.getGreen(),chosen.getBlue(),previous==null?255:previous.getAlpha()));applyProperties();}
                });properties.add(color,"span 2, growx");
            }
            properties.add(new JLabel("<html>Colors: #RRGGBB, #RRGGBBAA or NA.<br>line_type: solid / dashed / dotted<br>head_style: open / filled; auto sizes allowed.</html>"),"span 2");
        } else {JButton matrix=new JButton("Edit matrix and palette…");matrix.addActionListener(e->heatmapDialog(layers.get(selected),false));properties.add(matrix,"span 2, growx");}
        Font font=UnifiedAccessPoint.getLaunchProperty().getDefaultFont();if(font!=null)applyFont(properties,font);properties.revalidate();properties.repaint();
    }
    static String parseColor(String text){String value=text.trim();if(value.equalsIgnoreCase("NA"))return null;if(!value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))throw new IllegalArgumentException("Use #RRGGBB, #RRGGBBAA or NA for colors.");return value.length()==7?value.toUpperCase(Locale.ROOT)+"FF":value.toUpperCase(Locale.ROOT);}
    private void applyProperties(){
        if(selected<0)return;JSONObject changed=FreeShapes.copy(layers.get(selected));
        try{
            for(var entry:fields.entrySet()){
                String key=entry.getKey(),text=entry.getValue().getText(),target=key.equals("color")?"fill_color":key;
                if(key.contains("color")){changed.put(target,parseColor(text));continue;}
                if(key.equals("text")){changed.put(key,text);continue;}
                if(List.of("locator","line_type","head_style").contains(key)){
                    List<String> valid=key.equals("locator")?List.of("left","center","right"):key.equals("line_type")?List.of("solid","dashed","dotted"):List.of("open","filled");
                    if(!valid.contains(text.trim()))throw new IllegalArgumentException(key+": choose "+String.join(" / ",valid));changed.put(key,text.trim());continue;
                }
                if(key.startsWith("head_")&&(text.isBlank()||text.equalsIgnoreCase("auto")||text.equalsIgnoreCase("NULL"))){changed.remove(key);continue;}
                double value=Double.parseDouble(text.trim());if(!Double.isFinite(value))throw new IllegalArgumentException(key+" must be finite.");
                if(List.of("width","height","size","head_length","head_width").contains(key)&&value<=0)throw new IllegalArgumentException(key+" must be positive.");
                if(key.equals("line_thickness")&&value<0)throw new IllegalArgumentException("line_thickness must be nonnegative.");changed.put(key,value);
            }
            if(!changed.equals(layers.get(selected))){remember(state());layers.set(selected,changed);}refresh();
        }catch(IllegalArgumentException error){showError(error.getMessage());}
    }
    private void showError(String text){if(dialog!=null)SwingDialog.showErrorMSGDialog(dialog,"Free shapes",text);}
    private static String matrixText(JSONObject source){if(source==null)return "0\t1\n1\t0";StringBuilder out=new StringBuilder();for(Object row:source.getJSONArray("matrix")){List<String> values=new ArrayList<>();for(Object v:(JSONArray)row)values.add(v==null?"NA":v.toString());out.append(String.join("\t",values)).append('\n');}return out.toString();}
    private void heatmapDialog(JSONObject target,boolean creating){
        JSONObject existing=target.getJSONObject("source");JTextArea input=new JTextArea(matrixText(existing),10,35);
        JTextField colors=new JTextField(existing==null?"#2166AC, #FFFFFF, #B2182B":String.join(", ",existing.getJSONArray("colors").toJavaList(String.class)));
        JTextField limits=new JTextField(existing==null||existing.get("limits")==null?"auto":existing.getJSONArray("limits").get(0)+", "+existing.getJSONArray("limits").get(1));
        JPanel form=new JPanel(new MigLayout("fill, wrap 1","[grow,fill]","[][grow,fill][][][]"));form.add(new JLabel("Paste a numeric matrix (NA or blank cells are allowed)."));form.add(new JScrollPane(input));form.add(new JLabel("Palette colors (#RRGGBB or #RRGGBBAA, comma separated)"));form.add(colors);form.add(new JLabel("Limits: auto, or minimum, maximum"));form.add(limits);
        showHeatmapForm(target,creating,form,input,colors,limits);
    }
    private void showHeatmapForm(JSONObject target,boolean creating,JPanel form,JTextArea input,JTextField colors,JTextField limits){
        if(JOptionPane.showConfirmDialog(dialog,form,creating?"Add free_heatmap":"Edit free_heatmap",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        String matrixInput=input.getText(),paletteInput=colors.getText(),limitInput=limits.getText();long job=++heatmapJob;
        if(message!=null)message.setText("Preparing heatmap…");
        new SwingWorker<HeatmapResult,Void>(){
            @Override protected HeatmapResult doInBackground(){
                JSONObject source=new JSONObject(true);source.put("matrix",FreeShapes.parseMatrix(matrixInput));JSONArray palette=new JSONArray();for(String color:paletteInput.split(",",-1)){String parsed=parseColor(color);if(parsed==null)throw new IllegalArgumentException("Palette colors cannot be NA.");palette.add(parsed);}source.put("colors",palette);
                if(!limitInput.trim().equalsIgnoreCase("auto")){String[] range=limitInput.trim().split("[,\\s]+");if(range.length!=2)throw new IllegalArgumentException("Supply two limits, or auto.");JSONArray values=new JSONArray();for(String v:range)values.add(Double.parseDouble(v));source.put("limits",values);}else source.put("limits",null);
                JSONObject changed=FreeShapes.copy(target);changed.put("source",source);JSONArray cells=FreeShapes.heatColors(source);changed.put("cells",cells);return new HeatmapResult(changed,FreeShapes.heatmapImage(cells));
            }
            @Override protected void done(){
                try{
                    HeatmapResult result=get();if(disposed||!active||job!=heatmapJob){result.image().flush();return;}
                    if(creating){geometry.cacheHeatmap(result.layer().getJSONArray("cells"),result.image());add(result.layer());}
                    else {int index=-1;for(int i=0;i<layers.size();i++)if(layers.get(i)==target)index=i;if(index<0){result.image().flush();showError("The shape changed while the matrix was being prepared. Please apply again.");return;}remember(FreeShapeEditor.this.state());geometry.cacheHeatmap(result.layer().getJSONArray("cells"),result.image());layers.set(index,result.layer());selected=index;refresh();}
                }catch(Exception error){if(!disposed&&active&&job==heatmapJob){Throwable cause=error.getCause()==null?error:error.getCause();showError(cause.getMessage());showHeatmapForm(target,creating,form,input,colors,limits);}}
            }
        }.execute();
    }
    private record HeatmapResult(JSONObject layer,BufferedImage image) { }
    void dispose(){close();disposed=true;codeTimer.stop();if(codeWorker!=null)codeWorker.cancel(true);if(dialog!=null){dialog.dispose();dialog=null;}geometry.clear();}
}
