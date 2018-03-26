/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.views;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * A View Filter specifying whether, for a given method,
 * it should be included in an operation.  The two configurable
 * parts are static methods or instance methods, and public-only
 * vs package-private&protected&public.
 */
@OnThread(Tag.FXPlatform)
public final class ViewFilter implements Predicate<MemberView>
{
    public static enum StaticOrInstance { STATIC, INSTANCE; }
    
    private final StaticOrInstance staticOrInstance;
    // Show only items callable from another class in the given
    // package.  If null, only show public items.
    private final String callingPackage;

    /**
     * Create a View filter.
     * @param staticOrInstance Do you want only-static, or only-instance?
     * @param callingPackage Show only items callable from another class in the given package.
     *                       If null, only show public items. 
     */
    public ViewFilter(StaticOrInstance staticOrInstance, String callingPackage)
    {
        this.staticOrInstance = staticOrInstance;
        this.callingPackage = callingPackage;
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public boolean test(MemberView member)
    {
        boolean wantStatic = staticOrInstance == StaticOrInstance.STATIC;
        boolean isStatic = member.isStatic();
        // We either want static or instance, member must match that:
        if (wantStatic != isStatic)
            return false;
        
        // If public, definitely in:
        if ((member.getModifiers() & Modifier.PUBLIC) != 0)
            return true;
        // If it's not the same package (and already know it's not public), can't include it:
        boolean samePackage = member.getDeclaringView().getPackageName().equals(callingPackage);
        if (!samePackage)
            return false;
        // Otherwise, same package, so we include if not private:
        return (member.getModifiers() & Modifier.PRIVATE) == 0;
    }
}
