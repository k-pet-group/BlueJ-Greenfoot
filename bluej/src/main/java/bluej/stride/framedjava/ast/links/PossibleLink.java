/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.slots.UnderlineContainer;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A possible link is an item in a slot which could be a hyper link to:
 *  - a declared method (subclass: {@link PossibleMethodUseLink})
 *  - a superclass method (subclass: {@link PossibleKnownMethodLink})
 *  - a type (subclass: {@link PossibleTypeLink})
 *  - a variable (subclass: {@link PossibleVarLink})
 *  
 *  It's created by the slots, where we know which things *could* be a link,
 *  but we don't know for sure (e.g. this looks like a variable, but it could
 *  be undeclared, or it could be a method missing parameters, or it could
 *  be somewhere that we don't currently detect, e.g. a protected field in
 *  a superclass).  So we create a PossibleLink object with the details,
 *  which will then be inspected in the editor and it will be confirmed or not.
 */
public abstract class PossibleLink
{
    /** The start position within the slot, 0 is before the first char */
    protected final int startPosition;
    /** The start position within the slot, 1 is after the first char */
    protected final int endPosition;
    /** The slot which this possible link is in */
    protected final UnderlineContainer slot;
    /** Whether this link finding request has been cancelled.
     * Generally, possible links are created and searched when the
     * user is mousing around while holding a modifier key.  If
     * they mouse out of a link, it will be cancelled.
     */
    private boolean cancelled;

    public PossibleLink(int startPosition, int endPosition, UnderlineContainer slot)
    {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.slot = slot;
    }

    @OnThread(Tag.FX)
    public synchronized void cancel()
    {
        cancelled = true;
    }

    @OnThread(Tag.Any)
    protected synchronized boolean isCancelled()
    {
        return cancelled;
    }

    public int getEndPosition()
    {
        return endPosition;
    }

    public int getStartPosition()
    {
        return startPosition;
    }

    public UnderlineContainer getSlot()
    {
        return slot;
    }
}
