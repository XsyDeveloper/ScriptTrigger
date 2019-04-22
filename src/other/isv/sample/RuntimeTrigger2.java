package other.isv.sample;

import java.util.*;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.RuntimeTest1__c;
import com.rkhd.platform.sdk.data.model.RuntimeTest2__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
//该trigger包含的场景较多，如根据id获取name并填充到其他字段，获取单选字段的label，将整条数据映射到另一个实体等。
public class RuntimeTrigger2 implements Trigger{
	private static final Logger log = LoggerFactory.getLogger();
	
	@Override
	public TriggerResponse execute(TriggerRequest arg0) throws ScriptBusinessException {
		// TODO Auto-generated method stub
		List<XObject> xList = arg0.getDataList();
		TriggerResponse triggerResponse = new TriggerResponse();
		Map<String, JSONArray> describe = null;
		if(xList != null && xList.size() > 0){
			describe = getDescribe(xList.get(0).getApiKey());
		}
		List<RuntimeTest1__c> test1List = new ArrayList<RuntimeTest1__c>();
		for(XObject xObject : xList){
			try{
				RuntimeTest2__c test2 = (RuntimeTest2__c) xObject;
				
				//获取ownerId，调用接口获取到用户名，作为文本字段的值
				Long ownerId = test2.getOwnerId();
				String name = getUserNameById(ownerId);
				test2.setText1__c(name);
				
				//获取到所有关联实体的id， 并调用接口获取到关联实体的主属性，作为文本字段的值
				Long accountId1 = test2.getRele1__c();
				String accountName1 = getAccountNameById(accountId1);
				test2.setText2__c(accountName1);
				
				Long accountId2 = test2.getRele2__c();
				String accountName2 = getAccountNameById(accountId2);
				test2.setText3__c(accountName2);
				
				//获取到所有单选字段的value，调用描述接口后做value/label的映射，作为文本字段的值
				//该测试实体一共有5个单选字段，分别为select1__c,select2__c,select3__c,select4__c,select5__c;
				for(int i = 1; i <= 5; ++i){
					String apikey = "select" + i + "__c";
					String textApiKey = "text" + (3+i) + "__c";
					Integer value = Integer.parseInt(xObject.getAttribute(apikey).toString());
					JSONArray itemDes = describe.get(apikey);
					for(Object object : itemDes){
						JSONObject json = (JSONObject) object;
						if(value == json.getInteger("value")){
							xObject.setAttribute(textApiKey, json.getString("label"));
							break;
						}
					}
				}
				
				//将该数据映射到实体RuntimeTest1__c
				test1List.add(convert(test2));

				triggerResponse.addDataResult(new DataResult(true, null, xObject));
			}catch (Exception e) {
				// TODO: handle exception
				log.error(e.getMessage(), e);
				triggerResponse.addDataResult(new DataResult(false, e.getMessage(), xObject));
				if(!arg0.getPartialSuccess()){
					triggerResponse.setSuccess(false);
					triggerResponse.setMsg(e.getMessage());
					return triggerResponse;
				}
			}
			
		}
		
		try{
			BatchOperateResult result = XObjectService.instance().insert(test1List);
			//这里只关心总体的成功失败，如果具体关心搞每一条数据是否插入成功，应该校验result里每一条OperateResult
			if(!result.getSuccess()){
				return new TriggerResponse(false, result.getErrorMessage(), null);
			}
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ScriptBusinessException(e);
		}
		
		triggerResponse.setSuccess(true);
		return triggerResponse;
	}
	
	private Map<String, JSONArray> getDescribe(String apiKey) throws ScriptBusinessException{
		try{
			RkhdHttpData data = RkhdHttpData.newBuilder()
								.callType("GET")
								.callString("/rest/data/v2/objects/" + apiKey + "/description")
								.build();
			JSONObject result = RkhdHttpClient.instance().execute(data, ResponseBodyHandlers.ofJSON());
			if(200 != result.getInteger("code")){
				throw new ScriptBusinessException(result.getString("msg"));
			}
			Map<String, JSONArray> map = new HashMap<>();
			for(Object obj : result.getJSONObject("result").getJSONArray("fields")){
				JSONObject jsonObject = (JSONObject) obj;
				//将单选类型的描述保存起来
				if("picklist".equals(jsonObject.getString("type"))){
					map.put(jsonObject.getString("apiKey"), jsonObject.getJSONArray("selectitem"));
				}
			}
			return map;
		}catch (Exception e) {
			// TODO: handle exception
			log.error(e.getMessage(), e);
			throw new ScriptBusinessException(e);
		}
	}
	
	private String getUserNameById(Long id) throws Exception{
		RkhdHttpClient rkhdHttpClient = RkhdHttpClient.instance();
		RkhdHttpData data = RkhdHttpData.newBuilder()
							.callType("GET")
							.callString("/data/v1/objects/user/info?id=" + id)
							.build();
		JSONObject jsonObject = rkhdHttpClient.execute(data, ResponseBodyHandlers.ofJSON());
		return jsonObject.getString("name");
	}
	
	private String getAccountNameById(Long id) throws Exception{
		RkhdHttpClient rkhdHttpClient = RkhdHttpClient.instance();
		RkhdHttpData data = RkhdHttpData.newBuilder()
							.callType("GET")
							.callString("/data/v1/objects/account/info?id=" + id)
							.build();
		JSONObject jsonObject = rkhdHttpClient.execute(data, ResponseBodyHandlers.ofJSON());
		return jsonObject.getString("accountName");
	}
	
	private RuntimeTest1__c convert(RuntimeTest2__c test2){
		RuntimeTest1__c test1 = new RuntimeTest1__c();
		test1.setName(test2.getName());
		test1.setEntityType(test2.getEntityType());
		
		test1.setText1__c(test2.getText1__c());
		test1.setText2__c(test2.getText2__c());
		test1.setText3__c(test2.getText3__c());
		test1.setText4__c(test2.getText4__c());
		test1.setText5__c(test2.getText5__c());
		test1.setText6__c(test2.getText6__c());
		test1.setText7__c(test2.getText7__c());
		test1.setText8__c(test2.getText8__c());
		test1.setText9__c(test2.getText9__c());
		test1.setText10__c(test2.getText10__c());
		
		test1.setRele1__c(test2.getRele1__c());
		test1.setRele2__c(test2.getRele2__c());
		
		test1.setCalculate1__c(test2.getCalculate1__c());
		test1.setCalculate2__c(test2.getCalculate2__c());
		test1.setCalculate3__c(test2.getCalculate3__c());
		test1.setCalculate4__c(test2.getCalculate4__c());
		test1.setCalculate5__c(test2.getCalculate5__c());
		test1.setCalculate6__c(test2.getCalculate6__c());
		test1.setCalculate7__c(test2.getCalculate7__c());
		test1.setCalculate8__c(test2.getCalculate8__c());
		
		test1.setSelect1__c(test2.getSelect1__c());
		test1.setSelect2__c(test2.getSelect2__c());
		test1.setSelect3__c(test2.getSelect3__c());
		test1.setSelect4__c(test2.getSelect4__c());
		test1.setSelect5__c(test2.getSelect5__c());
		
		test1.setInteger1__c(test2.getInteger1__c());
		test1.setInteger2__c(test2.getInteger2__c());
		test1.setInteger3__c(test2.getInteger3__c());
		test1.setInteger4__c(test2.getInteger4__c());
		test1.setInteger5__c(test2.getInteger5__c());
		
		return test1;

	}

}
