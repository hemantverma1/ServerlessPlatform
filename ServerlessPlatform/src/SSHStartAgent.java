
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


public class SSHStartAgent{

	// private static String USERNAME ="dhawnit"; // username for remote host
	// private static String PASSWORD ="dhawkhur"; // password of the remote host
	// private static String host = "10.2.128.187"; // remote host address
	private static int port=22;

	/**
	 * This method will execute the script file on the server.
	 * This takes file name to be executed as an argument
	 * The result will be returned in the form of the list
	 * @param scriptFileName
	 * @return
	 */
	public void executeFile(String USERNAME,String PASSWORD,String host,String command, boolean create_or_kill)
	{
		//	 String command = "sleep 100";
		//	 System.out.println("yahan aaya "+command);
		//     List<String> result = new ArrayList<String>();
		try
		{

			/**
			 * Create a new Jsch object
			 * This object will execute shell commands or scripts on server
			 */
			JSch jsch = new JSch();

			/*
			 * Open a new session, with your username, host and port
			 * Set the password and call connect.
			 * session.connect() opens a new connection to remote SSH server.
			 * Once the connection is established, you can initiate a new channel.
			 * this channel is needed to connect to remotely execution program
			 */
			Session session = jsch.getSession(USERNAME, host, port);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(PASSWORD);
			session.connect();

			//create the excution channel over the session
			ChannelExec channelExec = (ChannelExec)session.openChannel("exec");

			// Gets an InputStream for this channel. All data arriving in as messages from the remote side can be read from this stream.
			InputStream in = channelExec.getInputStream();

			// Set the command that you want to execute
			// In our case its the remote shell script
			if (create_or_kill)
			{
				channelExec.setCommand(command);
			}
			else
			{
				//        	 System.out.println("killink: pkill -f "+command);
				command  = "pkill -f \""+command+"\"";
				System.out.println(command);
				channelExec.setCommand(command);
			}


			// Execute the command
			channelExec.connect();

			// Read the output from the input stream we set above
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			System.out.println(line);
			reader.close();

			//Read each line from the buffered reader and add it to result list
			// You can also simple print the result here 
			//         line = reader.readLine();
			//         System.out.println(line);

			//retrieve the exit status of the remote command corresponding to this channel
			int exitStatus = channelExec.getExitStatus();

			//Safely disconnect channel and disconnect session. If not done then it may cause resource leak
			channelExec.disconnect();
			session.disconnect();

			if(exitStatus < 0){
				System.out.println("Done, but exit status not set!");
			}
			else if(exitStatus > 0){
				System.out.println("Done, but with error!");
			}
			else{
				System.out.println("Done!");
			}

		}
		catch(Exception e)
		{
			System.err.println("Error: " + e);
		}
		//     return result;
	}

	public StringPair executeCmd(String USERNAME,String PASSWORD,String host,String command)
	{
		StringPair result = new StringPair();
		try
		{
			JSch jsch = new JSch();
			Session session = jsch.getSession(USERNAME, host, port);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(PASSWORD);
			session.connect();
			ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
			InputStream in = channelExec.getInputStream();
			channelExec.setCommand(command);
			channelExec.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			//while((line = reader.readLine()) != null)
			result.hostName = reader.readLine();
			result.ip=reader.readLine();
			reader.close();
			int exitStatus = channelExec.getExitStatus();
			channelExec.disconnect();
			session.disconnect();
			if(exitStatus < 0){
				System.out.println("Done, but exit status not set!");
			}
			else if(exitStatus > 0){
				System.out.println("Done, but with error!");
			}
			else{
				System.out.println("Done!");
			}
			return result;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e);
			return result;
		}
	}
}