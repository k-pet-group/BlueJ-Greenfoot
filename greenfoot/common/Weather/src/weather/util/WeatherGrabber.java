package weather.util;

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

    String city;
    String country;
    DataSet dataset;

    private static final int TIMEOUT = 1300;
    
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
        if ( country.toLowerCase().equals("united kingdom") ) {
            country = "UK";
        }
        this.country = country;
        this.city = city;
        
        new Thread(new Runnable() {
            public void run() {
                initializeData();
            }
        }).start();
    }

    private void initializeData()
    {
        WeatherStation weatherStation = getWeatherStation(country, city);
        HttpDataReaderService reader = new HttpDataReaderService();
        reader.setWeatherStation(weatherStation);
        initializeDataset(reader, country, city);
    }
    
    private WeatherStation getWeatherStation(String country, String city)
    {
        // find all weather stations for the country
        List<WeatherStation> stations = new WeatherStationService().findAllWeatherStationsByCountry(country);

        // iterate over all founded weather stations to get one in the city
        for (WeatherStation weatherStation : stations) {
            if ( weatherStation.getCity().toLowerCase().contains(city.toLowerCase()) ) {
                return weatherStation;
            }
        }
        throw new RuntimeException("Can not find weather information for " + city + ", " + country + ". Please check spelling or try another location");
    }

    private synchronized void initializeDataset(HttpDataReaderService reader, String country, String city)
    {
        int wait = 10;
        do {
            try {
                Thread.sleep(wait);
            }
            catch (InterruptedException e) { }
            finally {
                wait *= 2;
                dataset = reader.getCurrentData();
            }
        }
        while(dataset == null && wait < TIMEOUT);

        if (dataset == null) {
            throw new RuntimeException("Can not find weather information for " + city + ", " + country + ". Please check spelling or try another location");
        }
    }

    public DataSet getDataset()
    {
        if (dataset == null) {
            synchronized (this) {
                if (dataset == null) {
                    initializeData();
                }
            }
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