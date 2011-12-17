package org.vi_server.diststatanalyse;

public interface InterpreterObserver {
	void newLogEvent(LogEventUnknown e);
	void newLogEvent(LogEventCmdline e);
	void newLogEvent(LogEventStat e);
	void newLogEvent(LogEventIO e);
	void newLogEvent(LogEventEndOfTasksInfo e);
	void newLogEvent(LogEventStartOfTasksInfo e);
}
