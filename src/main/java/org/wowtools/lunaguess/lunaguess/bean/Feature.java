package org.wowtools.lunaguess.lunaguess.bean;
/**
 * 要素。要素是属性的组合，表征了用户浏览的内容对象。如论坛系统中的帖子
 * @author liuyu
 *
 */
public class Feature {

	/**
	 * 属性
	 */
	private Property[] properties;

	/**
	 * 属性权重
	 */
	private double[] propertyWeights;

	public Property[] getProperties() {
		return properties;
	}

	public void setProperties(Property[] properties) {
		this.properties = properties;
	}

	public double[] getPropertyWeights() {
		return propertyWeights;
	}

	public void setPropertyWeights(double[] propertyWeights) {
		this.propertyWeights = propertyWeights;
	}

}
