package org.vi_server.diststatanalyse;

import java.util.regex.Pattern;

public class LogEventStat extends LogEvent {
	
	/**
	 * The filename of the executable, in parentheses.  
	 * This is visible whether or not the executable
     * is swapped out.
	 */
	public String comm; // (sshd)
	
	/**
	 * One character from the string "RSDZTW" where R is running, S is sleeping in  an  interruptible
                          wait,  D  is  waiting in uninterruptible disk sleep, Z is zombie, T is traced or stopped (on a
                          signal), and W is paging. 
	 */
	public char state; 
	
	/** The PID of the parent. */
	public int ppid;
	
	/**
	 * The process group ID of the process.
	 */
	public int pgrp;
	
	/** The session ID of the process. */
	public int session;
	
	/** The controlling terminal of the process.  (The minor device number is contained in the  combination of bits 31 to 20 and 7 to 0; the major device number is in bits 15 to 8.) */
	public int tty_nr;
	
	/** The ID of the foreground process group of the controlling terminal of the process. */
	public int tpgid;
	
	/** The  kernel  flags  word  of  the  process.   For  bit  meanings,  see  the  PF_*  defines  in &lt;linux/sched.h>.  Details depend on the kernel version. */
	public int flags;
	
	/** The number of minor faults the process has made which have not required loading a memory  page from disk. */
	public long minflt;
	
	/** The number of minor faults that the process's waited-for children have made. */
	public long cminflt;
	
	/** The number of major faults the process has made which have required loading a memory page from disk. */
	public long majflt;
	
	/** The number of major faults that the process's waited-for children have made. */
	public long cmajflt;
	
	/** Amount of time that this process has been scheduled in user  mode,  measured  in  clock  ticks
                          (divide  by  sysconf(_SC_CLK_TCK).  This includes guest time, guest_time (time spent running a
                          virtual CPU, see below), so that applications that are not aware of the guest  time  field  do
                          not lose that time from their calculations
     */
	public long utime;
	
	/** Amount  of  time  that this process has been scheduled in kernel mode, measured in clock ticks
                          (divide by sysconf(_SC_CLK_TCK).*/
	public long stime;
	
	/**  Amount of time that this process's waited-for children have been scheduled in user mode,  mea‐
                          sured  in  clock  ticks  (divide by sysconf(_SC_CLK_TCK).  (See also times(2).)  This includes
                          guest time, cguest_time (time spent running a virtual CPU, see below).*/
	public long cutime;
	
	/** Amount of time that this process's waited-for children have been  scheduled  in  kernel  mode,
                          measured in clock ticks (divide by sysconf(_SC_CLK_TCK). */
	public long cstime;
	
	/** number  in the range -2 to -100, corresponding to real-time priorities 1 to 99. */
	public int priority;
	
	/** The  nice value (see setpriority(2)), a value in the range 19 (low priority) to -20 (high pririty) **/
	public int nice;
	
	/** Number of threads in this process */
	public int num_threads;
	
	/* itrealvalue */
	
	/** The time in jiffies the process started after system boot */
	public long starttime;
	
	/** Virtual memory size in bytes  */
	public long vsize;
	
	/** Resident Set Size: number of pages the process has in real memory.  This  is  just  the  pages
                          which  count  towards  text, data, or stack space.  This does not include pages which have not
                          been demand-loaded in, or which are swapped out.*/
	public long rss;
	
	/** Current soft limit in bytes on the rss of the process; see the description  of  RLIMIT_RSS  in
            getpriority(2).*/
	public long rsslim; 
	
	// there are more things, not included now
	
	static Pattern splitter = Pattern.compile("\\s+");
	
	/* Numbers can be bigger than maximum allowed for long */
	static long tryParseLong(String str) {
		try {
			return Long.parseLong(str);
		} catch (Exception e) {
			return -1;
		}
	}
	
	public LogEventStat(double time, String hostname, int pid, String args) {
		super(time, hostname, pid);
		
		//  args sample
		// 14674 (sshd) S 14659 14659 14659 0 -1 4202816 482 9284 0 0 0 1 6 15 20 0 1 0 189841457 8708096 377 4294967295 1 1 0 0 0 0 0 4096 65536 4294967295 0 0 17 2 0 0 0 0 0
		String[] a = splitter.split(args);
		
		assert(Integer.parseInt(a[0])==pid);
		
		comm          = a[1];
		state         = a[2].charAt(0);
		ppid          = Integer.parseInt(a[3]);
		pgrp          = Integer.parseInt(a[4]);
		session       = Integer.parseInt(a[5]);
		tty_nr        = Integer.parseInt(a[6]);
		tpgid         = Integer.parseInt(a[7]);
		flags         = (int)tryParseLong(a[8]);
		minflt        = Long.parseLong(a[9]);
		cminflt       = Long.parseLong(a[10]);        
		majflt        = Long.parseLong(a[11]);
		cmajflt       = Long.parseLong(a[12]);
		utime         = Long.parseLong(a[13]);
		stime         = Long.parseLong(a[14]);
		cutime        = Long.parseLong(a[15]);
		cstime        = Long.parseLong(a[16]);
		priority      = Integer.parseInt(a[17]);
		nice          = Integer.parseInt(a[18]);
		num_threads   = Integer.parseInt(a[19]);
		//itrealvalue = Integer.parseInt(a[20]);
		starttime     = tryParseLong(a[21]);
		vsize         = tryParseLong(a[22]);
		rss           = tryParseLong(a[23]);
		rsslim        = tryParseLong(a[24]);
		//...		
	}

	@Override
	public void beObserved(InterpreterObserver o) {
		o.newLogEvent(this);
	}

}
