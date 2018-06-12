package other.isv.sample;

import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.param.ScriptTriggerParam;
import com.rkhd.platform.sdk.param.ScriptTriggerResult;

public class ScriptTriggerImpl implements com.rkhd.platform.sdk.ScriptTrigger{

	@Override
	public ScriptTriggerResult execute(ScriptTriggerParam arg0) throws ScriptBusinessException {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		return null;
	}

}
