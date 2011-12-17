package org.vi_server.diststatanalyse;

public class LogEventUnknown extends LogEvent {
	public String name;
	public String args;
	
	public LogEventUnknown(double time, String hostname, int pid, String name, String args) {
		super(time, hostname, pid);
		this.name = name;
		this.args = args;
	}

	@Override
	public void beObserved(InterpreterObserver o) {
		o.newLogEvent(this);
	}

}
