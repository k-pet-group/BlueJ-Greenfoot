/**
 ** hello.java
 **
 ** Test program to demonstrate JavaBlue
 **/
 
public class hello
{
	public Integer I = new Integer(17);

	/** Method that does the work **/
	public void go()
	{
		System.out.println("hello, world");
	}
	
	/** main method for testing outside JavaBlue **/
	public static void main(String[] args)
	{
		hello h = new hello();
		h.go();
	}
}