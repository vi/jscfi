package org.vi_server.diststatanalyse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogEventIO extends LogEvent {
	
	
	/** The number of bytes which this task has caused to be read from storage. This
is simply the sum of bytes which this process passed to read() and pread().
It includes things like tty IO and it is unaffected by whether or not actual
physical disk IO was required (the read might have been satisfied from
pagecache) */
	long rchar;
	
	/** The number of bytes which this task has caused, or shall cause to be written
to disk. Similar caveats apply here as with rchar. */
	long wchar;
	
	/** Attempt to count the number of read I/O operations, i.e. syscalls like read()
and pread(). */
	long syscr;
	
	/** Attempt to count the number of write I/O operations, i.e. syscalls like
write() and pwrite(). */
	long syscw;
	
	/** Attempt to count the number of bytes which this process really did cause to
be fetched from the storage layer. Done at the submit_bio() level, so it is
accurate for block-backed filesystems. */
	long read_bytes;
	
	/** Attempt to count the number of bytes which this process caused to be sent to
the storage layer. This is done at page-dirtying time. */
	long write_bytes;
	
	/** The big inaccuracy here is truncate. If a process writes 1MB to a file and
then deletes the file, it will in fact perform no writeout. But it will have
been accounted as having caused 1MB of write.
In other words: The number of bytes which this process caused to not happen,
by truncating pagecache. A task can cause "negative" IO too. If this task
truncates some dirty pagecache, some IO which another task has been accounted
for (in its write_bytes) will not be happening. We _could_ just subtract that
from the truncating task's write_bytes, but there is information loss in doing
that. */
	long cancelled_write_bytes;
	
	static Pattern pattern = Pattern.compile("rchar=(\\d+) wchar=(\\d+) syscr=(\\d+) syscw=(\\d+) read_bytes=(\\d+) write_bytes=(\\d+) cancelled_write_bytes=(\\d+)");
	
	public LogEventIO(double time, String hostname, int pid, String args) {
		super(time, hostname, pid);
		
		//  args sample
		// rchar=86 wchar=97 syscr=4 syscw=3 read_bytes=0 write_bytes=4096 cancelled_write_bytes=0
		
		Matcher a = pattern.matcher(args);
		
		if(!a.matches()) {
			rchar=-1; // flag value
			return;
		}
		
		rchar                 = Long.parseLong(a.group(1));
		wchar                 = Long.parseLong(a.group(2));
		syscr                 = Long.parseLong(a.group(3));
		syscw                 = Long.parseLong(a.group(4));
		read_bytes            = Long.parseLong(a.group(5));
		write_bytes           = Long.parseLong(a.group(6));
		cancelled_write_bytes = Long.parseLong(a.group(7));
		
	}

	@Override
	public void beObserved(InterpreterObserver o) {
		o.newLogEvent(this);
	}

}
