package org.wowtools.lunaguess.lunaguess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wowtools.lunaguess.lunaguess.bean.Feature;
import org.wowtools.lunaguess.lunaguess.bean.KeyWord;
import org.wowtools.lunaguess.lunaguess.bean.Property;
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
	 * @param esUrls
	 *            es集群地址 ，如http://127.0.0.1:9200/
	 * @param idxName
	 *            存放到es的索引名
	 * @param featureTypeName
	 *            存放要素的es type名
	 * @param behaviorTypeName
	 *            存放要素的es type名
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
	 **/
	private void buildNested() {
		try {
			hh.doGet(getRandomUrl().toString());
		} catch (Exception e) {
			throw new RuntimeException("建立索引" + idxName + "异常:", e);
		}
		StringBuffer sbParam = new StringBuffer();
		sbParam.append("{\"mappings\":{\"").append(behaviorTypeName).append("\":{\"_parent\":{\"type\":\"")
				.append(featureTypeName).append("\"}}}}");
		StringBuffer sbUrl = getRandomUrl();
		try {
			hh.doPostNotReturn(sbUrl.toString(), sbParam.toString());
		} catch (Exception e) {
			throw new RuntimeException("在es中构建behavior和feature的nested-parent关系异常:", e);
		}
	}

	/**
	 * 使索引支持中文分词，feature中包含中文时务必调用此方法
	 */
	public void initIkAnalyzer(String[] propertyNames) {
		String url = getRandomUrl().toString();
		try {
			// 设置索引的分词
			hh.doGetNotReturn(url + "_close");
			String param = "{\"analysis\": {\"analyzer\":{\"ikAnalyzer\":{\"type\":\"org.elasticsearch.index.analysis.IkAnalyzerProvider\",\"alias\":\"ik\"}}}}";
			hh.doPostNotReturn(url + "_settings", param);
			// hh.doGetNotReturn(url+"_open");
			// 设置mapping的分词
			StringBuffer sbMapping = new StringBuffer();
			sbMapping.append("{\"properties\":{");
			int n = propertyNames.length - 1;
			for (int i = 0; i < n; i++) {
				sbMapping.append("\"").append(propertyNames[i])
						.append("\":{\"type\":\"string\",\"indexAnalyzer\":\"ik\",\"searchAnalyzer\":\"ik\"},");
			}
			sbMapping.append("\"").append(propertyNames[n])
					.append("\":{\"type\":\"string\",\"indexAnalyzer\":\"ik\",\"searchAnalyzer\":\"ik\"}}}");
			hh.doPostNotReturn(url + featureTypeName + "/_mapping", sbMapping.toString());
		} catch (Exception e) {
			throw new RuntimeException("在es中构建behavior和feature的nested-parent关系异常:", e);
		}
	}

	/**
	 * 批量添加要素
	 *
	 * @param features
	 *            要素，如帖子
	 * @param needReturn
	 *            是否需要返回执行结果
	 */
	public String bulkAddFeature(Feature[] features, boolean needReturn) {
		// 将features转为符合 es rest api 的bulk方法 的json
		StringBuffer sbParam = new StringBuffer();
		for (Feature f : features) {
			Property[] properties = f.getProperties();
			int n = properties.length - 1;
			if (n < 1) {
				continue;
			}
			String fid = f.getId();
			sbParam.append("{\"index\":{\"_id\":\"").append(fid).append("\"}}\n");
			sbParam.append("{");
			for (int i = 0; i < n; i++) {
				Property p = properties[i];
				String content = p.getContent();
				if (null == content) {
					continue;
				}
				String name = p.getName();
				sbParam.append("\"").append(name).append("\":\"").append(content).append("\",");
			}
			Property p = properties[n];
			String content = p.getContent();
			if (null == content) {
				sbParam.append("}");
			} else {
				String name = p.getName();
				sbParam.append("\"").append(name).append("\":\"").append(content).append("\"}\n");
			}
		}
		// 存入featureType中
		StringBuffer sbUrl = getRandomUrl(featureTypeName).append("_bulk");
		try {
			if (needReturn) {
				String res = hh.doPost(sbUrl.toString(), sbParam.toString());
				return res;
			} else {
				hh.doPostNotReturn(sbUrl.toString(), sbParam.toString());
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException("bulkAddFeature异常:", e);
		}
	}

	/**
	 * 批量添加用户行为
	 *
	 * @param featureIds
	 *            要素id，如用户浏览的帖子id
	 * @param userIds
	 *            用户id
	 * @param needReturn
	 *            是否需要返回执行结果
	 */
	public String bulkAddBehavior(String[] featureIds, String[] userIds, boolean needReturn) {
		StringBuffer sbParam = new StringBuffer();
		for (int i = 0; i < featureIds.length; i++) {
			String userId = userIds[i];
			String featureId = featureIds[i];
			sbParam.append("{\"index\":{\"_id\":\"").append(userId).append("-").append(featureId)// id
					.append("\",\"parent\":\"").append(featureId)// parent为featureId
					.append("\"}}\n");
			sbParam.append("{\"uid\":\"").append(userId).append("\"}\n");
		}
		// 存入behaviorType中
		StringBuffer sbUrl = getRandomUrl(behaviorTypeName).append("_bulk");
		try {
			if (needReturn) {
				String res = hh.doPost(sbUrl.toString(), sbParam.toString());
				return res;
			} else {
				hh.doPostNotReturn(sbUrl.toString(), sbParam.toString());
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException("bulkAddBehavior异常:", e);
		}
	}

	/**
	 * 分析用户行为产生的关键字。如用户浏览的帖子中出现最多的词汇
	 * 
	 * @param uid
	 *            用户唯一标识
	 * @param propertyNames
	 *            需要分析的属性名
	 * @param maxReturn
	 *            每个属性最多返回关键字数
	 * @return Map<String,List<KeyWord>> 关键字数组，与propertyNames对应，List
	 *         <KeyWord>的最大长度为maxReturn
	 */
	public Map<String, List<KeyWord>> analyzeBehaviorKeyWords(String uid, String[] propertyNames, int maxReturn) {
		StringBuffer sbParam = new StringBuffer();
		sbParam.append("{");
		// 过滤条件
		sbParam.append("\"query\":{\"filtered\":{\"filter\":{\"has_child\":{\"type\": \"").append(behaviorTypeName)
				.append("\",\"query\": {\"term\":{\"uid\":\"");
		sbParam.append(uid);
		sbParam.append("\"}}}}}},");
		// 聚合各属性中的关键字
		int n = propertyNames.length - 1;
		sbParam.append("\"aggs\":{");
		for (int i = 0; i < n; i++) {
			String property = propertyNames[i];
			sbParam.append("\"").append(property).append("\":{\"terms\":{\"field\":\"").append(property)
					.append("\",\"size\":").append("}}}}},");
		}
		String property = propertyNames[n];
		sbParam.append("\"").append(property).append("\":{\"significant_terms\":{\"field\":\"").append(property)
				.append("\",\"size\":").append(maxReturn).append("}}}}");
		StringBuffer sbUrl = getRandomUrl(featureTypeName).append("_search?search_type=count");
		try {
			String json = hh.doPost(sbUrl.toString(), sbParam.toString());
			JSONObject jo = new JSONObject(json);
			JSONObject joKws = jo.getJSONObject("aggregations");
			Map<String, List<KeyWord>> res = new HashMap<String, List<KeyWord>>(propertyNames.length);
			for (String p : propertyNames) {
				try {
					JSONObject joKw = joKws.getJSONObject(p);
					JSONArray jaBuckets = joKw.getJSONArray("buckets");
					int bkLen = jaBuckets.length();
					List<KeyWord> buckets = new ArrayList<KeyWord>(bkLen);
					for (int i = 0; i < bkLen; i++) {
						JSONObject joBucket = jaBuckets.getJSONObject(i);
						String key = joBucket.getString("key");
						double keyCount = joBucket.getDouble("score");
						KeyWord bucket = new KeyWord();
						bucket.setValue(key);
						bucket.setWeight(keyCount);
						buckets.add(bucket);
					}
					res.put(p, buckets);
				} catch (Exception e) {
				}
			}
			return res;
		} catch (Exception e) {
			throw new RuntimeException("analyzeBehaviorKeyWords异常:", e);
		}
	}

	/**
	 * 根据关键词搜索最匹配的要素
	 * 
	 * @param propertyNames
	 *            返回的属性
	 * @param keyWordsMap
	 *            关键词 <属性名, 关键词[]>
	 * @param maxReturn
	 *            最大返回条数
	 * @return
	 */
	public List<Feature> searchFeatureByKeyWord(String[] propertyNames, Map<String, List<KeyWord>> keyWordsMap,
			int maxReturn) {
		// 组装查询语句
		JSONObject joParam = new JSONObject();
		joParam.put("size", maxReturn);
		JSONArray jaFields = new JSONArray();
		for (String p : propertyNames) {
			jaFields.put(p);
		}
		joParam.put("fields", jaFields);
		JSONObject joQuery = new JSONObject();
		joParam.put("query", joQuery);
		JSONObject joBool = new JSONObject();
		joQuery.put("bool", joBool);
		JSONArray jaShould = new JSONArray();
		joBool.put("should", jaShould);
		Iterator<Entry<String, List<KeyWord>>> iterator = keyWordsMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, List<KeyWord>> entry = iterator.next();
			String propertyName = entry.getKey();
			List<KeyWord> keyWords = entry.getValue();
			for (KeyWord kw : keyWords) {
				JSONObject joShould = new JSONObject();
				jaShould.put(joShould);
				JSONObject joConstantScore = new JSONObject();
				joShould.put("constant_score", joConstantScore);
				JSONObject joFilter = new JSONObject();
				joConstantScore.put("filter", joFilter);
				JSONObject joTerm = new JSONObject();
				joFilter.put("term", joTerm);
				joTerm.put(propertyName, kw.getValue());
				if (kw.isNeedWeight()) {
					joConstantScore.put("boost", kw.getWeight());
				}
			}
		}
		String param = joParam.toString();
		// 查询并解析
		String url = getRandomUrl(featureTypeName).append("/_search").toString();
		try {
			String json = hh.doPost(url, param);
			JSONObject jo = new JSONObject(json);
			JSONArray jaHits = jo.getJSONObject("hits").getJSONArray("hits");
			int n = jaHits.length();
			List<Feature> res = new ArrayList<Feature>(n);
			for (int i = 0; i < n; i++) {
				JSONObject joFeature = jaHits.getJSONObject(i);
				Feature f = new Feature();
				f.setId(joFeature.getString("_id"));
				JSONObject joFields = joFeature.getJSONObject("fields");
				Property[] properties = new Property[joFields.length()];
				int j = 0;
				Iterator<String> keys = joFields.keys();
				while(keys.hasNext()){
					String key = keys.next();
					String value = joFields.get(key).toString();
					properties[j] = new Property(key, value);
					j++;
				}
				f.setProperties(properties);
				res.add(f);
			}
			return res;
		} catch (Exception e) {
			throw new RuntimeException("searchFeatureByKeyWord异常:", e);
		}

	}

	private StringBuffer getRandomUrl() {
		StringBuffer sb = new StringBuffer();
		sb.append(esUrls[random.nextInt(esUrls.length)]);
		sb.append(idxName);
		sb.append("/");
		return sb;
	}

	private StringBuffer getRandomUrl(String typeName) {
		StringBuffer sb = getRandomUrl();
		sb.append(typeName).append("/");
		return sb;
	}

}
