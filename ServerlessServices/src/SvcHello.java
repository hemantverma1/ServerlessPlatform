import java.util.ArrayList;

public class SvcHello extends coreServiceFramework{
	public String func1(String a){
		ArrayList<functionParam> list = new ArrayList<functionParam>();
		list.add(encodeFunctionParams("serverlessplt@gmail.com"));
		list.add(encodeFunctionParams("serverless"));
		list.add(encodeFunctionParams("hemantverma1@gmail.com"));
		list.add(encodeFunctionParams("hello there"));
		list.add(encodeFunctionParams("Someone called Your service"));
		callService("SendMailSSL", "send", list);
		doLogging("HelloSvc Called successfully", "INFO");
		return "Hello "+a;
		
	}
}
