import java.util.*;

public class PollutionService extends coreServiceFramework{
	
	private String[] strs = { "Delhi","Bangalore","Kolkata","Mumbai","Chennai","Kanpur"
			,"Ghaziabad","Pune","Hyderabad","Goa",
			"Indore","Jabalpur","Jagdalpur","Agra",
			"Gurgaon"}; 

	public int getPollutionLevel(String cityName){
		Random rand = new Random();
		int  n = rand.nextInt(150) + 100;
		return n;
	}
	
	public int getPollutionLevel(String cityName, int numberofYearsAgo){
		Random rand = new Random();
		int  n = rand.nextInt(150) + 100;
		return n;
	}
	
	public String getTopPollutedCities(int topX){
		String result="";
		for (int i=0; i<topX; i++){
			if(i==0)
				result = result+strs[i];
			else
				result = result + "," + strs[i];
		}
		return result;
	}
	
	public Boolean alertForCity(String cityName, int pollutionLevelThreshold, 
			   				String toEmailId){
		Random rand = new Random();
		int  n = rand.nextInt(150) + 100;
		if(n > pollutionLevelThreshold){
			ArrayList<functionParam> list = new ArrayList<functionParam>();
			list.add(encodeFunctionParams("serverlessplt@gmail.com"));
			list.add(encodeFunctionParams("serverless"));
			list.add(encodeFunctionParams(toEmailId));
			list.add(encodeFunctionParams("Pollution Forecast"));
			list.add(encodeFunctionParams("Pollution level above threshold"));
			callService("SendMailSSL", "send", list);			
			return true;
		}
		return false;	
	}
	
	public static void main(String... argv){
		PollutionService obj = new PollutionService();
		String r = obj. getTopPollutedCities(8);
		System.out.println(r);
	}
	
}
