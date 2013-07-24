
import weather.util.Locator;
import weather.util.WeatherGrabber;

/**
 * A helper class that fetch weather status from weather stations 
 * for a specific location.
 * 
 * <pre>
 * class WeatherQuery
 * {
 *     private Weather weather = new Weather();
 *     
 *     public exampleMethod()
 *     {
 *         String localCondition   = weather.getLocalCondition();
 *         // Canterbury Coordinates: 51.2786 N, 1.0794 E
 *         String conditionAtPoint = weather.getCondition(51.2786, 1.0794);
 *         double temperature      = weather.getCelsiusTemperature("France", "Paris");
 *     }
 * }
 * </pre>
 * 
 * @author Amjad Altadmri
 * @version 1.0
 */
public class Weather 
{
    private static WeatherGrabber weatherGrabber = WeatherGrabber.getInstance();
    
    /**
     * Return the local weather condition.
     * Location will be decided based on the device IP address.
     * 
     * @return A string describes the weather condition
     */
    public String getLocalCondition()
    {
        return weatherGrabber.getCondition(Locator.getLocalLocation());
    }
    
    /**
     * Return the weather condition for a specific city in a specific country.
     * 
     * @param country     The country name
     * @param city         The city name
     * 
     * @return            A string describes the weather condition
     */
    public String getCondition(String country, String city)
    {
         return weatherGrabber.getCondition(Locator.getLocation(country, city));
    }
    
    /**
     * Return the weather condition for a specific location on earth
     * described by the latitude and longitude values.
     * 
     * @param latitude        The latitude value of the location
     * @param longitude     The longitude value of the location
     * 
     * @return                A string describes the weather condition
     */
    public String getCondition(double latitude, double longitude)
    {
         return weatherGrabber.getCondition(Locator.getLocation(latitude, longitude));
    }
    
    
    /**
     * Return the local temperature in celsius.
     * Location will be decided based on the device IP address.
     * 
     * @return The temperature in celsius. 
     */
    public double getLocalCelsiusTemperature()
    {                            
        return weatherGrabber.getTemperature(Locator.getLocalLocation());
    }
    
    /**
     * Return the temperature in celsius for a specific city
     * in a specific country.
     * 
     * @param country     The country name
     * @param city         The city name
     * 
     * @return            The temperature in celsius.
     */
    public double getCelsiusTemperature(String country, String city)
    {
        return weatherGrabber.getTemperature(Locator.getLocation(country, city));
    }
    
    /**
     * Return the temperature in celsius for a specific location on earth
     * described by the latitude and longitude values.
     * 
     * @param latitude        The latitude value of the location
     * @param longitude     The longitude value of the location
     * 
     * @return                The temperature in celsius.
     */
    public double getCelsiusTemperature(double latitude, double longitude)
    {
        return weatherGrabber.getTemperature(Locator.getLocation(latitude, longitude));
    }
}