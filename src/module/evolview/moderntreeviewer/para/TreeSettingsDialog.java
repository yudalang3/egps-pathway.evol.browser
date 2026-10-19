package module.evolview.moderntreeviewer.para;

import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TreeSettingsDialog extends JDialog {
    private boolean saving;

    public TreeSettingsDialog(Component parent, TreeSettingsPreferences preferences, TreeSettingsPreferences.State state,
                              PhylogeneticTreePanel tree, Supplier<PhylogeneticTreePanel> current) {
        super(SwingUtilities.getWindowAncestor(parent), "Advanced parameters", ModalityType.APPLICATION_MODAL);
        var fonts = UnifiedAccessPoint.getLaunchProperty();
        setFont(fonts.getDialogTitleFont());
        TreeSettingsSession session = new TreeSettingsSession(tree, current, state.values());
        TreeSettingsEditor editor = new TreeSettingsEditor(session.text());
        add(editor, BorderLayout.CENTER);
        JTextArea message = new JTextArea(2, 0);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setOpaque(false);
        message.setFont(fonts.getDialogContentFont());
        JButton apply = new JButton("Apply");
        JButton save = new JButton("Save as user defaults…");
        JButton close = new JButton("Close");
        JButton repair = state.warning() == null ? null : new JButton("Repair user defaults…");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (JButton button : new JButton[]{apply, save, close}) {
            button.setFont(fonts.getDialogButtonFont());
            button.setMargin(new Insets(6, 18, 6, 18));
            buttons.add(button);
        }
        if (repair != null) {
            repair.setFont(fonts.getDialogButtonFont());
            repair.setMargin(new Insets(6, 18, 6, 18));
            buttons.add(repair, 2);
            save.setEnabled(false);
            save.setToolTipText("Repair the saved user defaults before saving new ones.");
            repair.addActionListener(event -> showRepairDialog(preferences, state.text(), repairedState -> {
                repair.setVisible(false);
                save.setEnabled(true);
                save.setToolTipText(null);
                if (tree == null) {
                    session.refreshDefaults(repairedState.values());
                    editor.setText(session.text());
                }
                message.setForeground(UIManager.getColor("Label.foreground"));
                message.setText("User defaults repaired.");
                message.setToolTipText(null);
            }));
        }
        apply.setEnabled(session.canApply());
        apply.setToolTipText(session.canApply() ? "Validate all parameters, then apply changes to the current tree." : session.unavailableReason());
        if (state.warning() != null) {
            message.setForeground(new Color(165, 45, 45));
            message.setText("Saved user defaults could not be loaded. Use Repair user defaults to edit the original file.");
            message.setToolTipText(state.warning());
        } else message.setText(session.unavailableReason());
        apply.addActionListener(event -> {
            try {
                session.apply(editor.text()); editor.setText(session.text());
                message.setForeground(UIManager.getColor("Label.foreground"));
                message.setText("Applied to the current tree.");
                message.setToolTipText(null);
            } catch (IllegalArgumentException error) {
                message.setForeground(new Color(165, 45, 45));
                message.setText(error.getMessage()); message.setToolTipText(error.getMessage());
            }
            apply.setEnabled(session.canApply());
            if (!session.canApply()) apply.setToolTipText(session.unavailableReason());
        });
        save.addActionListener(event -> {
            java.util.Map<String, Object> values;
            try { values = session.values(editor.text()); }
            catch (IllegalArgumentException error) { message.setText(error.getMessage()); return; }
            saving = true;
            apply.setEnabled(false); save.setEnabled(false); close.setEnabled(false);
            editor.setEditingEnabled(false);
            message.setText("Saving user defaults…");
            preferences.saveDefaultsAsync(values).whenComplete((commit, failure) -> SwingUtilities.invokeLater(() -> {
                saving = false;
                apply.setEnabled(session.canApply()); save.setEnabled(true); close.setEnabled(true);
                editor.setEditingEnabled(true);
                Throwable cause = failure == null ? null : failure.getCause() == null ? failure : failure.getCause();
                message.setText(cause == null ? "User defaults saved. Apply updates the current tree separately." : cause.getMessage());
            }));
        });
        close.addActionListener(event -> dispose());
        JPanel bottom = new JPanel(new BorderLayout(10, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        bottom.add(message, BorderLayout.CENTER); bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { if (!saving) dispose(); }
        });
        getRootPane().registerKeyboardAction(event -> { if (!saving) dispose(); }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Rectangle screen = parent.getGraphicsConfiguration() == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds() : parent.getGraphicsConfiguration().getBounds();
        setSize(Math.min(getWidth(), screen.width - 40), Math.min(getHeight(), screen.height - 60));
        setLocationRelativeTo(parent);
    }

    private void showRepairDialog(TreeSettingsPreferences preferences, String originalText,
                                  Consumer<TreeSettingsPreferences.State> repaired) {
        JDialog dialog = new JDialog(this, "Repair user defaults", true);
        TreeSettingsEditor editor = new TreeSettingsEditor(originalText, "User defaults — Repair");
        dialog.add(editor, BorderLayout.CENTER);
        JTextArea error = new JTextArea(2, 0);
        error.setEditable(false);
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setOpaque(false);
        error.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
        error.setForeground(new Color(165, 45, 45));
        JButton save = new JButton("Save repaired defaults");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (JButton button : new JButton[]{save, cancel}) {
            button.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
            button.setMargin(new Insets(6, 18, 6, 18));
            buttons.add(button);
        }
        JPanel bottom = new JPanel(new BorderLayout(10, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        bottom.add(error, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        dialog.add(bottom, BorderLayout.SOUTH);
        boolean[] savingRepair = {false};
        save.addActionListener(event -> {
            String text = editor.text();
            try { TreeSettings.parse(text); }
            catch (IllegalArgumentException invalid) {
                error.setText(invalid.getMessage());
                error.setToolTipText(invalid.getMessage());
                return;
            }
            savingRepair[0] = true;
            save.setEnabled(false);
            cancel.setEnabled(false);
            editor.setEditingEnabled(false);
            error.setText("Saving repaired defaults…");
            preferences.saveAsync(text).whenComplete((commit, failure) -> SwingUtilities.invokeLater(() -> {
                savingRepair[0] = false;
                if (failure == null) {
                    dialog.dispose();
                    repaired.accept(commit.state());
                    return;
                }
                save.setEnabled(true);
                cancel.setEnabled(true);
                editor.setEditingEnabled(true);
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                error.setText(cause.getMessage());
                error.setToolTipText(cause.getMessage());
            }));
        });
        cancel.addActionListener(event -> dialog.dispose());
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { if (!savingRepair[0]) dialog.dispose(); }
        });
        dialog.getRootPane().registerKeyboardAction(event -> { if (!savingRepair[0]) dialog.dispose(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        Rectangle screen = getGraphicsConfiguration().getBounds();
        dialog.setSize(Math.min(dialog.getWidth(), screen.width - 40), Math.min(dialog.getHeight(), screen.height - 60));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
