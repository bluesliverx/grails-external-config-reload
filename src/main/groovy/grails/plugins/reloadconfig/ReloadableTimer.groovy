package grails.plugins.reloadconfig

// From http://stackoverflow.com/questions/32001/resettable-java-timer/32057#32057
/**
 * Always runs as a daemon, and cancels the underlying timer task instead of this timer itself, so it
 * never needs to be recreated.
 */
class ReloadableTimer extends Timer {
	Closure runnable
	private TimerTask timerTask;
	
	public ReloadableTimer() {
		super(true)
	}

	public boolean reschedule(long interval) {
		def stopped = timerTask?.cancel() ?: false
		timerTask = [run:runnable] as TimerTask
		scheduleAtFixedRate(timerTask, interval, interval)
		return stopped
	}
	
	public boolean cancelSchedule() {
		return timerTask?.cancel() ?: false
	}
}