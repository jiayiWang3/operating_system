package nachos.threads;

import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.*;

/**
 * A synchronized queue.
 */
public class SynchList<Item> {
	/**
	 * Allocate a new synchronized queue.
	 */
	public SynchList() {
		list = new LinkedList<Item>();
		lock = new Lock();
		listEmpty = new Condition(lock);
	}

	/**
	 * Add the specified object to the end of the queue. If another thread is
	 * waiting in <tt>removeFirst()</tt>, it is woken up.
	 * 
	 * @param i the object to add. Must not be <tt>null</tt>.
	 */
	public void add(Item i) {
		Lib.assertTrue(i != null);

		lock.acquire();
		list.add(i);
		listEmpty.wake();
		lock.release();
	}

	/**
	 * Remove an object from the front of the queue, blocking until the queue is
	 * non-empty if necessary.
	 * 
	 * @return the element removed from the front of the queue.
	 */
	public Item removeFirst() {
		Item i;

		lock.acquire();
		while (list.isEmpty())
			listEmpty.sleep();
		i = list.removeFirst();
		lock.release();

		return i;
	}

	private static class PingTest implements Runnable {
		PingTest(SynchList ping, SynchList pong) {
			this.ping = ping;
			this.pong = pong;
		}

		public void run() {
			for (int i = 0; i < 10; i++)
				pong.add(ping.removeFirst());
		}

		private SynchList ping;

		private SynchList pong;
	}

	/**
	 * Test that this module is working.
	 */
	public static void selfTest() {
		SynchList ping = new SynchList();
		SynchList pong = new SynchList();

		new KThread(new PingTest(ping, pong)).setName("ping").fork();

		for (int i = 0; i < 10; i++) {
			Integer o = Integer.valueOf(i);
			ping.add(o);
			Lib.assertTrue(pong.removeFirst() == o);
		}
	}

	private LinkedList<Item> list;

	private Lock lock;

	private Condition listEmpty;
}
