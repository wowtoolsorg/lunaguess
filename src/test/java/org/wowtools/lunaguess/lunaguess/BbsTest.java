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

	/**
	 * 模拟生成的帖子数
	 */
	private static int topicCount = 10;

	/**
	 * 每个帖子平均有几个词(10%的波动)
	 */
	private static int topicWordNum = 350;

	/**
	 * 模拟用户tom阅读了多少帖子
	 */
	private static int tomReadNum = 3;

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
		initData();


		/** 查询tom最感兴趣的帖子正文中的关键字 **/
		Map<String, List<KeyWord>> favoriteKwMap = lunaGuess.analyzeBehaviorKeyWords("tom", new String[]{"content"}, 2);
		List<KeyWord> favoriteKwInContent = favoriteKwMap.get("content");
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
		
		lunaGuess.initIkAnalyzer( new String[]{"title","content"});//帖子中有中文信息，所以调用initIkAnalyzer来支持ik插件的中文分词
		System.out.println("initIkAnalyzer success");

		/** 模拟生成几个帖子，并作为要素存入LunaGuess **/
		Feature[] topics = buildTopics(topicCount,topicWordNum);
		lunaGuess.bulkAddFeature(topics, false);
		System.out.println("bulkAddFeature success");

		/** 模拟用户tom浏览帖子，并将其浏览记录作为行为存入LunaGuess **/
		String[] readIds = new String[tomReadNum];
		String[] userIds = new String[tomReadNum];
		Random r = new Random();
		for(int i = 0;i<tomReadNum;i++){
			readIds[i] = "topic"+r.nextInt(topicCount);
			userIds[i] = "tom";
		}
		lunaGuess.bulkAddBehavior(readIds, userIds, true);
		System.out.println("bulkAddBehavior success");
	}

	/**
	 * 模拟生成几个帖子，帖子包括标题(title)和内容
	 * @return
	 */
	private static Feature[] buildTopics(int topicCount,int wordNum){
		String[] words = readWords();
		Feature[] topics = new Feature[topicCount];
		for(int i = 0;i<topics.length;i++){
			Feature topic = new Feature();
			topic.setId("topic"+i);
			topic.setProperties(new Property[]{
					new Property("title",buildText(words, 10)),
					new Property("content",buildText(words, wordNum)),
			});
			topics[i] = topic;
		}
		return topics;
	}

	private static String[] readWords(){
		String res = null;
		String classPath;
		classPath = BbsTest.class.getClassLoader().getResource("/")!=null?
				BbsTest.class.getClassLoader().getResource("/").getPath():
					BbsTest.class.getResource("/").getPath();
		classPath = classPath.replace("%20", " ");
		File file = new File(classPath+"words.dic") ;
		try {
			InputStream is = new FileInputStream(file) ;
			byte b[] = new byte[is.available()] ;
			is.read(b) ;
			res = new String(b);
			is.close() ;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return res.split("\n");
	}

	private static String buildText(String[] words,int wordNum){
		Random r = new Random();
		StringBuffer sb = new StringBuffer();
		wordNum = (int) ((0.9+Math.random()*0.2)*wordNum);
		for(int i = 0;i<wordNum;i++){
			sb.append(words[r.nextInt(words.length)].trim());
		}
		return sb.toString();
	}

}
