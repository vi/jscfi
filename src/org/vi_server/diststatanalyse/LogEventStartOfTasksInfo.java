package org.vi_server.diststatanalyse;

public class LogEventStartOfTasksInfo extends LogEvent {
	public LogEventStartOfTasksInfo(double time, String hostname, int pid) {
		super(time, hostname, pid);
	}

	@Override
	public void beObserved(InterpreterObserver o) {
		o.newLogEvent(this);
	}

}
