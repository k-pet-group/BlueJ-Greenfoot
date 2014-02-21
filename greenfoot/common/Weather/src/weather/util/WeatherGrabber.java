package weather.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import com.smartechz.tools.mygeoloc.Geobytes;

import de.mbenning.weather.wunderground.api.domain.DataSet;
import de.mbenning.weather.wunderground.api.domain.WeatherStation;
import de.mbenning.weather.wunderground.impl.services.HttpDataReaderService;
import de.mbenning.weather.wunderground.impl.services.WeatherStationService;

/**
 * A class that grab weather information through public API  
 * 
 * @author amjad
 */
public class WeatherGrabber {

    private String city;
    private String country;
    private final HttpDataReaderService reader = new HttpDataReaderService();;
    private DataSet dataset;

    private static final int TIMEOUT = 10000; // Takes about 5 seconds on my machine
    
    public WeatherGrabber()
    {
        setLocalLocation();
    }

    public WeatherGrabber(String country, String city)
    {
        setLocation(country, city);
    }

    private void setLocalLocation()
    {
        try {
            country = Geobytes.get(Geobytes.Country);
            city = Geobytes.get(Geobytes.City);
        }
        catch(Exception e) {
            throw new RuntimeException("Can not detect current location, check your internet connection");
        }
        setLocation(country, city);
    }

    private void setLocation(String country, String city)
    {
        // Wunderground have an entry under UK with outdated stations; up-to-date version is under United Kingdom:
        if ( country.toLowerCase().equals("UK") ) {
            country = "United Kingdom";
        }
        this.country = country;
        this.city = city;
        
        new Thread(new Runnable() {
            public void run() {
                fetchData();
            }
        }).start();
    }

    private void fetchData()
    {
        reader.setWeatherStation(getWeatherStation(country, city));
        DataSet t = reader.getCurrentData();
        synchronized (this) {
            dataset = t;
            notify();
        }       
    }
    
    // It seems that the API doesn't encode strings into URLs properly, so we must do it: 
    private static String encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private WeatherStation getWeatherStation(String country, String city)
    {
        // find all weather stations for the country
        List<WeatherStation> stations = new WeatherStationService().findAllWeatherStationsByCountry(encode(country));

        // iterate over all founded weather stations to get one in the city
        for (WeatherStation weatherStation : stations) {
            if (weatherStation.getCity() != null &&  weatherStation.getCity().toLowerCase().contains(city.toLowerCase()) ) {
                return weatherStation;
            }
        }
        throw new RuntimeException("Can not find weather information for " + city + ", " + country + ". Please check spelling or try another location");
    }

    public synchronized DataSet getDataset()
    {
        if (dataset == null)
        {
            try
            {
                wait(TIMEOUT);
            }
            catch (InterruptedException e) { }
        }
        
        if (dataset == null) {
            throw new RuntimeException("Can not find weather information for " + city + ", " + country + ". Operation timed out.");
        }
        return dataset;
    }
    
    public String getCity()
    {
        return city;
    }

    public String getCountry()
    {
        return country;
    }
}