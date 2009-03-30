/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bluej.Config;

/**
 * Represents a version number. A version is a sequence of numbers separated by
 * full stops and an optional string at the end.
 * 
 * @author Poul Henriksen
 * 
 */
public class Version
{
    /**
     * A change in this number indicates a breaking change that will be likely
     * to break some scenarios.
     */
    private int breakingNumber;

    /**
     * A change in this number indicates a visible (to the user) change that
     * should not break anything in most cases.
     */
    private int nonBreakingNumber;

    /** A change in this number indicates an internal change only. */
    private int internalNumber;

    /** The version number was bad or non-existent */
    private boolean badVersion = false;

    /**
     * Create a new Version from the string.
     * 
     * @param versionString A string in the format X.Y.Z. If the string is null
     *            or invalid, it will be flagged and can be determined by
     *            calling {@link #isBad()}.
     */
    public Version(String versionString)
    {
        if (versionString == null) {
            badVersion = true;
            return;
        }

        String[] split = versionString.split("\\.");
        List<Integer> numbers = new ArrayList<Integer>();

        String lastString = null;
        for (String s : split) {
            try {
                numbers.add(Integer.valueOf(Integer.parseInt(s)));
            }
            catch (NumberFormatException nfe) {
                lastString = s;
                break;
            };
        }

        // Make sure to handle the last number - even if there is something
        // after it, like an extra string.
        if (numbers.size() < 3 && lastString != null) {
            // split around any sequence of non-digits.
            String[] endSplit = lastString.split("[^0-9]+");
            // The first element of the array now contains a number
            if (endSplit.length > 0) {
                String candidate = endSplit[0];
                // if the candidate number is matching the beginning, we have
                // found a part of the version number.
                if (lastString.startsWith(candidate)) {
                    numbers.add(Integer.valueOf(Integer.parseInt(candidate)));
                }
            }
        }

        if (numbers.size() == 3) {
            breakingNumber = numbers.get(0);
            nonBreakingNumber = numbers.get(1);
            internalNumber = numbers.get(2);
        }
        else {
            badVersion = true;
        }
    }

    /**
     * True if this version number is older than the other version number in a
     * way that will be likely to break some scenarios. Or if any of the
     * versions is a bad version number.
     * 
     */
    public boolean isOlderAndBreaking(Version other)
    {
        return this.breakingNumber < other.breakingNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if this version number is different than the other version number in
     * a way that will be unlikely to break scenarios. Or if any of the versions
     * is a bad version number.
     */
    public boolean isNonBreaking(Version other)
    {
        return this.nonBreakingNumber != other.nonBreakingNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if this version number is different than the other version number
     * but will only contain in internal changes and will not break scenarios.
     * Or if any of the versions is a bad version number.
     * 
     */
    public boolean isInternal(Version other)
    {
        return this.internalNumber != other.internalNumber || this.badVersion || other.badVersion;
    }

    /**
     * True if the version number was not correctly formated.
     * 
     */
    public boolean isBad()
    {
        return badVersion;
    }

    /**
     * Returns the version in the format X.Y.Z.
     */
    public String toString()
    {
        return breakingNumber + "." + nonBreakingNumber + "." + internalNumber;
    }

    /**
     * Return a message that shows the changes introduced in apiVersion compared to this version.
     * 
     * @param apiVersion The API version that for which the changes should be shown.
     * @return
     */
    public String getChangesMessage(Version apiVersion)
    {
        StringBuffer message = new StringBuffer(Config.getString("project.version.older.part1") + this
                + Config.getString("project.version.older.part2") + apiVersion
                + Config.getString("project.version.older.part3") + "\n");
        
        int changeNumber = 1;
        String changesString = Config.getString("project.version.changes." + changeNumber, "EMPTY").trim();
        
        while(!changesString.equals("EMPTY")) {
            int spaceIndex = changesString.indexOf(' ');
            if(spaceIndex < 5) {
                // Incorrect version number format
                return "";
            }
            String versionString = changesString.substring(0,spaceIndex);
            Version changeVersion = new Version(versionString);
            if(this.isOlderAndBreaking(changeVersion)) {
                String text = changesString.substring(spaceIndex + 1);  
                message.append("\n \n  " + text);
            }
            changeNumber++;
            changesString = Config.getString("project.version.changes." + changeNumber, "EMPTY");
        }
        

        return message.toString();
    }

    /**
     * This will return a message about the version being VERY old (before we
     * introduced version numbers). This is very unlikely to ever be used.
     * 
     */
    public String getBadMessage()
    {
        return Config.getString("project.version.none");
    }

    /**
     * Return a message that says that this project is a newer version.
     */
    public String getNewerMessage()
    {
        return Config.getString("project.version.newer.part1") + this
        + Config.getString("project.version.newer.part2");
    }

    /**
     * Get message if this is not a Greenfoot version number.
     */
    public String getNotGreenfootMessage(File projectDir)
    {
        return Config.getString("project.version.notGreenfoot") + projectDir;
    }

}
