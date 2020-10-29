package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static Lock lock;
    static Condition co, co_ac, co_c, cm;
    static Semaphore sc;
    static Communicator cn;
    static boolean fl_ac, fl_c, bm;
    
    public static void selfTest()
    {
//	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);

		// This is the test, using our own BoatGrader, we do not submit the BoatGrader.
/*
        for(int na = 0; na < 8; na++) {
            for(int nc = 2; nc < 8; nc++) {
                System.out.println("\n ***Testing Boats with " + nc + " children, " + na + " adult(s)***");
                BoatGrader b = new BoatGrader();
                b.adults = na;
                b.children = nc;
                begin(na, nc, b);
                b.AssertFinished();
            }
        }*/
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	lock = new Lock();
	co = new Condition(lock);
	co_ac = new Condition(lock);
	co_c = new Condition(lock);
	cm = new Condition(lock);
	sc = new Semaphore(0);
	cn = new Communicator();
	fl_ac = false;
	fl_c = false;
	bm = false;
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
	for(int i = 0; i < adults; i++) {
	    Runnable r = new Runnable() {
	        public void run() {
	            AdultItinerary();
	        }
	    };
	    KThread t = new KThread(r);
	    t.setName("adult " + i);
	    t.fork();
	}
	for(int i = 0; i < children; i++) {
	    Runnable r = new Runnable() {
	        public void run() {
	            ChildItinerary();
	        }
	    };
	    KThread t = new KThread(r);
	    t.setName("child" + i);
	    t.fork();
	}
	int n = adults + children;
	while(n > 0) {
	    n += cn.listen();
	}
	lock.acquire();
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	sc.P();
	lock.acquire();
	while(fl_ac) {
	    co.sleep();
	}
	fl_ac = true;
	while(bm) {
	    cm.wake();
	    co_ac.sleep();
	}
	bg.AdultRowToMolokai();
	cn.speak(-1);
	bm = true;
	if(fl_c) {
	    co_c.wake();
	}
	else {
	    fl_ac = false;
	    fl_c = true;
	    co.wake();
	}
	lock.release();
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	lock.acquire();
	for(;;) {
	    if(fl_c) {
	        while(fl_ac) {
	            co.sleep();
	        }
	        fl_ac = true;
	        while(bm) {
	            cm.wake();
	            co_ac.sleep();
	        }
	        bg.ChildRowToMolokai();
	        cn.speak(-1);
	        co_c.wake();
	    }
	    else {
	        fl_c = true;
	        co_c.sleep();
	        if(bm) {
	            fl_ac = false;
	            co.wake();
	            continue;
	        }
	        bg.ChildRideToMolokai();
	        cn.speak(-1);
	        fl_ac = false;
	        bm = true;
	        sc.V();
	        co.wake();
	    }
	    cm.sleep();
	    while(!bm) {
	        cm.sleep();
	    }
	    bg.ChildRowToOahu();
	    cn.speak(1);
	    fl_c = false;
	    bm = false;
	    co_ac.wake();
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
