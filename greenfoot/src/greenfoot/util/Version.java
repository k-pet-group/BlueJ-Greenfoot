package greenfoot.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a version number. A version is a sequence o numbers separated by
 * full stops and an optional string at the end.
 * 
 * @author Poul Henriksen
 * 
 */
public class Version
    implements Comparable<Version>
{
    /**
     * Used to represent something without a version. Version less things are
     * considered less than things with a version.
     */
    public final static Version NO_VERSION = new Version();

    /** The version string as passed into the constructor */
    private String versionString;

    /**
     * The numbers parsed from the versionString. With the most signicant number
     * first
     */
    private int[] numbers;

    private Version()
    {
        // only used for the NO-VERSION version...
        versionString = "NO_VERSION";
    }

    /**
     * Create new version.
     * 
     */
    public Version(String version)
    {
        versionString = version;
        String[] split = versionString.split("\\.");
        List<Integer> numbers = new ArrayList<Integer>();

        String lastString = null;
        for (String s : split) {
            try {
                numbers.add(new Integer(Integer.parseInt(s)));
            }
            catch (NumberFormatException nfe) {
                lastString = s;
                break;
            };
        }
        // Make sure to hand the last number - even if there is something after
        // it.
        if (lastString != null) {
            // split around any sequence of non-digits.

            String[] endSplit = lastString.split("[^0-9]+");
            // if there is at least one string now, there is a number in there
            // somewhere.
            if (endSplit.length > 0) {
                String candidate = endSplit[0];
                // if the candidate number is matching the beginning, we have
                // found a part of the version number
                if (lastString.startsWith(candidate)) {
                    numbers.add(new Integer(Integer.parseInt(candidate)));
                }
            }
        }

        this.numbers = new int[numbers.size()];
        int i = 0;
        for (Integer number : numbers) {
            this.numbers[i++] = number;
        }
    }

    /**
     * Only looks at the numbers in the version string that are in the begining
     * of the string and separated by full stops. A trailing string will be
     * ignored. If there is not the same number of numbers in both version, any
     * extra numbers will be ignored. <br>
     * Version less things are considered less than things with a version
     */
    public int compareTo(Version other)
    {
        if (this == other) {
            return 0;
        }
        if (this == NO_VERSION) {
            return -1;
        }
        if (other == NO_VERSION) {
            return 1;
        }

        int length = numbers.length < other.numbers.length ? numbers.length : other.numbers.length;
        for (int i = 0; i < length; i++) {
            if (numbers[i] != other.numbers[i]) {
                return numbers[i] - other.numbers[i];
            }
        }
        return 0;
    }

    public boolean equals(Object other)
    {
        return (compareTo((Version) other) == 0);
    }

    public String toString()
    {
        return versionString;
    }
}
