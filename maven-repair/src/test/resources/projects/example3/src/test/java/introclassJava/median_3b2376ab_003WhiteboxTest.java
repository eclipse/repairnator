package introclassJava;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class median_3b2376ab_003WhiteboxTest {

    @Test (timeout = 1000) public void test1 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 0 is the median";
        mainClass.scanner = new java.util.Scanner ("0 0 0");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test2 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 1 is the median";
        mainClass.scanner = new java.util.Scanner ("2 0 1");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test3 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 0 is the median";
        mainClass.scanner = new java.util.Scanner ("0 0 1");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test4 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 0 is the median";
        mainClass.scanner = new java.util.Scanner ("0 1 0");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test5 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 1 is the median";
        mainClass.scanner = new java.util.Scanner ("0 2 1");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test6 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 2 is the median";
        mainClass.scanner = new java.util.Scanner ("0 2 3");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
}
