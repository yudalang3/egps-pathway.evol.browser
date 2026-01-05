/**
 * 
 */
package module.evoltrepipline;

import javax.swing.*;
import java.awt.*;

/**
* @author YFQ
* @date 2019-07-02 10:46:40
* @version 1.0
* <p>Description:</p>
*/
public class EGPSComboBoxRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 2675688129924550384L;

	public EGPSComboBoxRenderer() {
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		  
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		  
	  if(value instanceof Item){
		 this.setToolTipText(((Item)value).getTip());
	  } else {
		  this.setToolTipText(null);
	  }
	  
      return this;
	}

}

