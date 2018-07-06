package fr.inria.spirals.repairnator.process.utils4tests;

public class Utils4Tests {

    public static String getZ3SolverPath() {
        String z3SolverPath;
        if (isMac()) {
            z3SolverPath = Constants4Tests.Z3_SOLVER_PATH_DIR +Constants4Tests.Z3_SOLVER_NAME_MAC;
        } else {
            z3SolverPath = Constants4Tests.Z3_SOLVER_PATH_DIR +Constants4Tests.Z3_SOLVER_NAME_LINUX;
        }
        return z3SolverPath;
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("mac"));
    }

}
