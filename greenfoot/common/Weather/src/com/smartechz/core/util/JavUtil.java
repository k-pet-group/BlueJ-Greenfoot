package com.smartechz.core.util;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class JavUtil {

    public static ByteArrayInputStream getStream(String str)
    {
        return new ByteArrayInputStream(str.getBytes());
    }

    @SuppressWarnings("rawtypes")
    public static ArrayList<String> getFields(Class objClass, Class retClass, int mod)
            throws IllegalArgumentException, IllegalAccessException
    {
        ArrayList<String> matches = new ArrayList<String>();
        Field[] fields = objClass.getFields();
        for (int f = 0; f < fields.length; f++) {
            if (fields[f].getModifiers() == mod
                    && fields[f].getType().equals(retClass))
                matches.add(fields[f].getName());
            System.out.println(fields[f].get(null));
        }
        return matches;
    }

    @SuppressWarnings("rawtypes")
    public static ArrayList<String> getFieldsValues(Class objClass, Class retClass, int mod)
            throws IllegalArgumentException, IllegalAccessException
    {
        ArrayList<String> matches = new ArrayList<String>();
        Field[] fields = objClass.getFields();
        for (int f = 0; f < fields.length; f++) {
            if (fields[f].getModifiers() == mod
                    && fields[f].getType().equals(retClass))
                matches.add((String) fields[f].get(null));
        }
        return matches;
    }
}
