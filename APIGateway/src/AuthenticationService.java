import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.*;
import java.io.*;

import java.text.*;
import java.security.*;
import java.math.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



public class AuthenticationService {
	static String ldapadminuname = null;
	static String ldapadminpassword = null;
	static String url = null;
	final static String nfsDir = "/var/serverless/";

	public static void loadAdmindetails()
	{
		try 
		{
			String filename = "ldapAdminDetails.cfg";
			BufferedReader read = new BufferedReader(new FileReader(nfsDir+filename));
			String line = null;
			line = read.readLine();	
			StringTokenizer tokens = new StringTokenizer(line);
			String ip = tokens.nextToken();
			ldapadminuname = tokens.nextToken();
			ldapadminpassword = tokens.nextToken();
			url = "ldap://" + ip;
			read.close();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked"})
	public JSONObject authenticateUser(String username, String userPassword) throws Exception{
		try
		{
			loadAdmindetails();
			System.out.println(url);
			System.out.println("Details Loaded");
			System.out.println(username+" "+userPassword);
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "cn="+ldapadminuname+",dc=serverless,dc=com");//adminuser - User with special priviledge, dn user
			props.put(Context.SECURITY_CREDENTIALS, ldapadminpassword);//dn user password
			InitialDirContext context = new InitialDirContext(props);
			System.out.println("connected");
			SearchControls ctrls = new SearchControls();
			try {
	            NamingEnumeration<SearchResult> answers = context.search("ou=ias,dc=serverless,dc=com", "(uid=" + username + ")", ctrls);
	            javax.naming.directory.SearchResult result = answers.nextElement();

	            String user = result.getNameInNamespace();
	            props = new Properties();
	            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	            props.put(Context.PROVIDER_URL, url);
	            props.put(Context.SECURITY_PRINCIPAL, user);
	            props.put(Context.SECURITY_CREDENTIALS, userPassword);

	            context = new InitialDirContext(props);
	            
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	JSONObject obj = new JSONObject();
				obj.put("authSuccessful","false");
		        obj.put("token","null");
				return obj;
	        }
	        
		} catch (Exception e) {	
			e.printStackTrace();
			JSONObject obj = new JSONObject();
			obj.put("authSuccessful","false");
	        obj.put("token","null");
			return obj;
		}
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		String s = timeStamp+username; 
		MessageDigest m=MessageDigest.getInstance("MD5");
	    m.update(s.getBytes(),0,s.length());
	    String token = new BigInteger(1,m.digest()).toString(64);
	    JSONObject obj = new JSONObject();
        obj.put("authSuccessful","true");
        obj.put("token",token);
        return obj;
	}
	public static boolean authenticateJndi(String adminuname, String adminpassword) throws Exception{
		try
		{
			loadAdmindetails();
			System.out.println("Details Loaded");
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "cn="+adminuname+",dc=serverless,dc=com");//adminuser - User with special priviledge, dn user
			props.put(Context.SECURITY_CREDENTIALS, adminpassword);//dn user password
			InitialDirContext context = new InitialDirContext(props);
			System.out.println("connected");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static boolean addUser(String newuser, String newpassword) {  

		try{
			loadAdmindetails();
			System.out.println("Details Loaded");
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "cn="+ldapadminuname+",dc=serverless,dc=com");//adminuser - User with special priviledge, dn user
			props.put(Context.SECURITY_CREDENTIALS, ldapadminpassword);//dn user password
			InitialDirContext context = new InitialDirContext(props);
			System.out.println("connected");
			String entryDN = "uid="+newuser+",ou=ias,dc=serverless,dc=com";  
			// entry's attributes  
			String Directory = "/home/"+newuser;
			String login_Shell = "/bin/bash";
			Attribute cn = new BasicAttribute("cn", newuser);  
			Attribute uid = new BasicAttribute("uid",newuser);  
			Attribute uidNumber = new BasicAttribute("uidNumber", "16589");  
			Attribute gidNumber = new BasicAttribute("gidNumber", "100");
			Attribute homeDirectory = new BasicAttribute("homeDirectory", Directory);
			Attribute loginShell = new BasicAttribute("loginShell", login_Shell);
			Attribute gecos = new BasicAttribute("gecos", newuser);
			Attribute userPassword = new BasicAttribute("userPassword", newpassword);
			Attribute oc = new BasicAttribute("objectClass");  
			oc.add("top");  
			oc.add("account");  
			oc.add("posixAccount");  
			oc.add("shadowAccount");  
			DirContext ctx = null;    
			ctx = new InitialDirContext(props);  
			BasicAttributes entry = new BasicAttributes();  
			entry.put(cn);  
			entry.put(uid);  
			entry.put(uidNumber);  
			entry.put(gidNumber); 
			entry.put(homeDirectory);  
			entry.put(loginShell); 
			entry.put(gecos);  
			entry.put(userPassword); 
			entry.put(oc);  
			ctx.createSubcontext(entryDN, entry);   
		} catch (Exception e) {
			return false;
		}  
		return true;


	}
	public static ArrayList<String> userList()
	{
			loadAdmindetails();
			System.out.println("Details Loaded");

			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "cn="+ldapadminuname+",dc=serverless,dc=com");//adminuser - User with special priviledge, dn user
			props.put(Context.SECURITY_CREDENTIALS, ldapadminpassword);//dn user password

			ArrayList<String> resp = new ArrayList<String>();
			try {
				LdapContext ctx = new InitialLdapContext(props, null);
				ctx.setRequestControls(null);
				NamingEnumeration<?> namingEnum = ctx.search("ou=ias,dc=serverless,dc=com", "(objectclass=posixAccount)", AuthenticationService.getSimpleSearchControls());
				while (namingEnum.hasMore ()) {
					SearchResult result = (SearchResult) namingEnum.next ();    
					Attributes attrs = result.getAttributes ();
					resp.add(attrs.get("cn").toString().substring(4));
				} 
				namingEnum.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return resp;
		}
	private static SearchControls getSimpleSearchControls() {
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchControls.setTimeLimit(30000);
		return searchControls;
	}
	public static void deleteUser(String userID) {

		try {
			loadAdmindetails();
			System.out.println("Details Loaded");
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "cn="+ldapadminuname+",dc=serverless,dc=com");//adminuser - User with special priviledge, dn user
			props.put(Context.SECURITY_CREDENTIALS, ldapadminpassword);//dn user password
			InitialDirContext context = new InitialDirContext(props);
		context.destroySubcontext("uid="+userID+",ou=ias,dc=serverless,dc=com");
		context.close();

		} catch (Exception e) {	e.printStackTrace();	}
	}
}