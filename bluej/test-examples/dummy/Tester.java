import java.io.*;
import java.util.*;

/**
 ** Class Tester - write a description of the class here
 ** 
 ** @author: Michael Kolling
 ** @version: 1.3
 **/
public class Tester
{
     // instance variables - replace the example 
    private static int x;

    { 
      x = 44;
	}
    public String name = "Michael";
    private String privname = "Michael";
    Tester t;
    public static Object[] sintArray;
    private static String[] sstringArray;
    private static Tester[] stestArray;
    private static Object[] sobjArray;

    public int [] intArray;
    private String[] stringArray;
    private Tester[] testArray;
    public Object[] objArray;

    /**
     ** Constructor for objects of class Tester
     **/
    public Tester()
    {
        // initialise instance vari ables
        x = 0;
        intArray = new int[4];
        intArray[2] = 42;
   } 

    /**
     * An example of a method - replace this comment with your own
     * 
     * @param  y   a sample parameter for a method 
     * @return     the sum of x and y 
     **/
    public void infiniteLoop()
    {
		for(int i = 0; i < 10; i--) {
			int j = i;
		}
    }

    public void makeArrays()
		throws Exception
    {
        stringArray = new String[] {"mik", "koe", null, "home"} ;
        testArray = new Tester[] {new Tester(), null} ;
        objArray = new Object[] {"mik", new Integer(3), null} ;
        sstringArray = new String[] {"mik", "koe", null, "home"} ;
        sobjArray = new Object[] {"mik", new Integer(3), null} ;
	}
	
    /**
     ** An example of a method - replace this comment with your own
     ** 	
     ** @param  y   a sample parameter for a method
     ** @return     the sum of x and y 
     **/
    public int goodtest()
    {
	       return 42;
    }

    public void exctest(int i)
		throws Throwable
    {
        throw new Throwable("my error");
    }

    public void DivExc()
    {
        int n = 0;
        int i = 56 / n;
    }

    /**
     * An example of a method - replace this comment with your own
     * 
     * @param  y   a sample parameter for a method 
     * @return     the sum of x and y 
     **/
    public int exit()
    {
chdjs:
        // put your code here
        System.out.println("about to exit...");
        System.exit(3);
        System.out.println("after exit!"); 
        return 33;
    }
    public void voidexit()
    {
        System.exit(0);
    }
}

    class Nested {
	}

