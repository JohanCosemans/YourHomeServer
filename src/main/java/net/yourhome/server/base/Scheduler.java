/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.yourhome.server.base;

import org.apache.log4j.Logger;

import java.util.*;

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
		this.cronScheduler = new it.sauronsoftware.cron4j.Scheduler();
		// log.debug("CronScheduler started with timezone
		// "+cronScheduler.getTimeZone().toString());
		// Stops the scheduler.
		// s.stop();
		// repeatingTimers = new ArrayList<Timer>();
		this.nonRepeatingTimer = new Timer();
		this.cronScheduler.start();

		// schedules = new ArrayList<ISchedule>();
	}

	public static Scheduler getInstance() {

		Scheduler r = Scheduler.instance;
		if (r == null) {
			synchronized (Scheduler.lock) { // while we were waiting for the
											// lock, another
				r = Scheduler.instance; // thread may have instantiated the
										// object
				if (r == null) {
					r = new Scheduler();
					Scheduler.instance = r;
				}
			}
		}
		return Scheduler.instance;
	}

	public String scheduleCron(TimerTask task, String cronString) {
		String jobId = this.cronScheduler.schedule(cronString, task);
		Scheduler.log.debug("Cron schedule added for " + cronString + ", type: " + task.getClass());
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
