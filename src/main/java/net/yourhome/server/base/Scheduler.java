package net.yourhome.server.base;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class Scheduler {

	private it.sauronsoftware.cron4j.Scheduler cronScheduler;
	private static Scheduler instance;
	private static Object lock = new Object();
	private Timer nonRepeatingTimer;
	// private List<Timer> repeatingTimers;
	private static Logger log = Logger.getLogger(Scheduler.class);
	// private List<ISchedule> schedules;

	private Map<Integer, TimerTask> registeredTimerTasks = new HashMap<Integer, TimerTask>();

	private Scheduler() {
		// Creates a Scheduler instance.
		cronScheduler = new it.sauronsoftware.cron4j.Scheduler();
		// log.debug("CronScheduler started with timezone
		// "+cronScheduler.getTimeZone().toString());
		// Stops the scheduler.
		// s.stop();
		// repeatingTimers = new ArrayList<Timer>();
		nonRepeatingTimer = new Timer();
		cronScheduler.start();

		// schedules = new ArrayList<ISchedule>();
	}

	public static Scheduler getInstance() {

		Scheduler r = instance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = instance; // thread may have instantiated the object
				if (r == null) {
					r = new Scheduler();
					instance = r;
				}
			}
		}
		return instance;
	}

	public String scheduleCron(TimerTask task, String cronString) {
		String jobId = cronScheduler.schedule(cronString, task);
		log.debug("Cron schedule added for " + cronString + ", type: " + task.getClass());
		return jobId;
	}

	public void descheduleCron(String cronId) {
		this.cronScheduler.deschedule(cronId);
	}

	public boolean deschedule(String timerId) {
		TimerTask timerTask = this.registeredTimerTasks.get(timerId);
		if (timerTask != null) {
			return timerTask.cancel();
		}
		return false;
	}

	/*
	 * public void schedule(final TimedTrigger timedTrigger) {
	 * 
	 * TimerTask timerTask = new TimerTask() {
	 * 
	 * @Override public void run() { timedTrigger.trigger(); } };
	 * 
	 * try { if(timedTrigger.isRepeating()) { this.scheduleRepeating(timerTask,
	 * timedTrigger.getStartDate(), timedTrigger.getDelay()); }else {
	 * this.schedule(timerTask, timedTrigger.getStartDate(),
	 * timedTrigger.getDelay()); }
	 * registeredTimerTasks.put(timedTrigger.getId(), timerTask); } catch
	 * (Exception e) { } }
	 */

	public TimerTask schedule(TimerTask task, Date firstTime, int delay) throws Exception {
		this.nonRepeatingTimer.schedule(task, new Date(firstTime.getTime() + delay * 1000));
		return task;
	}

	public Timer scheduleRepeating(TimerTask task, Date firstTime, int delay) throws Exception {
		Date now = new Date();
		Timer t;
		long difference = firstTime.getTime() - now.getTime();
		if (difference >= 0) {
			t = new Timer();
			t.schedule(task, firstTime, delay);
			return t;
		} else {
			throw new Exception("[Error] Schedule date cannot be in the past (provided first time: " + firstTime.toString() + " | current time: " + now.toString());
		}
	}

}
