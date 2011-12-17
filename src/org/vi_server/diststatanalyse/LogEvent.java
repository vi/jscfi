package org.vi_server.diststatanalyse;

/** Event received from logs */
public abstract class LogEvent {
	double time; /* UNIX time */
	String host;
	int pid;
	/* other data in subclasses */
	
	
	public LogEvent(double time, String host, int pid) {
		super();
		this.time = time;
		this.host = host;
		this.pid = pid;
	}	
	
	public abstract void beObserved(InterpreterObserver o);
	
	public String hostPid() {
		return host + ":" + Integer.toString(pid);
	}
}
