public class VmManager {
	final static String slaveMachineUser = "admin";
	final static String slaveMachinePasswd = "admin";
	final static String nfsDir = "/var/serverless/";
	
	public static StringPair startNewVM(String hostIp, String nfsIp){
		StringPair result = null;
		SSHStartAgent obj = new SSHStartAgent();
		try{
			String cmd = "echo "+slaveMachinePasswd
					+" > /tmp/.passwd && echo >>  /tmp/.passwd "
					+ "&& sudo -S mount "+nfsIp+":"+nfsDir+" "+nfsDir
					+" < /tmp/.passwd";
			System.out.println(cmd);
			obj.executeFile(slaveMachineUser, slaveMachinePasswd, hostIp, cmd, true);
			cmd = "bash /var/serverless/startVM.sh";
			result = obj.executeCmd(slaveMachineUser, slaveMachinePasswd, hostIp, cmd);
			if(result!=null && result.hostName!=null && !result.hostName.equals("null")){
				System.out.println(result.hostName.toString());
				System.out.println(result.ip.toString());				
			}
			return result; 
		}
		catch(Exception e){
			e.printStackTrace();
			return result;
		}	
	}
	
	/*public static void main(String[] args){
		String hostIp = "192.168.1.114";
		String nfsIp = "192.168.1.101";
		startNewVM(hostIp, nfsIp);
	}*/
}
