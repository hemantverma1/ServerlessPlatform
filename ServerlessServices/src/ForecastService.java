import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

public class ForecastService {
	
	 private static final String APP_ID = "0e06c160be2841b2274fedfe7a2fbdb1";
	 private static final String URL ="http://api.openweathermap.org/data/2.5/forecast/daily?";
	
//	 http://api.openweathermap.org/data/2.5/forecast/daily?q=London&mode=json&units=metric&cnt=7&appid=0e06c160be2841b2274fedfe7a2fbdb1
	
	
	
	public static void main(String[] args) {
        ForecastService psv = new ForecastService();
        System.out.println("Forecast : "+ psv.getForecast("Hyderabad",6));
        
    }
	
	public String getForecast(String cityName, int nextNdays){
		String predict_temp="";
		String url = URL;
		url+="q="+cityName+"&mode=json&units=metric&cnt="+nextNdays+"&appid="+APP_ID;
		JSONArray tempJA=null;
		String response = fetchJsonData(url);
        JSONObject jo;
		try {
			jo = (JSONObject)JSONValue.parse(response);
			tempJA= (JSONArray)jo.get("list");
			for (int i = 0; i < tempJA.size(); i++) {
				
				Object day_temp=(Object)((JSONObject)(((JSONObject)tempJA.get(i))).get("temp")).get("day");
				long unixSeconds = (Long)((JSONObject)tempJA.get(i)).get("dt");
				Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
				sdf.setTimeZone(TimeZone.getTimeZone("GMT-4")); // give a timezone reference for formating (see comment at the bottom
				String formattedDate = sdf.format(date);
				
				
				predict_temp += formattedDate +"	"+ day_temp +"\n";
				
				//Date date =new Date((Long)((JSONObject)tempJA.get(i)).get("dt"));
				//System.out.println(date.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return predict_temp;
	}
	
	 private String fetchJsonData(String myURL) {

	        String toret = null;

	        try {
	            String uurl = myURL,
	                    proxy = "proxy.iiit.ac.in",
	                    port = "8080";
	            Properties systemProperties = System.getProperties();
	            systemProperties.setProperty("http.proxyHost",proxy);
	            systemProperties.setProperty("http.proxyPort",port);
	            URL url = new URL (uurl);
	            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("GET");
	            connection.setDoOutput(true);
	            InputStream content = (InputStream)connection.getInputStream();
	            BufferedReader in   = new BufferedReader (new InputStreamReader (content));
	            String line;
	            toret = in.readLine();
	            while ((line = in.readLine()) != null) {
	                toret += line;
	            }
	        } catch(Exception e) {
	            e.printStackTrace();
	        }
	        return toret;
	    }

}
