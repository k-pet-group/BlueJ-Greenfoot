/**
 ** hello.java
 **
 ** Test program to demonstrate JavaBlue
 **/
 
public class hello
{
    static final int cons = 42;
    static int stats = 0;
    public Integer number = new Integer(17);
    private String name = "mik";

    /** Method that does the work **/
    public void go()
    {
        int nummer;

        nummer = 42;
        System.out.println("hello, world");
        System.out.println("starting count...");
	for(int i = 0; i < 50000000; i++) {
	    nummer++;
	}
        System.out.println("answer = " + nummer);
    }
	
	/** main method for testing outside JavaBlue **/
	public static void main(String[] args)
	{
		hello h = new hello();
		h.go();
	}
}
