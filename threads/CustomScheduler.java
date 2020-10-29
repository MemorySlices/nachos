package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Random;

/**
 * A round-robin scheduler tracks waiting threads in FIFO queues, implemented
 * with linked lists. When a thread begins waiting for access, it is appended
 * to the end of a list. The next thread to receive access is always the first
 * thread in the list. This causes access to be given on a first-come
 * first-serve basis.
 */
public class CustomScheduler extends Scheduler {
    public CustomScheduler() {
        System.out.println("using custom scheduler");
    }
    
    /**
     * Allocate a new FIFO thread queue.
     *
     * @param	transferPriority	ignored. Round robin schedulers have
     *					no priority.
     * @return	a new FIFO thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new WeightQueue();
    }

    private class WeightQueue extends ThreadQueue {
		/**
		 * Add a thread to the end of the wait queue.
		 *
		 * @param	thread	the thread to append to the queue.
		 */    
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
				
			waitQueue.add(thread);
			int w = rng.nextInt(16) + 1;
			Lib.assertTrue(w > 0);
			weights.add(w);
			sum_weight += w;
		}

		/**
		 * Remove a thread from the beginning of the queue.
		 *
		 * @return	the first thread on the queue, or <tt>null</tt> if the
		 *	       	queue is
		*		empty.
		*/
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
				
			if (waitQueue.isEmpty())
				return null;
			
			int p = rng.nextInt(sum_weight), w;
			Lib.assertTrue(p>=0&&p<sum_weight);
			Iterator i=waitQueue.iterator(), j=weights.iterator();
			for(; (p-=(w=(int)j.next())) >= 0; i.next());
			KThread t = (KThread)i.next();
			sum_weight -= w;
			i.remove();
			j.remove();
			return t;
		}

		/**
		 * The specified thread has received exclusive access, without using
		 * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
		 * threads are waiting for access.
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
				
			Lib.assertTrue(waitQueue.isEmpty());
		}

		/**
		 * Print out the contents of the queue.
		 */
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			Iterator j=weights.iterator();
			for (Iterator i=waitQueue.iterator(); i.hasNext(); )
				System.out.print((KThread) i.next() + "(" + (int)j.next() + ") ");
			System.out.println("sum=" + sum_weight);
		}

		private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
		private LinkedList<Integer> weights = new LinkedList<Integer>();
		private int sum_weight = 0;
		private Random rng = new Random();
    }
}
