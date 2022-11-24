/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

/**
 * This exception will be thrown when an Extension that has been unloaded
 * from BlueJ still tries to access methods of the BlueJ class.
 * If the Extension terminates its activities when the Extension.terminate() 
 * method is called then this exception will never be thrown.
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */

public class ExtensionUnloadedException extends RuntimeException 
{
}