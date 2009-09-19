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
package greenfoot.event;

import rmiextension.wrappers.event.RCompileEvent;

/**
 * Listens for compile events in greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileListener.java 6725 2009-09-19 04:55:02Z davmac $
 */
public interface CompileListener
{
    public void compileStarted(RCompileEvent event);

    public void compileError(RCompileEvent event);

    public void compileWarning(RCompileEvent event);

    public void compileSucceeded(RCompileEvent event);

    public void compileFailed(RCompileEvent event);
}
