package module.evolview.phylotree.visualization.graphics.struct;

public class CircularLayoutProperty {
	
	int globalStartDegree = 285;
	
	int globalExtendDegree = 360;
	
	int innerCircleRadicus = 50;
	private Double innerRadiusRatio = 0.2;

	public int getGlobalStartDegree() {
		return globalStartDegree;
	}

	public void setGlobalStartDegree(int globalStartDegree) {
		this.globalStartDegree = globalStartDegree;
	}


	public int getInnerCircleRadicus() {
		return innerCircleRadicus;
	}

	public void setInnerCircleRadicus(int innerCircleRadicus) {
		this.innerCircleRadicus = innerCircleRadicus;
		innerRadiusRatio = 0.2;
	}

	public Double getInnerRadiusRatio() { return innerRadiusRatio; }

	public void setInnerRadiusRatio(Double ratio) {
		if (ratio != null && (!Double.isFinite(ratio) || ratio < 0.2 || ratio > 1))
			throw new IllegalArgumentException("Inner radius ratio must be between 0.2 and 1");
		innerRadiusRatio = ratio == null ? 0.2 : ratio;
	}

	public double resolveInnerRadius(double outerRadius) {
		return outerRadius * innerRadiusRatio;
	}

	public int getGlobalExtendDegree() {
		return globalExtendDegree;
	}

	public void setGlobalExtendDegree(int globalExtendDegree) {
		this.globalExtendDegree = globalExtendDegree;
	}

	
	
}
