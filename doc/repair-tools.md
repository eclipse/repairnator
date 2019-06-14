# Program repair tools

This page describes the program repair tools that have been implemented in Repairnator and the different strategies that can be used for each of them.

## NPEFix

[NPEFix](https://github.com/Spirals-Team/npefix) is a program repair tool developed to fix NullPointerException.
There is no specific strategy for it in Repairnator: if a NPE is detected, NPEFix is called directly to try fixing it.

It can be used [in the option](repairnator-config.md#REPAIR_TOOLS) with this value: `NPEFix`.

## Nopol

[Nopol](https://github.com/SpoonLabs/nopol) is a program repair tool developed to fix conditional statements.
Repairnator supports different strategies to run Nopol.
Nopol is currently configured in Repairnator to find a patch in a maximum of 4 hours.

### NopolAllTests

With this strategy, Nopol will try to find a patch that fix all the failing tests, from all tests classes of the project.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `NopolAllTests`.

### NopolSingleTest

With this strategy, Nopol will be launched back for each test class with a failing test: it will try to find a patch for each test class in failure.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `NopolSingleTest`.

### NopolTestExclusionStrategy

In this strategy, we consider that there is more chance to find a patch for a failing test (i.e. a test with an `AssertionError`) than for an erroring test (i.e. a test which fail with any other exception).
This strategy will launch Nopol for each test class, but will try to ignore erroring test first to find a patch, and then it will look for a patch ignoring failing tests.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `NopolTestExclusionStrategy`.

## Astor

[Astor](https://github.com/SpoonLabs/astor) is a program repair tool that use mutation techniques and genetic programming to obtain patches.
Repairnator supports different strategies to run Astor.
Astor is currently configured in Repairnator to find a patch in a maximum of 100 minutes.

### AstorJGenProg

In this strategy, we are using Astor [with the mode JGenProg](https://github.com/SpoonLabs/astor#jgenprog), an implementation of GenProg.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `AstorJGenProg`.

### AstorJKali

In this strategy, we are using Astor [with the mode JKali](https://github.com/SpoonLabs/astor#jkali), an implementation of Kali.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `AstorJKali`.

### AstorJMut

In this strategy, we are using Astor [with the mode JMutRepair](https://github.com/SpoonLabs/astor#jmutrepair), an implementation of mutation-based repair.

This strategy can be used [in the configuration](repairnator-config.md#REPAIR_TOOLS) with this value: `AstorJMut`.

## AssertFixer

[AssertFixer](https://github.com/STAMP-project/AssertFixer) is a program repair tool developped to fix the tests instead of the program.
Repairnator currently supports only one strategy for AssertFixer.

It can be used [in the option](repairnator-config.md#REPAIR_TOOLS) with this value: `AssertFixer`.