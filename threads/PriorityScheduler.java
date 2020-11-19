package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.Random;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */

	// !!change threadqueue to priority queue 
	
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}
	
	/*
    public PriorityQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}
	*/

    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
				
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
				
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
				
		Lib.assertTrue(priority >= priorityMinimum &&
			priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
				
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
				
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
    }

	public boolean in(KThread t, PriorityQueue q){
		if(q.holder==getThreadState(t)) return true;

		for(KThread t1: q.queue){
			if(t1==t) return true;
		}

		return false;
	}

	public void check(KThread t){
		ThreadState state=getThreadState(t);
		
		int ma=state.getPriority();
		for(PriorityQueue Q: state.hold){
			if(Q.transferPriority==false) continue;
			for(KThread t1: Q.queue){
				ThreadState state1=getThreadState(t1);
				ma=Math.max(ma,state1.getEffectivePriority());
			}
		}
		Lib.assertTrue(ma==state.getEffectivePriority());
		if(ma!=state.getEffectivePriority()){
			System.out.println("Priority Error!");
		}
	}

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(){
		}

		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me

			if(holder!=null){
				holder.hold.remove(this);
				holder.calculate_priority();
				holder=null;
			}

			ThreadState state=pickNextThread();
			if(state!=null){
				holder=state;
				queue.remove(state.thread);
				state.hold.add(this);
				state.wait.remove(this);
				state.calculate_priority();
				return state.thread;
			}
			else
				return null;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		*/
		
		public boolean cmp(int pa, long ta, int pb, long tb){
			return (pa>pb || (pa==pb && ta<tb)); 
		}

		protected ThreadState pickNextThread() {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			
			//print();

			int ma=0;
			long mT=1000000000L;
			ThreadState ret=null;
			for(KThread t: queue){
				ThreadState state=getThreadState(t);
				if(cmp(state.getEffectivePriority(),state.T,ma,mT)){
					ma=state.getEffectivePriority();
					mT=state.T;
					ret=state;
				}
				//System.out.println("hi:"+ma+" "+mT+" "+ret.thread);
			}

			/*
			System.out.println("-----start pick next thread-----");
			System.out.println(this);
			if(ret!=null) System.out.println(ret.thread);
			print();
			System.out.println("-----end pick next thread-----");
			*/
			
			return ret;
		}
		
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)

			if(holder!=null){
				System.out.print(" Holder "+holder.thread+" EP: "+holder.getEffectivePriority());
			}

			for(KThread t: queue){
				ThreadState state=getThreadState(t);
				System.out.print(", ("+t+" P: "+state.getPriority()+" EP: "+state.getEffectivePriority()+" T: "+state.T+")");
			}
			System.out.println();
		}

		public ThreadState getHolder(){
			return holder;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		ThreadState holder=null;

		LinkedList<KThread> queue = new LinkedList<KThread>();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(){
		}

		public ThreadState(KThread thread) {
			this.thread = thread;
			
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			return effectivepriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			
			this.priority = priority;
			
			// implement me

			//this.effectivepriority=priority;
			this.calculate_priority();
		}

		public void calculate_priority(){
			// need recursion
			int ma=this.getPriority();
			for(PriorityQueue Q: hold){
				if(Q.transferPriority==false) continue;
				for(KThread t: Q.queue){
					ThreadState state=getThreadState(t);
					ma=Math.max(ma,state.getEffectivePriority());
				}
			}
			if(this.effectivepriority!=ma){
				this.effectivepriority=ma;
				for(PriorityQueue Q: wait){
					if(Q.transferPriority==false) continue;
					if(Q.holder!=null){
						Q.holder.calculate_priority();
					}
				}
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		*
		* @see	nachos.threads.ThreadQueue#waitForAccess
		*/
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me

			Lib.assertTrue(Machine.interrupt().disabled());

			KThread t,h;
			t=this.thread;
			this.T=Machine.timer().getTime();
			waitQueue.queue.add(t);
			this.wait.add(waitQueue);
			
			ThreadState hstate=waitQueue.holder;
			if(hstate!=null){
				hstate.calculate_priority();
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me

			ThreadState hstate=waitQueue.holder;
			KThread t;
			if(hstate!=null){
				//t=hstate.thread;
				hstate.hold.remove(waitQueue);
				hstate.calculate_priority();
			}
			waitQueue.holder=this;
			this.hold.add(waitQueue);
			//t=hstate.thread;
			this.calculate_priority();
		}	

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		
		int effectivepriority;
		long T;

		public LinkedList<PriorityQueue> hold = new LinkedList<PriorityQueue>(), wait = new LinkedList<PriorityQueue>();
	}

	public static void selfTest(){
		System.out.println("-----Start Priority Scheduler selfTest-----");

		PriorityScheduler s=new PriorityScheduler();
		//PriorityQueue q[]= new PriorityQueue[100];
		PriorityQueue q[]= new PriorityQueue[100];
		int i,j,n,m,step,cnt,flag,lim;
		Random ra=new Random(123);

		n=10;
		m=2;

		KThread t[]=new KThread[100];
		/*
		KThread t2=new KThread();
		KThread t3=new KThread();
		KThread t4=new KThread();
		KThread t5=new KThread();
		KThread t6=new KThread();
		KThread t7=new KThread();
		t1.setName("T1");
		t2.setName("T2");
		t3.setName("T3");
		t4.setName("T4");
		t5.setName("T5");
		t6.setName("T6");
		t7.setName("T7");
		s.getThreadState(t1).setPriority(1);
		s.getThreadState(t2).setPriority(2);
		s.getThreadState(t3).setPriority(3);
		s.getThreadState(t4).setPriority(4);
		s.getThreadState(t5).setPriority(5);
		s.getThreadState(t6).setPriority(6);
		s.getThreadState(t7).setPriority(7);
		*/

		boolean intStatus = Machine.interrupt().disable();

		for(i=1;i<=m;i++){
			//q[i]=s.newThreadQueue(true);
			//q[i]=new PriorityQueue(true);
		}
		
		for(i=1;i<=n;i++){
			t[i]=new KThread();
			t[i].setName("T"+i);
			s.getThreadState(t[i]).setPriority(1+ra.nextInt(7));
		}	
		for(step=1;step<=1000;step++){
			flag=ra.nextInt(10);
			cnt=0;
			for(i=1;i<=n;i++)
				for(j=1;j<=m;j++)
					if(s.in(t[i],q[j])) cnt++;
			
			if(cnt<=n*m/3)
				lim=3;
			else
				lim=5;

			if(flag<lim || cnt==n*m){
				i=1+ra.nextInt(m);
				q[i].nextThread();
				System.out.println("Step "+step+" : Queue "+i+" nextThread");
			}
			else{
				i=1+ra.nextInt(n);
				j=1+ra.nextInt(m);
				while(s.in(t[i],q[j])){
					i=1+ra.nextInt(n);
					j=1+ra.nextInt(m);
				}
				if(q[j].getHolder()==null)
					q[j].acquire(t[i]);
				else
					q[j].waitForAccess(t[i]);
				System.out.println("Step "+step+" : Thread "+i+" wait for queue "+j);
			}
			for(j=1;j<=m;j++){
				System.out.print("Queue "+j+" : ");
				q[j].print();
			}
			System.out.println();
			for(i=1;i<=n;i++){
				s.check(t[i]);
				//System.out.println("Thread "+i+" Pass Priority Check");
			}
		}

		Machine.interrupt().restore(intStatus);

		System.out.println("-----End Priority Scheduler selfTest-----");
	}
}
