/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011, 2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot;

public class PlayerDataVisitor
{
    private static PlayerData myData;
    
    public static PlayerData allocate(String userName, int rank, String singletonUserName)
    {
        if (singletonUserName != null && singletonUserName.equals(userName))
        {
            if (myData != null && myData.getUserName().equals(singletonUserName))
            {
                myData.setRank(rank);
            }
            else
            {
                myData = new PlayerData(userName, rank);
            }
            return myData;
        }
        else
        {
            return new PlayerData(userName, rank);
        }
    }
    
    public static GreenfootImage readImage(byte[] imageFileContents)
    {
        return new GreenfootImage(imageFileContents);
    }
}
