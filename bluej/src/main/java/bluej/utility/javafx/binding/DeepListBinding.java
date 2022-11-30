/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014, 2015 Michael Kölling and John Rosenberg 
 
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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * This class is useful when you want to bind one (observable) list to some function of another observable list
 * but you need to not only listen to changes in that list, but also further lists inside it. 
 * 
 */
public abstract class DeepListBinding<DEST>
{
    private final ObservableList<DEST> dest;
    private final MultiListener<ObservableList<?>> multiListener;
    
    public DeepListBinding(ObservableList<DEST> dest)
    {
        this.dest = dest;
        final ListChangeListener<Object> listener = c -> update();
        this.multiListener = new MultiListener<>(l -> { l.addListener(listener); return () -> l.removeListener(listener);});
    }
    
    public void startListening()
    {
        update();
    }

    protected abstract Stream<ObservableList<?>> getListenTargets();
    
    protected abstract Stream<DEST> calculateValues();

    protected void update()
    {
        // First, update all the listeners:
        multiListener.listenOnlyTo(getListenTargets());
        
        // Now alter our bound list:
        dest.setAll(calculateValues().collect(Collectors.toList()));
    }
}
