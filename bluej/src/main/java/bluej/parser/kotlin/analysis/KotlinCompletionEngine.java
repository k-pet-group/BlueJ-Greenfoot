/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.StandardNames;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.kotlin.descriptors.DescriptorVisibilityUtils;
import org.jetbrains.kotlin.descriptors.FindClassInModuleKt;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.PropertiesConventionUtilKt;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier;
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Computes Kotlin code completion suggestions, following BlueJ's Java completion closely: the
 * members of the receiver after a dot, and a curated list (locals, members of {@code this}, project
 * functions and a small teaching set) when nothing precedes the caret.
 */
@OnThread(Tag.Worker)
public final class KotlinCompletionEngine
{
    // Inserted at the caret before analysis, so that incomplete code such as "x." parses as a member access.
    static final String PLACEHOLDER = "BlueJCompletionPlaceholder";

    // Standard library functions offered when nothing precedes the caret, in place of Java's System.out shortcut.
    private static final List<String> TEACHING_FUNCTIONS = List.of("println", "print", "readln", "listOf",
        "mutableListOf", "repeat", "maxOf", "minOf");

    /**
     * The prefix of the Greenfoot static methods offered when Greenfoot is imported.
     */
    public static final String GREENFOOT_PREFIX = "Greenfoot.";

    private static final FqName GREENFOOT_PACKAGE = new FqName("greenfoot");
    private static final FqName GREENFOOT_CLASS = new FqName("greenfoot.Greenfoot");
    private static final FqName JAVA_DEPRECATED = new FqName("java.lang.Deprecated");
    // Marks extensions that Kotlin prefers over members of the same name (e.g. forEach).
    private static final FqName HIDES_MEMBERS = new FqName("kotlin.internal.HidesMembers");

    private final KotlinAnalysis analysis;
    private final BindingContext bindingContext;
    private final int placeholderOffset;
    // Candidates by signature: a declaration reachable in several ways is offered once, first way wins.
    private final Map<String, KotlinCompletionCandidate> candidates = new LinkedHashMap<>();
    private LexicalScope scope;
    private DeclarationDescriptor scopeOwner;
    private Collection<DeclarationDescriptor> scopeCallables;

    private KotlinCompletionEngine(KotlinAnalysis analysis, int placeholderOffset)
    {
        this.analysis = analysis;
        this.bindingContext = analysis.bindingContext();
        this.placeholderOffset = placeholderOffset;
    }

    /**
     * Start computing completions at a caret position.
     *
     * @param service the project's analysis service
     * @param request the request for the file being edited, with its current text
     * @param caret   the caret offset in that text
     * @return a future result, completed as described by {@link KotlinAnalysisService#submit}. Its
     *         value is null if nothing can be completed at the caret: in a comment, string, type,
     *         import or declaration name, or after a receiver whose type cannot be resolved.
     */
    @OnThread(Tag.Any)
    public static CompletableFuture<KotlinCompletionResult> complete(KotlinAnalysisService service,
                                                                     KotlinAnalysisRequest request, int caret)
    {
        String text = request.targetText();
        int identifierStart = caret;
        while (identifierStart > 0 && Character.isJavaIdentifierPart(text.charAt(identifierStart - 1))) {
            identifierStart--;
        }
        int replaceStart = identifierStart;
        String withPlaceholder = text.substring(0, caret) + PLACEHOLDER + text.substring(caret);
        return service.submit(request.withTargetText(withPlaceholder), analysis -> {
            List<KotlinCompletionCandidate> found = new KotlinCompletionEngine(analysis, caret).complete();
            return found == null ? null : new KotlinCompletionResult(replaceStart, found);
        });
    }

    // Null if there is no completion context at the placeholder.
    private List<KotlinCompletionCandidate> complete()
    {
        PsiElement leaf = analysis.targetFile().findElementAt(placeholderOffset);
        KtSimpleNameExpression name = PsiTreeUtil.getParentOfType(leaf, KtSimpleNameExpression.class, false);
        if (name == null || !name.getReferencedName().contains(PLACEHOLDER) || !isExpressionPosition(name)) {
            return null;
        }
        scope = findScope(name);
        if (scope == null) {
            return null;
        }
        scopeOwner = scope.getOwnerDescriptor();

        // With the caret just before an argument list, the placeholder is the callee of a call.
        KtExpression expression = name.getParent() instanceof KtCallExpression call && call.getCalleeExpression() == name
            ? call : name;
        if (expression.getParent() instanceof KtQualifiedExpression qualified && qualified.getSelectorExpression() == expression) {
            if (!completeQualified(qualified.getReceiverExpression())) {
                return null;
            }
        }
        else {
            completePlain();
        }
        return List.copyOf(candidates.values());
    }

    // Types, imports, package names and callable references are not completed (nor are their Java equivalents).
    private static boolean isExpressionPosition(KtSimpleNameExpression name)
    {
        return !(name.getParent() instanceof KtUserType)
            && !(name.getParent() instanceof KtCallableReferenceExpression)
            && PsiTreeUtil.getParentOfType(name, KtImportDirective.class, KtPackageDirective.class) == null;
    }

    private LexicalScope findScope(PsiElement element)
    {
        for (PsiElement e = element; e != null; e = e.getParent()) {
            if (e instanceof KtElement ktElement) {
                LexicalScope found = bindingContext.get(BindingContext.LEXICAL_SCOPE, ktElement);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // --- After a dot ---

    // False if the receiver cannot be resolved.
    private boolean completeQualified(KtExpression receiver)
    {
        Qualifier qualifier = bindingContext.get(BindingContext.QUALIFIER, receiver);
        if (qualifier instanceof ClassQualifier classQualifier) {
            completeClassQualified(classQualifier.getDescriptor());
            return true;
        }
        KotlinType type = bindingContext.getType(receiver);
        if (type == null || KotlinTypeKt.isError(type)) {
            return false;
        }
        // Members are offered after "?." and also after "." on a nullable receiver.
        addReceiver(TypeUtils.makeNotNullable(type), true, Set.of(), null);
        // Smart casts are not recorded for the unresolved placeholder, so ask the data flow directly.
        DataFlowValue value = new DataFlowValueFactoryImpl(analysis.languageVersionSettings())
            .createDataFlowValue(receiver, type, bindingContext, scopeOwner);
        for (KotlinType stable : BindingContextUtilsKt.getDataFlowInfoBefore(bindingContext, receiver)
                .getStableTypes(value, analysis.languageVersionSettings())) {
            addReceiver(TypeUtils.makeNotNullable(stable), true, Set.of(), null);
        }
        return true;
    }

    // "ClassName.": like Java's static-only completion -- statics, companion/object members and enum entries.
    private void completeClassQualified(ClassDescriptor classDescriptor)
    {
        for (DeclarationDescriptor d : allIn(classDescriptor.getStaticScope(), DescriptorKindFilter.ALL)) {
            addMemberOrEnumEntry(d);
        }
        for (DeclarationDescriptor d : allIn(classDescriptor.getUnsubstitutedInnerClassesScope(), DescriptorKindFilter.CLASSIFIERS)) {
            addMemberOrEnumEntry(d);
        }
        ClassDescriptor object = DescriptorUtils.isObject(classDescriptor) ? classDescriptor
            : classDescriptor.getCompanionObjectDescriptor();
        if (object != null) {
            addTypeMembers(object.getDefaultType(), false, Set.of(), Set.of(), null);
        }
    }

    private void addMemberOrEnumEntry(DeclarationDescriptor descriptor)
    {
        if (descriptor instanceof CallableDescriptor callable) {
            addCallable(callable, "");
        }
        else if (DescriptorUtils.isEnumEntry(descriptor) && isOffered(descriptor)) {
            add(CandidateFactory.enumEntry((ClassDescriptor) descriptor));
        }
    }

    // --- Nothing before the caret ---

    private void completePlain()
    {
        // Locals first, so that member properties they hide can be recognised.
        Set<String> localNames = new HashSet<>();
        for (DeclarationDescriptor d : ScopeUtilsKt.collectDescriptorsFiltered(scope, DescriptorKindFilter.ALL,
                MemberScope.Companion.getALL_NAME_FILTER(), false)) {
            // Classifiers are never offered: Java completion does not offer class names either.
            if (!(d instanceof CallableDescriptor callable)) {
                continue;
            }
            DeclarationDescriptor container = callable.getContainingDeclaration();
            if (container instanceof CallableDescriptor) {
                // Local variables, parameters (including a lambda's "it") and local functions
                addCallable(callable, "");
                if (callable instanceof VariableDescriptor) {
                    localNames.add(callable.getName().asString());
                }
            }
            else if (container instanceof PackageFragmentDescriptor) {
                // The project's own top-level functions and properties; library ones only via the teaching set
                if (callable.getExtensionReceiverParameter() == null && isProjectSource(callable)) {
                    addCallable(callable, "");
                }
            }
            else if (container instanceof ClassDescriptor && DescriptorUtils.isStaticDeclaration(callable)) {
                // Java statics of the class and its supertypes can be used unqualified
                addCallable(callable, "");
            }
        }

        boolean innermost = true;
        for (ReceiverParameterDescriptor receiver : ScopeUtilsKt.getImplicitReceiversHierarchy(scope)) {
            // As in Java, a member property hidden by a local is offered as "this.name" (innermost receiver only).
            addReceiver(receiver.getType(), false, localNames, innermost ? "this." : null);
            innermost = false;
        }

        addTeachingFunctions();
        if (importsGreenfoot()) {
            addGreenfootStatics();
        }
    }

    private void addTeachingFunctions()
    {
        for (String functionName : TEACHING_FUNCTIONS) {
            // Of overloads with the same parameter names (println(Int), println(Any?), ...), offer the most general.
            Map<String, FunctionDescriptor> mostGeneral = new LinkedHashMap<>();
            for (FunctionDescriptor function : ScopeUtilsKt.collectFunctions(scope, Name.identifier(functionName),
                    NoLookupLocation.FROM_IDE)) {
                // Only the top-level functions themselves, not extensions sharing the name (CharSequence.repeat).
                if (function.getExtensionReceiverParameter() != null || !isOffered(function)) {
                    continue;
                }
                String shape = function.getValueParameters().stream().map(p -> p.getName().asString())
                    .collect(Collectors.joining(","));
                FunctionDescriptor best = mostGeneral.get(shape);
                if (best == null || acceptsArgumentsOf(function, best) && !acceptsArgumentsOf(best, function)) {
                    mostGeneral.put(shape, function);
                }
            }
            mostGeneral.values().forEach(function -> addCallable(function, ""));
        }
    }

    // True if every parameter of "general" accepts what the corresponding parameter of "specific" does.
    private static boolean acceptsArgumentsOf(FunctionDescriptor general, FunctionDescriptor specific)
    {
        for (int i = 0; i < general.getValueParameters().size(); i++) {
            KotlinType generalType = generalize(general.getValueParameters().get(i).getType(), general);
            KotlinType specificType = generalize(specific.getValueParameters().get(i).getType(), specific);
            if (!TypeUtilsKt.isSubtypeOf(specificType, generalType)) {
                return false;
            }
        }
        return true;
    }

    // As in Java, Greenfoot's static methods are offered as "Greenfoot.name" when Greenfoot is imported.
    private boolean importsGreenfoot()
    {
        for (KtImportDirective directive : analysis.targetFile().getImportDirectives()) {
            FqName imported = directive.getImportedFqName();
            if (imported != null && imported.equals(directive.isAllUnder() ? GREENFOOT_PACKAGE : GREENFOOT_CLASS)) {
                return true;
            }
        }
        return false;
    }

    private void addGreenfootStatics()
    {
        ClassDescriptor greenfoot = FindClassInModuleKt.findClassAcrossModuleDependencies(
            DescriptorUtils.getContainingModule(scopeOwner), ClassId.topLevel(GREENFOOT_CLASS));
        if (greenfoot != null) {
            for (DeclarationDescriptor d : allIn(greenfoot.getStaticScope(), DescriptorKindFilter.FUNCTIONS)) {
                if (d instanceof FunctionDescriptor function) {
                    addCallable(function, GREENFOOT_PREFIX);
                }
            }
        }
    }

    // --- Members and extensions of a receiver type ---

    // Add a receiver type's members and the extensions applicable to it. Library extensions are only
    // offered after a dot; otherwise just the project's own. Member properties hidden by a local name are
    // given shadowedPrefix (e.g. "this."), or omitted if it is null.
    private void addReceiver(KotlinType type, boolean libraryExtensions, Set<String> shadowedNames, String shadowedPrefix)
    {
        List<CallableDescriptor> extensions = new ArrayList<>();
        Set<String> hiddenMembers = new HashSet<>();
        for (DeclarationDescriptor d : scopeCallables()) {
            if (d instanceof CallableDescriptor callable && callable.getExtensionReceiverParameter() != null
                    && (libraryExtensions || isProjectSource(callable)) && acceptsReceiver(callable, type)) {
                extensions.add(callable);
                if (annotations(callable).hasAnnotation(HIDES_MEMBERS)) {
                    hiddenMembers.add(callable.getName().asString());
                }
            }
        }
        Set<String> memberShapes = addTypeMembers(type, true, hiddenMembers, shadowedNames, shadowedPrefix);
        for (CallableDescriptor extension : extensions) {
            // A member always wins over an extension with the same signature, so such extensions cannot be called.
            if (hiddenMembers.contains(extension.getName().asString()) || !memberShapes.contains(shape(extension))) {
                addCallable(extension, "");
            }
        }
    }

    // Add a type's members; returns the shapes (see shape()) of all its members, offered or not.
    private Set<String> addTypeMembers(KotlinType type, boolean includeAnyMembers, Set<String> hiddenMembers,
                                       Set<String> shadowedNames, String shadowedPrefix)
    {
        Set<String> memberShapes = new HashSet<>();
        List<CallableDescriptor> members = new ArrayList<>();
        // Java getters are used as Kotlin properties: getWorld() as world, isTouching... stays a method.
        Map<String, FunctionDescriptor> getters = new HashMap<>();
        for (DeclarationDescriptor d : allIn(type.getMemberScope(), DescriptorKindFilter.CALLABLES)) {
            if (d instanceof CallableDescriptor callable) {
                memberShapes.add(shape(callable));
            }
            if (d instanceof CallableDescriptor callable && !hiddenMembers.contains(callable.getName().asString())
                    && (includeAnyMembers || !isDeclaredInAny(callable))) {
                members.add(callable);
                Name property = callable instanceof FunctionDescriptor function ? javaGetterPropertyName(function) : null;
                if (property != null) {
                    getters.putIfAbsent(property.asString(), (FunctionDescriptor) callable);
                }
            }
        }

        for (CallableDescriptor member : members) {
            FunctionDescriptor function = member instanceof FunctionDescriptor f ? f : null;
            Name getterProperty = function == null ? null : javaGetterPropertyName(function);
            if (function != null && isSetterOfGetterProperty(function, getters)) {
                // Assigning the property replaces the setter (rotation = 90 rather than setRotation(90)).
                continue;
            }
            boolean property = getterProperty != null || !(member instanceof FunctionDescriptor);
            String name = getterProperty != null ? getterProperty.asString() : member.getName().asString();
            String prefix = "";
            if (property && shadowedNames.contains(name)) {
                if (shadowedPrefix == null) {
                    continue;
                }
                prefix = shadowedPrefix;
            }
            if (getterProperty != null) {
                if (isOffered(member)) {
                    add(CandidateFactory.javaGetterProperty(function, name, prefix));
                }
            }
            else {
                addCallable(member, prefix);
            }
        }
        return memberShapes;
    }

    // Name and parameter count: an approximation of a signature, enough to spot extensions hidden by members.
    private static String shape(CallableDescriptor callable)
    {
        return callable.getName().asString() + "/" + (callable instanceof FunctionDescriptor ? callable.getValueParameters().size() : -1);
    }

    // The property name a Java getter can be used as, or null if this is not a Java getter.
    private static Name javaGetterPropertyName(FunctionDescriptor function)
    {
        KotlinType returnType = function.getReturnType();
        if (!(CandidateFactory.original(function) instanceof JavaMethodDescriptor) || !function.getValueParameters().isEmpty()
                || !function.getTypeParameters().isEmpty() || function.getExtensionReceiverParameter() != null
                || DescriptorUtils.isStaticDeclaration(function) || returnType == null || KotlinBuiltIns.isUnit(returnType)) {
            return null;
        }
        return PropertiesConventionUtilKt.propertyNameByGetMethodName(function.getName());
    }

    // True for a Java setter whose parameter matches the type of a getter shown as a property.
    private static boolean isSetterOfGetterProperty(FunctionDescriptor function, Map<String, FunctionDescriptor> getters)
    {
        if (!(CandidateFactory.original(function) instanceof JavaMethodDescriptor) || function.getValueParameters().size() != 1
                || DescriptorUtils.isStaticDeclaration(function)) {
            return false;
        }
        KotlinType parameterType = FlexibleTypesKt.lowerIfFlexible(function.getValueParameters().getFirst().getType());
        for (Name property : PropertiesConventionUtilKt.propertyNamesBySetMethodName(function.getName())) {
            FunctionDescriptor getter = getters.get(property.asString());
            if (getter != null && getter.getReturnType() != null && KotlinTypeChecker.DEFAULT.equalTypes(
                    FlexibleTypesKt.lowerIfFlexible(getter.getReturnType()), parameterType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsReceiver(CallableDescriptor extension, KotlinType type)
    {
        return TypeUtilsKt.isSubtypeOf(type, generalize(extension.getExtensionReceiverParameter().getType(), extension));
    }

    // Replace the callable's own type parameters in a type by what they may stand for: a bare type
    // parameter (T.let) by its upper bound, a type argument (List<T>) by a star projection.
    private static KotlinType generalize(KotlinType type, CallableDescriptor owner)
    {
        List<TypeParameterDescriptor> typeParameters = owner.getTypeParameters();
        if (typeParameters.isEmpty()) {
            return type;
        }
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor typeParameter
                && typeParameters.contains(typeParameter)) {
            type = TypeUtilsKt.getRepresentativeUpperBound(typeParameter);
        }
        Map<TypeConstructor, TypeProjection> starProjections = new HashMap<>();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            starProjections.put(typeParameter.getTypeConstructor(), TypeUtils.makeStarProjection(typeParameter));
        }
        KotlinType substituted = TypeSubstitutor.create(starProjections).substitute(type, Variance.INVARIANT);
        return substituted != null ? substituted : TypeUtilsKt.replaceArgumentsWithStarProjections(type);
    }

    // --- Adding candidates ---

    private void addCallable(CallableDescriptor callable, String prefix)
    {
        if (isOffered(callable)) {
            add(CandidateFactory.callable(callable, prefix));
        }
    }

    private void add(KotlinCompletionCandidate candidate)
    {
        String signature = candidate.prefix() + candidate.name() + (candidate.parameters() == null ? ""
            : candidate.parameters().stream().map(KotlinCompletionCandidate.Parameter::type)
                .collect(Collectors.joining(",", "(", ")")));
        candidates.putIfAbsent(signature, candidate);
    }

    // Visible from the caret, not deprecated, and not a compiler-internal or placeholder name.
    private boolean isOffered(DeclarationDescriptor descriptor)
    {
        Annotations annotations = annotations(descriptor);
        return !descriptor.getName().isSpecial()
            && !descriptor.getName().asString().contains(PLACEHOLDER)
            && !annotations.hasAnnotation(StandardNames.FqNames.deprecated)
            && !annotations.hasAnnotation(JAVA_DEPRECATED)
            && (!(descriptor instanceof DeclarationDescriptorWithVisibility withVisibility)
                || DescriptorVisibilityUtils.isVisibleIgnoringReceiver(withVisibility, scopeOwner,
                    analysis.languageVersionSettings()));
    }

    private Collection<DeclarationDescriptor> scopeCallables()
    {
        if (scopeCallables == null) {
            scopeCallables = ScopeUtilsKt.collectDescriptorsFiltered(scope, DescriptorKindFilter.CALLABLES,
                MemberScope.Companion.getALL_NAME_FILTER(), false);
        }
        return scopeCallables;
    }

    private static Collection<DeclarationDescriptor> allIn(MemberScope memberScope, DescriptorKindFilter filter)
    {
        return memberScope.getContributedDescriptors(filter, MemberScope.Companion.getALL_NAME_FILTER());
    }

    private static boolean isProjectSource(CallableDescriptor callable)
    {
        return DescriptorToSourceUtils.descriptorToDeclaration(CandidateFactory.original(callable)) instanceof KtDeclaration;
    }

    private static boolean isDeclaredInAny(CallableDescriptor callable)
    {
        return CandidateFactory.original(callable).getContainingDeclaration() instanceof ClassDescriptor container
            && KotlinBuiltIns.isAny(container);
    }

    private static Annotations annotations(DeclarationDescriptor descriptor)
    {
        return CandidateFactory.original(descriptor).getAnnotations();
    }
}
