package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

import java.util.LinkedList;

import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    public static final int priorityDefault = 1;
    public static final int priorityMinimum = 1;
    public static final int priorityMaximum = Integer.MAX_VALUE;  

	
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
	}
	
	/*
	public LotteryQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
	}
	*/
	

    protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
	}
	
	public boolean in(KThread t, LotteryQueue q){
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
				ma+=state1.getEffectivePriority();
			}
		}
		Lib.assertTrue(ma==state.getEffectivePriority());
		if(ma!=state.getEffectivePriority()){
			System.out.println("Lottery Error!");
		}
	}

    protected class LotteryQueue extends PriorityQueue{
		LotteryQueue() {
		}

        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            ra=new Random(1114);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me

			if(holder!=null){
				holder.hold.remove(this);
				holder.calculate_priority();
				holder=null;
			}

			LotteryThreadState state=pickNextThread();
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

        protected LotteryThreadState pickNextThread() {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			
			//print();

			int sum=0,tic;
			LotteryThreadState ret=null;

			if(queue.size()==0){
				return ret;
			}

			for(KThread t: queue){
				LotteryThreadState state=getThreadState(t);
				sum+=state.getEffectivePriority();
				System.out.println(t);
				System.out.println(state.getEffectivePriority());
			}
			
			//System.out.println("sum of tickets: "+sum);

            tic=ra.nextInt(sum);

            for(KThread t: queue){
				LotteryThreadState state=getThreadState(t);
                int state_tic=state.getEffectivePriority();
                if(tic<state_tic){
                    ret=state;
                    break;
                }
                tic-=state_tic;
            }
            
			return ret;
		}
		
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)

			if(holder!=null){
				System.out.print(" Holder "+holder.thread+" EP: "+holder.getEffectivePriority());
			}

			for(KThread t: queue){
				LotteryThreadState state=getThreadState(t);
				System.out.print(", ("+t+" P: "+state.getPriority()+" EP: "+state.getEffectivePriority()+" T: "+state.T+")");
			}
			System.out.println();
		}

		public LotteryThreadState getHolder(){
			return holder;
		}
		
		LotteryThreadState holder=null;

        public Random ra;
    }

    protected class LotteryThreadState extends ThreadState{
		public LotteryThreadState() {
		}

        public LotteryThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
		}

		public void calculate_priority(){
			// need recursion
			int sum=this.getPriority();
			for(PriorityQueue Q: hold){
				if(Q.transferPriority==false) continue;
				for(KThread t: Q.queue){
					LotteryThreadState state=getThreadState(t);
					sum+=state.getEffectivePriority();
				}
			}
			if(this.effectivepriority!=sum){
				this.effectivepriority=sum;
				for(PriorityQueue Q: wait){
					if(Q.transferPriority==false) continue;
					if(Q.holder!=null){
						Q.holder.calculate_priority();
					}
				}
			}
		}

		public void waitForAccess(LotteryQueue waitQueue) {
			// implement me

			Lib.assertTrue(Machine.interrupt().disabled());

			KThread t,h;
			t=this.thread;
			this.T=Machine.timer().getTime();
			waitQueue.queue.add(t);
			this.wait.add(waitQueue);
			
			LotteryThreadState hstate=waitQueue.holder;
			if(hstate!=null){
				hstate.calculate_priority();
			}
		}

		public void acquire(LotteryQueue waitQueue) {
			// implement me

			LotteryThreadState hstate=waitQueue.holder;
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

		public LinkedList<LotteryQueue> hold = new LinkedList<LotteryQueue>(), wait = new LinkedList<LotteryQueue>();
	}

	public static void selfTest(){
		System.out.println("-----Start Lottery Scheduler selfTest-----");

		LotteryScheduler s=new LotteryScheduler();
		//PriorityQueue q[]= new PriorityQueue[100];
		LotteryQueue q[]= new LotteryQueue[100];

		//if(q[0]==null) System.out.println("loterryqueue in null");

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
			q[i]=(LotteryQueue)s.newThreadQueue(true);
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
				for(j=1;j<=m;j++){
					if(q[j]==null) System.out.println("loterryqueue in null");
					if(s.in(t[i],q[j])) cnt++;
				}
			
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
