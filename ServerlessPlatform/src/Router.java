public class Router extends DoLogging implements Runnable{
	GlobalData gd;
	public Router(GlobalData gd) {
		this.gd=gd;
	}
	@Override
	public void run() {
		int numberOfThreads = 5;
		try {
			ThreadPool pool = new ThreadPool(numberOfThreads);
			for(int i=0; i<numberOfThreads; i++)
				pool.execute(new RouterWorker());
				/*synchronized (gd) {
					gd.wait();
					System.out.println(gd.toString());
					System.out.println("------------------------------------------------------");
					gd.notify();
				}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
