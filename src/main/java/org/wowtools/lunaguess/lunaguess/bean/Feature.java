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
	 * 要素id。必须能唯一地标识要素，如帖子id
	 */
	private String id;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Property[] getProperties() {
		return properties;
	}

	public void setProperties(Property[] properties) {
		this.properties = properties;
	}

}
