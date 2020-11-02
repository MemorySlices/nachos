package nachos.ag;

import nachos.machine.*;
import nachos.threads.*;
import java.util.HashMap;

public class BoatGrader {

    /**
     * BoatGrader consists of functions to be called to show that
     * your solution is properly synchronized. This version simply
     * prints messages to standard out, so that you can watch it.
     * You cannot submit this file, as we will be using our own
     * version of it during grading.
     * Note that this file includes all possible variants of how
     * someone can get from one island to another. Inclusion in
     * this class does not imply that any of the indicated actions
     * are a good idea or even allowed.
     */
    public int adults, children, m_adults = 0, m_children = 0, boatState = 0;
    public HashMap<String, Integer> threadState = new HashMap<String, Integer>();
    private boolean isChild(String t) {
        return t.substring(0, 5).equals("child");
    }
    private boolean isAdult(String t) {
        return t.substring(0, 5).equals("adult");
    }
    public boolean Finished() {
        return m_children == children && m_adults == adults;
    }
    public void AssertFinished(){
        Lib.assertTrue(Finished());
    }
    //NEW ADDITION FOR 2014
    //MUST BE CALLED AT THE START OF CHILDITINERARY!
    public void initializeChild(){
        //System.out.println("A child has forked.");
        String t = KThread.currentThread().getName();
        Lib.assertTrue(isChild(t));
        threadState.put(t, 1);
    }
    
    //NEW ADDITION FOR 2014
    //MUST BE CALLED AT THE START OF ADULTITINERARY!
    public void initializeAdult(){
        //System.out.println("An adult as forked.");
        String t = KThread.currentThread().getName();
        Lib.assertTrue(isAdult(t));
        threadState.put(t, 1);
    }

    /* ChildRowToMolokai should be called when a child pilots the boat
       from Oahu to Molokai */
    public void ChildRowToMolokai() {
    //System.out.println("**Child rowing to Molokai.");
    System.out.print("C");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isChild(t));
    Lib.assertTrue(threadState.get(t) == 1);
    threadState.put(t, 2);
    Lib.assertTrue(++m_children <= children);
    Lib.assertTrue(boatState < 2);
    boatState = 2;
    }

    /* ChildRowToOahu should be called when a child pilots the boat
       from Molokai to Oahu*/
    public void ChildRowToOahu() {
    //System.out.println("**Child rowing to Oahu.");
    System.out.print("B");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isChild(t));
    Lib.assertTrue(threadState.get(t) == 2);
    threadState.put(t, 1);
    Lib.assertTrue(!Finished());
    Lib.assertTrue(--m_children >= 0);
    Lib.assertTrue(boatState > 1);
    boatState = 1;
    }

    /* ChildRideToMolokai should be called when a child not piloting
       the boat disembarks on Molokai */
    public void ChildRideToMolokai() {
    //System.out.println("**Child arrived on Molokai as a passenger.");
    System.out.print("c");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isChild(t));
    Lib.assertTrue(threadState.get(t) == 1);
    threadState.put(t, 2);
    Lib.assertTrue(++m_children <= children);
    Lib.assertTrue(boatState == 2);
    boatState = 3;
    }

    /* ChildRideToOahu should be called when a child not piloting
       the boat disembarks on Oahu */
    public void ChildRideToOahu() {
    //System.out.println("**Child arrived on Oahu as a passenger.");
    System.out.print("b");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isChild(t));
    Lib.assertTrue(threadState.get(t) == 2);
    threadState.put(t, 1);
    Lib.assertTrue(!Finished());
    Lib.assertTrue(--m_children >= 0);
    Lib.assertTrue(boatState == 1);
    boatState = 0;
    }

    /* AdultRowToMolokai should be called when a adult pilots the boat
       from Oahu to Molokai */
    public void AdultRowToMolokai() {
    //System.out.println("**Adult rowing to Molokai.");
    System.out.print("A");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isAdult(t));
    Lib.assertTrue(threadState.get(t) == 1);
    threadState.put(t, 2);
    Lib.assertTrue(++m_adults <= adults);
    Lib.assertTrue(boatState < 2);
    boatState = 3;
    }

    /* AdultRowToOahu should be called when a adult pilots the boat
       from Molokai to Oahu */
    public void AdultRowToOahu() {
    //System.out.println("**Adult rowing to Oahu.");
    System.out.print("-");
    String t = KThread.currentThread().getName();
    Lib.assertTrue(isAdult(t));
    Lib.assertTrue(threadState.get(t) == 2);
    threadState.put(t, 1);
    Lib.assertTrue(!Finished());
    Lib.assertTrue(--m_adults >= 0);
    Lib.assertTrue(boatState > 1);
    boatState = 0;
    }

    /* AdultRideToMolokai should be called when an adult not piloting
       the boat disembarks on Molokai */
    public void AdultRideToMolokai() {
    //System.out.println("**Adult arrived on Molokai as a passenger.");
    Lib.assertTrue(false);
    }

    /* AdultRideToOahu should be called when an adult not piloting
       the boat disembarks on Oahu */
    public void AdultRideToOahu() {
    //System.out.println("**Adult arrived on Oahu as a passenger.");
    Lib.assertTrue(false);
    }
}

