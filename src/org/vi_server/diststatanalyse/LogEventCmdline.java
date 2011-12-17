package org.vi_server.diststatanalyse;

public class LogEventCmdline extends LogEvent {
	public String args;
	
	public LogEventCmdline(double time, String hostname, int pid, String args) {
		super(time, hostname, pid);
		this.args = args;
	}
	
	public String getCommandLine() {
		return args
				.replace("\\n", "\n")
				.replace("\\t", "\t")
				.replace("\\0", "\0")
				.replace("\\ ", " ")
				.replace("\\\\", "\\");
	}

	@Override
	public void beObserved(InterpreterObserver o) {
		o.newLogEvent(this);
	}

}
