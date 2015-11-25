import weather.util.WeatherGrabber;

/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * A helper class that fetch weather status for a specific location.
 * It uses "The Weather Underground Core" API. This class is licensed
 * under the terms of the GNU General Public License version 3.
 * 
 * <p>Example usage:</p>
 * <pre>
 * class WeatherQuery
 * {
 *     private Weather weather = new Weather("France", "Paris");
 *     
 *     public exampleMethod()
 *     {
 *         double temperature = weather.getTemperature();
 *         String windDirection = weather.getWindDirection();
 *         int humidity = weather.getHumidity();
 *         
 *         System.out.println("The current temperature in " + 
 *             weather.getCity() + " is " + temperature + " degrees");
 *     }
 * }
 * </pre>
 * 
 * @author amjad
 * @version 2.0
 */
public class Weather {
    
    private WeatherGrabber weatherGrabber;

    /**
     * construct an object to get local weather status.
     */
    public Weather()
    {
        weatherGrabber = new WeatherGrabber();
    }

    /**
     * Construct an object to get weather status in a specific city.
     * 
     *  @param country  the country name
     *  @param city     the city name
     */
    public Weather(String country, String city)
    {
        weatherGrabber = new WeatherGrabber(country, city);
    }

    /**
     * Fetch the current temperature in Celsius.
     */
    public double getTemperature()
    {                            
        return weatherGrabber.getDataset().getTemperature();
    }

    /**
     * Fetch the current speed of the wind in km/h.
     */
    public double getWindSpeed()
    {
        return weatherGrabber.getDataset().getWindSpeedKmh();
    }

    /**
     * Fetch the direction of the wind.
     */
    public String getWindDirection()
    {
        return weatherGrabber.getDataset().getWindDirection();
    }

    /**
     * Fetch the current humidity.
    */
    public int getHumidity()
    {
        return weatherGrabber.getDataset().getHumidity();
    }

    /**
     * Fetch the current atmospheric air pressure in hectopascal (hPa).
    */
    public double getPressure()
    {
        return weatherGrabber.getDataset().getPressurehPa();
    }
    
    /**
     * get the city name.
    */
    public String getCity()
    {
        return weatherGrabber.getCity();
    }
    
    /**
     * get the country name.
    */
    public String getCountry()
    {
        return weatherGrabber.getCountry();
    }
}