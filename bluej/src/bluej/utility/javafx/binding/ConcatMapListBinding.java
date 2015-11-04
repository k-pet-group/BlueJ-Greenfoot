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
package bluej.utility.javafx.binding;

import java.util.function.Function;
import java.util.stream.Stream;

import javafx.collections.ObservableList;

/**
 * Binds a destination list to the concatenation of some observable-list accessor
 * on items in an observable list.
 * 
 */
public class ConcatMapListBinding<DEST, SRC> extends DeepListBinding<DEST>
{
    private final ObservableList<SRC> src;
    private final Function<SRC, ObservableList<? extends DEST>> via;

    public static <DEST, SRC> void bind(ObservableList<DEST> dest, ObservableList<SRC> src, Function<SRC, ObservableList<? extends DEST>> via)
    {
        new ConcatMapListBinding<>(dest, src, via).startListening();
    }
    
    private ConcatMapListBinding(ObservableList<DEST> dest, ObservableList<SRC> src, Function<SRC, ObservableList<? extends DEST>> via)
    {
        super(dest);
        this.src = src;
        this.via = via;
    }

    @Override
    protected Stream<ObservableList<?>> getListenTargets()
    {
        return Stream.concat(Stream.of((ObservableList<?>)src), src.stream().map(via));
    }

    @Override
    protected Stream<DEST> calculateValues()
    {
        return src.stream().map(via).flatMap(ObservableList::stream);
    }

}
