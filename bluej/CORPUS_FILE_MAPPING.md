# Test Corpus File Mapping - Test Expectations vs Actual Files

## Missing Files That Need Creation

These test expectations have NO matching corpus file:

1. **testTailrecFunction** - expects "Tailrec" → ❌ NO FILE
2. **testScopeFunctions** - expects "Scope" → ❌ NO FILE  
3. **testLateinitProperty** - expects "Lateinit" or "Late" → ❌ NO FILE
4. **testMultiplatformExpectActual** - expects "Multiplatform" or "Expect" → ❌ NO FILE
5. **testClassWithMethods** - expects "Method" → ❌ NO FILE
6. **testSingleLineFile** - expects "SingleLine" or "OneLine" → ❌ NO FILE
7. **testContracts** - expects "Contract" → ❌ NO FILE
8. **testBackingField** - expects "Backing" or "Field" → ❌ NO FILE
9. **testInitBlock** - expects "Init" → ❌ NO FILE
10. **testReifiedTypeParameters** - expects "Reified" → ❌ NO FILE

## File Name Mismatches That Need Filter Updates

These test filter patterns don't match existing file names:

1. **testSimpleFunction** - expects "SimpleFunction" → actual: `TopLevelFunctions.kt`
2. **testSimpleProperty** - expects "SimpleProperty" → actual: `PropertiesOnly.kt`
3. **testDeeplyNested** - expects "Deep" or "Nested" → actual: `NestedClasses.kt` (should match "Nested")
4. **testSealedClass** - expects "Sealed" (simple) → actual: `SealedClasses.kt` (complex) (should match)
5. **testSealedClassHierarchy** - expects "SealedHierarchy" → actual: `SealedClasses.kt`
6. **testCompanionObject** - expects "Companion" → actual: `CompanionObject.kt` (should match)
7. **testDestructuringDeclarations** - expects "Destructuring" → actual: `DestructuringDeclarations.kt` (should match)
8. **testHigherOrderFunctions** - expects "HigherOrder" → actual: `HigherOrderFunctions.kt` (should match)
9. **testVarianceAnnotations** - expects "Variance" → actual: `Contravariance.kt` (partial match?)
10. **testLambdaExpressions** - expects "Lambda" → actual: `LambdaExpression.kt` (should match)
11. **testAnnotationClass** - expects "Annotation" → actual: `Annotations.kt` (should match)

## Actual Corpus Inventory (45 files)

### simple/ (10 files)
- BasicClass.kt
- CompanionObject.kt
- DataClass.kt
- ExtensionFunction.kt
- LambdaExpression.kt
- ObjectDeclaration.kt
- PropertiesOnly.kt
- SimpleEnum.kt
- SimpleInterface.kt
- TopLevelFunctions.kt

### moderate/ (15 files)
- AbstractClass.kt
- DefaultParameters.kt
- DelegatedProperties.kt
- DestructuringDeclarations.kt
- HigherOrderFunctions.kt
- InfixFunction.kt
- Inheritance.kt
- InterfaceImpl.kt
- NullableTypes.kt
- OperatorOverloading.kt
- PrimaryConstructor.kt
- PropertyAccessors.kt
- SecondaryConstructor.kt
- VarargParameters.kt
- WhenExpression.kt

### complex/ (15 files)
- Annotations.kt
- Contravariance.kt
- Coroutines.kt
- DelegationPattern.kt
- DSLBuilder.kt
- ExceptionHandling.kt
- Generics.kt
- InlineClasses.kt
- InlineFunctions.kt
- MultipleInheritance.kt
- NestedClasses.kt
- SealedClasses.kt
- SmartCasts.kt
- TypeAliases.kt

### edge-cases/ (6 files)
- CommentsOnly.kt
- EmptyFile.kt
- LongLineContent.kt
- SpecialCharacters.kt
- StringTemplates.kt
- UnicodeIdentifiers.kt

## Recommended Fix Strategy

**Option A**: Update test filters to match actual files (RECOMMENDED)
**Option B**: Rename corpus files to match test expectations
**Option C**: Create stub files for missing expectations

Recommend Option A as it requires minimal changes and corpus files have good descriptive names.