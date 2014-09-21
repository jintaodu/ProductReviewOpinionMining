
public class Synonym {//这个类用来记录同义词对

	/**
	 * @param args
	 */
	private String x;
	private String y;
	public Synonym(String x_axis, String y_axis) {
		x = x_axis;
		y = y_axis;
	}
	public boolean equals(Object o) {
		Synonym co = (Synonym) o;
		return (x.equals(co.x)&& y.equals(co.y))||(x.equals(co.y)&& y.equals(co.x));
	}

	public int hashCode() {
		return x.hashCode() + y.hashCode();
	}

}

