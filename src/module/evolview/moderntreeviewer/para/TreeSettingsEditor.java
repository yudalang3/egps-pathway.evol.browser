package module.evolview.moderntreeviewer.para;

import egps2.UnifiedAccessPoint;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The two editor panes are also renderable in headless regression tests. */
public final class TreeSettingsEditor extends JPanel {
    private static final Pattern TOKENS = Pattern.compile("//[^\\r\\n]*|/\\*.*?(?:\\*/|$)|\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?|\\b(?:true|false|null)\\b", Pattern.DOTALL);
    private final JTextPane defaults = new CodePane();
    private final JTextPane user = new CodePane();
    private final JScrollPane defaultsScroll = new JScrollPane(defaults);
    private final JScrollPane userScroll = new JScrollPane(user);
    private boolean synchronizingScroll;
    private boolean restoringView;
    private String original;
    private boolean modified;

    public TreeSettingsEditor(String userText) {
        this(userText, "Current parameters — Editable");
    }

    TreeSettingsEditor(String userText, String rightTitle) {
        super(new BorderLayout(0, 10));
        original = userText;
        boolean repairing = rightTitle.endsWith("— Repair");
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel guidance = new JLabel(repairing ? "Edit the saved JSONC text to repair user defaults." : "Configure all tree parameters");
        guidance.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
        add(guidance, BorderLayout.NORTH);
        configure(defaults, TreeSettings.defaultText(), false);
        configure(user, userText, true);
        user.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { modified = true; }
            public void removeUpdate(DocumentEvent event) { modified = true; }
            public void changedUpdate(DocumentEvent event) { }
        });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                pane("Default settings — Read only", defaultsScroll), pane(rightTitle, userScroll));
        linkScrollBars(defaultsScroll.getVerticalScrollBar(), userScroll.getVerticalScrollBar());
        linkScrollBars(defaultsScroll.getHorizontalScrollBar(), userScroll.getHorizontalScrollBar());
        split.setResizeWeight(0.5);
        split.setDividerLocation(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        JLabel priority = new JLabel(repairing ? "Saving the repaired file updates user defaults for future trees."
                : "Program defaults → User defaults → Explicit VOICE parameters → Current tree edits");
        priority.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
        add(priority, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(1100, 680));
    }

    public void setText(String text) { user.setText(text); original = text; modified = false; }

    public void setTextPreservingView(String text) {
        if (text.equals(text())) return;
        int dot = user.getCaret().getDot(), mark = user.getCaret().getMark();
        Point left = defaultsScroll.getViewport().getViewPosition();
        Point right = userScroll.getViewport().getViewPosition();
        restoringView = true;
        setText(text);
        int length = user.getDocument().getLength();
        user.getCaret().setDot(Math.min(mark, length));
        user.getCaret().moveDot(Math.min(dot, length));
        restorePosition(defaultsScroll, left);
        restorePosition(userScroll, right);
        // Text layout and caret visibility updates can run after the action listener.
        SwingUtilities.invokeLater(() -> {
            try {
                defaultsScroll.validate();
                userScroll.validate();
                restorePosition(defaultsScroll, left);
                restorePosition(userScroll, right);
            } finally { restoringView = false; }
        });
    }

    private static void restorePosition(JScrollPane scroll, Point position) {
        JViewport viewport = scroll.getViewport();
        Dimension size = viewport.getViewSize(), extent = viewport.getExtentSize();
        viewport.setViewPosition(new Point(Math.max(0, Math.min(position.x, size.width - extent.width)),
                Math.max(0, Math.min(position.y, size.height - extent.height))));
    }

    private void linkScrollBars(JScrollBar left, JScrollBar right) {
        left.addAdjustmentListener(event -> synchronizeScroll(left, right));
        right.addAdjustmentListener(event -> synchronizeScroll(right, left));
    }

    private void synchronizeScroll(JScrollBar source, JScrollBar target) {
        if (synchronizingScroll || restoringView) return;
        int sourceRange = source.getMaximum() - source.getMinimum() - source.getVisibleAmount();
        int targetRange = target.getMaximum() - target.getMinimum() - target.getVisibleAmount();
        if (sourceRange <= 0 || targetRange <= 0) return;
        synchronizingScroll = true;
        try {
            double progress = (source.getValue() - source.getMinimum()) / (double) sourceRange;
            target.setValue(target.getMinimum() + (int) Math.round(progress * targetRange));
        } finally { synchronizingScroll = false; }
    }

    public String text() { return modified ? user.getText() : original; }
    public void setEditingEnabled(boolean enabled) { user.setEditable(enabled); }

    private static final class CodePane extends JTextPane {
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport viewport && getUI().getPreferredSize(this).width <= viewport.getWidth();
        }
    }

    private JPanel pane(String title, JScrollPane scroll) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setMinimumSize(new Dimension(220, 200));
        panel.setPreferredSize(new Dimension(540, 600));
        JLabel heading = new JLabel(title);
        heading.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogTitleFont());
        panel.add(heading, BorderLayout.NORTH);
        scroll.setRowHeaderView(new LineNumbers((JTextPane) scroll.getViewport().getView()));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void configure(JTextPane editor, String text, boolean editable) {
        editor.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
        editor.setMargin(new Insets(8, 10, 8, 10));
        editor.setText(text);
        editor.setEditable(editable);
        editor.setToolTipText(editable
                ? TreeSettingsHelp.tooltip("Edit parameters", "Comments and trailing commas are allowed.",
                        "Apply checks your edits before updating the current tree.")
                : TreeSettingsHelp.tooltip("Program defaults", "Read only. You can select and copy text.",
                        "Each parameter includes an explanation and its VOICE key."));
        editor.setFocusTraversalKeysEnabled(!editable);
        editor.setCaretPosition(0);
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "four-spaces");
        editor.getActionMap().put("four-spaces", new AbstractAction() {
            public void actionPerformed(ActionEvent event) { if (editor.isEditable()) editor.replaceSelection("    "); }
        });
        Timer timer = new Timer(100, event -> highlight(editor));
        timer.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { timer.restart(); }
            public void removeUpdate(DocumentEvent event) { timer.restart(); }
            public void changedUpdate(DocumentEvent event) { }
        });
        highlight(editor);
    }

    private static void highlight(JTextPane editor) {
        StyledDocument document = editor.getStyledDocument();
        String text;
        try { text = document.getText(0, document.getLength()); }
        catch (BadLocationException error) { throw new IllegalStateException(error); }
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, editor.getForeground());
        document.setCharacterAttributes(0, document.getLength(), normal, true);
        boolean dark = editor.getBackground().getRed() < 128;
        Matcher matcher = TOKENS.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            Color color;
            if (token.startsWith("/")) color = dark ? new Color(160, 170, 160) : new Color(103, 115, 110);
            else if (token.startsWith("\"")) {
                int next = matcher.end();
                while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
                boolean key = next < text.length() && text.charAt(next) == ':';
                color = key ? (dark ? new Color(140, 190, 245) : new Color(38, 85, 150))
                        : (dark ? new Color(170, 215, 160) : new Color(40, 115, 65));
            } else color = dark ? new Color(210, 170, 235) : new Color(135, 70, 155);
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, color);
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), attributes, false);
        }
    }

    private static final class LineNumbers extends JComponent {
        private final JTextPane editor;
        LineNumbers(JTextPane editor) {
            this.editor = editor;
            setFont(editor.getFont());
            setOpaque(true);
            setBackground(UIManager.getColor("Panel.background"));
            editor.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) { revalidate(); repaint(); }
            });
            editor.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) { revalidate(); repaint(); }
                public void removeUpdate(DocumentEvent event) { revalidate(); repaint(); }
                public void changedUpdate(DocumentEvent event) { repaint(); }
            });
        }
        @Override public Dimension getPreferredSize() {
            int digits = Integer.toString(editor.getDocument().getDefaultRootElement().getElementCount()).length();
            return new Dimension(getFontMetrics(getFont()).charWidth('0') * Math.max(3, digits) + 14,
                    editor.getPreferredSize().height);
        }
        @Override protected void paintComponent(Graphics graphics) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setFont(getFont());
            graphics.setColor(UIManager.getColor("Label.disabledForeground"));
            Element root = editor.getDocument().getDefaultRootElement();
            Rectangle clip = graphics.getClipBounds();
            int first = root.getElementIndex(editor.viewToModel2D(new Point(0, clip.y)));
            for (int i = first; i < root.getElementCount(); i++) {
                try {
                    Rectangle2D bounds = editor.modelToView2D(root.getElement(i).getStartOffset());
                    if (bounds == null || bounds.getY() > clip.getMaxY()) break;
                    String number = Integer.toString(i + 1);
                    graphics.drawString(number, getWidth() - 7 - graphics.getFontMetrics().stringWidth(number),
                            (int) bounds.getY() + graphics.getFontMetrics().getAscent());
                } catch (BadLocationException error) { throw new IllegalStateException(error); }
            }
        }
    }
}
