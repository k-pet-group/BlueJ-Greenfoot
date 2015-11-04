/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.generic;

import java.util.function.Function;

public class DefaultFrameFactory<T extends Frame> implements FrameFactory<T>
{
    private final Class<T> cls;
    private final Function<InteractionManager, T> create;

    /**
     * You typically use this in a static getFactory() method as follows:
     * 
     * public static FrameFactory<BlankFrame> getFactory()
     * {
     *     return new DefaultFrameFactory<>(BlankFrame.class, BlankFrame::new);
     * }
     */
    public DefaultFrameFactory(Class<T> cls, Function<InteractionManager, T> create)
    {
        this.cls = cls;
        this.create = create;
    }
    
    @Override
    public T createBlock(InteractionManager editor)
    {
        return create.apply(editor);
    }

    @Override
    public Class<T> getBlockClass()
    {
        return cls;
    }

}
