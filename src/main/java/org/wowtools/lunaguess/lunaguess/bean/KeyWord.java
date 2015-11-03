package org.wowtools.lunaguess.lunaguess.bean;
/**
 * 关键字
 * @author liuyu
 *
 */
public class KeyWord {

	/**
	 * 关键字
	 */
	private String value;
	
	/**
	 * 权重
	 */
	private double weight;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
}
