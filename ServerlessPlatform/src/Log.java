import java.util.zip.*;
import org.json.simple.JSONObject;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Log {
	static int MAX_SIZE = 10000;
	static final int BUFFER = 2048;
	static final String name="LogFile_";
	static final String dirPath = "/var/serverless/Log/";
		
	private String parse(String severity , String uid,String reqid,String processname,String serviceid,String processid,String invokermethod,String log){
		String result="";
		result += new Date() + " ";
		result += severity + " " + uid + " " + reqid + " "+ processname + " "+ serviceid + " "+ processid + " "+ invokermethod + " "+ log;
		return result+"\n";
	}

	/*private void zipFile (String filename) {
	    try {
	        String zipfile =  filename;
	        zipfile = zipfile.replace(".log",".zip");
	        BufferedInputStream origin = null;
	        FileOutputStream dest = new FileOutputStream(zipfile);
	        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
	        byte data[] = new byte[BUFFER];
			// get a list of files from current directory
        	//System.out.println("Zipping : "+zipfile);
            FileInputStream fi = new FileInputStream(filename);
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(zipfile);
            out.putNextEntry(entry);
            int count;
            while((count = origin.read(data, 0, BUFFER)) != -1) {
               out.write(data, 0, count);
            }
            origin.close();
            out.closeEntry();
	        out.close();
	        File f = new File(filename);
	        f.delete();
	    }catch(Exception e) {
	        e.printStackTrace();
	   	}
   	}*/

	private boolean getFileSize(File f) {
		if(f.length() > MAX_SIZE)
			return true;
		return false;
	}
	
	private File getLatestFilefromDir(String path){
	    File dir = new File(path);
	    File[] files = dir.listFiles(new FilenameFilter() { 
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".log"); }
    	} );
	    
	    if (files == null || files.length == 0) {
	        return null;
	    }

	    File lastModifiedFile = files[0];
	    for (int i = 1; i < files.length; i++) {
	       if ( lastModifiedFile.lastModified() < files[i].lastModified()) {
	           lastModifiedFile = files[i];
	       }
	    }
	    return lastModifiedFile;		
	}

	public void writeLog(String severity,String uid,String reqid,
						String processname,String serviceid,
						String processid,String invokermethod,String log) throws IOException{
		
		String path = dirPath + processname + "/";
		File file = null;
		//if(path directory exists else create it)
		File dir = new File(path);
		if (! dir.exists()){
			dir.mkdirs();
			//System.out.println("dir create");
	    }

		file=getLatestFilefromDir(path);
		System.out.println("got file as "+file);
		if(file == null || getFileSize(file)){
			file = new File(path+name+new Date()+".log");
			file.createNewFile();
			file.setReadable(true, false);
		}
		try{
		    FileWriter fw = new FileWriter(file,true); //the true will append the new data
		    fw.write(parse(severity , uid,reqid,processname,serviceid,processid,invokermethod,log));//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe){
		    System.err.println("IOException: " + ioe.getMessage());
		}
	}
	

	/*void removeFileFromVarServerLessLog() {
		try {
			FileUtils.cleanDirectory(new File(dirPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	public static void main(String []args){
		File dir = new File(dirPath);
		if (! dir.exists()){
			dir.mkdirs();
	    }
		Log l=new Log();
		/*Initialy remove all file from /var/serverless/log/
		l.removeFileFromVarServerLessLog(); */	
		communicateWithMQ mqHandler = new communicateWithMQ();
		mqHandler.connectToBroker();
		JSONObject msg = null;
		while(true){
			msg = mqHandler.listenAndConsume("svcQueueLogging");
			System.out.println(msg);
			String severity = msg.get("severity").toString();
			String uid = "system";
			if(msg.containsKey("usrId"))
				uid = msg.get("usrId").toString();
			String reqid = "systemReq";
			if(msg.containsKey("reqId"))
				reqid = msg.get("reqId").toString();
			String processname = msg.get("process_type").toString();
			String processid = msg.get("processid").toString();
			String serviceid = msg.get("serverid").toString();
			String invokermethod = msg.get("method").toString();
			String log = msg.get("log").toString();
			try {
				l.writeLog(severity, uid, reqid, processname, serviceid, processid, invokermethod, log);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}