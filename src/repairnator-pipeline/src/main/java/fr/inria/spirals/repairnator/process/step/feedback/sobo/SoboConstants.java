package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import java.util.ArrayList;
import java.util.Arrays;

public class SoboConstants {
    public static final String SOBO_TOOL_NAME = "SoboBot";
    public static final String RULES_SLOT_1="S109, S1155, S1481";
    public static final String RULES_SLOT_2="S1213, S1871";
    public static final String RULES_SLOT_3="S2119, S2039, noSonar";
    public static final String RULES_SLOT_4="S1192, S1104";
    public static final String HELP="--help";
    public static final String STOP= "--stop";
    public static final String GO="--go";
    public static final String MORE="--more";
    public static final String RULE="--rule";
    public static final String COMMAND_ISSUE_TITLE="SOBO - Command Issue";
    public static final String FEEDBACK_ISSUE_TITLE="SOBO - Commit Analyzer";

    public static final ArrayList COMMANDS= new ArrayList<>(
            Arrays.asList("--help", "--stop", "--go", "--more", "-rule"));

    public static final String[] SOBO_RULES={"S109", "S1155", "S1481","S1213", "S1871","S2119","S2039", "noSonar","S1192", "S1104"};

}
