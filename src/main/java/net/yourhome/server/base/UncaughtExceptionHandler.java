package net.yourhome.server.base;

import org.apache.log4j.Logger;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static Logger log = Logger.getLogger(UncaughtExceptionHandler.class);

	@Override
	public void uncaughtException(Thread t, Throwable ex) {
		log.error("Uncaught exception in thread: " + t.getName(), ex);
	}

}
