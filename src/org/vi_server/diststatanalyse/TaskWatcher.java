package org.vi_server.diststatanalyse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TaskWatcher implements InterpreterObserver {

	Map<String, Task> hostpid_tasks = new HashMap<String, Task>();
	
	int endeventcount; // To know when something stopped
	
	public List<TaskWatcherObserver> observers = new LinkedList<TaskWatcherObserver>();
	
	@Override
	public void newLogEvent(LogEventUnknown e) {
		
	}

	Task countTask(LogEvent e) {
		String key = e.hostPid();
		if(hostpid_tasks.containsKey(key)) {
			Task t = hostpid_tasks.get(key);
			t.host = e.host;
			t.pid = e.pid;
			t.lastrefreshevent = endeventcount;
			return t;
		} else {
			Task t = new Task();
			t.host = e.host;
			t.pid = e.pid;
			t.lastrefreshevent = endeventcount;
			hostpid_tasks.put(key, t);
			return t;
		}
	}
	
	@Override
	public void newLogEvent(LogEventCmdline e) {
		Task t = countTask(e);
		t.cmdline = e.getCommandLine();
	}

	@Override
	public void newLogEvent(LogEventIO e) {
		Task t = countTask(e);
		t.newIOEvent(e);
	}
	
	@Override
	public void newLogEvent(LogEventStat e) {
		String key = e.hostPid();
		if(hostpid_tasks.containsKey(key)) {
			Task t = hostpid_tasks.get(key);
			if(t.lastStatEvent != null && !t.lastStatEvent.comm.equals(e.comm)) {
				// Seems to that other task occupied the same PID
				// This detection is not reliable
				Task newtask;
				for(TaskWatcherObserver o : observers) o.taskFinished(t);
				hostpid_tasks.remove(key);
				newtask = new Task();
				newtask.cmdline = t.cmdline;
				t = newtask;
				t.host = e.host;
				t.pid = e.pid;
				hostpid_tasks.put(key, t);
				for(TaskWatcherObserver o : observers) o.taskStarted(t);
			}
			t.newStatEvent(e);
			t.lastrefreshevent = endeventcount;
		} else {
			Task t = new Task();
			t.host = e.host;
			t.pid = e.pid;
			t.lastrefreshevent = endeventcount;
			hostpid_tasks.put(key, t);
			for(TaskWatcherObserver o : observers) o.taskStarted(t);
		}
	}

	@Override
	public void newLogEvent(LogEventEndOfTasksInfo e) {
		List<Task> rm = new LinkedList<Task>();
		for(Task t : hostpid_tasks.values()) {
			if(t.lastrefreshevent < endeventcount && t.host.equals(e.host)) {
				for(TaskWatcherObserver o : observers) o.taskFinished(t);
				rm.add(t);
			} else {
				if(t.penultimateStatEvent!=null) {
					for(TaskWatcherObserver o : observers) o.taskStatUpdated(t);
				} else {
					for(TaskWatcherObserver o : observers) o.taskStarted(t);
				}
			}
		}
		hostpid_tasks.values().removeAll(rm);
		++endeventcount;
		for(TaskWatcherObserver o : observers) o.hostProcessingFinished(e.host);
	}

	@Override
	public void newLogEvent(LogEventStartOfTasksInfo e) {
		for(TaskWatcherObserver o : observers) o.hostProcessingStarted(e.host);
	}
}
