/**
 ** Class Tester - write a description of the class here
 ** 
 ** @author: 
 ** Date: 
 **/
public class Tester
{
    // instance variables - replace the example below with your own
    static int st = 33;
    
    private int x;
    private String name = "Michael";
    Tester t;
    private int [] intArray;
    private String[] stringArray;
    private Tester[] testArray;
    private Object[] objArray;
    /**
     ** Constructor for objects of class Tester
     **/
    public Tester()
    {
        // initialise instance variables
        x = 0;
        intArray = new int[4];
        intArray[0] = 42;
        intArray[2] = 84;
    }

    /**
     * An example of a method - replace this comment with your own
     * 
     * @param  y   a sample parameter for a method 
     * @return     the sum of x and y 
     **/
    public int goodtest()
    {
        return 43;
    }

    public void makeArrays()
    {
        stringArray = new String[] {"mik", "koe", null, "home"} ;
        testArray = new Tester[] {new Tester(), null, new SubTester()} ;
        objArray = new Object[] {"mik", new Integer(3), null} ;
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

    public void exctest()
    throws Exception
    {
        throw new Exception("This is mik's fault!");
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
