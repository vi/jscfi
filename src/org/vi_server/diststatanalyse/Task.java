package org.vi_server.diststatanalyse;

public class Task {
	public LogEventStat lastStatEvent;
	public LogEventStat penultimateStatEvent;
	public LogEventIO penultimateIOEvent;
	public LogEventIO lastIOEvent;
	
	public String cmdline;
	public String host;
	public int pid;
	int lastrefreshevent;
	
	/** Returns CPU usage (both kernel and user) approximately from 0 to 100 */
	public double getOwnCpuUsage() {
		if(lastStatEvent == null || penultimateStatEvent == null) return 0;
		long udelta = lastStatEvent.utime - penultimateStatEvent.utime;
		long sdelta = lastStatEvent.stime - penultimateStatEvent.stime;
		double tdelta = lastStatEvent.time - penultimateStatEvent.time;
		
		return (sdelta + udelta)/tdelta;
	}
	
	/** Returns user CPU usage approximately from 0 to 100 */
	public double getUserCpuUsage() {
		if(lastStatEvent == null || penultimateStatEvent == null) return 0;
		long udelta = lastStatEvent.utime - penultimateStatEvent.utime;
		double tdelta = lastStatEvent.time - penultimateStatEvent.time;
		
		return udelta/tdelta;
	}
	
	/** Returns CPU usage  (kernel and user, includes waited child processes) approximately from 0 to 100 */
	public double getTotalCpuUsage() {
		if(lastStatEvent == null || penultimateStatEvent == null) return 0;
		long udelta = lastStatEvent.utime - penultimateStatEvent.utime;
		long sdelta = lastStatEvent.stime - penultimateStatEvent.stime;
		long cudelta = lastStatEvent.cutime - penultimateStatEvent.cutime;
		long csdelta = lastStatEvent.cstime - penultimateStatEvent.cstime;
		double tdelta = lastStatEvent.time - penultimateStatEvent.time;
		
		return (sdelta + udelta + csdelta + cudelta)/tdelta;
	}
	
	/** Returns IO read and write rate */
	public double getIORate() {
		if(lastIOEvent == null || penultimateIOEvent == null) return 0;
		long drchar = lastIOEvent.rchar - penultimateIOEvent.rchar;
		long dwchar = lastIOEvent.rchar - penultimateIOEvent.wchar;
		double tdelta = lastIOEvent.time - penultimateIOEvent.time;
		
		return (drchar + dwchar)/tdelta;
	}
	
	/** Returns IO read and write-related syscalls number per seconds*/
	public double getIOSyscallRate() {
		if(lastIOEvent == null || penultimateIOEvent == null) return 0;
		long dsyscr = lastIOEvent.syscr - penultimateIOEvent.syscr;
		long dsyscw = lastIOEvent.syscw - penultimateIOEvent.syscw;
		double tdelta = lastIOEvent.time - penultimateIOEvent.time;
		
		return (dsyscr + dsyscw)/tdelta;
	}
		
	public double getPageFaultRate() {
		if(lastStatEvent == null || penultimateStatEvent == null) return 0;
		long dminflt = lastStatEvent.minflt - penultimateStatEvent.minflt;
		long dcminflt = lastStatEvent.cminflt - penultimateStatEvent.cminflt;
		long dmajflt = lastStatEvent.majflt - penultimateStatEvent.majflt;
		long dcmajflt = lastStatEvent.cmajflt - penultimateStatEvent.cmajflt;
		double tdelta = lastStatEvent.time - penultimateStatEvent.time;
		
		return (dminflt+dcminflt+dmajflt+dcmajflt)/tdelta;
	}
	
	public double getMajorPageFaultRate() {
		if(lastStatEvent == null || penultimateStatEvent == null) return 0;
		long dmajflt = lastStatEvent.majflt - penultimateStatEvent.majflt;
		long dcmajflt = lastStatEvent.cmajflt - penultimateStatEvent.cmajflt;
		double tdelta = lastStatEvent.time - penultimateStatEvent.time;
		
		return (dmajflt+dcmajflt)/tdelta;
	}
	
	public long getVirtualMemoryUsage() {
		if(lastStatEvent == null) return 0;
		return lastStatEvent.vsize;
	}
	
	public long getMemoryUsage() {
		if(lastStatEvent == null) return 0;
		return lastStatEvent.rss;
	}
	
	public String getShortName() {
		if(lastStatEvent == null) return "";
		return lastStatEvent.comm;
	}
	
	public String getCommandLine() {
		if(cmdline == null) return "";
		return cmdline;
	}
	
	public void newStatEvent(LogEventStat e) {
		penultimateStatEvent = lastStatEvent;
		lastStatEvent = e;
	}

	public void newIOEvent(LogEventIO e) {
		penultimateIOEvent = lastIOEvent;
		lastIOEvent = e;
	}
	public String hostPid() {
		return host + ":" + Integer.toString(pid);
	}

}
