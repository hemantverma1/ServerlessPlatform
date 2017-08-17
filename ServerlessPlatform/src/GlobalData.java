import java.util.HashMap;

public class GlobalData {
	HashMap<String, MachineStats> machineStatsHM;
	HashMap<String, Integer> totalSvcInstance;
	HashMap<String, ServiceStats> serviceStatHM;
	
	public HashMap<String, Integer> getTotalSvcInstance() {
		return totalSvcInstance;
	}

	public HashMap<String, ServiceStats> getServiceStatHM() {
		return serviceStatHM;
	}

	public void setServiceStatHM(HashMap<String, ServiceStats> serviceStatHM) {
		this.serviceStatHM = serviceStatHM;
	}

	public void setTotalSvcInstance(HashMap<String, Integer> totalSvcInstance) {
		this.totalSvcInstance = totalSvcInstance;
	}

	public HashMap<String, MachineStats> getMachineStatsHM() {
		return machineStatsHM;
	}

	public void setMachineStatsHM(HashMap<String, MachineStats> machineStatsHM) {
		this.machineStatsHM = machineStatsHM;
	}

	public GlobalData(HashMap<String, MachineStats> machineStatsHM, HashMap<String, ServiceStats> serviceStatHM, HashMap<String, Integer> totalSvcInstance) {
		this.machineStatsHM=machineStatsHM;
		this.serviceStatHM=serviceStatHM;
		this.totalSvcInstance=totalSvcInstance;
	}
	
}
