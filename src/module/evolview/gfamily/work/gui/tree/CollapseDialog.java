package module.evolview.gfamily.work.gui.tree;

import egps2.UnifiedAccessPoint;
import graphic.engine.guibean.ColorIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollapseDialog extends JDialog {
	private static final Logger log = LoggerFactory.getLogger(CollapseDialog.class);

	private final JPanel contentPanel = new JPanel();
	private BiConsumer<Color, Integer> callBackConsumer;
	private JButton btnCol;
	private JSpinner spinner;

	final private String triangleSize = "Triangle size";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			CollapseDialog dialog = new CollapseDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public CollapseDialog(Color initialColor) {
		this(UnifiedAccessPoint.isGULaunched() ? UnifiedAccessPoint.getInstanceFrame() : null, initialColor);
	}

	public CollapseDialog(Window owner, Color initialColor) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setTitle("Collapse clade");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblTraiangleSize = new JLabel(triangleSize);
			lblTraiangleSize.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
			GridBagConstraints gbc_lblTraiangleSize = new GridBagConstraints();
			gbc_lblTraiangleSize.insets = new Insets(0, 0, 5, 5);
			gbc_lblTraiangleSize.gridx = 0;
			gbc_lblTraiangleSize.gridy = 0;
			contentPanel.add(lblTraiangleSize, gbc_lblTraiangleSize);
		}
		{

			int previousValue = 15;
			try { previousValue = Preferences.userNodeForPackage(getClass()).getInt(triangleSize, 15); }
			catch (SecurityException error) { log.warn("Collapse size preference is unavailable: {}", error.getMessage()); }

			spinner = new JSpinner();
			spinner.setModel(new SpinnerNumberModel(15, 1, 30, 1));
			spinner.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
			spinner.setValue(Math.max(1, Math.min(30, previousValue)));
			((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setFont(
					UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
			GridBagConstraints gbc_spinner = new GridBagConstraints();
			gbc_spinner.fill = GridBagConstraints.HORIZONTAL;
			gbc_spinner.insets = new Insets(0, 0, 5, 0);
			gbc_spinner.gridx = 2;
			gbc_spinner.gridy = 0;
			contentPanel.add(spinner, gbc_spinner);
		}
		{
			JLabel Color = new JLabel("Color");
			Color.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont());
			GridBagConstraints gbc_Color = new GridBagConstraints();
			gbc_Color.insets = new Insets(0, 0, 0, 5);
			gbc_Color.gridx = 0;
			gbc_Color.gridy = 1;
			contentPanel.add(Color, gbc_Color);
		}
		{
			btnCol = new JButton("Col");
			btnCol.setIcon(new ColorIcon(initialColor));
			btnCol.setForeground(initialColor);
			btnCol.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
			GridBagConstraints gbc_btnCol = new GridBagConstraints();
			gbc_btnCol.gridx = 2;
			gbc_btnCol.gridy = 1;
			contentPanel.add(btnCol, gbc_btnCol);
			btnCol.addActionListener(e -> {
				Color newColor = JColorChooser.showDialog(this, "Choose color", initialColor);
				if (newColor == null) return;
				btnCol.setForeground(newColor);
				btnCol.setIcon(new ColorIcon(newColor));
			});
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
				okButton.addActionListener(e -> {
					if (callBackConsumer != null) {
						Color background = btnCol.getForeground();
						Integer value = (Integer) spinner.getValue();
						callBackConsumer.accept(background, value);
					}

					try { Preferences.userNodeForPackage(getClass()).putInt(triangleSize, (int) spinner.getValue()); }
					catch (SecurityException error) { log.warn("Unable to save collapse size preference: {}", error.getMessage()); }
					this.dispose();
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
				cancelButton.addActionListener(e -> {
					this.dispose();
				});
				buttonPane.add(cancelButton);
			}
		}

		this.setLocationRelativeTo(getOwner());
		this.setSize(270, 200);
	}

	/**
	 * Create the dialog.
	 */
	public CollapseDialog() {
		this(Color.red);
	}

	public void setCallBackConsumer(BiConsumer<Color, Integer> callBackConsumer) {
		this.callBackConsumer = callBackConsumer;
	}

	public void setTriangleSize(int size) {
		spinner.setValue(size);
	}
}
