package org.vi_server.diststatanalyse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;

public class Main implements TaskWatcherObserver {
	PrintStream out;

	public Main(PrintStream out) {
		super();
		this.out = out;
	}

	public static void main(String[] args) throws FileNotFoundException {
		if(args.length!=1) {
			System.err.println("Usage: diststatanalyse filename");
			System.exit(1);
		}
		
		InputStream is = new FileInputStream(new File(args[0]));
		Interpreter ii = new Interpreter(is);
		
		TaskWatcher tw = new TaskWatcher();
		tw.observers.add(new Main(System.out));
		ii.observers.add(tw);
		
		ii.run();
	}
	
	public static void main(InputStream is) {
		System.out.println("Simple monitoring log demo initialized");
		
		try {		
			Interpreter ii = new Interpreter(is);
			
			TaskWatcher tw = new TaskWatcher();
			tw.observers.add(new Main(System.out));
			ii.observers.add(tw);

			System.out.println("MM AA");
			ii.run();
			System.out.println("MM BB");
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	
	
	@Override
	public void taskStarted(Task t) {
		out.println(String.format("Task started: %s %s", t.hostPid(), t.getShortName()));
	}


	@Override
	public void taskFinished(Task t) {
		out.println(String.format("Task finished: %s %s", t.hostPid(), t.getShortName()));
	}


	@Override
	public void taskStatUpdated(Task t) {
		out.println(String.format(
				"%20s %20s CPU:%7.3g/%7.3g mem:%11d/%11d IO:%10g(%7g calls) page_faults:%8g(%6g maj)",
				t.hostPid(), 
				t.getShortName(),
				t.getUserCpuUsage(), t.getTotalCpuUsage(),
				t.getMemoryUsage(), t.getVirtualMemoryUsage(),
				t.getIORate(), t.getIOSyscallRate(),
				t.getPageFaultRate(), t.getMajorPageFaultRate()));
	}


	@Override
	public void hostProcessingFinished(String host) {
		out.println(String.format("Host processing finished: %s", host));
	}


	@Override
	public void hostProcessingStarted(String host) {
		out.println(String.format("Host processing started: %s", host));		
	}

}
