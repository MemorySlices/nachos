package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

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

    protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
    }

    protected class LotteryQueue extends PriorityQueue{
        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            ra=new Random(1114);
		}

        protected LotteryThreadState pickNextThread() {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			
			//print();

			int sum=0,tic;
			LotteryThreadState ret=null;
			for(KThread t: queue){
				LotteryThreadState state=getThreadState(t);
				sum+=state.getEffectivePriority();
			}

            tic=ra.nextInt(sum);

            for(KThread t: queue){
				LotteryThreadState state=getThreadState(t);
                state_tic=state.getEffectivePriority();
                if(tic<state_tic){
                    ret=state;
                    break;
                }
                tic-=state_tic;
            }
            
			return ret;
        }
        
        public Random ra;
    }

    protected class LotteryThreadState extends ThreadState{
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
	}
}
