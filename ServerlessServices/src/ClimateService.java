import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClimateService extends coreServiceFramework{
	
    private static final String APP_ID = "0e06c160be2841b2274fedfe7a2fbdb1";
    private static final String CITY_ID = "1269843";     //Hyderabad - code
    private static final String URL = "http://api.openweathermap.org/data/2.5/weather?id="+CITY_ID+"&APPID="+APP_ID;
    private static final String currentWeather = "/var/serverless/currentWeather.txt";
    //private static final String currentWeather = "";
    //Need to make this file in fileSvc not here!!

    public static void main(String[] args) {
        ClimateService csv = new ClimateService();
        System.out.println("Temp [C]: "+ csv.getWeather(true));
        System.out.println("Temp [F]: "+ csv.getWeather(false));
        System.out.println("Humidity [%]: "+ csv.getHumidity());
        System.out.println("Speed [meter/sec]: " + csv.getWindSpeed());
    }

    /*
     * Temperature is available in Fahrenheit, Celsius and Kelvin units.
     *
     * - For temperature in Fahrenheit use units=imperial
     * - For temperature in Celsius use units=metric
     * - Temperature in Kelvin is used by default, no need to use units parameter in API call
     * http://api.openweathermap.org/data/2.5/weather?id=1269843&APPID=0e06c160be2841b2274fedfe7a2fbdb1
     * http://api.openweathermap.org/data/2.5/weather?id="+CITY_ID+"&APPID="+APP_ID
     */


    public double getWeather(boolean inCelsius){
        double temperature=0;
        String url = URL;
        if(inCelsius) url += "&units=metric";
        else url += "&units=imperial";
        doLogging("Request came for getWeather", "INFO");
        String response = fetchWeather(url);
        System.out.println(response);
        JSONParser parser = new JSONParser();
        try {
        	
			JSONObject jo=(JSONObject)parser.parse(response);
			// maintain cache of this JSONObject
			temperature = Double.parseDouble((((JSONObject)jo.get("main")).get("temp")).toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return temperature; 
    }

    public double getWeather(){
    	doLogging("Request came for getWeather", "INFO");
        return getWeather(true);
    }

    public long getHumidity(){
        long humidity=0;
        String response = fetchWeather(URL);
        JSONParser parser = new JSONParser();
        doLogging("Request came for getHumidity", "INFO");
        try {
			JSONObject jo=(JSONObject)parser.parse(response);
			//long speed = (long)((JSONObject)jo.get("wind")).get("speed");
			humidity = (long)((JSONObject)jo.get("main")).get("humidity");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			doLogging("Error in getHumidity "+e.getStackTrace(), "ERROR");
		}
        return humidity;
    }

    public double getWindSpeed(){
        double speed=0;
        String response = fetchWeather(URL);
        JSONParser parser = new JSONParser();
        try {
			JSONObject jo=(JSONObject)parser.parse(response);
			speed = (double)((JSONObject)jo.get("wind")).get("speed");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return speed;
    }
    
    private String readFile(){
    	BufferedReader br = null;
		FileReader fr = null;
		String file_content="";
		try {
			fr = new FileReader(currentWeather);
			br = new BufferedReader(fr);
			String line;
			br = new BufferedReader(new FileReader(currentWeather));
			while ((line = br.readLine()) != null) {
				file_content +=  line + "\n";
			}	
		} catch (IOException e) {
		} finally {
			try {
				if (br != null)
					br.close();
				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		if (!file_content.equals("")) return file_content.trim();
		return "";
    }
    
    private HashMap<String, String> parse(String file_content){
    	HashMap<String, String> hmap = new HashMap<String, String>();
    	if (file_content=="") {
			return hmap;
		}
    	
    	String [] parts = file_content.split("\n");
    	for (int i = 0; i < parts.length; i++) {
			String [] subparts = parts[i].split("\\|");
			hmap.put(subparts[0], subparts[1]+"|"+subparts[2]);
		}
    	return hmap;
    }
    
    private String getTimestamp() {
    	return ""+System.currentTimeMillis();
	}
    
    private String fetchWeather(String myURL) {
        String toret = null;
        String file_content = readFile();
        System.out.println("File contents: "+file_content);
        HashMap<String, String> hmap = parse(file_content);
        
        // check if for current URL already request has been made and result is still valid        
        if(hmap.containsKey(myURL) && Long.parseLong(hmap.get(myURL).split("\\|")[0]) + 7200000 >= Long.parseLong(getTimestamp())){
        	System.out.println("Sending from the file");
        	return hmap.get(myURL).split("\\|")[1];
        }
        
    	else {
    		System.out.println("Connecting the Web API");
	        try {
	            String uurl = myURL;
	            /*String proxy = "proxy.iiit.ac.in", port = "8080";
	            Properties systemProperties = System.getProperties();
	            systemProperties.setProperty("http.proxyHost",proxy);
	            systemProperties.setProperty("http.proxyPort",port);*/
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
	            
		        // update currentWeather file	        	
		        hmap.put(myURL, getTimestamp()+"|"+toret);
		        FileWriter f = new FileWriter(currentWeather);
		        //write on file
		        for (String key : hmap.keySet()) {
		           f.write(key+"|"+hmap.get(key)+"\n");
		        }
		        f.close();
	        } catch(Exception e) {
	            e.printStackTrace();
	        }

    	}
        return toret;
    }
}