/**
 *nfkdlvnf
 */
 
import java.util.Random;

/**
 * This class is clased MyClass.
 */
 

public class MyClass
{
start:
    // instance variables - replace the example below with your own - replace the example below with your own - repla
    private int x = 22222;
    private String name;
    public Tester tst;
    private static String sname;
    public static Tester stst;
   
    private Random rand;
    /**
     ** Constructor for objects of class MyClass
     **/
    public MyClass()
    {
        	// initialise instance variables
	 x = 4;
        	name = "K'mxnsja ";
 'cdsc' njcds "jcks"  

  String s = "ncds";
  

        tst  = new Tester();
    }

    /**
	 * This is a comment number 1
	 */
    public Tester getTester()
    {
        return tst;
    }

    /**
	 * This is a comment number 2
	 */
    public int shortloop(int count)
    {
        	int sum = 44;
        Tester t;

        for (int i=0; i<10; i++) {
            sum = sum + i;
            sum = sum -200;
        }
        t = new Tester();
        	sum = t.goodtest();
        return sum + 2;
    }

    /**
	 * This is a comment number 3
	 */
    public int loop(int count)
    {
        	int sum = 0;

        	for (int i=0; i<count; i++) {
            sum = sum + i;
            sum = sum -200;
        }
        	return x + 5;
    }

    public int longloop()
    {
        int sum = 0;

        for (int i=0; i<20000000; i++) {
          sum = sum + i;
          sum = sum -200;
        }
        return x + 5;
    }

    public int nested()
    {
        int sum = 0;
        sum = nested1(sum) + 3;
        return sum;
    }

    private int nested1(int par)
    {
        int sum = 0;
        sum = nested2(sum) + 3;
        return sum;
    }

    private int nested2(int par2)
    {
        int sum = 0;
        sum = nested3(par2) + 3;
        return sum;
    }

    private int nested3(int par3)
    {
        int sum = par3 + 1;
        return sum;
    }

   public void newMethod()
   {
        x = 88;
        shortloop(4);
        System.out.println("The debugger is shit!");
        System.out.println("line2");
        System.out.println("line3");
    
   }

    public int factorial(int n)
    {
        int temp;
        if(n==1) {
            return 1;
        }
        else {
            temp = factorial(n-1) * n;
            return temp;
        }
    }
}
