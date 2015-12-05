package org.wowtools.lunaguess.lunaguess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.wowtools.lunaguess.lunaguess.bean.Feature;
import org.wowtools.lunaguess.lunaguess.bean.KeyWord;
import org.wowtools.lunaguess.lunaguess.bean.Property;
import org.wowtools.util.HtmlFormatUtil;
import org.wowtools.util.HttpHelper;

/**
 * 以一个论坛系统中分析用户最喜好的帖子为例，介绍lunaguess的用法
 * @author liuyu
 *
 */
public class BbsTest {

	private static String[] esUrls = new String[]{"http://localhost:9200/"};

	/**
	 * 做一个索引wowtools，存放Topic type、ReadingHistory type
	 */
	private static String idxName = "wowtools";

	/**
	 * 把帖子信息存到Topic type下
	 */
	private static String featureTypeName = "Topic";

	/**
	 * 把用户浏览帖子的浏览记录存到ReadingHistory type下
	 */
	private static String behaviorTypeName = "ReadingHistory";

	private static LunaGuess lunaGuess;

	public static void main(String[] args) throws Exception {
		//删掉旧索引
		try {
			new HttpHelper().doDelete(esUrls[0]+"/"+idxName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		lunaGuess = new LunaGuess(esUrls, idxName, featureTypeName, behaviorTypeName);
		lunaGuess.buildNested();
		//模拟生成一些帖子数据和浏览记录
		
		lunaGuess.initIkAnalyzer( new String[]{"title","content"});//帖子中有中文信息，所以调用initIkAnalyzer来支持ik插件的中文分词
		System.out.println("initIkAnalyzer success");
		
		initData();


		/** 查询tom最感兴趣的帖子标题中的关键字 **/
		Map<String, List<KeyWord>> favoriteKwMap = lunaGuess.analyzeBehaviorKeyWords("tom", new String[]{"title"}, 2);
		List<KeyWord> favoriteKwInContent = favoriteKwMap.get("title");
		System.out.println("tom最感兴趣的正文关键字:");
		System.out.println("关键字\t权重");
		for(KeyWord kw:favoriteKwInContent){
			System.out.println(kw.getValue()+"\t"+kw.getWeight());
		}
		System.out.println("-----------------------");
		/** 根据查到的关键字，查询tom最喜欢的文章 **/
		List<Feature> favoriteTopics = lunaGuess.searchFeatureByKeyWord(new String[]{"title"}, favoriteKwMap, 3);
		System.out.println("tom最喜欢的文章:");
		System.out.println("id\t标题");
		for(Feature ft:favoriteTopics){
			System.out.println(ft.getId()+"\t"+ft.getProperties()[0].getContent());
		}
	}

	private static void initData(){

		/** 模拟生成几个帖子，并作为要素存入LunaGuess **/
		//HtmlFormatUtil.delHTMLTag(htmlStr);//若文本从用户提交，建议调用HtmlFormatUtil.delHTMLTag去除html标签等，否则会存储es异常
		Feature[] topics = new Feature[]{
				buildTopic("t1", "搜索引擎测试", ""),
				buildTopic("t2", "测试这行辛苦么", ""),
				buildTopic("t3", "哈哈", ""),
				buildTopic("t4", "测试一下能用不", ""),
				buildTopic("t5", "阿伟测试为提供", ""),
				buildTopic("t6", "面向文档的存储服务", ""),
				buildTopic("t6", "面向文档的存储服务", ""),
		};
		lunaGuess.bulkAddFeature(topics, false);
		System.out.println("bulkAddFeature success");

		/** 模拟用户tom浏览帖子，并将其浏览记录作为行为存入LunaGuess **/
		//tom阅读了id为"t1","t3","t4","t5"的四个帖子
		String[] readIds = new String[]{
				"t1","t2","t4",
		};
		String[] userIds = new String[]{
				"tom","tom","tom",
		};
		lunaGuess.bulkAddBehavior(readIds, userIds, true);
		System.out.println("bulkAddBehavior success");
	}

	/**
	 * 模拟生成帖子，帖子包括标题和内容
	 * @return
	 */
	private static Feature buildTopic(String id,String title,String content){
		Feature topic = new Feature();
		topic.setId(id);
		topic.setProperties(new Property[]{
				new Property("title",title),
				new Property("content",content),
		});
		return topic;
	}


}
