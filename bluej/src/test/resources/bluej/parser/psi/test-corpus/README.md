# Kotlin Test Corpus for PSI Visitor Validation

This directory contains a comprehensive test corpus of Kotlin source files for validating the PSI visitor implementation in Milestone 2.3.

## Purpose

These test files serve as input for validation testing, ensuring that:
- [`CallbackRecorder`](../../../../../../main/java/bluej/parser/psi/CallbackRecorder.java) correctly captures PSI visitor callbacks
- [`TraversalComparator`](../../../../../../main/java/bluej/parser/psi/TraversalComparator.java) accurately compares traversal sequences
- The PSI visitor infrastructure handles diverse Kotlin language constructs

## Directory Structure

```
test-corpus/
├── simple/          (10 files) - Basic Kotlin constructs
├── moderate/        (15 files) - Intermediate features
├── complex/         (14 files) - Advanced patterns
├── edge-cases/      (5 files)  - Special scenarios
└── README.md        (this file)
```

## Test Categories

### Simple (10 files)
Basic, single-concept Kotlin files for foundational validation:
- [`BasicClass.kt`](simple/BasicClass.kt) - Simple class with method
- [`DataClass.kt`](simple/DataClass.kt) - Data class declaration
- [`ObjectDeclaration.kt`](simple/ObjectDeclaration.kt) - Singleton object
- [`TopLevelFunctions.kt`](simple/TopLevelFunctions.kt) - Package-level functions
- [`PropertiesOnly.kt`](simple/PropertiesOnly.kt) - Properties with getters
- [`SimpleInterface.kt`](simple/SimpleInterface.kt) - Interface with default methods
- [`SimpleEnum.kt`](simple/SimpleEnum.kt) - Enum declarations
- [`CompanionObject.kt`](simple/CompanionObject.kt) - Companion object pattern
- [`ExtensionFunction.kt`](simple/ExtensionFunction.kt) - Extension functions
- [`LambdaExpression.kt`](simple/LambdaExpression.kt) - Lambda expressions

### Moderate (15 files)
Intermediate complexity demonstrating common Kotlin patterns:
- [`Inheritance.kt`](moderate/Inheritance.kt) - Class inheritance with override
- [`InterfaceImpl.kt`](moderate/InterfaceImpl.kt) - Multiple interface implementation
- [`PropertyAccessors.kt`](moderate/PropertyAccessors.kt) - Custom getters/setters
- [`PrimaryConstructor.kt`](moderate/PrimaryConstructor.kt) - Primary constructor with init
- [`SecondaryConstructor.kt`](moderate/SecondaryConstructor.kt) - Secondary constructors
- [`AbstractClass.kt`](moderate/AbstractClass.kt) - Abstract classes
- [`WhenExpression.kt`](moderate/WhenExpression.kt) - Sealed classes with when
- [`DelegatedProperties.kt`](moderate/DelegatedProperties.kt) - Property delegation
- [`DefaultParameters.kt`](moderate/DefaultParameters.kt) - Default parameter values
- [`NullableTypes.kt`](moderate/NullableTypes.kt) - Nullable type handling
- [`OperatorOverloading.kt`](moderate/OperatorOverloading.kt) - Operator overloading
- [`InfixFunction.kt`](moderate/InfixFunction.kt) - Infix function notation
- [`VarargParameters.kt`](moderate/VarargParameters.kt) - Vararg parameters
- [`DestructuringDeclarations.kt`](moderate/DestructuringDeclarations.kt) - Destructuring
- [`HigherOrderFunctions.kt`](moderate/HigherOrderFunctions.kt) - Higher-order functions

### Complex (14 files)
Advanced Kotlin features and patterns:
- [`NestedClasses.kt`](complex/NestedClasses.kt) - Nested and inner classes
- [`Generics.kt`](complex/Generics.kt) - Generic types and constraints
- [`Annotations.kt`](complex/Annotations.kt) - Annotation declarations and usage
- [`SealedClasses.kt`](complex/SealedClasses.kt) - Sealed class hierarchies
- [`TypeAliases.kt`](complex/TypeAliases.kt) - Type alias declarations
- [`InlineClasses.kt`](complex/InlineClasses.kt) - Value classes
- [`InlineFunctions.kt`](complex/InlineFunctions.kt) - Inline and reified functions
- [`Contravariance.kt`](complex/Contravariance.kt) - Variance annotations
- [`DelegationPattern.kt`](complex/DelegationPattern.kt) - Class delegation
- [`MultipleInheritance.kt`](complex/MultipleInheritance.kt) - Multiple interface inheritance
- [`DSLBuilder.kt`](complex/DSLBuilder.kt) - DSL-style builders
- [`Coroutines.kt`](complex/Coroutines.kt) - Suspend functions
- [`SmartCasts.kt`](complex/SmartCasts.kt) - Smart casting with sealed classes
- [`ExceptionHandling.kt`](complex/ExceptionHandling.kt) - Try-catch-finally

### Edge Cases (5 files)
Special scenarios and boundary conditions:
- [`EmptyFile.kt`](edge-cases/EmptyFile.kt) - Minimal valid file
- [`UnicodeIdentifiers.kt`](edge-cases/UnicodeIdentifiers.kt) - Unicode in identifiers
- [`CommentsOnly.kt`](edge-cases/CommentsOnly.kt) - Comments without code
- [`LongLineContent.kt`](edge-cases/LongLineContent.kt) - Very long identifiers/strings
- [`SpecialCharacters.kt`](edge-cases/SpecialCharacters.kt) - Escaped characters, backticks

## Usage in Tests

These files are designed to be loaded and parsed by test infrastructure:

```java
// Example test usage
File testFile = new File("test-corpus/simple/BasicClass.kt");
CallbackRecorder recorder = new CallbackRecorder();
// Parse file with PSI visitor
// Compare recorded callbacks against expected patterns
```

## Validation Strategy

1. **Baseline Recording**: Parse each file and record callback sequences
2. **Comparison Testing**: Compare sequences between different parser implementations
3. **Regression Testing**: Ensure consistent traversal across code changes
4. **Coverage Analysis**: Verify all Kotlin language constructs are represented

## File Count Summary

- **Total Files**: 44 Kotlin test files
  - Simple: 10 files
  - Moderate: 15 files
  - Complex: 14 files
  - Edge Cases: 5 files

## Notes

- All files use `package test.*` to avoid conflicts with production code
- Files are self-contained and compilable with standard Kotlin compiler
- Each file demonstrates specific language features for targeted testing
- Edge cases intentionally test parser robustness with unusual inputs

---

*Test corpus created for Milestone 2.3: Validation Infrastructure*
*Part of PSI Visitor Foundation implementation*