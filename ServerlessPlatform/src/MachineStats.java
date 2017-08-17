import java.util.HashMap;
public class MachineStats {
	private String machine;
	private double ram;
	private double cpu;
	private Integer process;
	private HashMap<String, Integer> instances;
	public String getMachine() {
		return machine;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	public double getRam() {
		return ram;
	}
	public void setRam(double ram) {
		this.ram = ram;
	}
	public double getCpu() {
		return cpu;
	}
	public void setCpu(double cpu) {
		this.cpu = cpu;
	}
	
	public Integer getProcess() {
		return process;
	}
	public void setProcess(Integer process) {
		this.process = process;
	}
	public HashMap<String, Integer> getInstances() {
		return instances;
	}
	public void setInstances(HashMap<String, Integer> instances) {
		this.instances = instances;
	}
}
