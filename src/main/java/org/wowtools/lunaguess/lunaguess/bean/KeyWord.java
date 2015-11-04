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
	 * 权重，默认值1
	 */
	private double weight;
	
	private boolean needWeight = false;

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
		needWeight = true;
	}

	public boolean isNeedWeight() {
		return needWeight;
	}
	
}
