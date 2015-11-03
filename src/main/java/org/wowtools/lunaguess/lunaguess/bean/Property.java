package org.wowtools.lunaguess.lunaguess.bean;
/**
 * 属性。属性是用户浏览内容的一个单元。如论坛系统中，帖子的标题、正文等
 * @author liuyu
 *
 */
public class Property {

	/**
	 * 属性名称
	 */
	private String name;

	/**
	 * 属性值/内容
	 */
	private String content;
	
	/**
	 * 
	 * @param name 属性名称
	 * @param content 属性值/内容
	 */
	public Property(String name,String content){
		this.name = name;
		this.content = content;
	}
	
	public Property(){
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
