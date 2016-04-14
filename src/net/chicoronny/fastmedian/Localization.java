package net.chicoronny.fastmedian;


public class Localization {

	final private double X,Y;
	final protected long ID;
	static private long curID = 0;
	private boolean isLast;
	final private long frame;
	final private double photons;
	
	
	public Localization(double x, double y, double intensity, long frame) {
		X=x; Y=y; ID=curID++; this.frame=frame; isLast=false; this.photons = intensity;
	}
	
	public boolean isLast() {
		return isLast;
	}

	/**
	 * @param isLast - switch
	 */
	public void setLast(boolean isLast) {
		this.isLast = isLast;
	}

	public double getX() {
		return X;
	}

	public double getY() {
		return Y;
	}

	public long getFrame() {
		return frame;
	}
	
	public double getIntensity() {
		return photons;
	}

	@Override
	public String toString(){
		return "" + getX() + "\t" + getY() + "\t" + getIntensity() + "\t" + getFrame();
	}	

}
