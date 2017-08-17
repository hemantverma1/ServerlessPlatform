import java.util.HashMap;

public class ServiceStats {
	String serviceName="";
	HashMap<String, Integer> serviceList;   //mapping of machine and their running instances
	
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public HashMap<String, Integer> getServiceList() {
		return serviceList;
	}
	public void setServiceList(HashMap<String, Integer> serviceList) {
		this.serviceList = serviceList;
	}

}
