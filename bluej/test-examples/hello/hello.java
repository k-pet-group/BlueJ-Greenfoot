
/**
 ** hello.java
 **
 ** Test program to demonstrate JavaBlue
 **/

public class hello
{
    static final int cons = 42;
    static int stats = 0;
    private String name = "mik";

    /** Method that does the work **/
    public void go()
    {
        int nummer;
        nummer = 42;
        System.out.println("hello, world");		
    }
	
    	/**
     * main method for testing outside JavaBlue 
     */
    public static void main(String[] args)
    {
	       hello h = new hello();
	       h.go();
        for(int i = 0; i < args.length; i++) 
            System.out.println("Hello " + args[i]);
    }
}
