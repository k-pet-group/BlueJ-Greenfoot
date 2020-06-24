/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2014,2015,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.parser.entity.*;
import bluej.parser.nodes.FieldNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.role.Kind;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeArrayClass;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Utilities for parsers.
 *
 * @author Davin McCall
 */
public class ParseUtils
{
    private static class DepthRef
    {
        int depth = 0;
    }

    /**
     * Interface for receiving assist content
     */
    public interface AssistContentConsumer
    {
        public void consume(AssistContent ac, boolean overridden);
    }


    /**
     * Get the possible code completions, based on the provided suggestions context.
     * If there are can be no valid completions in the given context, returns null.
     */
    @OnThread(Tag.FXPlatform)
    public static AssistContent[] getPossibleCompletions(ExpressionTypeInfo suggests, JavadocResolver javadocResolver, AssistContentConsumer consumer, ParsedNode currentPosNode)
    {
        GenTypeClass exprType = initGetPossibleCompletions(suggests);
        if (exprType != null)
        {
            List<AssistContent> completions = getCompletionsForTarget(exprType, suggests, javadocResolver, consumer, currentPosNode);
            return completions.toArray(new AssistContent[completions.size()]);
        }
        return null;
    }

    @OnThread(Tag.FXPlatform)
    public static List<AssistContentThreadSafe> getLocalTypes(Package pkg, Class<?> superType, Set<Kind> kinds)
    {
        return pkg.getClassTargets()
            .stream()
            .filter(ct -> {
                if (superType != null)
                {
                    ClassInfo info = ct.getSourceInfo().getInfoIfAvailable();
                    if (info == null)
                        return false;
                    // This code won't pick up the case where A extends B, and B has "superType"
                    // as a super type, but I'm not sure how we can easily tell that.
                    boolean hasSuperType = false;
                    hasSuperType |= superType.getName().equals(info.getSuperclass());
                    // Check interfaces:
                    hasSuperType |= info.getImplements().stream().anyMatch(s -> superType.getName().equals(s));
                    if (!hasSuperType)
                        return false;
                }

                if (ct.isInterface())
                    return kinds.contains(Kind.INTERFACE);
                else if (ct.isEnum())
                    return kinds.contains(Kind.ENUM);
                else
                    return kinds.contains(Kind.CLASS_FINAL) || kinds.contains(Kind.CLASS_NON_FINAL);
            })
            .map(ct -> new AssistContentThreadSafe(LocalTypeCompletion.getCompletion(ct)))
            .collect(Collectors.toList());
    }

    /**
     * Determine the target type for which members can be suggested (for code completion).
     * This utility method wraps primitives arrays as a suitable class type.
     *
     * @param suggests The code completion data
     * @return A suitable GenTypeClass representing the target type for completion
     * purposes, or null if there is no such suitable type.
     */
    public static GenTypeClass initGetPossibleCompletions(ExpressionTypeInfo suggests)
    {
        if (suggests != null)
        {
            GenTypeClass exprType = suggests.getSuggestionType().asClass();
            if (exprType == null)
            {
                final JavaType arrayComponent = suggests.getSuggestionType().getArrayComponent();
                if (arrayComponent != null && arrayComponent.isPrimitive())
                {
                    // Array of primitives:
                    // For code completion purposes, consider this as an array of object with a tweaked name:
                    exprType = new GenTypeArrayClass(new ParsedArrayReflective(new JavaReflective(Object.class), "Object")
                    {
                        @Override
                        @OnThread(Tag.Any)
                        public String getSimpleName()
                        {
                            return arrayComponent.toString() + "[]";
                        }

                    }, arrayComponent);
                }
                else
                {
                    return null;
                }
            }
            return exprType;
        }

        return null; // no completions
    }

    /**
     * Gets the available completions for a given target: methods and fields (not constructors)
     *
     * @param exprType        The target type from which to get completions.  This class and all super-types are scanned.
     * @param suggests        Information about the code suggestions
     * @param javadocResolver Resolver for fetching Javadoc
     * @param consumer        The consumer to be called with each AssistContent, if non-null (may be null)
     * @param currentPosNode  The node in which the editor cursors is currently in (significant node, with a name)
     *
     * @return The list of found completions.
     */
    @OnThread(Tag.FXPlatform)
    private static List<AssistContent> getCompletionsForTarget(GenTypeClass exprType, ExpressionTypeInfo suggests,
                                                               JavadocResolver javadocResolver, AssistContentConsumer consumer, ParsedNode currentPosNode)
    {
        GenTypeClass accessType = suggests.getAccessType();
        Reflective accessReflective = (accessType != null) ? accessType.getReflective() : null;

        // Use two sets, one to keep track of which types we have already processed,
        // another for individual methods.
        Set<String> contentSigs = new HashSet<String>();
        Set<String> typesDone = new HashSet<String>();
        List<AssistContent> completions = new ArrayList<AssistContent>();

        LinkedList<GenTypeClass> typeQueue = new LinkedList<GenTypeClass>();
        typeQueue.add(exprType);
        GenTypeClass origExprType = exprType;

        while (!typeQueue.isEmpty())
        {
            exprType = typeQueue.removeFirst();

            if (!typesDone.add(exprType.getReflective().getName()))
            {
                // we've already done this type...
                continue;
            }
            Map<String, Set<MethodReflective>> methods = exprType.getReflective().getDeclaredMethods();
            Map<String, GenTypeParameter> typeArgs = exprType.getMap();

            for (String name : methods.keySet())
            {
                Set<MethodReflective> mset = new HashSet<>(methods.get(name));
                mset.removeIf(method -> accessReflective != null
                    && !JavaUtils.checkMemberAccess(method.getDeclaringType(),
                    origExprType,
                    suggests.getAccessType().getReflective(),
                    method.getModifiers(), suggests.isStatic()));
                completions.addAll(discoverElements(exprType, javadocResolver, contentSigs,
                    typeArgs, mset, consumer));
                // Look for local variables of this method if matching the method found at the current location
                if (currentPosNode != null && currentPosNode instanceof MethodNode)
                {
                    MethodNode currentPosMethodNode = ((MethodNode) currentPosNode);
                    mset.forEach(methodReflective -> {
                        //Check if the methods' signatures are the same
                        if (methodReflective.getName().equals(currentPosMethodNode.getName())
                            && methodReflective.getParamTypes().size() == currentPosMethodNode.getParamTypes().size()
                            && methodReflective.getReturnType().asType().equals(currentPosMethodNode.getReturnType().getType())
                            && methodReflective.getParamTypes().equals(currentPosMethodNode.getParamTypes().stream().flatMap(javaEntity -> Stream.of(javaEntity.getType())).collect(Collectors.toList())))
                        {
                            // The signature are the same, we can save the local variable in the corrections list
                            // The variables are not at the MethodNode level, we need to dig into the MethodBodyLevel
                            Map<String, Set<FieldNode>> locVars = currentPosMethodNode.getLocVarNodes();
                            locVars.forEach((varName, fieldNodeSet) -> {
                                // Depth of set values should be 1...
                                FieldNode locVarFieldNode = fieldNodeSet.iterator().next();
                                JavaType type = locVarFieldNode.getFieldType().getType();
                                if (type != null)
                                {
                                    GenTypeParameter fieldType = type.getUpperBound();
                                    FieldCompletion completion = new FieldCompletion(fieldType.toString(true), varName,
                                            locVarFieldNode.getModifiers(), methodReflective.getDeclaringType().getName() + "."
                                            + methodReflective.getName());
                                    completions.add(completion);
                                }
                            });
                        }
                    });
                }
            }

            Map<String, FieldReflective> fields = exprType.getReflective().getDeclaredFields();
            for (String name : fields.keySet())
            {
                FieldReflective field = fields.get(name);
                if (accessReflective != null &&
                    !JavaUtils.checkMemberAccess(field.getDeclaringType(),
                        origExprType,
                        suggests.getAccessType().getReflective(),
                        field.getModifiers(), suggests.isStatic()))
                {
                    continue;
                }

                // Determine field type from expression type:
                Map<String, GenTypeParameter> declMap =
                    exprType.mapToSuper(field.getDeclaringType().getName()).getMap();
                GenTypeParameter fieldType = field.getType().mapTparsToTypes(declMap).getUpperBound();

                FieldCompletion completion = new FieldCompletion(fieldType.toString(true), field.getName(),
                    field.getModifiers(), field.getDeclaringType().getName());

                completions.add(completion);

                if (consumer != null)
                {
                    consumer.consume(completion, false);
                }
            }

            for (GenTypeClass stype : exprType.getReflective().getSuperTypes())
            {
                if (typeArgs != null)
                {
                    typeQueue.add(stype.mapTparsToTypes(typeArgs));
                }
                else
                {
                    typeQueue.add(stype.getErasedType());
                }
            }

            // Sort the completions by name
            Collections.sort(completions, (o1, o2) -> o1.getName().compareTo(o2.getName()));
            Reflective outer = exprType.getReflective().getOuterClass();
            if (outer != null)
            {
                typeQueue.add(new GenTypeClass(outer));
            }

        }
        return completions;
    }

    /**
     * Check whether the given methods should be added to the set of possible code completions (i.e. if they
     * have a unique signature), and do so if necessary. Returns a collection of AssistContent objects
     * representing any methods that were added (methods which were not added because they were already
     * present are not returned).
     *
     * @param gclass          The declaring class for the methods.
     * @param javadocResolver The Javadoc resolver used to look up Javadoc for the method.
     * @param contentSigs     The set of existing method signatures.  The newly-found method will be
     *                        added if and only if it is not already in the set.
     * @param typeArgs        The relevant type arguments (used for generic methods)
     * @param methods         The methods to be scanned.  Must all come from the same declaring class.
     * @param consumer        If non-null, it will be passed the completion (regardless of whether the completion
     *                        was already in the set, but the overridden flag will be passed accordingly to the
     *                        consumer)
     * @return If the method was added to the set (and was not already there), it is returned.
     * If the method was already in the set, null will be returned.
     */
    @OnThread(Tag.FXPlatform)
    private static Collection<AssistContent> discoverElements(GenTypeClass gclass,
                                                              JavadocResolver javadocResolver, Set<String> contentSigs,
                                                              Map<String, GenTypeParameter> typeArgs,
                                                              Collection<MethodReflective> methods, AssistContentConsumer consumer)
    {
        boolean resolveJavadoc = false;
        Set<MethodCompletion> completions = new HashSet<>();
        for (MethodReflective method : methods)
        {
            completions.add(new MethodCompletion(method, typeArgs, javadocResolver));
            resolveJavadoc |= (method.getJavaDoc() == null);
        }

        // Scan all methods for Javadoc in one go first (saves a lot of time):
        if (resolveJavadoc)
        {
            javadocResolver.getJavadoc(gclass.getReflective(), methods);
        }

        List<AssistContent> allNewMethods = new ArrayList<>();

        for (MethodCompletion completion : completions)
        {
            String sig = completion.getSignature();

            if (contentSigs.add(sig))
            {
                if (consumer != null)
                {
                    consumer.consume(completion, false /* not overridden */);
                }
                allNewMethods.add(completion);
            }
            else
            {
                if (consumer != null)
                {
                    consumer.consume(completion, true /* overridden */);
                }
                // Deliberately not added to allNewMethods
            }
        }
        return allNewMethods;
    }

    /**
     * Get an entity for an imported type specifier. This is different from a non-imported type
     * because in that it must be qualified.
     *
     * @param resolver Entity resolver which will (eventually) resolve the entity
     * @param tokens   The tokens making up the specification
     */
    @OnThread(Tag.FXPlatform)
    public static JavaEntity getImportEntity(EntityResolver resolver,
                                             Reflective querySource, List<LocatableToken> tokens)
    {
        if (tokens.isEmpty())
        {
            return null;
        }

        Iterator<LocatableToken> i = tokens.iterator();
        LocatableToken tok = i.next();
        if (tok.getType() != JavaTokenTypes.IDENT)
        {
            return null;
        }

        List<String> names = new LinkedList<String>();
        names.add(tok.getText());

        while (i.hasNext())
        {
            tok = i.next();
            if (tok.getType() != JavaTokenTypes.DOT || !i.hasNext())
            {
                return null;
            }
            tok = i.next();
            if (tok.getType() != JavaTokenTypes.IDENT)
            {
                return null;
            }
            names.add(tok.getText());
        }

        return new ImportedEntity(resolver, names, querySource);
    }

    /**
     * Get an entity for a type specification (specified by a list of tokens). The
     * returned entity might be unresolved.
     *
     * @param resolver    Entity resolver which will (eventually) resolve the entity
     * @param querySource The source of the query - a fully qualified class name
     * @param tokens      The tokens specifying the type
     */
    @OnThread(Tag.FXPlatform)
    public static JavaEntity getTypeEntity(EntityResolver resolver,
                                           Reflective querySource, List<LocatableToken> tokens)
    {
        DepthRef dr = new DepthRef();
        return getTypeEntity(resolver, querySource, tokens.listIterator(), dr);
    }

    /**
     * Get an entity for a type specification. The returned entity may be unresolved.
     * Returns null if the type specification appears to be invalid.
     */
    @OnThread(Tag.FXPlatform)
    private static JavaEntity getTypeEntity(EntityResolver resolver, Reflective querySource,
                                            ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        LocatableToken token = i.next();
        if (JavaParser.isPrimitiveType(token))
        {
            JavaType type = null;
            switch (token.getType())
            {
                case JavaTokenTypes.LITERAL_int:
                    type = JavaPrimitiveType.getInt();
                    break;
                case JavaTokenTypes.LITERAL_short:
                    type = JavaPrimitiveType.getShort();
                    break;
                case JavaTokenTypes.LITERAL_long:
                    type = JavaPrimitiveType.getLong();
                    break;
                case JavaTokenTypes.LITERAL_char:
                    type = JavaPrimitiveType.getChar();
                    break;
                case JavaTokenTypes.LITERAL_byte:
                    type = JavaPrimitiveType.getByte();
                    break;
                case JavaTokenTypes.LITERAL_boolean:
                    type = JavaPrimitiveType.getBoolean();
                    break;
                case JavaTokenTypes.LITERAL_double:
                    type = JavaPrimitiveType.getDouble();
                    break;
                case JavaTokenTypes.LITERAL_float:
                    type = JavaPrimitiveType.getFloat();
                    break;
                case JavaTokenTypes.LITERAL_void:
                    type = JavaPrimitiveType.getVoid();
            }

            while (i.hasNext())
            {
                token = i.next();
                if (token.getType() == JavaTokenTypes.LBRACK)
                {
                    type = type.getArray();
                    i.next();  // RBRACK
                }
                else
                {
                    return null;
                }
            }

            return new TypeEntity(type);
        }

        String text = token.getText();

        JavaEntity poc = UnresolvedEntity.getEntity(resolver, text, querySource);
        while (poc != null && i.hasNext())
        {
            token = i.next();
            if (token.getType() == JavaTokenTypes.LT)
            {
                // Type arguments
                poc = processTypeArgs(resolver, querySource, poc, i, depthRef);
                if (poc == null)
                {
                    return null;
                }
                if (!i.hasNext())
                {
                    return poc;
                }
                token = i.next();
            }
            if (token.getType() != JavaTokenTypes.DOT)
            {
                while (token.getType() == JavaTokenTypes.LBRACK)
                {
                    poc = new UnresolvedArray(poc);
                    if (i.hasNext())
                    {
                        token = i.next(); // RBRACK
                    }
                    if (!i.hasNext())
                    {
                        return poc;
                    }
                    token = i.next();
                }

                i.previous(); // allow token to be re-read by caller
                return poc;
            }
            token = i.next();
            if (token.getType() != JavaTokenTypes.IDENT)
            {
                break;
            }
            poc = poc.getSubentity(token.getText(), querySource);
        }

        return poc;
    }

    /**
     * Process tokens as type arguments
     *
     * @param base     The base type, i.e. the type to which the arguments are applied
     * @param i        A ListIterator to iterate through the tokens
     * @param depthRef The current argument depth; will be adjusted on return
     * @return A JavaEntity representing the type with type arguments applied (or null)
     */
    @OnThread(Tag.FXPlatform)
    private static JavaEntity processTypeArgs(EntityResolver resolver, Reflective querySource,
                                              JavaEntity base, ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        int startDepth = depthRef.depth;
        List<TypeArgumentEntity> taList = new LinkedList<TypeArgumentEntity>();
        depthRef.depth++;

        mainLoop:
        while (i.hasNext() && depthRef.depth > startDepth)
        {
            LocatableToken token = i.next();
            if (token.getType() == JavaTokenTypes.QUESTION)
            {
                if (!i.hasNext())
                {
                    return null;
                }
                token = i.next();
                if (token.getType() == JavaTokenTypes.LITERAL_super)
                {
                    JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                    if (taEnt == null)
                    {
                        return null;
                    }
                    taList.add(new WildcardSuperEntity(taEnt));
                }
                else if (token.getType() == JavaTokenTypes.LITERAL_extends)
                {
                    JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                    if (taEnt == null)
                    {
                        return null;
                    }
                    taList.add(new WildcardExtendsEntity(taEnt));
                }
                else
                {
                    taList.add(new UnboundedWildcardEntity(resolver));
                    i.previous();
                }
            }
            else
            {
                i.previous();
                JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                if (taEnt == null)
                {
                    return null;
                }
                taList.add(new SolidTargEntity(taEnt));
            }

            if (depthRef.depth <= startDepth)
            {
                // We've hit the closing '>' already
                break;
            }

            if (!i.hasNext())
            {
                return null;
            }
            token = i.next();
            int ttype = token.getType();
            while (ttype == JavaTokenTypes.GT || ttype == JavaTokenTypes.SR || ttype == JavaTokenTypes.BSR)
            {
                switch (ttype)
                {
                    case JavaTokenTypes.BSR:
                        depthRef.depth--;
                        // fall through to next case:
                    case JavaTokenTypes.SR:
                        depthRef.depth--;
                        // fall through to default case:
                    default:
                        depthRef.depth--;
                }
                if (!i.hasNext())
                {
                    break mainLoop;
                }
                token = i.next();
                ttype = token.getType();
            }

            if (ttype != JavaTokenTypes.COMMA)
            {
                i.previous();
                break;
            }
        }
        return base.setTypeArgs(taList);
    }
}
