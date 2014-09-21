package WordAlignmentModel;

public class Pair {//这个类用来记录同义词对

	/**
	 * @param args
	 */
	private String x;
	private String y;
	public Pair(String x_axis, String y_axis) {
		x = x_axis;
		y = y_axis;
	}
	public boolean equals(Object o) {
		Pair co = (Pair) o;
		return (x.equals(co.x)&& y.equals(co.y));
	}

	public int hashCode() {
		return x.hashCode() + y.hashCode();
	}
	public String toString()
	{
		return x+" "+y;
	}
	
	public String getx()
	{
		return x;
	}
	public String gety()
	{
		return y;
	}

}

