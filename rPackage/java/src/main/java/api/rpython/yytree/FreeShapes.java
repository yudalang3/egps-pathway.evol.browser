package api.rpython.yytree;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Shared logical geometry, painting and R serialization for free layers. */
final class FreeShapes {
    final YyTreeBridge.YyTreePanel canvas;
    private final Map<JSONArray, BufferedImage> heatmaps = new IdentityHashMap<>();
    FreeShapes(YyTreeBridge.YyTreePanel canvas) { this.canvas = canvas; }
    static JSONObject copy(JSONObject layer) {
        JSONObject result=new JSONObject(true);
        // Matrix sources and cell arrays are immutable; history shares them rather than
        // copying a potentially large matrix on every mouse press.
        for(var entry:layer.entrySet())result.put(entry.getKey(),List.of("source","cells").contains(entry.getKey())?entry.getValue():copyGeometry(entry.getValue()));
        return result;
    }
    private static Object copyGeometry(Object value) {
        if(value instanceof JSONArray array){JSONArray result=new JSONArray();for(Object item:array)result.add(copyGeometry(item));return result;}
        if(value instanceof JSONObject object){JSONObject result=new JSONObject(true);for(var entry:object.entrySet())result.put(entry.getKey(),copyGeometry(entry.getValue()));return result;}
        return value;
    }
    static double n(JSONObject layer, String key, double fallback) { return YyTreeBridge.number(layer,key,fallback); }
    double unit(JSONObject layer, boolean horizontal) {
        double extent = horizontal ? canvas.logicalWidth : canvas.logicalHeight;
        return switch (layer.getString("mode")) { case "relative" -> extent; case "percent" -> extent / 100; default -> 1; };
    }
    double value(JSONObject layer, String key, boolean horizontal) { return n(layer,key,0) * unit(layer,horizontal); }
    Point2D point(JSONObject layer, JSONArray point) { return new Point2D.Double(point.getDoubleValue(0)*unit(layer,true),point.getDoubleValue(1)*unit(layer,false)); }
    static JSONArray pair(double x, double y) { JSONArray result=new JSONArray(); result.add(x);result.add(y);return result; }
    Point2D anchor(JSONObject l) { return new Point2D.Double(value(l,"x",true),value(l,"y",false)); }
    void setPoint(JSONObject l, String x, String y, Point2D p) { l.put(x,p.getX()/unit(l,true));l.put(y,p.getY()/unit(l,false)); }
    void setPoint(JSONObject l, String key, int index, Point2D p) { l.getJSONArray(key).set(index,pair(p.getX()/unit(l,true),p.getY()/unit(l,false))); }
    Font font(JSONObject l) { return canvas.labelFont.deriveFont((float)n(l,"size",12)); }
    Rectangle2D textBounds(JSONObject l) {
        var metrics=canvas.metricsHost.getFontMetrics(font(l));
        double w=metrics.stringWidth(l.getString("text")),x=value(l,"x",true),y=value(l,"y",false),gap=n(l,"gap",10);
        x+=switch(l.getString("locator")){case "right"->-w-gap;case "center"->-w/2;default->gap;};
        return new Rectangle2D.Double(x,y-metrics.getAscent(),Math.max(1,w),metrics.getHeight());
    }
    Path2D curve(JSONObject l,String key) {
        JSONArray ps=l.getJSONArray(key);Point2D a=point(l,ps.getJSONArray(0)),b=point(l,ps.getJSONArray(1)),c=point(l,ps.getJSONArray(2)),d=point(l,ps.getJSONArray(3));
        Path2D p=new Path2D.Double();p.moveTo(a.getX(),a.getY());p.curveTo(b.getX(),b.getY(),c.getX(),c.getY(),d.getX(),d.getY());return p;
    }
    record Arrow(Shape shaft, Shape head, boolean filled) { }
    Arrow arrow(JSONObject l) {
        double x1=value(l,"x1",true),y1=value(l,"y1",false),x2=value(l,"x2",true),y2=value(l,"y2",false);
        double distance=Point2D.distance(x1,y1,x2,y2),t=n(l,"line_thickness",1);
        if(distance==0)return new Arrow(new Path2D.Double(),new Path2D.Double(),false);
        double length=n(l,"head_length",Math.max(9,4*t)),width=n(l,"head_width",Math.max(7,2.5*t));
        double factor=Math.min(1,.8*distance/length);length*=factor;width*=factor;
        double ux=(x2-x1)/distance,uy=(y2-y1)/distance;
        boolean filled="filled".equals(l.getString("head_style"));
        Path2D path=new Path2D.Double();path.moveTo(-length,width/2);path.lineTo(0,0);path.lineTo(-length,-width/2);
        Shape head;
        if(filled){path.closePath();head=path;}
        else {
            // Normalize the stroked V envelope to the requested dimensions. Otherwise
            // a thick miter extends far past the endpoint and closes the V's opening.
            Shape outline=new BasicStroke((float)Math.min(t,width/4),BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER).createStrokedShape(path);
            Rectangle2D bounds=outline.getBounds2D();AffineTransform fit=new AffineTransform();fit.translate(-length, -width/2);fit.scale(length/bounds.getWidth(),width/bounds.getHeight());fit.translate(-bounds.getX(),-bounds.getY());head=fit.createTransformedShape(outline);
        }
        AffineTransform placement=AffineTransform.getTranslateInstance(x2,y2);placement.rotate(Math.atan2(uy,ux));head=placement.createTransformedShape(head);
        double shaftEnd=filled?length:Math.min(distance,Math.max(t/2,length/4));
        return new Arrow(new Line2D.Double(x1,y1,x2-shaftEnd*ux,y2-shaftEnd*uy),head,filled);
    }
    Shape shape(JSONObject l) {
        double x=value(l,"x",true),y=value(l,"y",false),w=value(l,"width",true),h=value(l,"height",false);
        return switch(l.getString("type")) {
            case "text" -> textBounds(l);
            case "ellipse" -> new Ellipse2D.Double(x-w/2,y-h/2,w,h);
            case "rectangle","free_heatmap" -> new Rectangle2D.Double(x,y,w,h);
            case "line" -> new Line2D.Double(value(l,"x1",true),value(l,"y1",false),value(l,"x2",true),value(l,"y2",false));
            case "arrow" -> { Arrow a=arrow(l);Path2D p=new Path2D.Double();p.append(a.shaft(),false);p.append(a.head(),false);yield p; }
            case "bezier" -> curve(l,"points");
            case "bezier_ring" -> {
                Path2D p=curve(l,"points");JSONArray ps=l.getJSONArray("points2");Point2D d=point(l,ps.getJSONArray(3)),c=point(l,ps.getJSONArray(2)),b=point(l,ps.getJSONArray(1)),a=point(l,ps.getJSONArray(0));
                p.lineTo(d.getX(),d.getY());p.curveTo(c.getX(),c.getY(),b.getX(),b.getY(),a.getX(),a.getY());p.closePath();yield p;
            }
            case "polygon" -> {
                Path2D p=new Path2D.Double();JSONArray ps=l.getJSONArray("points");for(int i=0;i<ps.size();i++){Point2D v=point(l,ps.getJSONArray(i));if(i==0)p.moveTo(v.getX(),v.getY());else p.lineTo(v.getX(),v.getY());}p.closePath();yield p;
            }
            default -> throw new IllegalArgumentException("Unknown free element: "+l.getString("type"));
        };
    }
    BasicStroke stroke(JSONObject l) {
        float width=(float)Math.max(.01,n(l,"line_thickness",1));String type=l.getString("line_type");
        return new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,"dashed".equals(type)?new float[]{6,4}:"dotted".equals(type)?new float[]{1,3}:null,0);
    }
    boolean hit(JSONObject l,Point2D p,double tolerance) {
        Shape s=shape(l);String type=l.getString("type");
        boolean interior=type.equals("text")||type.equals("free_heatmap")||l.get("fill_color")!=null||("arrow".equals(type)&&"filled".equals(l.getString("head_style")));
        return (interior&&s.contains(p))||new BasicStroke((float)Math.max(2*tolerance,n(l,"line_thickness",1))).createStrokedShape(s).contains(p);
    }
    void prepareHeatmap(JSONObject l) {
        if(!"free_heatmap".equals(l.getString("type")))return;
        JSONArray cells=l.getJSONArray("cells");if(heatmaps.containsKey(cells))return;
        heatmaps.put(cells,heatmapImage(cells));
    }
    static BufferedImage heatmapImage(JSONArray cells) {
        BufferedImage image=new BufferedImage(cells.getJSONArray(0).size(),cells.size(),BufferedImage.TYPE_INT_ARGB);
        for(int row=0;row<image.getHeight();row++)for(int col=0;col<image.getWidth();col++){
            Color color=YyTreeBridge.color(cells.getJSONArray(row).getString(col));image.setRGB(col,row,color==null?0:color.getRGB());
        }
        return image;
    }
    void cacheHeatmap(JSONArray cells,BufferedImage image) { heatmaps.put(cells,image); }
    void clear() { heatmaps.values().forEach(BufferedImage::flush);heatmaps.clear(); }
    void draw(Graphics2D original,JSONObject l) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color fill=YyTreeBridge.color(l.getString("fill_color")),line=YyTreeBridge.color(l.getString("line_color"));
            String type=l.getString("type");g.setStroke(stroke(l));
            if(type.equals("text")){if(fill!=null){g.setFont(font(l));g.setColor(fill);g.drawString(l.getString("text"),(float)textBounds(l).getX(),(float)value(l,"y",false));}return;}
            if(type.equals("free_heatmap")){
                prepareHeatmap(l);BufferedImage image=heatmaps.get(l.getJSONArray("cells"));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                AffineTransform transform=AffineTransform.getTranslateInstance(value(l,"x",true),value(l,"y",false));
                transform.scale(value(l,"width",true)/image.getWidth(),value(l,"height",false)/image.getHeight());g.drawImage(image,transform,null);return;
            }
            if(type.equals("arrow")){
                if(line==null||n(l,"line_thickness",1)<=0)return;
                Arrow a=arrow(l);g.setColor(line);g.draw(a.shaft());
                g.fill(a.head());return;
            }
            Shape shape=shape(l);if(fill!=null){g.setColor(fill);g.fill(shape);}if(line!=null&&n(l,"line_thickness",1)>0){g.setColor(line);g.draw(shape);}
        } finally {g.dispose();}
    }
    void transform(JSONObject l,AffineTransform tx) {
        String type=l.getString("type");
        if(l.containsKey("points")) {
            for(String key:new String[]{"points","points2"})if(l.containsKey(key))for(int i=0;i<l.getJSONArray(key).size();i++)setPoint(l,key,i,tx.transform(point(l,l.getJSONArray(key).getJSONArray(i)),null));
        } else if(type.equals("line")||type.equals("arrow")) {
            for(int i=1;i<=2;i++)setPoint(l,"x"+i,"y"+i,tx.transform(new Point2D.Double(value(l,"x"+i,true),value(l,"y"+i,false)),null));
        } else {
            if(type.equals("text")) {
                // Scale the anchor as well as the logical-pixel font and alignment gap.
                setPoint(l,"x","y",tx.transform(anchor(l),null));
                double scale=Math.abs(tx.getScaleX());l.put("size",Math.max(.1,n(l,"size",12)*scale));l.put("gap",n(l,"gap",10)*scale);
            } else {
                setPoint(l,"x","y",tx.transform(anchor(l),null));
                l.put("width",Math.max(.000001,n(l,"width",1)*Math.abs(tx.getScaleX())));l.put("height",Math.max(.000001,n(l,"height",1)*Math.abs(tx.getScaleY())));
            }
        }
    }
    void mode(JSONObject l,String mode) {
        double oldX=unit(l,true),oldY=unit(l,false);l.put("mode",mode);double sx=oldX/unit(l,true),sy=oldY/unit(l,false);
        for(String key:new String[]{"x","x1","x2","width"})if(l.containsKey(key))l.put(key,n(l,key,0)*sx);
        for(String key:new String[]{"y","y1","y2","height"})if(l.containsKey(key))l.put(key,n(l,key,0)*sy);
        for(String key:new String[]{"points","points2"})if(l.containsKey(key))for(int i=0;i<l.getJSONArray(key).size();i++){JSONArray p=l.getJSONArray(key).getJSONArray(i);l.getJSONArray(key).set(i,pair(p.getDoubleValue(0)*sx,p.getDoubleValue(1)*sy));}
    }
    static String quote(String value) {
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '\\'->out.append("\\\\");case '"'->out.append("\\\"");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<32||c==127)out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}}
        return out.append('"').toString();
    }
    static String r(Object value) { return value==null?"NA":value instanceof String?quote((String)value):value.toString(); }
    static String matrix(JSONArray rows) {
        List<String> values=new ArrayList<>();for(Object row:rows)for(Object cell:(JSONArray)row)values.add(cell==null?"NA_real_":r(cell));
        return "matrix(c("+String.join(", ",values)+"), nrow = "+rows.size()+", byrow = TRUE)";
    }
    static String vector(JSONArray values) { List<String> out=new ArrayList<>();for(Object v:values)out.add(r(v));return "c("+String.join(", ",out)+")"; }
    static String code(JSONObject l) {
        String type=l.getString("type"),name=type.equals("free_heatmap")?type:"free_"+type;List<String> args=new ArrayList<>();
        for(String key:new String[]{"x","y","width","height","x1","y1","x2","y2","text"})if(l.containsKey(key))args.add(key+" = "+r(l.get(key)));
        if(l.containsKey("points"))args.add((type.equals("bezier_ring")?"points1":"points")+" = "+matrix(l.getJSONArray("points")));
        if(l.containsKey("points2"))args.add("points2 = "+matrix(l.getJSONArray("points2")));
        if(type.equals("free_heatmap")) {
            JSONObject source=l.getJSONObject("source");if(source==null)throw new IllegalStateException("A heatmap has no source matrix. Paste its matrix in Properties, or rerun the original script with the updated package.");
            args.add("matrix = "+matrix(source.getJSONArray("matrix")));args.add("colors = "+vector(source.getJSONArray("colors")));
            args.add("limits = "+(source.get("limits")==null?"NULL":vector(source.getJSONArray("limits"))));
        }
        args.add("mode = "+quote(l.getString("mode")));
        if(type.equals("text")) {
            args.add("locator = "+quote(l.getString("locator")));args.add("gap = "+r(l.get("gap")));args.add("color = "+r(l.get("fill_color")));args.add("size = "+r(l.get("size")));
        } else if(!type.equals("free_heatmap")) {
            if(!List.of("line","arrow","bezier").contains(type))args.add("fill_color = "+r(l.get("fill_color")));
            args.add("line_color = "+r(l.get("line_color")));args.add("line_thickness = "+n(l,"line_thickness",1));args.add("line_type = "+quote(l.getString("line_type")));
            if(type.equals("arrow")){args.add("head_style = "+quote(l.getString("head_style")==null?"open":l.getString("head_style")));for(String key:new String[]{"head_length","head_width"})args.add(key+" = "+(l.get(key)==null?"NULL":r(l.get(key))));}
        }
        return name+"("+String.join(", ",args)+")";
    }
    static String code(List<JSONObject> layers) {
        List<String> calls=new ArrayList<>();for(JSONObject l:layers)calls.add(code(l));return calls.isEmpty()?"# No free shapes": "+ "+String.join(" +\n  ",calls);
    }
    static JSONArray parseMatrix(String text) {
        JSONArray rows=new JSONArray();int rowNumber=0,columns=-1;
        for(String line:text.replaceAll("(?:\\r?\\n)+$", "").split("\\R",-1)){
            rowNumber++;String[] cells=line.contains("\t")?line.split("\t",-1):line.contains(",")?line.split(",",-1):line.trim().split("\\s+",-1);
            if(columns<0)columns=cells.length;if(cells.length!=columns)throw new IllegalArgumentException("Row "+rowNumber+" has "+cells.length+" columns; expected "+columns+".");
            JSONArray row=new JSONArray();for(int col=0;col<cells.length;col++){
                String token=cells[col].trim();if(token.isEmpty()||token.equalsIgnoreCase("NA")){row.add(null);continue;}
                try{double value=Double.parseDouble(token);if(!Double.isFinite(value))throw new NumberFormatException();row.add(value);}catch(NumberFormatException error){throw new IllegalArgumentException("Invalid number at row "+rowNumber+", column "+(col+1)+".");}
            }rows.add(row);
        }
        if(text.isBlank())throw new IllegalArgumentException("Paste a nonempty numeric matrix.");return rows;
    }
    /** Matches .heat_colors(): linear RGBA palette, 256 bins and ties-to-even indices. */
    static JSONArray heatColors(JSONObject source) {
        JSONArray rows=source.getJSONArray("matrix"),colors=source.getJSONArray("colors"),limits=source.getJSONArray("limits");
        if(colors==null||colors.isEmpty())throw new IllegalArgumentException("Choose at least one palette color.");
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        for(Object row:rows)for(Object cell:(JSONArray)row)if(cell!=null){double v=((Number)cell).doubleValue();low=Math.min(low,v);high=Math.max(high,v);}
        if(low==Double.POSITIVE_INFINITY){low=0;high=1;}
        if(limits!=null){low=limits.getDoubleValue(0);high=limits.getDoubleValue(1);}
        if(!Double.isFinite(low)||!Double.isFinite(high)||low>high)throw new IllegalArgumentException("Limits must be two finite increasing or equal values.");
        Color[] palette=new Color[256];
        for(int i=0;i<256;i++){
            double sample=i*(1.0/255),step=colors.size()==1?1:1.0/(colors.size()-1);
            int a=0,b=colors.size()-1;
            while(a+1<b){int middle=(a+b)/2;if(middle*step<=sample)a=middle;else b=middle;}
            double left=a*step,right=colors.size()==1?1:b*step;
            double f=a==b?0:(sample-left)/(right-left);
            Color ca=YyTreeBridge.color(colors.getString(a)),cb=YyTreeBridge.color(colors.getString(b));if(ca==null||cb==null)throw new IllegalArgumentException("Palette colors cannot be NA.");
            palette[i]=new Color(channel(ca.getRed(),cb.getRed(),f),channel(ca.getGreen(),cb.getGreen(),f),channel(ca.getBlue(),cb.getBlue(),f),channel(ca.getAlpha(),cb.getAlpha(),f));
        }
        JSONArray cells=new JSONArray();double span=high-low;
        for(Object object:rows){JSONArray row=new JSONArray();for(Object v:(JSONArray)object){
            if(v==null){row.add(null);continue;}double clipped=Math.max(low,Math.min(high,((Number)v).doubleValue()));
            double fraction=Double.isFinite(span)?(clipped-low)/span:(clipped/Math.max(Math.abs(low),Math.abs(high))-low/Math.max(Math.abs(low),Math.abs(high)))/(high/Math.max(Math.abs(low),Math.abs(high))-low/Math.max(Math.abs(low),Math.abs(high)));
            int index=span==0?127:(int)Math.rint(255*Math.max(0,Math.min(1,fraction)));Color c=palette[index];
            row.add(String.format(java.util.Locale.ROOT,"#%02X%02X%02X%02X",c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()));
        }cells.add(row);}return cells;
    }
    private static int channel(int a,int b,double fraction){
        // colorRamp interpolates normalized channels, then rgb truncates after
        // multiplying by 255. Reordering these operations changes some bins by 1.
        if(fraction==0)return a;if(fraction==1)return b;
        double low=a/255.0,high=b/255.0;return (int)(255*Math.max(0,Math.min(1,low+(high-low)*fraction)));
    }
}
