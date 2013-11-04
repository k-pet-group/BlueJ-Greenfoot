package com.smartechz.tools.mygeoloc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.smartechz.core.util.JavUtil;
import com.smartechz.core.webserver.HTTPRequest;

public class Geobytes {
    public static boolean loaded = false;
    private static HashMap<String, String> variables = new HashMap<String, String>();

    public static final String CountryId = "CountryId";
    public static final String Country = "Country";
    public static final String Fips104 = "Fips104";
    public static final String Iso2 = "Iso2";
    public static final String Iso3 = "Iso3";
    public static final String Ison = "Ison";
    public static final String Internet = "Internet";
    public static final String Capital = "Capital";
    public static final String MapReference = "MapReference";
    public static final String NationalitySingular = "NationalitySingular";
    public static final String NationalityPlural = "NationalityPlural";
    public static final String Currency = "Currency";
    public static final String CurrencyCode = "CurrencyCode";
    public static final String Population = "Population";
    public static final String CountryTitle = "CountryTitle";
    public static final String RegionId = "RegionId";
    public static final String Region = "Region";
    public static final String Code = "Code";
    public static final String Adm1Code = "Adm1Code";
    public static final String CityId = "CityId";
    public static final String City = "City";
    public static final String Latitude = "Latitude";
    public static final String Longitude = "Longitude";
    public static final String Timezone = "Timezone";
    public static final String LocationCode = "LocationCode";
    public static final String Dma = "Dma";
    public static final String Certainty = "Certainty";
    public static final String IsProxyForwarderFor = "IsProxyForwarderFor";
    public static final String IsProxyNetwork = "IsProxyNetwork";
    public static final String IpAddress = "IpAddress";

    public static String get(String var) throws MalformedURLException,
        IllegalArgumentException, IllegalAccessException, IOException
    {
        if (!loaded) {
            refresh();
            loaded = true;
        }
        return variables.get(var);
    }

    public static void refresh() throws MalformedURLException, IOException,
        IllegalArgumentException, IllegalAccessException
    {
        String url = "http://gd.geobytes.com/gd?after=-1&variables=Geobytes";
        ArrayList<String> fVals = JavUtil.getFieldsValues(Geobytes.class,
                String.class, 25);

        for (int f = 0; f < fVals.size(); f++) {
            url += ",Geobytes" + fVals.get(f);
        }

        String res = HTTPRequest.getString(url);

        String regBegin = "^var\\ss";
        String regEnd = ";$";
        String regSplit = ";var\\ss";
        String regParams = "Geobytes(.*)=(.*)";
        String regIsString = "\"(.*)\"";

        res = res.replaceAll(regBegin, "").replaceAll(regEnd, "");
        String[] fields = res.split(regSplit);

        Pattern varPtrn = Pattern.compile(regParams);
        Pattern strPtrn = Pattern.compile(regIsString);

        for (int f = 0; f < fields.length; f++) {
            Matcher varMatchr = varPtrn.matcher(fields[f]);
            if (varMatchr.find()) {
                Matcher strMatchr = strPtrn.matcher(varMatchr.group(2));
                if (strMatchr.find()) {
                    variables.put(varMatchr.group(1), strMatchr.group(1));
                } else {
                    variables.put(varMatchr.group(1), varMatchr.group(2));
                }
            }
        }
    }

    public static String getMyLocation() throws MalformedURLException,
        IllegalArgumentException, IllegalAccessException, IOException
    {
        return get(Geobytes.City) + ", " + get(Geobytes.Region) + ", "
                + get(Geobytes.Country) + ", " + get(Geobytes.MapReference);
    }
}
