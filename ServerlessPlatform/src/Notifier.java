public class Notifier implements Runnable{
	GlobalData gd;
	public Notifier(GlobalData gd) {
		// TODO Auto-generated constructor stub
		this.gd=gd;
	}
	@Override
	public void run() {
		try {
			while(true) {
				synchronized (gd) {
					gd.notifyAll();
					//java.lang.Thread.sleep(100); //100 milliseconds sleep
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
