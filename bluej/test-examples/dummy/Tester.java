import java.util.*;

/**
 ** Class Tester - write a description of the class here
 ** 
 ** @author: 
 ** Date: vnjf vjf
 **/
public class Tester 
{
    // instance variables - replace the example below with your own
    public static int st = 33;
    
    private int x;
    public String name = "Michael";
    private String privname = "Michael";
    Tester t;
    public static int [] sintArray;
    private static String[] sstringArray;
    private static Tester[] stestArray;
    private static Object[] sobjArray;

    public int [] intArray;
    private String[] stringArray;
    private Tester[] testArray;
    private Object[] objArray;
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
    public int goodtest()
    {
		double pi = 3.1415;
		double smallpi = Math.round(pi * 1000) / 1000.0;

		System.out.println("result: " + smallpi);

        return 43;
    }

    public void makeArrays()
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
    public int test()
    {
        x = t.test();
	       // create NullpointerException
	       return x;
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
