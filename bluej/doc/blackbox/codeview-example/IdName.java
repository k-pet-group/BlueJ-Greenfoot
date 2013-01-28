/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

/**
 * A simple utility class for showing things with an identifier number and name
 */
public class IdName
{
    public int id;
    public String name;

    public String toString()
    {
        return id + ": " + name;
    }
}