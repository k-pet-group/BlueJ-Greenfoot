/**
 ** Class MyClass - write a description of the class here
 ** 
 ** @author: 
 ** Date: 
 **/
public class MyClass
{
    // instance variables - replace the example below with your own - replace the example below with your own - repla
    private int x;
    private String name;

    /**
     ** Constructor for objects of class MyClass
     **/
    public MyClass()
    {
	// initialise instance variables
	x = 0;
	name = "Michael";
    }
    
    /**
     ** An example of a method - replace this comment with your own
     ** 	
     ** @param  y   a sample parameter for a method
     ** @return     the sum of x and y 
     **/
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

	for (int i=0; i<5000000; i++) {
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
	System.out.println("The debugger is shit!");
   }
}
