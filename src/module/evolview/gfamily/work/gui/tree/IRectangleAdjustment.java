package module.evolview.gfamily.work.gui.tree;

public interface IRectangleAdjustment {
	
	boolean contains(double x, double y);
	
	boolean isAvailable();
	
	/**
	 * This method should be called when the contains method return true!
	 * @return The Cursor type index!
	 */
	int getCursorType();

	void adjustPaintings(double d, double e);
}
