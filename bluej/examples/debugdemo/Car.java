/**
 * Class Car - nonsense class for debugger demo
 * 
 * @author Michael Kölling
 * @version 13 August 1999
 */
public class Car
{
    private int numberOfFrontSeats;
    private int numberOfBackSeats;

    /**
     * Create a car with seat numbers
     */
    public Car(int frontSeats, int backSeats)
    {
        numberOfFrontSeats = frontSeats;
        numberOfBackSeats = backSeats;
    }

    /**
     * Return the number of seats in the car.
     */
    public int seats()
    {
        int totalSeats;

        totalSeats = numberOfFrontSeats;
        totalSeats += numberOfBackSeats;

        return totalSeats;
    }
}
