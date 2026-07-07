---
id: bluej-testmgr
type: submodule-design
title: "Test Manager"
status: active
parent: bluej
---

# Test Manager

Unit test recording and replay system (14 files). Provides a UI for viewing test results and a recording subsystem that captures user interactions (object creation, method calls, inspections) as replayable JUnit test fixtures.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `TestDisplayFrame` | JavaFX frame displaying unit test results with run/debug controls |
| `InvokerRecord` (abstract) | Base class for recorded user interactions; tracks assertions and results |

---

## Record Sub-package

The `record/` sub-package contains concrete `InvokerRecord` implementations for each interaction type: `ConstructionInvokerRecord`, `MethodInvokerRecord`, `VoidMethodInvokerRecord`, `ExpressionInvokerRecord`, `GetInvokerRecord`, `ObjectInspectInvokerRecord`, `ClassInspectInvokerRecord`, `ArrayElementInspectorRecord`, `ArrayElementGetRecord`, `ExistingFixtureInvokerRecord`, `StatementInvokerRecord`.

---

## Dependencies

Uses: `debugger/` (DebuggerTestResult), `debugmgr/` (Invoker integration)
