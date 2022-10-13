/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2017,2020 Michael Kölling and John Rosenberg
 
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
package bluej.parser;

import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.pkgmgr.target.ClassTarget;
import bluej.stride.framedjava.ast.Parser;

@OnThread(Tag.FXPlatform)
public class LocalTypeCompletion extends AssistContent
{
    private final String type;
    
    private LocalTypeCompletion(ClassTarget ct)
    {
        this.type = ct.getIdentifierName();
    }

    // If it's an invalid name, null is returned
    public static AssistContent getCompletion(ClassTarget ct)
    {
        if (Parser.parseableAsNameDef(ct.getIdentifierName()))  {
            // Valid type name, fine to complete:
            return new LocalTypeCompletion(ct);
        }
        return null;
    }
    
    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return type;
    }

    @Override
    public String getType()
    {
        return null;
    }

    @Override
    public String getDeclaringClass()
    {
        return null;
    }

    @Override
    public CompletionKind getKind()
    {
        return CompletionKind.TYPE;
    }

    @Override
    public String getJavadoc()
    {
        return "";
    }
    
    @Override
    public List<ParamInfo> getParams()
    {
        // Can't have parameters, so we return null:
        return null;
    }

    @Override
    public Access getAccessPermission()
    {
        // TODO
        return null;
    }

    @Override
    public String getPackage()
    {
        return "";
    }
}
