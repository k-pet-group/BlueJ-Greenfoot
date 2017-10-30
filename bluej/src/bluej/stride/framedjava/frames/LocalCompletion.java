/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2017 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.frames;

import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.AssistContent;
import bluej.stride.framedjava.ast.Parser;

@OnThread(Tag.FXPlatform)
public class LocalCompletion extends AssistContent
{
    private final String type;
    private final String name;
    private final boolean formalParam;
    
    private LocalCompletion(String type, String name, boolean formalParam)
    {
        this.type = type;
        this.name = name;
        this.formalParam = formalParam; 
    }

    // If it's an invalid name, null is returned
    public static AssistContent getCompletion(String type, String name, boolean formalParam)
    {
        if (Parser.parseableAsNameDef(name))  {
            // Valid name, fine to complete (type doesn't need to be valid):
            return new LocalCompletion(type, name, formalParam);
        }
        return null;
    }
    
    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return name;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getDeclaringClass()
    {
        return null;
    }

    @Override
    public CompletionKind getKind()
    {
        return formalParam ? CompletionKind.FORMAL_PARAM : CompletionKind.LOCAL_VAR;
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
        // TODO Auto-generated method stub
        return null;
    }
}
