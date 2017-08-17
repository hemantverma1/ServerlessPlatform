public class LoadBalancer extends DoLogging{
	public static void main(String[] args){
		try{
			InitializeGlobalData init=new InitializeGlobalData();
			GlobalData gd=init.initGlobalData();
			init.disconnect();
						
			//Notifier Thread - if a notification is lost, 
			//this will make sure that threads dont sleep infinitely
			Notifier notifier=new Notifier(gd);
			new Thread(notifier,"notifier").start();
			
			//Load Balancer thread
			Balancer balancerObj=new Balancer(gd);
			new Thread(balancerObj,"loadBalancer").start();
			
			//Router Thread
			Router routerObj=new Router(gd);
			new Thread(routerObj,"router").start();	
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}	
}
