import java.io.*;
import java.util.Random;
    
/**
 * This class is clased MyClass.
 */
 

public clas MyClass
{
   public
   {
      vhjfvdf
      nnn
		
   }

    private Rando rand;
    public static void main(String[] args)
    {
        (new MyClass()).read();
    }

    public MyClass()
    {
            // initialise instance variables
    }

    public void read()
    {
        System.out.println("Hello, world");
        System.out.print("Enter something: ");
        InputStreamReader r = new InputStreamReader(System.in);
        try {
            int ch = r.read();
            while((ch != -1) && (ch != 'q')) {
                System.out.println("  read:" + ch);
                ch = r.read();
            }
            System.out.println("end of file");
        }
        catch(IOException e) {}
    }
    
    public void terminalTest(int cnt)
    {
        long time = System.currentTimeMillis();
        for(int i=0; i<cnt; i++)
            System.out.println("wrinting a line (line #)");
        long time2 = System.currentTimeMillis();
        System.out.println(cnt + " lines written. Time: " + (time2 - time) +
                           "ms.");
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
