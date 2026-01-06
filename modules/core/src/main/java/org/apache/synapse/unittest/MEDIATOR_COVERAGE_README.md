# Mediator Coverage Tracking for Synapse Unit Tests

## Overview

This implementation tracks mediator execution coverage during unit tests for WSO2 Synapse APIs and Sequences. It provides detailed coverage metrics showing which mediators were executed during test runs.

## Solution

### High-Level Approach

The mediator coverage tracking solution follows a **register-track-report** pattern:

1. **Registration Phase** (Deployment Time)
   - When APIs and Sequences are deployed for testing, all mediators are discovered and registered recursively
   - Each mediator receives a unique, hierarchical identifier (e.g., `api/HelloWorld/GET[/]/in/3.Filter/then/1.PayloadFactory`)
   - Conditional branches (Filter's then/else, Switch's cases) are explicitly registered to capture complete structure
   - Registration order is preserved using `LinkedHashMap` to maintain top-to-bottom configuration flow

2. **Tracking Phase** (Execution Time)
   - During test execution, `AbstractListMediator` hooks into the mediation flow
   - Each mediator marks itself as executed when invoked during unit tests
   - Tracking only occurs when `IS_RUNNING_AS_UNIT_TEST` flag is set, ensuring no overhead in production
   - Thread-safe tracking using `ConcurrentHashMap` allows parallel test execution

3. **Reporting Phase** (Post-Execution)
   - After all test cases in a suite complete, coverage report is generated
   - All registered mediators are listed with their execution status (`executed: true/false`)
   - Coverage percentage calculated: (executed mediators / total mediators) × 100
   - JSON report includes both per-artifact and overall coverage metrics
   - Tracker is cleared after report generation to ensure test suite isolation

### Key Design Decisions

- **Hierarchical Naming**: Human-readable IDs like `GET[/]/in/3.Filter/else/2.Log` instead of cryptic internal references
- **Explicit Branch Registration**: All conditional branches registered upfront, not just executed ones
- **Ordered Reporting**: Mediators listed in registration order matching configuration structure
- **Per-Suite Isolation**: Each test suite gets independent coverage tracking with no carryover
- **Execution Status Model**: Show all mediators with boolean flags rather than just listing executed ones

## Features

- **Recursive Mediator Tracking**: Tracks all mediators including nested ones in sequences, conditional branches, etc.
- **Per-Test-Suite Coverage**: Coverage is accumulated across all test cases **within a single test suite execution**
- **Test Suite Isolation**: Each test suite execution starts with a clean coverage tracker (no carryover between runs)
- **Detailed Reporting**: Includes executed vs total mediators, percentage, and identifiers for executed mediators
- **JSON Output**: Coverage report is included in the test suite summary as JSON

## Architecture

### Key Components

1. **MediatorCoverage** (`testcase/data/classes/MediatorCoverage.java`)
   - Data class holding coverage information for a single artifact
   - Stores artifact type, name, total/executed mediators, and mediator details

2. **MediatorCoverageTracker** (`MediatorCoverageTracker.java`)
   - Singleton tracker managing all coverage data
   - Registers artifacts and tracks mediator execution
   - Thread-safe implementation using ConcurrentHashMap

3. **AbstractListMediator** (modified)
   - Hooks into mediation flow to track execution
   - Calls tracker when mediators are executed during unit tests

4. **TestingAgent** (modified)
   - Registers artifacts for coverage tracking after deployment
   - Generates coverage report after test execution

5. **TestSuiteSummary** (modified)
   - Stores coverage data and generates JSON report
   - Includes coverage report in test results

## How It Works

### 1. Artifact Registration

When an API or Sequence is deployed for testing:
- The artifact is registered with `MediatorCoverageTracker`
- All mediators are recursively discovered and assigned unique identifiers
- Identifiers follow a hierarchical, human-readable format

#### Mediator ID Format

**For APIs:**
```
api/{apiName}/{method}[{uriTemplate}]/{sequenceType}/{position}.{MediatorType}[{context}]/{branch}/{position}.{MediatorType}
```

**For Sequences:**
```
seq/{sequenceName}/{position}.{MediatorType}[{context}]/{branch}/{position}.{MediatorType}
```

**Components:**
- `{apiName}` / `{sequenceName}`: Artifact name
- `{method}[{uriTemplate}]`: HTTP method + URI (e.g., `GET[/]`, `POST[/users/{id}]`)
- `{sequenceType}`: `in`, `out`, or `fault`
- `{position}`: Sequential number (1, 2, 3...) reset at each nesting level
- `.{MediatorType}`: Mediator type without "Mediator" suffix (e.g., `PayloadFactory`, `Log`, `Filter`)
- `[{context}]`: Optional context (property name, variable, etc.)
- `/{branch}/`: Branch path for conditionals (`/then/`, `/else/`, `/case[1]/`, `/default/`)

**Example API Identifiers:**
```
api/HelloWorld/GET[/]/in/1.PayloadFactory
api/HelloWorld/GET[/]/in/2.Log
api/HelloWorld/GET[/]/in/3.Filter
api/HelloWorld/GET[/]/in/3.Filter/then/1.PayloadFactory
api/HelloWorld/GET[/]/in/3.Filter/else/1.PayloadFactory
api/HelloWorld/GET[/]/in/3.Filter/else/2.Log
api/HelloWorld/GET[/]/in/4.Respond
api/HelloWorld/GET[/]/fault/1.Log
```

**Example Sequence Identifiers:**
```
seq/sample-seq/1.PayloadFactory
seq/sample-seq/2.Variable[var1]
seq/sample-seq/3.Switch
seq/sample-seq/3.Switch/case[1]/1.Log
seq/sample-seq/3.Switch/case[2]/1.Log
seq/sample-seq/3.Switch/default/1.Log
seq/sample-seq/4.Log
```

### 2. Execution Tracking

During test execution:
- Message context contains `COVERAGE_ARTIFACT_KEY` property with artifact identifier
- `AbstractListMediator` tracks both itself (when mediatorPosition == 0) and all child mediators
- `AbstractListMediator.trackMediatorExecution()` marks each mediator as executed
- Tracking only occurs when `IS_RUNNING_AS_UNIT_TEST` is true
- **Important**: Both container mediators (SequenceMediator) and their children are tracked
- **Conditional Mediators**: All branches are registered (FilterMediator's then/else, SwitchMediator's cases), but only executed branches are marked

### 3. Report Generation

After all test cases in the test suite complete:
- `TestingAgent.generateCoverageReport()` collects data from tracker
- Creates `MediatorCoverage` objects for each artifact
- Calculates coverage percentages
- Generates JSON report and adds to `TestSuiteSummary`
- Tracker is then cleared (`MediatorCoverageTracker.getInstance().clear()`) for the next test suite execution

## JSON Output Format

The coverage report is included in the test response under `mediatorCoverage`:

```json
{
  "testSuite": "HelloWorldTests",
  "artifacts": [
    {
      "artifactType": "API",
      "artifactName": "HelloWorld",
      "executedMediators": 5,
      "totalMediators": 8,
      "coveragePercentage": "62.50",
      "mediatorDetails": [
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/1.PayloadFactory",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/2.Log",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/3.Filter",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/3.Filter/then/1.PayloadFactory",
          "executed": false
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/3.Filter/else/1.PayloadFactory",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/3.Filter/else/2.Log",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/in/4.Respond",
          "executed": true
        },
        {
          "mediatorId": "api/HelloWorld/GET[/]/fault/1.Log",
          "executed": false
        }
      ]
    },
    {
      "artifactType": "Sequence",
      "artifactName": "sample-seq",
      "executedMediators": 4,
      "totalMediators": 7,
      "coveragePercentage": "57.14",
      "mediatorDetails": [
        {
          "mediatorId": "seq/sample-seq/1.PayloadFactory",
          "executed": true
        },
        {
          "mediatorId": "seq/sample-seq/2.Variable[var1]",
          "executed": true
        },
        {
          "mediatorId": "seq/sample-seq/3.Switch",
          "executed": true
        },
        {
          "mediatorId": "seq/sample-seq/3.Switch/case[1]/1.Log",
          "executed": true
        },
        {
          "mediatorId": "seq/sample-seq/3.Switch/case[2]/1.Log",
          "executed": false
        },
        {
          "mediatorId": "seq/sample-seq/3.Switch/default/1.Log",
          "executed": false
        },
        {
          "mediatorId": "seq/sample-seq/4.Log",
          "executed": false
        }
      ]
    }
  ],
  "overallCoverage": "60.00"
}
```

## Usage

The coverage tracking is automatic for unit tests. No additional configuration is needed. The coverage report will be included in the test response when tests are executed through the unittest framework.

### Understanding Mediator Details

Each mediator in the `mediatorDetails` array includes:
- **mediatorId**: Unique identifier for the mediator (e.g., `api/HelloWorld/GET[/]/in/1.PayloadFactory`)
- **executed**: Boolean flag indicating whether the mediator was executed during the test (`true` or `false`)

**Important: Mediator Order**
- Mediators are listed in **registration order** (top-to-bottom as they appear in the configuration)
- This makes it easy to follow the flow and identify gaps
- Order is preserved using `LinkedHashMap` internally

This format allows you to:
- ✅ See **all** mediators in your artifact (not just executed ones)
- ✅ **Ordered list** matching the configuration structure (top-to-bottom)
- ✅ Identify exactly which mediators were **not executed** during testing
- ✅ Understand which **branches** of conditional mediators (Filter, Switch) were tested
- ✅ Easily calculate coverage gaps by filtering for `"executed": false`

### Test Suite Execution Model

**Important**: Each test suite execution is **independent and isolated**:

1. **Test Suite Starts** → Coverage tracker is empty
2. **Artifacts Deployed** → Mediators are registered in the tracker
3. **Test Cases Execute** → Executed mediators are tracked and accumulated
4. **Coverage Report Generated** → Shows coverage for all test cases in this suite
5. **Test Suite Ends** → Coverage tracker is cleared for the next run

This means:
- ✅ Coverage **accumulates** across multiple test cases **within the same test suite**
- ❌ Coverage **does NOT accumulate** across separate test suite executions
- Each `runTestingAgent()` call = One independent test suite with its own coverage report

### Example: Multiple Test Cases in One Suite

If you have 3 test cases in one test suite, and:
- Test case 1 executes mediators A, B
- Test case 2 executes mediators B, C
- Test case 3 executes mediators C, D

The final coverage will show: **A, B, C, D all executed** (accumulated within that suite)

### Example Test Response

```json
{
  "deploymentStatus": "PASSED",
  "mediationStatus": "PASSED",
  "testCases": [...],
  "mediatorCoverage": {
    "testSuite": "UnitTestSuite",
    "artifacts": [...],
    "overallCoverage": "75.00"
  }
}
```

## Understanding Coverage for Conditional Mediators

### Example: API with FilterMediator

Consider an API with this structure:
```xml
<inSequence>
  <payloadFactory/>      <!-- Mediator 1 -->
  <log/>                 <!-- Mediator 2 -->
  <filter>               <!-- Mediator 3: FilterMediator container -->
    <then>
      <payloadFactory/>  <!-- Mediator 4: then branch -->
    </then>
    <else>
      <payloadFactory/>  <!-- Mediator 5: else branch -->
    </else>
  </filter>
  <respond/>             <!-- Mediator 6 -->
</inSequence>
```

**Registration (Deployment):**
- Total mediators registered: **6** (all mediators in both branches are counted)

**Test Case 1: Testing ONLY the "then" path**
- Executed: SequenceMediator, PayloadFactory#1, Log, FilterMediator, PayloadFactory#2 (then), Respond
- Executed count: **6 mediators**
- Coverage: **6/7 = 85.71%** (missing the else branch PayloadFactory)

**Test Case 2: Testing ONLY the "else" path**
- Executed: SequenceMediator, PayloadFactory#1, Log, FilterMediator, PayloadFactory#3 (else), Respond
- Executed count: **6 mediators**
- Coverage: **6/7 = 85.71%** (missing the then branch PayloadFactory)

**Test Case 3: Testing BOTH paths (2 separate test cases in same suite)**
- Executed: All 7 mediators across both test cases
- Coverage: **7/7 = 100%**

### Why This Matters

❌ **Without this logic**: Both then and else would not be registered, giving false 100% coverage  
✅ **With this logic**: Coverage accurately shows which branches were tested

## Test Suite Lifecycle and Coverage Tracking

### Complete Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Test Suite Execution 1 (e.g., sample-seq test suite)        │
├─────────────────────────────────────────────────────────────┤
│ 1. START: MediatorCoverageTracker is empty                  │
│ 2. Deploy artifacts → Register mediators                    │
│ 3. Execute test case 1 → Track executions                   │
│ 4. Execute test case 2 → Track executions (accumulate)      │
│ 5. Execute test case N → Track executions (accumulate)      │
│ 6. Generate coverage report → 100% coverage                 │
│ 7. Undeploy artifacts                                       │
│ 8. CLEAR: MediatorCoverageTracker.clear()                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Test Suite Execution 2 (e.g., HelloWorld API test suite)    │
├─────────────────────────────────────────────────────────────┤
│ 1. START: MediatorCoverageTracker is empty (fresh start)    │
│ 2. Deploy artifacts → Register mediators                    │
│ 3. Execute test case 1 → Track executions                   │
│ 4. Execute test case 2 → Track executions (accumulate)      │
│ 5. Execute test case N → Track executions (accumulate)      │
│ 6. Generate coverage report → 80% coverage                  │
│ 7. Undeploy artifacts                                       │
│ 8. CLEAR: MediatorCoverageTracker.clear()                   │
└─────────────────────────────────────────────────────────────┘
```

### Key Points

- **Each test suite execution** (`RequestHandler.runTestingAgent()`) is completely independent
- Coverage accumulates **within** a test suite but **resets between** test suites
- The two reports you see are from **two separate, sequential test suite executions**
- This design ensures:
  - Clean state for each test suite
  - No interference between different test suites
  - Accurate per-test-suite coverage metrics

## Implementation Details

### Mediator Identification

Each mediator gets a unique, hierarchical identifier that is human-readable and clearly shows the structure:

**Format Components:**
- **Position numbering**: Sequential (1, 2, 3...) reset at each nesting level
- **Mediator type**: Class name without "Mediator" suffix (e.g., `Log`, `PayloadFactory`, `Filter`)
- **Optional context**: Property names, variable names shown in brackets `[name]`
- **Branch paths**: Explicit paths like `/then/`, `/else/`, `/case[1]/`, `/default/`

**Examples:**

API mediators:
```
api/HelloWorld/GET[/]/in/1.PayloadFactory
api/HelloWorld/GET[/]/in/3.Filter/then/1.PayloadFactory
api/HelloWorld/POST[/users/{id}]/in/2.Property[userId]
```

Sequence mediators:
```
seq/sample-seq/1.PayloadFactory
seq/sample-seq/2.Variable[var1]
seq/sample-seq/3.Switch/case[1]/1.Log
```

**Benefits:**
- ✅ Human-readable and self-documenting
- ✅ Clear hierarchy with `/` separators
- ✅ Branch paths are explicit (no "AnonymousListMediator" noise)
- ✅ Position numbering resets at each level for clarity
- ✅ Sortable and easy to parse

### Coverage Calculation

- **Per Artifact**: (Executed Mediators / Total Mediators) × 100
- **Overall**: Sum of all executed mediators / Sum of all total mediators × 100

### Mediator Ordering

Mediators are stored and returned in **registration order** (top-to-bottom as they appear in configuration):

**Ordering Implementation:**
- Registration: `LinkedHashMap` stores mediators preserving insertion order
- Retrieval: `LinkedHashSet` maintains order from the map's keySet
- Coverage: `LinkedHashMap` preserves order in mediator execution status
- Output: JSON maintains the same order throughout

**Benefits:**
- ✅ Preserves the top-to-bottom flow as defined in the configuration
- ✅ Makes it easy to understand which mediators are missing coverage
- ✅ Maintains consistent ordering across multiple test runs
- ✅ Branches (then/else, cases) are registered in depth-first order

**Example order for a Filter mediator:**
1. `3.Filter` (the container)
2. `3.Filter/then/1.PayloadFactory` (then branch, first mediator)
3. `3.Filter/else/1.PayloadFactory` (else branch, first mediator)
4. `3.Filter/else/2.Log` (else branch, second mediator)

**Example order for a Switch mediator:**
1. `3.Switch` (the container)
2. `3.Switch/case[1]/1.Log` (case 1 mediators)
3. `3.Switch/case[2]/1.Log` (case 2 mediators)
4. `3.Switch/default/1.Log` (default case mediators)

### Conditional Branch Handling

**FilterMediator (if-then-else):**
- Registers: FilterMediator container + all "then" branch mediators + all "else" branch mediators
- Tracks: Only mediators in the executed branch(es)
- Example: If FilterMediator has 2 mediators in "then" and 1 in "else", total count = 1 (FilterMediator) + 2 (then) + 1 (else) = 4 mediators
- If only "then" path is tested: 3 executed / 4 total = 75% coverage

**SwitchMediator (switch-case):**
- Registers: SwitchMediator container + all case branch mediators + default case mediators
- Tracks: Only mediators in the executed case branch
- Example: Switch with 3 cases (2 mediators each) + default (1 mediator) = 1 + 6 + 1 = 8 mediators
- If only case 1 is tested: 3 executed / 8 total = 37.5% coverage

This approach ensures that:
- **Coverage percentage reflects actual mediator execution**, not just "some path was taken"
- **Low coverage indicates incomplete testing** of conditional logic branches
- **100% coverage requires testing all branches** of all conditional mediators

### Thread Safety

- Uses `ConcurrentHashMap` for thread-safe tracking
- `AtomicInteger` for index generation
- `ConcurrentHashMap.newKeySet()` for executed mediator sets

## Challenges and Solutions

### Challenge 1: API Resources Had Zero Coverage
**Problem**: HTTP-invoked APIs weren't setting the coverage artifact key in the message context, resulting in 0% coverage for all API resources.

**Root Cause**: Coverage tracking relied on `COVERAGE_ARTIFACT_KEY` being set in the message context, but `Resource.java` (which handles HTTP requests) wasn't setting this key before invoking sequences.

**Solution**: Modified `Resource.java` to set `COVERAGE_ARTIFACT_KEY = "API:" + apiName` in the message context before calling `sequence.mediate()`, ensuring API mediators are properly tracked during HTTP request processing.

---

### Challenge 2: Confusing and Verbose Mediator IDs
**Problem**: Original format like `seq/sample-seq/1:SequenceMediator/4:SwitchMediator/5:AnonymousListMediator/6:LogMediator` was:
- Too verbose and hard to read
- Exposed internal implementation details (AnonymousListMediator)
- Used global sequential indexing (1-11) making hierarchy unclear
- Lacked context about branches (then/else, case[1])

**Solution**: Redesigned to hierarchical, human-readable format:
- Position-based numbering: `1.PayloadFactory`, `2.Log`, `3.Filter` (reset at each level)
- Removed "Mediator" suffix: `Filter` instead of `FilterMediator`
- Explicit branch paths: `/then/`, `/else/`, `/case[1]/`, `/default/`
- API context: `GET[/]`, `POST[/users/{id}]` shows HTTP method and URI template
- Example: `api/HelloWorld/GET[/]/in/3.Filter/else/2.Log` clearly shows it's the 2nd mediator in the else branch

---

### Challenge 3: Inaccurate Coverage Percentages
**Problem**: Container mediators (SequenceMediator, FilterMediator) weren't being tracked as executed, leading to artificially low coverage percentages.

**Root Cause**: Only leaf mediators (Log, PayloadFactory, etc.) were tracked. Container mediators that hold child mediators weren't marking themselves as executed.

**Solution**: Modified `AbstractListMediator` to track itself when `mediatorPosition == 0`:
```java
if (mediatorPosition == 0) {
    trackMediatorExecution(this, synCtx); // Track container itself
}
```
This ensures both the container (e.g., `3.Filter`) and its children are counted.

---

### Challenge 4: Conditional Branches Not Fully Registered
**Problem**: FilterMediator and SwitchMediator were only registering the `then` branch (stored in inherited List), missing the `else` branch and switch cases.

**Root Cause**: FilterMediator stores:
- Then branch: in inherited `List<Mediator>` (registered by default)
- Else branch: in separate `AnonymousListMediator` (not traversed)

**Solution**: Added special handling in `registerMediatorRecursively()`:
- **FilterMediator**: Explicitly register both `/then/` and `/else/` branches
- **SwitchMediator**: Register all `/case[1]/`, `/case[2]/`, `/default/` branches
- Order matters: Check for Filter/Switch BEFORE generic ListMediator to avoid double-registration

---

### Challenge 5: Mediators Returned in Random Order
**Problem**: Mediators appeared in random order in JSON output, making it difficult to follow the configuration flow.

**Root Cause**: Using `HashMap` for storage and `HashSet` for retrieval, both of which don't preserve insertion order.

**Solution**: Three-level ordering fix:
1. **Storage**: Changed to `LinkedHashMap` in `registerAPI()` and `registerSequence()`
2. **Retrieval**: Changed `getAllMediatorIds()` to return `LinkedHashSet` instead of `HashSet`
3. **Coverage**: `MediatorCoverage` uses `LinkedHashMap` for execution status

Result: Mediators now appear in top-to-bottom registration order matching configuration structure.

---

### Challenge 6: Showing Only Executed Mediators vs. All Mediators
**Problem**: Initial design only showed executed mediators, making it impossible to identify which mediators were NOT executed without comparing against the configuration.

**Solution**: Changed response format from array of strings to array of objects:
```json
// OLD: Only executed mediators
"mediatorDetails": ["seq/sample-seq/1.PayloadFactory", "seq/sample-seq/2.Log"]

// NEW: All mediators with execution status
"mediatorDetails": [
  {"mediatorId": "seq/sample-seq/1.PayloadFactory", "executed": true},
  {"mediatorId": "seq/sample-seq/2.Variable", "executed": true},
  {"mediatorId": "seq/sample-seq/3.Switch/case[2]/1.Log", "executed": false}
]
```
This provides complete visibility into coverage gaps without needing the original configuration.

---

### Challenge 7: Supporting Artifacts Not Tracked (Known Limitation - Improvement Needed)
**Problem**: Only artifacts explicitly deployed in test suites are registered. Supporting artifacts invoked during execution (e.g., sequences called via `<sequence key="..."/>`) are not tracked.

**Impact**: 
- Incomplete coverage picture - missing transitively invoked sequences
- Cannot measure end-to-end coverage across all executed artifacts
- Example scenario:
  ```xml
  <api name="MyAPI">               <!-- Registered and tracked ✓ -->
    <inSequence>
      <sequence key="helper"/>     <!-- helper sequence NOT tracked ✗ -->
    </inSequence>
  </api>
  ```

**Proposed Solution** (Not Yet Implemented):
Implement **lazy registration on first execution**:

1. **Detect Supporting Artifacts**: When a `CallMediator` or sequence reference executes, check if the target is registered
2. **Auto-Register on Execution**: If not registered, register it dynamically before tracking execution
3. **Mark as Supporting**: Flag these artifacts as `"isSupporting": true` in the report
4. **Report Separately**: Show both main artifacts and supporting artifacts with clear distinction

**Implementation Additions Needed**:
- Add `isArtifactRegistered(artifactKey)` method to `MediatorCoverageTracker`
- Add `markAsSupportingArtifact(artifactKey)` to track artifact type
- Modify `AbstractListMediator.trackMediatorExecution()` to auto-register SequenceMediators
- Update `MediatorCoverage` to include `isSupporting` boolean field
- Update JSON output to show supporting artifacts distinctly

This enhancement would provide complete end-to-end coverage visibility across all executed artifacts, not just those explicitly deployed in the test suite.

## Files Modified

1. `/modules/core/src/main/java/org/apache/synapse/unittest/MediatorCoverageTracker.java` (NEW)
2. `/modules/core/src/main/java/org/apache/synapse/unittest/testcase/data/classes/MediatorCoverage.java` (NEW)
3. `/modules/core/src/main/java/org/apache/synapse/unittest/testcase/data/classes/TestSuiteSummary.java` (MODIFIED)
4. `/modules/core/src/main/java/org/apache/synapse/mediators/AbstractListMediator.java` (MODIFIED)
5. `/modules/core/src/main/java/org/apache/synapse/unittest/TestingAgent.java` (MODIFIED)
6. `/modules/core/src/main/java/org/apache/synapse/unittest/TestCasesMediator.java` (MODIFIED)
7. `/modules/core/src/main/java/org/apache/synapse/unittest/RequestHandler.java` (MODIFIED)
8. `/modules/core/src/main/java/org/apache/synapse/unittest/Constants.java` (MODIFIED)

## Current Limitations

### 1. Supporting Artifacts Not Tracked
**Problem**: Only artifacts explicitly deployed in the test suite are registered for coverage tracking. Supporting artifacts (e.g., sequences called via `<sequence key="..."/>` from APIs or other sequences) are not automatically registered, leading to incomplete coverage.

**Impact**: 
- If API calls a supporting sequence, that sequence's mediators are not tracked
- Coverage report only shows the main artifact, missing execution in supporting artifacts
- True end-to-end coverage cannot be measured

**Current Behavior**:
```xml
<!-- API is registered ✓ -->
<api name="MyAPI">
  <inSequence>
    <log/>
    <sequence key="supportingSequence"/>  <!-- This sequence is NOT registered ✗ -->
    <respond/>
  </inSequence>
</api>

<!-- Supporting sequence exists but not tracked -->
<sequence name="supportingSequence">
  <payloadFactory/>
  <log/>
</sequence>
```

**Proposed Solution**:
Implement **lazy registration on first execution**:

1. **During Registration Phase**: Register only explicitly deployed artifacts as before
2. **During Execution Phase**: 
   - When `CallMediator` or sequence reference is executed, check if target sequence is registered
   - If not registered, register it on-the-fly before execution
   - Mark it as "supporting artifact" in the report
3. **During Reporting Phase**: 
   - Report both main artifacts and supporting artifacts
   - Distinguish between explicitly tested artifacts and transitively invoked ones

**Implementation Approach**:
```java
// In AbstractListMediator or appropriate mediator
public void trackMediatorExecution(Mediator mediator, MessageContext synCtx) {
    if (synCtx.getProperty(Constants.IS_RUNNING_AS_UNIT_TEST) != null) {
        String artifactKey = (String) synCtx.getProperty(Constants.COVERAGE_ARTIFACT_KEY);
        
        // New: Auto-register supporting sequences on first execution
        if (mediator instanceof SequenceMediator) {
            SequenceMediator seqMediator = (SequenceMediator) mediator;
            String seqKey = "Sequence:" + seqMediator.getName();
            
            if (!tracker.isArtifactRegistered(seqKey)) {
                tracker.registerSequence(seqMediator);
                tracker.markAsSupportingArtifact(seqKey); // Mark as supporting
            }
        }
        
        tracker.markMediatorExecuted(mediator, artifactKey);
    }
}
```

**Expected JSON Output** (with supporting artifacts):
```json
{
  "testSuite": "MyAPITests",
  "artifacts": [
    {
      "artifactType": "API",
      "artifactName": "MyAPI",
      "isSupporting": false,
      "executedMediators": 3,
      "totalMediators": 3,
      "coveragePercentage": "100.00"
    },
    {
      "artifactType": "Sequence",
      "artifactName": "supportingSequence",
      "isSupporting": true,
      "executedMediators": 2,
      "totalMediators": 2,
      "coveragePercentage": "100.00"
    }
  ],
  "overallCoverage": "100.00"
}
```

---

### 2. Branch Identification in Reports
**Problem**: While all branches are tracked, the report doesn't explicitly show "then branch covered, else branch not covered" - it just shows which specific mediators were executed

**Impact**: You need to look at mediator IDs to understand which branch was taken (e.g., presence of else-path mediators in executed list)

## Future Enhancements

### High Priority

- **Supporting Artifacts Coverage** (See Challenge 7 above): Auto-register and track sequences/endpoints invoked during test execution via `<sequence key="..."/>` or `<call-template>`. This would provide complete end-to-end coverage including transitively invoked artifacts.

### Medium Priority

- **Per-test-case coverage**: Currently, coverage accumulates across all test cases in a suite. Future enhancement could track which test case executed which mediator.
- **Cross-suite coverage history**: Track coverage trends across multiple test suite executions
- **Enhanced branch coverage reporting**: Explicitly show "then: covered, else: not covered" for conditional mediators instead of just listing executed mediators
- **Branch coverage percentage**: Separate metric for "X out of Y branches covered" in addition to mediator coverage
- **HTML coverage report**: Generate visual HTML reports similar to code coverage tools with branch highlighting
- **CI/CD integration**: Automated coverage reporting in build pipelines
- **Coverage thresholds**: Fail builds if coverage falls below configured thresholds
- **Differential coverage**: Show coverage changes between commits/branches
