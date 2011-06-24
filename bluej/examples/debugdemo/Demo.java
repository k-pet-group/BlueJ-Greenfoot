/**
 * Class Demo - this class is used in the BlueJ tutorial for demonstrating
 * the BlueJ debug functionality. It contains loops and nested method calls
 * that make interesting examples to set breakpoints.
 * 
 * @author M. Kölling
 * @version 13 August 1999
 */
public class Demo
{
    private String name;
    private int answer;

    /**
     * Constructor for objects of class Demo
     */
    public Demo()
    {
        name = "Marvin";
        answer = 42;
    }

    /** 
     * Loop for a while and do some meaningless computations.
     */
    public int loop(int count)
    {
        int sum = 17;

        for (int i=0; i<count; i++) {
            sum = sum + i;
            sum = sum - 2;
        }
        return sum;
    }

    /**
     * Method for demonstrating single stepping with nested method call.
     */
    public int carTest()
    {
        int places;
        Car myCar = new Car(2, 3);

        places = myCar.seats();
        return places;
    }

    public int longloop()
    {
        int sum = 0;

        for (int i=0; i<20000000; i++) {
          sum = sum + i;
          sum = sum -200;
        }
        return 42;
    }


}
