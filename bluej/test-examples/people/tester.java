/**
 ** Class tester - write a description of the class here
 ** 
 ** Author: 
 ** Date: 
 **/
public class tester
{
    // instance variables - replace the example below with your own
    private int x;
    public Database db;

    public static Staff stats()
    {
	return new Staff("Sophie", 1999, "C510");
    }

    public tester()
    {
	//db = new Database();
	Person p = new Staff("mik", 1999, "C10");
	db.addPerson(p);
    }

    /**
     ** An example of a method - replace with your own
     **/
    public void doexit()
    {
	System.exit(1);
    }

    public int exitfunc()
    {
	System.exit(33);
	return 4;
    }

    /**
     ** An example of a method - replace with your own
     **/
    public int sampleMethod(int y)
    {
	// put your code here
	//System.exit(1);
	for(int i=0; i<y; i++)
	  x = x+i;
	return x + y;
    }
}
