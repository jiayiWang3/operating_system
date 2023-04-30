package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});

        blockedThreadQueue = new PriorityQueue<>();
	}

    private class BlockedThread implements Comparable {
        private KThread thread;
        private long wakeTime;

        public BlockedThread(KThread thread, long wakeTime) {
            this.thread = thread;
            this.wakeTime = wakeTime;
        }

        @Override
        public int compareTo(Object o) {
            BlockedThread that = (BlockedThread)o;
            return (int)(this.wakeTime - that.wakeTime);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this.getClass() != o.getClass()) {
                return false;
            }
            BlockedThread that = (BlockedThread) o;
            return this.thread == that.thread;
        }
    }

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();

        while (!blockedThreadQueue.isEmpty() && blockedThreadQueue.peek().wakeTime <= Machine.timer().getTime()) {
            blockedThreadQueue.poll().thread.ready();
        }
		Machine.interrupt().restore(intStatus);

		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		// long wakeTime = Machine.timer().getTime() + x;
		// while (wakeTime > Machine.timer().getTime())
		// 	KThread.yield();

        if (x <= 0) {
            return;
        }

		boolean intStatus = Machine.interrupt().disable();

        /* critical section, the priority queue is shared */
        long wakeTime = Machine.timer().getTime() + x;
        BlockedThread blocked = new BlockedThread(KThread.currentThread(), wakeTime);
        blockedThreadQueue.offer(blocked);

        KThread.sleep();
        Machine.interrupt().restore(intStatus);
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {
        boolean intStatus = Machine.interrupt().disable();

        for (BlockedThread blockedthread : blockedThreadQueue) {
            if (blockedthread.thread == thread) {
                blockedThreadQueue.remove(blockedthread);
                blockedthread.thread.ready();
                System.out.println(KThread.currentThread().getName() + " cancelled " + thread.getName() + "'s timer interrupt.");
                Machine.interrupt().restore(intStatus);
                return true;
            }
        }

        Machine.interrupt().restore(intStatus);
        return false;
	}

    public static void alarmTest() {
        int durations[] = {-100, -10, 0, 1000, 10*1000, 100*1000};
        long t0, t1;
    
        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil (d);
            t1 = Machine.timer().getTime();
            System.out.println ("alarmTest - waitUntil(" + d + "): waited for " + (t1 - t0) + " ticks");
        }
    }

    public static void selfTest() {
		Lib.debug(dbgAlarm, "Enter Alarm.selfTest");
        if (Lib.test(dbgAlarm)) {
            alarmTest();
            System.out.println("End Alarm.selfTest");
        }
    }

	private static final char dbgAlarm = 'l';

    PriorityQueue<BlockedThread> blockedThreadQueue;
}
