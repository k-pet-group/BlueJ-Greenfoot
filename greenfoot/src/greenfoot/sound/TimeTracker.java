package greenfoot.sound;

public class TimeTracker {
	private long startTime;
	private boolean tracking;
	private long timeElapsed;
	
	
	public void start() {
		if(tracking) {
			return;
		}
		startTime = System.currentTimeMillis();	
		tracking = true;

	}
	
	public void pause()  {
		if(!tracking) {
			return;
		}
		long timeSincestart = getTimeSinceStart();
		timeElapsed += timeSincestart;
		tracking = false;
	}

	public void reset() {
		startTime = 0;
		tracking = false;
		timeElapsed = 0;
	}
	
	private long getTimeSinceStart() {
		if(tracking) {
			return System.currentTimeMillis() - startTime;
		}
		else {
			return 0;
		}
	}	
	
	public long getTimeTracked() {
		return timeElapsed + getTimeSinceStart();
	}
}
