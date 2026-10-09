# LostLink — Project Design Document

**Project type:** Console-based mini project  
**Language:** Core Java  
**Target build time:** 8 hours  
**Persistence:** Local text/CSV files  
**External dependencies:** None  
**Database / network / GUI:** Not required

---

## 1. Project Overview

LostLink is a console-based Lost & Found Management System for a campus or small community. Users can report lost belongings, register found items, search records, discover potential matches, submit ownership claims, and track an item's return.

The project should feel like a small, well-structured software system rather than a basic CRUD exercise. Its main innovation is an explainable matching engine that ranks possible lost/found pairs and shows why each candidate received its score.

### Core goals

- Run entirely in a terminal using Core Java.
- Use arrays and Java Collections meaningfully.
- Demonstrate classes, encapsulation, inheritance, and polymorphism.
- Demonstrate standard and custom exception handling.
- Save and reload records using local files.
- Provide useful matching, claim management, and summary statistics.
- Remain small enough to implement, integrate, and test in approximately eight hours with an AI coding agent.

### Scope boundaries

- No GUI, web server, database, external API, or third-party library.
- This is a local, single-user application. Staff review is simulated through a console workflow, not secure multi-user authentication.
- Match scores indicate similarity, not the probability of ownership.
- Ownership must not be approved automatically based only on a high match score.

---

## 2. Console Menu and User Experience

Display a clear numbered menu and return to it after each operation.

```text
====================================================
                    LOSTLINK
          Intelligent Lost & Found System
====================================================
1. Report a lost item
2. Report a found item
3. Find potential matches
4. Search all records
5. Browse lost and found items
6. Manage claims and returns
7. My reports
8. Statistics dashboard
9. Save data and exit
====================================================
Enter your choice:
```

### Menu functions

1. **Report a lost item:** Collect item name, category, color, last-seen location, date, description, reporter name, and contact details.
2. **Report a found item:** Collect item details, found location and date, reporter details, and optional identifying information that should not be displayed publicly.
3. **Find potential matches:** Select a lost report or scan eligible lost/found pairs; show the top three candidates with scores and reasons.
4. **Search all records:** Search by keyword, category, color, location, and status.
5. **Browse records:** List lost/found reports and optionally filter or sort them.
6. **Manage claims and returns:** Submit, review, approve, reject, and complete claims while enforcing valid status transitions.
7. **My reports:** Find reports by reporter name or ID and view or update eligible records.
8. **Statistics dashboard:** Show counts of lost reports, found reports, unresolved items, pending claims, returned items, and recovery rate.
9. **Save and exit:** Persist changes and close cleanly.

Invalid menu choices must display a helpful message and return to the menu without crashing the program.

---

## 3. Feature List

### 3.1 Must-have features

- Create lost-item reports.
- Create found-item reports.
- Generate unique IDs for items and claims.
- Validate required fields and dates.
- Search by partial, case-insensitive keywords.
- Filter by category, color, location, and status.
- Compare lost and found reports using a matching score.
- Rank candidates and explain each score.
- Submit and review ownership claims.
- Track claim status and item return status.
- Persist items and claims across application restarts.
- Recover gracefully from invalid input and file errors.
- Show a basic statistics dashboard.
- Record important events in a local activity log if time permits after the core workflow works.

### 3.2 Innovation features

#### Explainable match scoring

Use a weighted score out of 100:

| Criterion | Maximum points |
|---|---:|
| Category match | 30 |
| Name/description keyword overlap | 25 |
| Color match | 15 |
| Location proximity/similarity | 20 |
| Date proximity | 10 |
| **Total** | **100** |

These weights are configurable design choices, not statistically validated probabilities.

Implementation notes:
- Normalize text by trimming whitespace and comparing case-insensitively.
- Award partial points for keyword overlap and location similarity where practical.
- Compare dates using `LocalDate` and a clearly documented date window.
- Return a list of reasons for each score, such as `Category matched (+30)` or `Color matched (+15)`.
- Use a configurable minimum score to filter weak candidates.
- Never label the score as a probability of ownership.

#### Match ranking

- Sort candidates by descending score.
- Use deterministic tie-breaking, such as report date and then item ID.
- Display the top three eligible candidates.
- Exclude returned items and otherwise ineligible records.

#### Ownership verification workflow

- A claimant references a found-item ID and supplies identifying information.
- Keep sensitive identifying details out of public search results.
- A simulated staff/operator reviews the claim.
- Approve only after comparing the claim against the recorded details.
- Do not approve automatically because of a match score.

#### Statistics dashboard

Show:
- Total lost reports.
- Total found reports.
- Unresolved reports.
- Pending claims.
- Returned items.
- Recovery rate, with a defined denominator and safe handling when the denominator is zero.
- Most common categories and locations, if time permits.

#### Activity history (optional if time is short)

Record events such as item creation, claim submission, claim review, rejection, approval, and return in `data/activity.log`.

### 3.3 Optional features — only after core tests pass

- Sort reports by date, item name, or score.
- Flag reports that have remained unresolved for a configurable number of days.
- Add sample/demo records through an explicit demo-data option.
- Export a plain-text summary.
- Undo the last supported status change.

---

## 4. Architecture and Class Design

Use a small layered design. Keep model/entity classes separate from services, persistence, and console interaction.

### 4.1 Proposed project structure

```text
LostLink/
├── src/
│   ├── Main.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Student.java
│   │   ├── Staff.java
│   │   ├── Item.java
│   │   ├── LostItem.java
│   │   ├── FoundItem.java
│   │   ├── Claim.java
│   │   └── MatchResult.java
│   ├── service/
│   │   ├── ItemService.java
│   │   ├── MatchingService.java
│   │   └── ClaimService.java
│   ├── storage/
│   │   └── FileStorage.java
│   ├── exception/
│   │   ├── InvalidItemException.java
│   │   ├── ItemNotFoundException.java
│   │   └── InvalidClaimException.java
│   └── util/
│       └── InputHelper.java
├── data/
│   ├── items.csv
│   ├── claims.csv
│   └── activity.log
└── README.md
```

The agent may simplify the package layout if needed to stay within the time limit, but must preserve the separation of responsibilities and all mandatory Java concepts.

### 4.2 Class responsibilities

| Class | Responsibility |
|---|---|
| `Main` | Entry point, menu display, and coordination of user input |
| `User` | Abstract base class with common user fields and abstract `displayRole()` |
| `Student` | Student-specific user subtype |
| `Staff` | Operator/reviewer subtype |
| `Item` | Abstract base class for common item fields and abstract `getItemType()` |
| `LostItem` | Last-seen location and lost-report details |
| `FoundItem` | Found location and optional private identifying details |
| `Claim` | Claim ID, item ID, claimant information, claim date, status, and review details |
| `MatchResult` | Candidate pair, numeric score, and matching-reason list |
| `ItemService` | Create, find, filter, browse, and update item records |
| `MatchingService` | Compare records, calculate scores, explain scores, and rank matches |
| `ClaimService` | Submit and review claims and enforce claim/item status transitions |
| `FileStorage` | Load and save items and claims and append activity events |
| `InputHelper` | Reusable validated console input methods |

Suggested enums:
- `ItemStatus`: `OPEN`, `CLAIM_PENDING`, `RETURNED`, `CLOSED`
- `ClaimStatus`: `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`
- `ItemCategory`: use a small practical set such as `ELECTRONICS`, `BAGS`, `IDENTIFICATION`, `BOOKS`, `CLOTHING`, `ACCESSORIES`, `OTHER`

Adjust states only if needed to make transitions consistent. Avoid storing status as arbitrary free-form strings.

### 4.3 OOP requirements

- Encapsulation: private fields and controlled access through constructors/getters/setters as appropriate.
- Inheritance: `LostItem extends Item`, `FoundItem extends Item`, `Student extends User`, and `Staff extends User`.
- Polymorphism: override `getItemType()` in item subclasses and `displayRole()` in user subclasses.
- Abstraction: make `Item` and `User` abstract if their abstract methods are implemented and used.
- Use meaningful constructors and validation; do not create empty classes solely to satisfy a checklist.

---

## 5. Mandatory Core Java Requirements

These are non-negotiable and must appear in working code.

### 5.1 Arrays

Use arrays meaningfully in addition to collections. Examples:
- `String[]` for menu labels or fixed category choices.
- `Item[]` as a bounded temporary array for displaying a limited number of matches.
- Traverse the array using a loop.

Do not merely declare an unused array.

### 5.2 Collections

Use collections in actual workflows:
- `ArrayList<Item>` for item records.
- `ArrayList<Claim>` for claims.
- `HashMap<String, Item>` for fast item lookup by ID, if it can be kept synchronized with the item list.
- `ArrayList<MatchResult>` for candidate matches, sorted by score.

Avoid redundant collections unless their purpose is clear. If both a list and map are maintained, update them together in service methods.

### 5.3 Exception handling

Use standard exception handling for:
- Non-numeric menu input (`InputMismatchException` or safer string parsing).
- Invalid dates (`DateTimeParseException`).
- File read/write failures (`IOException`).
- Missing item or claim IDs.
- Invalid state transitions.

Define and use custom exceptions:
- `InvalidItemException`
- `ItemNotFoundException`
- `InvalidClaimException`

Catch exceptions at a suitable layer, display understandable messages, and let the user continue after recoverable errors. Use try-with-resources for file readers and writers. Avoid empty catch blocks and avoid using exceptions for ordinary branching.

---

## 6. Matching Algorithm

For each lost item, compare it against eligible found items.

### Algorithm outline

1. Get the selected lost item or all eligible lost reports.
2. Collect found reports that are not returned/closed.
3. Compare category, name/description keywords, color, location, and dates.
4. Calculate the weighted score from 0 to 100.
5. Build an explanation list containing the reasons and awarded points.
6. Filter candidates below the configured minimum score.
7. Sort by score descending; break ties deterministically.
8. Display the top three candidates with IDs, item summaries, scores, and explanations.
9. Let the user submit a claim if appropriate; never automatically return an item.

### Pseudocode

```text
findMatches(lostItem):
    results = empty list

    for each foundItem in all items:
        if foundItem is eligible:
            score, reasons = compare(lostItem, foundItem)
            if score >= minimumScore:
                add MatchResult(foundItem, score, reasons)

    sort results by score descending
    break ties consistently
    return first three results
```

A matching result is only a suggestion. A human/operator must verify ownership before the item is returned.

---

## 7. Claim and Item State Management

Use explicit status transitions so records cannot enter inconsistent states.

### Example claim flow

```text
Found item reported
       |
Claim submitted
       |
Operator reviews claim
       |
       +---- insufficient evidence ----> Rejected
       |
       +---- more information needed --> Pending review
       |
       +---- ownership verified -------> Approved
                                            |
                                       Handover occurs
                                            |
                                         Completed
                                            |
                                     Item marked RETURNED
```

Rules:
- A claim can be submitted only for an eligible found item.
- Only a pending claim can be approved or rejected.
- A rejected claim cannot be completed directly.
- A completed claim must correspond to an item that is marked returned.
- A returned item cannot receive another active claim.
- If a claim is rejected, the found item should become available for another claim unless another valid claim is pending.
- Keep state updates in service methods so the console layer cannot bypass validation.

The project simulates staff review; it does not provide secure account authentication.

---

## 8. File Persistence

Use standard Java file I/O with CSV or a documented delimiter-based format. If CSV is used, correctly escape commas, quotes, and line breaks. If a custom delimiter is used, escape the delimiter and newlines.

### Files

- `data/items.csv`: lost/found item fields and status.
- `data/claims.csv`: claim fields and status.
- `data/activity.log`: optional append-only human-readable event history.

### Persistence requirements

- Load existing records at startup.
- Create missing files or initialize empty collections.
- Save changes after important operations and on exit.
- Use try-with-resources.
- Validate records when loading and handle malformed lines without crashing.
- Do not overwrite all existing data when adding a single record.
- Keep ID generation safe from collisions by checking existing IDs or using a robust sequence.
- Document the file format and recovery behavior in `README.md`.

Use only JDK classes such as `Files`, `Path`, `BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter`, or `LocalDate`.

---

## 9. Error Handling and Validation

Validate:
- Required text fields are not blank.
- Dates use a documented format such as `yyyy-MM-dd`.
- Menu choices are within the supported range.
- IDs exist before an operation is attempted.
- IDs are unique.
- Categories and statuses use supported values.
- Claim transitions are legal.
- File records have the expected number of fields.

On a recoverable error:
1. Catch the relevant exception at an appropriate boundary.
2. Show a concise, actionable message.
3. Preserve existing valid data.
4. Return to the current menu or main menu safely.

Never silently discard a save failure or silently replace a corrupted data file.

---

## 10. Eight-Hour Implementation Plan

| Time | Task | Deliverable |
|---|---|---|
| Hour 1 | Project skeleton, models, enums, inheritance, custom exceptions | Project compiles with the model layer |
| Hour 2 | Console menus, reporting, IDs, arrays, collections | Lost and found records can be created |
| Hour 3 | Search, filtering, browsing, sorting | Search and listing work |
| Hour 4 | Matching score, explanations, ranking | Top-three match results work |
| Hour 5 | Claims, review workflow, status transitions | Claims and return workflow work |
| Hour 6 | File persistence and robust exception handling | Data survives restarts |
| Hour 7 | Integration and tests | Main workflows and edge cases pass |
| Hour 8 | Polish, sample data, README, demo preparation | Project is ready to demonstrate |

### Time-saving rules

- Ask the coding agent to implement one phase at a time.
- Compile and test after each phase before moving forward.
- Keep a known-good version or use version control if available.
- Prioritize a working end-to-end workflow over optional features.
- Defer activity logging, export, undo, and advanced analytics if the core is not stable.
- Do not remove arrays, collections, inheritance, exception handling, or persistence to save time.

---

## 11. Testing Plan

The coding agent must create and/or execute a clear set of tests. JUnit is optional only if permitted; manual console test cases are sufficient for the basic build.

### Functional tests

- Create a lost item.
- Create a found item.
- Search using a partial keyword with different letter casing.
- Filter by category and location.
- Create a matching lost/found pair and confirm the expected score/reasons.
- Confirm match results are sorted in descending order.
- Submit a claim for an existing found item.
- Reject a claim and confirm the item remains available.
- Approve a valid claim and complete the handover.
- Confirm returned items no longer appear as eligible matches.

### Edge cases

- Empty item list.
- Invalid menu input such as letters or unsupported numbers.
- Blank required fields.
- Invalid dates.
- Unknown item/claim ID.
- Duplicate ID.
- Malformed file row.
- Missing data directory or data file.
- File read/write error.
- Attempted invalid claim transition.
- Restart application and verify records persist.
- Equal match scores and deterministic tie-breaking.

---

## 12. Definition of Done

The project is complete only when all of the following are true:

- [ ] Application compiles and launches with a standard JDK.
- [ ] Entire user interface runs in the console.
- [ ] Lost and found reports can be created and assigned unique IDs.
- [ ] Arrays and Java Collections are both used meaningfully.
- [ ] Classes, encapsulation, inheritance, abstraction, and overriding are demonstrated.
- [ ] Standard and custom exceptions are used in relevant workflows.
- [ ] Search and filtering work.
- [ ] Matching produces scores, explanations, and ranked candidates.
- [ ] Claims follow valid status transitions.
- [ ] Items and claims survive a program restart.
- [ ] Missing or malformed data does not crash startup.
- [ ] README documents compile/run commands, features, file format, and limitations.
- [ ] The agent reports the actual test results and any known remaining issues.

---

## 13. README and Run Instructions

The final `README.md` should include:
- Project description and objectives.
- Core Java requirements demonstrated.
- Feature list.
- Folder structure.
- JDK version used.
- Exact compilation and run commands.
- Data-file format and persistence behavior.
- Sample demonstration scenario.
- Testing checklist.
- Limitations: local single-user storage, simulated staff review, and heuristic matching.

If packages are used, compile from the project root with the appropriate source paths or use a simple JDK build command. Do not add a build tool unless it materially helps and remains within scope.

---

## 14. AI Coding-Agent Execution Protocol

Use this document as the source of truth for the coding agent.

For every phase, require the agent to:
1. State the files it plans to create or modify.
2. Implement only the current phase while preserving earlier working features.
3. Compile the project using the available JDK.
4. Run relevant tests or provide reproducible manual test steps.
5. Fix compile/runtime errors before continuing.
6. Summarize completed features, test results, and known issues.
7. Avoid adding external libraries, databases, GUI code, web servers, or APIs.

### Suggested phase order

1. Models, enums, inheritance, and custom exceptions.
2. Console menu, reporting, arrays, and collections.
3. Search, matching, and ranking.
4. Claims and state transitions.
5. Persistence, validation, integration testing, and README.

**Final target:** A polished, fully local Core Java console application that demonstrates the required programming concepts, provides explainable lost/found matching, manages ownership claims, and persists records—within a deliberately limited eight-hour scope.
