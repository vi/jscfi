package org.vi_server.diststatanalyse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Main implements TaskWatcherObserver {

	public static void main(String[] args) throws FileNotFoundException {
		if(args.length!=1) {
			System.err.println("Usage: diststatanalyse filename");
			System.exit(1);
		}
		
		InputStream is = new FileInputStream(new File(args[0]));
		Interpreter ii = new Interpreter(is);
		
		TaskWatcher tw = new TaskWatcher();
		tw.observers.add(new Main());
		ii.observers.add(tw);
		
		ii.run();
	}
	
	public static void main(InputStream is) {
		Interpreter ii = new Interpreter(is);
		
		TaskWatcher tw = new TaskWatcher();
		tw.observers.add(new Main());
		ii.observers.add(tw);
		
		ii.run();
	}

	
	public void newLogEvent(LogEventUnknown e) {
		//System.out.println(String.format("unk %s %s %s", e.time, e.host, e.pid));
	}

	
	public void newLogEvent(LogEventCmdline e) {
		//System.out.println(String.format("cmd %s %s %s \"%s\"", e.time, e.host, e.pid, e.getCommandLine().substring(0, Math.min(e.getCommandLine().length(), 40))));
	}

	
	public void newLogEvent(LogEventStat e) {
		// TODO Auto-generated method stub
		 System.out.println(String.format("cmd %s %s %s \"%s\" %d", e.time, e.host, e.pid, e.comm, e.cutime));
	}


	@Override
	public void taskStarted(Task t) {
		// TODO Auto-generated method stub
		System.out.println(String.format("Task started: %s %s", t.hostPid(), t.getShortName()));
	}


	@Override
	public void taskFinished(Task t) {
		System.out.println(String.format("Task finished: %s %s", t.hostPid(), t.getShortName()));
	}


	@Override
	public void taskStatUpdated(Task t) {
		System.out.println(String.format(
				"%20s %20s CPU:%5.2g/%5.2g mem:%11d/%11d IO:%10g(%7g calls) page_faults:%8g(%6g maj)",
				t.hostPid(), 
				t.getShortName(),
				t.getUserCpuUsage(), t.getTotalCpuUsage(),
				t.getMemoryUsage(), t.getVirtualMemoryUsage(),
				t.getIORate(), t.getIOSyscallRate(),
				t.getPageFaultRate(), t.getMajorPageFaultRate()));
	}


	@Override
	public void hostProcessingFinished(String host) {
		//System.out.println(String.format("Host processing finished: %s", host));
	}


	@Override
	public void hostProcessingStarted(String host) {
		//System.out.println(String.format("Host processing started: %s", host));		
	}

}
