package org.vi_server.diststatanalyse;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpreter {
	InputStream is;
	public List<InterpreterObserver> observers = new LinkedList<InterpreterObserver>();
	
	static Pattern linePattern = Pattern.compile("^(\\S+)\\s+([-0-9.]+)\\s+([0-9-]+)\\s++(\\S+)\\s*(.*)");

	public Interpreter(InputStream is) {
		super();
		this.is = is;
	}
	
	void run() {
		Scanner s = new Scanner(is);
		while(s.hasNextLine()) {
			String l = s.nextLine();
			Matcher m = linePattern.matcher(l);
			if(!m.matches()) continue;
			String mhost = m.group(1);
			String mtime = m.group(2);
			String mpid = m.group(3);
			String subcommand = m.group(4);
			String scargs = m.group(5);
			
			String host = mhost;
			double time =  Double.parseDouble(mtime);
			int pid = Integer.parseInt(mpid);
			
			LogEvent e=null;
			
			if(subcommand.equals("cmd:")) {
				e = new LogEventCmdline(time, host, pid, scargs);
			} else if(subcommand.equals("stat:")) {
				e = new LogEventStat(time, host, pid, scargs);
			} else if(subcommand.equals("io:")) {
				LogEventIO ei = new LogEventIO(time, host, pid, scargs);
				if(ei.rchar!=-1) {
					e=ei;
				}
			} else if(subcommand.equals("===")) {
				e = new LogEventStartOfTasksInfo(time, host, pid);
			} else if(subcommand.equals("===.")) {
				e = new LogEventEndOfTasksInfo(time, host, pid);
			} else {
				e = new LogEventUnknown(time, host, pid, subcommand, scargs);
			}
			
			if(e!=null) {
				for (InterpreterObserver o : observers) {
					e.beObserved(o);
				}
			}
		}
	}
}
