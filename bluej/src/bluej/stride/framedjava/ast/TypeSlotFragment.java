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
package bluej.stride.framedjava.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.EmptyError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.errors.UnknownTypeError;
import bluej.stride.framedjava.errors.UnneededSemiColonError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TypeTextSlot;

public class TypeSlotFragment extends TextSlotFragment
{
    private TypeTextSlot slot;
    
    public TypeSlotFragment(String content, TypeTextSlot slot)
    {
        super(content);
        this.slot = slot;
    }
    
    public TypeSlotFragment(String content)
    {
        this(content, null);
    }

    @Override
    public String getJavaCode(Destination dest, ExpressionSlot<?> completing)
    {
        if (!dest.substitute() || (content != null && Parser.parseableAsType(content)))
            return content;
        else
            return "int";
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        if (content != null && content.isEmpty())
            return Stream.of(new EmptyError(this, "Type cannot be empty"));
        else if (content != null && content.endsWith(";"))
            // Must check this before general parse errors:
            return Stream.of(new UnneededSemiColonError(this, () -> getSlot().setText(content.substring(0, content.length() - 1))));
        else if (content == null || !Parser.parseableAsType(content))
            return Stream.of(new SyntaxCodeError(this, "Invalid type"));

        return Stream.empty();
    } 
    
    @Override
    public Future<List<CodeError>> findLateErrors(InteractionManager editor, CodeElement parent)
    {
        CompletableFuture<List<CodeError>> f = new CompletableFuture<>();
        
        // No point looking for a type that isn't syntactically valid:
        // Also, don't mess with arrays or generics:
        if (findEarlyErrors().count() > 0 || content.contains("[") || content.contains("<"))
        {
            f.complete(Collections.emptyList());
            return f;
        }
        
        editor.withTypes(types -> {
            
            for (AssistContentThreadSafe t : types)
            {
                if (t.getName().equals(content))
                {
                    // Match -- no error
                    f.complete(Collections.emptyList());
                    return;
                }
            }
            // Otherwise, give error and suggest corrections 
            f.complete(Arrays.asList(new UnknownTypeError(this, content, slot, editor, types.stream(), editor.getOtherPopularImports().stream())));
        });
        return f;
    }
    

    @Override
    public TextSlot<TypeSlotFragment> getSlot()
    {
        return slot;
    }

    @Override
    public void registerSlot(TextSlot slot)
    {
        if (this.slot == null)
            this.slot = (TypeTextSlot)slot;
    }
}
