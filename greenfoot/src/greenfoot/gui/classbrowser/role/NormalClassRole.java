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
package greenfoot.gui.classbrowser.role;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;

/**
 * Role for a "normal" (non-Actor, non-World) class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 6216 2009-03-30 13:41:07Z polle $
 */
public class NormalClassRole extends ClassRole
{
    private static NormalClassRole instance;

    private String template = "stdclass.tmpl";
    
    /**
     * Get the (singleton) instance of NormalClassRole.
     */
    public static NormalClassRole getInstance()
    {
        if (instance == null) {
            instance = new NormalClassRole();
        }
        
        return instance;
    }
    
    private NormalClassRole()
    {
        // Nothing to do.
    }
    
    public void buildUI(ClassView classView, GClass gClass)
    {
        classView.setIcon(null);
        classView.setText(gClass.getQualifiedName());
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

    @Override
    public void remove()
    {
       //do nothing
    }

    
}
