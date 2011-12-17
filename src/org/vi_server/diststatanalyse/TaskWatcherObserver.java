package org.vi_server.diststatanalyse;

public interface TaskWatcherObserver {
	public void hostProcessingStarted(String host);
	public void taskStarted(Task t);
	public void taskFinished(Task t);
	public void taskStatUpdated(Task t);
	public void hostProcessingFinished(String host);
}
