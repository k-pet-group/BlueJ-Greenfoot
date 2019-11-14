/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2019  Michael Kolling and John Rosenberg
 
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

import bluej.pkgmgr.PkgMgrFrame;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

/**
 * A wrapper for an array object in BlueJ.
 * Behaviour is similar to the Java reflection API.
 * 
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class BArray 
{
  /**
   * Given an array BObject, returns the item at the given index.
   * In the case that the array contains elements of primitive type (<code>int</code> etc.), 
   * the return value is of the appropriate Java wrapper type (<code>Integer</code> etc.).
   * In the case that the array is composed of BlueJ objects (including nested arrays) then 
   * an appropriate BObject will be returned. 
   * 
   * @param thisArray a array BObject to retrieve the specified item of.
   * @param itemIndex the index of the BArray's item to retrieve.
   * 
   * @return An Object that encapsulate the specific item or null if <code>thisArray</code> is not an array.
   * @throws ProjectNotOpenException if the project to which this array belongs has been closed by the user.
   * @throws PackageNotFoundException if the package to which this array belongs has been deleted by the user.
   */
    public static Object getValue (BObject thisArray, int itemIndex )
    throws ProjectNotOpenException, PackageNotFoundException
    {
        ObjectReference objRef = thisArray.getObjectReference();

        if ( ! ( objRef instanceof ArrayReference ) ) return null;

        ArrayReference array = (ArrayReference)objRef;

        Value val = array.getValue(itemIndex);

        PkgMgrFrame aFrame = thisArray.getPackageFrame();
        return BField.doGetVal(aFrame, "Array", val);
    }
}
