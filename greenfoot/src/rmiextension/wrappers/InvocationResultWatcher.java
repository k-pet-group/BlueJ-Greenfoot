/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package rmiextension.wrappers;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.testmgr.record.InvokerRecord;

/**
 * A result watcher used by the RObjectImpl class.
 * 
 * @author Davin McCall
 * @version $Id: InvocationResultWatcher.java 6170 2009-02-20 13:29:34Z polle $
 */
class InvocationResultWatcher implements ResultWatcher
{
    public String errorMsg = null;
    public DebuggerObject resultObj = null;
    
    public void putError(String error)
    {
        errorMsg = error;
        synchronized (this) {
            notify();
        }
    }
    
    public void putResult(DebuggerObject dObj, String name, InvokerRecord ir)
    {
        resultObj = dObj;
        synchronized (this) {
            notify();
        }
    }
    
    public ExpressionInformation getExpressionInformation()
    {
        return null;
    }

    public void putException(String message)
    {
        errorMsg = message;
        synchronized (this) {
            notify();
        }
    }
    
    public void putVMTerminated()
    {
        synchronized (this) {
            notify();
        }
    }
}
