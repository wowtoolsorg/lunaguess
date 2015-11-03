package org.wowtools.lunaguess.lunaguess;

import java.util.Random;

import org.wowtools.util.HttpHelper;

public class LunaGuess {
	HttpHelper hh = new HttpHelper();

	/**
	 * es集群地址
	 */
	private String[] esUrls;

	/**
	 * 存放到es的索引名
	 */
	private String idxName;

	/**
	 * 存放要素的es type名
	 */
	private String featureTypeName;

	/**
	 * 存放行为的es type名
	 */
	private String behaviorTypeName;

	private Random random = new Random();

	/**
	 *
	 * @param esUrls es集群地址 ，如http://127.0.0.1:9200/
	 * @param idxName 存放到es的索引名
	 * @param featureTypeName 存放要素的es type名
	 * @param behaviorTypeName 存放要素的es type名
	 */
	public LunaGuess(String[] esUrls, String idxName, String featureTypeName, String behaviorTypeName) {
		this.esUrls = esUrls;
		this.idxName = idxName;
		this.featureTypeName = featureTypeName;
		this.behaviorTypeName = behaviorTypeName;
		buildNested();
	}
	/**
	 * 在es中构建behavior和feature的nested-parent关系
	 * **/
	private void buildNested(){
		try {
			hh.doGet(getRandomUrl().toString());
		} catch(Exception e) {
			throw new RuntimeException("建立索引"+idxName+"异常:",e);
		}
		StringBuffer sbParam = new StringBuffer();
		sbParam.append("{\"mappings\":{\"").append(behaviorTypeName).append("\":{\"_parent\":{\"type\":\"").append(featureTypeName).append("\"}}}}");
		StringBuffer sbUrl = getRandomUrl();
		try {
			hh.doPostNotReturn(sbUrl.toString(), sbParam.toString());
		} catch(Exception e) {
			throw new RuntimeException("在es中构建behavior和feature的nested-parent关系异常:",e);
		}
	}

	/**
	 * 使索引支持中文分词，feature中包含中文时务必调用此方法
	 */
	public void initIkAnalyzer(){

	}

	/**
	 * 批量添加用户行为
	 *
	 * @param featureIds
	 *            要素id，如用户浏览的帖子id
	 * @param userIds
	 *            用户id
	 */
	public void bulkAddBehavior(String[] featureIds, String[] userIds){
		StringBuffer sbParam = new StringBuffer();
		for(int i = 0;i<featureIds.length;i++){
			String userId = userIds[i];
			String featureId = featureIds[i];
			sbParam.append("{\"index\":{\"_id\":\"")
			.append(userId).append("-").append(featureId)//id
			.append("\",\"parent\":\"").append(featureId)//parent为featureId
			.append("\"}}\n");
			sbParam.append("{\"fid\":\"").append(featureId).append(",\"uid\":\"").append(userId).append("\"}\n");
		}
		StringBuffer sbUrl = getRandomUrl(behaviorTypeName).append("/_bulk");
		try {
			hh.doPostNotReturn(sbUrl.toString(), sbParam.toString());
		} catch(Exception e) {
			throw new RuntimeException("bulkAddBehavior异常:",e);
		}
	}

	private StringBuffer getRandomUrl(){
		StringBuffer sb = new StringBuffer();
		sb.append(esUrls[random.nextInt(esUrls.length)]);
		sb.append(idxName);
		sb.append("/");
		return sb;
	}

	private StringBuffer getRandomUrl(String typeName){
		StringBuffer sb = getRandomUrl();
		sb.append(typeName).append("/");
		return sb;
	}

}

/*
curl -XPUT 192.168.0.195:9200/lt-data-common/_settings -d '
{
   "analysis": {
      "analyzer":{
             "ikAnalyzer":{
                 "type":"org.elasticsearch.index.analysis.IkAnalyzerProvider",
                    "alias":"ik"
                }
            }
     }
}
'
/elasticsearch -d -Xms20m -Xmx100m
curl -XPOST localhost:9200/test/1 -d '{"sss":1}'

curl -XPOST http://220.165.4.27:9200/lt-data-common/_close
curl -XPOST 192.168.0.195:9200/_close


curl -XPOST 192.168.0.195:9200/lt-data-common/_open

192.168.0.195:9200/lt-data-common/_analyze?analyzer=ik&text=中文分词
*/
