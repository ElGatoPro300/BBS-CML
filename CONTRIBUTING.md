# Contribution rules (BBS CML edition)

If you'd like to submit new features or changes to the BBS CML edition repository or submit a PR (pull request) to this repository, please carefully read following document. The failure to adhere to this document will result in immediate rejection of the features, commits or PR.

These rules might feel tyrannical, but they ensure that there is a proper order on the development of the project among all developers and that it stays maintainable for future years! Otherwise, there will come lots of conflicts between features and between the code, and the source code will turn into human-unmaintainable slop who will never support ever again (these kind of rules weren't enforced in Blockbuster mod, and look what happened: it was abandoned).

The following document has two main sections: ***Feature Contribution Rules*** and ***Code Contribution Rules***.

If you are using AI, you can let it handle the *Code Contribution Rules section*, but *Feature Contribution Rules* **must** be completely understood and fully read before you start suggesting, adding or developing any kind of feature, improvement or bugfix for the project.

# Feature Contribution Rules (FCR)

These are a couple of rules that every contributor **must** follow before uploading any changes to the project. Project Leaders may be exempt from those.

## General principles: a summary

- Keep in mind that here are multiple people working on this fork, so that is why a proper order and rules are needed to ensure that everything goes fine between all participants and contributions.

- Everyone should carefully think on what they are going to add/contribute to the mod before coding and all of their possible implications or relations with other features and parts of the mod. When we talk about development, **usually it takes a process of planning, design and <u>time</u> for thinking all possible functionalities and implications, specially if they (for some reason) modifies the current behaviour of the game or other existent features**.

- There should be a proper communication before adding anything new to the fork, mostly to ensure that new ideas doesn't introduce any inconsistencies with the rest of existing features and they get proper feedback and a solid design.

## List of feature contribution rules

Contribution rules will be categorized by the following importance order:

| Importance level | Meaning                                                                                                                                                                                                                                     |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 🟩Green🟩        | It is recommended to follow as a tip rather to be mandatory, but the application of these measures usually improves project management                                                                                                      |
| 🟨Yellow🟨       | Compilance of these rules could be required under certain circumstances, but generally not always.                                                                                                                                          |
| 🟧Orange🟧       | Compilance of these rules will be required most times, but maybe not on some specific circumstances.                                                                                                                                        |
| 🟥Red🟥          | Compilance will be mandatory for every contributor/developer with no exceptions. The failure to follow these rules will result as a decline of the proposed ideas/features or a temporal revert of those until they meets the requirements. |

In addition to the summary above of the previous secion and the previous legend of importance levels, here is the following list of rules that every developer and contributor **must** follow before uploading new features or changes to the project:

### Mandatory rules

The first one is the **MOST** important rules of all:

- 🟥**1.** New <u>features</u> or <u>improvements</u> must be previously communicated on the #[insertar nuevo canal] channel to all project leaders and developers **before adding any of them to the project**. This is to ensure that there is a general consensus among all developers where everyone makes sure that new ideas and features are solid enough or have a well-consistent design that does not break or affect other features, and also for discussing new feedback or needed improvements before adding them.

- 🟥**2.** When suggesting new features on the #[el nuevo canal de antes] channel, **you mustn't suggest more than 2-3 ideas/features at the same time or during the same beta period** (or posting multiple separate post as a "to-do" suggestion list). This is mostly to ensure that:
  
  1. Testers have enough <u>time</u> for properly testing all new features.
  
  2. Each possible bug introduced on new features can be spotted and properly fixed.
  
  3. Each beta/release/version doesn't get bloated with too many new features.
  
  Also, when suggesting a new idea or feature, you **must** explain:
  
  1. <u>What is the use of the new feature or what it does.</u>
  
  2. <u>Which problem(s) would solve and/or what does this new feature would apport to the mod or why it would be necessary to add to the mod/project.</u>
  
  If you want to separately list your future ideas, then you might want to use the existing #features thread channel, but keep in mind that **ideas or features listed on this channels won't count as valid until they are posted on the #[el nuevo canal de antes] channel and meets the requirements previously mentioned on this rule**.

- 🟥 **3.** When adding new features into the mod, you can add new UI elements or improve* them (after communicating the ideas and changes), but **they must preserve all existent UI elements from all features**, unless otherwise stated by project leaders with previous permission.
  
  *By improving, it means that you are allowed to change UI in a way that you consider that UI elements would be better optimized, **but not removing or hiding existing elements (sliders, buttons, inputs, tracks, etc...)**, mostly because:
  
  1. Removing and/or replacing existent elements and fields could potentially lead to a lot of bugs and inconsistences across the entire mod.
  
  2. Could affect other unrelated features where other contributors doesn't want those new changes.
  
  3. Could be some incompatibilities between older and new versions that, if not handled correctly, could even ruin proyects if the feature also isn't implemented correctly.
  
  4. Users could have issues when they come from older versions to new ones, where there could be difficulties understanding how the new UI is organized or how/why it works in a different way than before, **which could lead to consume more of their <u>time</u> for learning and understanding how to do the same things on new versions**, and therefore, to a possibility where they don't like anymore how the mod works (or even stop using the mod).

🟧There could be a few concrete cases where some exemptions could occur depending of the type of feature, but generally you may want to ask before implementing them.

- 🟥 **4.** The development (and addition of new features) **must follow the #dev-calendar channel**, where it will be indicated if new features can be added at the current time or only bugfixes would be allowed when entering a pre-release phase or "resting"/"testing" periods.

- 🟥 **5.** <u>Testing</u> **any** new additions, changes or bugfixes is required before uploading them to Github, mostly to ensure that they work as intended and don't introduce important bugs into the game.

- 🟥 **6.** Bugfixes will be accepted as long as they does not introduce more several bugs or any important bugs. If the bugfixes, for some reason, introduces way more bugs or those new ones are important/hard to fix, the main bugfix will be reverted temporarily until the situation is properly fixed.

- 🟥 **7.** You need to know all of the features that you will add or added to the latest version, in order to be possible to list all of them on the changelog of the next update.

### Important rules:

- 🟧 **8.** New changes (features, bugfixes, improvements, etc) **should be separatelly introduced on different commits** instead of generally grouping all changes into an one-time big commit, mostly to be as easy as possible to <u>identify</u> in which part of the git <u>history</u> certain parts of the code are found and for being able to locate them for future changes or corrections.

- 🟨 **9.** If you are thinking about adding a new big feature that is not necesarilly a need or a must-have for the main fork/project, <u>you should think about making it as an addon instead</u> (or a separate mod), **specially if it adds a considerable extra weight to the size of the mod .jar**. One objective of the mod is also to be as lightweight as possible, so if new features add too much weight, then making them as an addon could be a more viable alternative.

### Tips for contributing:

- 🟩 **10.** Usually on the development of applications or programs, users are the ones that decides if they use a program or not, and this is also why the application of the rules must be applied to ensure that the mod is as user-friendly as possible.

- 🟩 **11.** As the version control of project is managed on Github, it is recommended to understand (or ask) the basics of Github/Git.

- 🟩 **12.** A basic knowledge of programming would be ideal for contributing to the project even if all changes would be done through ai, but although it is not an actual requirement, understanding the basics of programming and how dev environments are managed could potentially help you to understand how these kind of contributibe projects works.

# Code Contribution Rules (CCR)

## General principles

- AI code is allowed as long as you adapted it, understood it, and tested it!
- No changes to `gradle` config (i.e. `gradle/`, `gradlew`, `gradlew.bat`, `gradle.properties`, and `build.gradle`)!

## Code style

Here is a sample of code that adheres to all of the code style rules of this project:

```java
public static void main(String[] args) 
{
    List<String> strings = new ArrayList<>();
    int a = 10;

    /* a is used instead of c, for some reason */
    for (int i = 0; i < a; i++)
    {
        strings.add(String.valueOf(i));
    }

    float x = 10F;
    float y = 15.5F;
    float d = findDistance(0F, 0F, x, y);

    System.out.println("Distance between " + x + " and " + y + " is " + d + " meters!");
}

/**
 * Given two 2D points, calculate the distance between them
 */
private static float findDistance(float x1, float y1, float x2, float y2)
{
    float dx = x2 - x1;
    float dy = y2 - y1;

    return Math.sqrt(dx * dx + dy * dy);
}
```

Here we have:

### Organization

- The structure of any class must have the following order:
  - Fields: constants, static, instance
  - Static constructor
  - Static methods
  - Instance constructors
  - Methods
  - Nested classes, interfaces, enums and records
- Any **dead code** (check for the name being gray in IDE, or references) must be deleted!
- One line constructions are allowed only **if they look very similar** (look harmonious).
- Multiple `if` constructions in a row must be separated with a new line to not mistake it for `else if`s!
- Blocks of variable definitions must be cluttered together (first go objects then primitives, unless it's impossible due to algorithm).
- Blocks of constructions, variable definitions, and method invocation must be separated by two new lines.
- Code repetition/duplication must be avoided! Make sure to check *Utils classes and JOML for any relevant methods.

### Formatting

- `{` is always on the next line.
- All instance method calls and field references must have `this`!
- Try to stay under 150 LOC methods. Refactor accordingly to keep method size small!

### Types

- No generic type provided where can be omitted (i.e. `new ArrayList<>()`).
- Float `1F`, double `2D` and long `3L` number specifiers must be in capital letters always, and if there is no decimal, period must be absent!
- No full references to the classes in the code (i.e. `new org.joml.Vector3f()`, unless there are conflicting names)!
- No `var`!

### Comments

- All comments must be in **English**!
- All comments in the body of the code must be within `/* ... */` and never with `//`.
- Self-explanatory comments must be avoided.
- JavaDocs comments must be present only above the method or class definition, but prior to any **annotations**!



# TLDR;

**Feature Contribution Rules** -> As long as you consult your ideas to the rest of people before adding them into the mod and wait until an approval is given, everything will be fine. Bugfixes and changes should also be properly implemented without adding more bugs.

**Code Contribution Rules** -> If you are using ai, tell it to use this as a coding guide and you should be fine. Otherwise, you must read this section.