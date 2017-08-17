import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class AuthorizationService {
	private final static String AUTHORIZED_USER_DETAILS = "authorization_details.xml";
	final static String nfsDir = "/var/serverless/";

	@SuppressWarnings("unchecked")
	public void saveTokenDetails(String token, String uname, String timeStamp)
	{
		try{
			JSONParser parser = new JSONParser();
			JSONObject saveJson = new JSONObject();
			saveJson.put("uname", uname);
			saveJson.put("timestamp", timeStamp);
			File f = new File(nfsDir+"authorizationTokens.json");
			JSONObject authorizationTokens = new JSONObject();
			if(f.exists() && !f.isDirectory()){
				FileReader r = new FileReader(f);
				authorizationTokens = (JSONObject)parser.parse(r);
				r.close();
			}
			authorizationTokens.put(token, saveJson);
			Writer fileHandle = new BufferedWriter(new FileWriter(f));
			fileHandle.write(authorizationTokens.toString());
			fileHandle.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean authorizeUser(String service, String token) //get uname against token and refresh timeStamp
	{
		try {
			JSONParser parser = new JSONParser();
			File f = new File(nfsDir+"authorizationTokens.json");
			JSONObject tokenDetailsMap = new JSONObject();
			if(f.exists() && !f.isDirectory()){
				FileReader r = new FileReader(f);
				tokenDetailsMap = (JSONObject)parser.parse(r);
				r.close();
			}
			String uname = null;
			String timeStamp = null;
			if(tokenDetailsMap.containsKey(token)){
				JSONObject saveJson = (JSONObject)tokenDetailsMap.get(token);
				uname = saveJson.get("uname").toString();
				timeStamp = saveJson.get("timestamp").toString();
				System.out.println(uname);
				String curTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				Timestamp user_ts = Timestamp.valueOf(timeStamp);
				Timestamp cur_ts = Timestamp.valueOf(curTimeStamp);
				long diff = cur_ts.getTime() - user_ts.getTime();

				if (diff > 120000){
					tokenDetailsMap.remove(token);	
					Writer fileHandle = new BufferedWriter(new FileWriter(f));
					fileHandle.write(tokenDetailsMap.toString());
					fileHandle.close();
					System.out.println("expired");
					return false;
				}
				else{ 
					String t = cur_ts.toString();
					saveJson.put("timestamp", t);
					tokenDetailsMap.put(token, saveJson);
					Writer fileHandle = new BufferedWriter(new FileWriter(f));
					fileHandle.write(tokenDetailsMap.toString());
					fileHandle.close();
				}
				
				File fXmlFile = new File(nfsDir+AUTHORIZED_USER_DETAILS);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(fXmlFile);
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("user");
				boolean authResult = false;
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					Element eElement = (Element) nNode;
					if(eElement.getAttribute("username").equals(uname)){
						try{
							String result = eElement.getElementsByTagName(service).item(0).getTextContent();
							if(result.equals("true"))
								authResult = true;
							break;
						}catch (Exception e) {
							System.out.println("Unauthorized");
						}
					}
				}
				System.out.println(authResult);
				return authResult;
			}
			else
				return false;
		} catch (Exception e) {
			return false;
		}
	}
}
