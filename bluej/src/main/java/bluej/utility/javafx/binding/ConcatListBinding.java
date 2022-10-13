/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx.binding;

import java.util.stream.Stream;

import javafx.collections.ObservableList;

/**
 * Binds a destination list to the concatenation of an observable list of observable lists.
 * 
 */
public class ConcatListBinding<T> extends DeepListBinding<T>
{
    private final ObservableList<ObservableList<? extends T>> src;

    public static <T> void bind(ObservableList<T> dest, ObservableList<ObservableList<? extends T>> src)
    {
        new ConcatListBinding<>(dest, src).startListening();
    }
    
    private ConcatListBinding(ObservableList<T> dest, ObservableList<ObservableList<? extends T>> src)
    {
        super(dest);
        this.src = src;
    }

    @Override
    protected Stream<ObservableList<?>> getListenTargets()
    {
        return Stream.concat(Stream.of((ObservableList<?>)src), src.stream());
    }

    @Override
    protected Stream<T> calculateValues()
    {
        return src.stream().flatMap(ObservableList::stream);
    }

}
