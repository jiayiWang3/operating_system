package nachos.threads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import nachos.machine.*;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        this.lock = new Lock();
        this.cv = new Condition(this.lock);
        this.first = new HashMap<>();
        this.second = new HashMap<>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */

    public int exchange (int tag, int value) {
        lock.acquire();
        if (!first.containsKey(tag)) {
            first.put(tag, value);

            while (!second.containsKey(tag)) {
                cv.sleep();
            }
            int secondVal = second.get(tag);
            second.remove(tag);
            lock.release();
            return secondVal;
        }

        second.put(tag, value);
        cv.wakeAll();
        int firstVal = first.get(tag);
        first.remove(tag);
        lock.release();
        return firstVal;
    }

    /* Test Case 1: simple test. */
    public static void rendezTest1() {
        System.out.println("===== Start of Rendezvous Test1 ======");
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t1.setName("t1");

        KThread t2 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
    
        t1.fork();
        t2.fork();

        t1.join();
        t2.join();
    }

    /* Test Case 2: many threads echange on the same tag. */
    public static void rendezTest2() {
        System.out.println("===== Start of Rendezvous Test2 ======");
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 10;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 20, "Was expecting " + 20 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");

        KThread t2 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 20;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 10, "Was expecting " + 10 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
    
        KThread t3 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 30;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 40, "Was expecting " + 40 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");

        KThread t4 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 40;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 30, "Was expecting " + 30 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork();
        t2.fork();
        t1.join();
        t2.join();

        t3.fork();
        t4.fork();
        t3.join();
        t4.join();

    }

    /* Test Case 3: exchange with different tags. */
    public static void rendezTest3() {
        System.out.println("===== Start of Rendezvous Test3 ======");
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");

        KThread t2 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");

        KThread t3 = new KThread(new Runnable () {
            public void run() {
                int tag = 2;
                int send = 3;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 4, "Was expecting " + 4 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");

        KThread t4 = new KThread(new Runnable () {
            public void run() {
                int tag = 2;
                int send = 4;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 3, "Was expecting " + 3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork();
        t2.fork();
        t3.fork();
        t4.fork();

        t1.join();
        t2.join();
        t3.join();
        t4.join();

    }

    /* Test Case 4: different order of exchange, a possible deadlock situation. */
    public static void rendezTest4() {
        System.out.println("===== Start of Rendezvous Test4 ======");
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 4, "Was expecting " + 4 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");

        KThread t2 = new KThread(new Runnable () {
            public void run() {
                int tag = 2;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 3, "Was expecting " + 3 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");

        KThread t3 = new KThread(new Runnable () {
            public void run() {
                int tag = 2;
                int send = 3;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t3.setName("t3");

        KThread t4 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 4;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t4.setName("t4");

        t1.fork();
        t2.fork();
        t3.fork();

        t3.join();
        t4.fork();

        t1.join();
        t2.join();
        t4.join();

    }

    public static void selfTest() {
        Lib.debug(dbgRendezvous, "Enter Rendezvous.selfTest");
        if (Lib.test(dbgRendezvous)) {
            rendezTest1();
            rendezTest2();
            rendezTest3();
            rendezTest4();
            System.out.println();
        }
        Lib.debug(dbgRendezvous, "End Rendezvous.selfTest\n");
    }

    private static final char dbgRendezvous = 'r';

    private Condition cv;

    private Lock lock;

    private Map<Integer, Integer> first;

    private Map<Integer, Integer> second;
}