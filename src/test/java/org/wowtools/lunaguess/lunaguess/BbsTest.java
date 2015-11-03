package org.wowtools.lunaguess.lunaguess;

import java.util.List;
import java.util.Map;

import org.wowtools.lunaguess.lunaguess.bean.Feature;
import org.wowtools.lunaguess.lunaguess.bean.KeyWord;
import org.wowtools.lunaguess.lunaguess.bean.Property;

/**
 * 以一个论坛系统中分析用户最喜好的帖子为例，介绍lunaguess的用法
 * @author liuyu
 *
 */
public class BbsTest {
	
	private static String[] esUrls = new String[]{"http://220.165.4.27:9200/"};
	
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
	
	private static LunaGuess lunaGuess = new LunaGuess(esUrls, idxName, featureTypeName, behaviorTypeName);

	public static void main(String[] args) throws Exception {
		lunaGuess.initIkAnalyzer();//帖子中有中文信息，所以调用initIkAnalyzer来支持ik插件的中文分词
		System.out.println("initIkAnalyzer success");
		
		/** 模拟生成3个帖子，并作为要素存入LunaGuess **/
		Feature[] topics = build3Topics();
		lunaGuess.bulkAddFeature(topics, false);
		System.out.println("bulkAddFeature success");
		
		topics = new Feature[100];
		for(int i = 0;i<topics.length;i++){
			topics[i] = new Feature();
			topics[i].setId(i+"");
			topics[i].setProperties(new Property[]{
				new Property("title",""+(i%10)),//标题
				new Property("content",""+(i%10)),//内容
			});
		}
		lunaGuess.bulkAddFeature(topics, false);
		
		/** 模拟用户tom、lily浏览帖子帖子，并将其浏览记录作为行为存入LunaGuess **/
		//tom阅读了topic1、topic2、topic1；lily阅读了topic2、topic3
		String[] readIds = new String[]{"topic1","topic2","topic1","topic2","topic3"};
		String[] userIds = new String[]{"tom","tom","tom","lily","lily"};
		lunaGuess.bulkAddBehavior(readIds, userIds, false);
		System.out.println("bulkAddBehavior success");
		
		/** 查询tom最感兴趣的帖子正文中的关键字 **/
		Map<String, List<KeyWord>> favoriteKwMap = lunaGuess.analyzeBehaviorKeyWords("tom", new String[]{"content"}, 2);
		List<KeyWord> favoriteKwInContent = favoriteKwMap.get("content");
		System.out.println("tom最感兴趣的正文关键字:");
		System.out.println("关键字\t权重");
		for(KeyWord kw:favoriteKwInContent){
			System.out.print(kw.getValue()+"\t"+kw.getWeight());
		}
	}
	
	/**
	 * 模拟生成3个帖子，帖子包括标题(title)和内容
	 * @return
	 */
	private static Feature[] build3Topics(){
		Feature topic1 = new Feature();
		topic1.setId("topic1");
		topic1.setProperties(new Property[]{
				new Property("title","这是我的第一个帖子"),//标题
				new Property("content","新人发帖，请多关照O(∩_∩)O~"),//内容
		});
		
		Feature topic2 = new Feature();
		topic2.setId("topic2");
		topic2.setProperties(new Property[]{
				new Property("title","Java反射的性能如何"),//标题
				new Property("content","getField方法非常耗时，set/get方法是很快的"),//内容
		});
		
		Feature topic3 = new Feature();
		topic3.setId("topic3");
		topic3.setProperties(new Property[]{
				new Property("title","如何正确使用设计模式"),//标题
				new Property("content","好的架构不是设计出来的，而是进化出来的"),//内容
		});
		
		Feature[] topics = new Feature[]{topic1,topic2,topic3};
		return topics;
	}

}

/*

GET /wowtools/Topic/_search
{
  "query": {
    "has_child": {
      "type": "ReadingHistory",
      "query": {
        "term": {
          "uid": {
            "value": "tom"
          }
        }
      }
    }
  }, 
  "aggs": {
    "NAME": {
      "significant_terms": {
        "field": "content"
      }
    }
  }
}
*/
