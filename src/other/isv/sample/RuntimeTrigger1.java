package other.isv.sample;

import java.util.*;

import com.rkhd.platform.sdk.data.model.RuntimeTest1__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
//该trigger用于创建实体时的字段查重
public class RuntimeTrigger1 implements Trigger{
	private Logger log = LoggerFactory.getLogger();
	private static final String QUERY_SQL_FROMAT = "select id from RuntimeTest1__c where name = '%s' and text1__c = '%s'";

	@Override
	public TriggerResponse execute(TriggerRequest arg0) throws ScriptBusinessException {
		List<XObject> xList = arg0.getDataList();
		TriggerResponse triggerResponse = new TriggerResponse();
		HashMap<Long, Integer> Map = new HashMap<>();
		for(XObject xObject : xList){
			try{
				RuntimeTest1__c runtimeTest = (RuntimeTest1__c) xObject;
				String name = runtimeTest.getName();
				String text1 = runtimeTest.getText1__c();
				Long totalNum = getNumInDB(name, text1);
				
				int text1HashCode = text1 == null ? 0 : text1.hashCode();
				//以name和text1两个属性做实体的联合查重
				Long hashCode = 31L * name.hashCode() + text1HashCode;
				Integer num = Map.get(hashCode);
				if(num == null){
					num = 0;
				}
				totalNum += num;
				//如果数据库或之前的数据中有重复的数据，则在name后添加后缀进行区分
				if(totalNum != 0){
					runtimeTest.setName(name + totalNum);
				}
				
				Map.put(hashCode, ++num);
				triggerResponse.addDataResult(new DataResult(true, "", xObject));
			}catch (Exception e) {
				// TODO: handle exception
				log.error(e.getMessage(), e);
				triggerResponse.addDataResult(new DataResult(false, e.getMessage(), xObject));
				
				//如果不支持部分成功，直接终止trigger
				if(!arg0.getPartialSuccess()){
					return triggerResponse;
				}
			}
		}
		triggerResponse.setSuccess(true);
		return triggerResponse;
		
	}
	
	private Long getNumInDB(String name, String text1) throws Exception{
		String sql = String.format(QUERY_SQL_FROMAT, name, text1);
		QueryResult result = XObjectService.instance().query(sql);
		if(!result.getSuccess()){
			throw new ScriptBusinessException(result.getErrorMessage());
		}
		log.info("DB num is " + result.getTotalCount());
		return result.getTotalCount();
	}

}
