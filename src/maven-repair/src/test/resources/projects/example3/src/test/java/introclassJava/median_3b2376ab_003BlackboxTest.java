package introclassJava;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class median_3b2376ab_003BlackboxTest {

    @Test (timeout = 1000) public void test1 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("2 6 8");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test2 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("2 8 6");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test3 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("6 2 8");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test4 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("6 8 2");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test5 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("8 2 6");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test6 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 6 is the median";
        mainClass.scanner = new java.util.Scanner ("8 6 2");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
    @Test (timeout = 1000) public void test7 () throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String expected =
            "Please enter 3 numbers separated by spaces > 9 is the median";
        mainClass.scanner = new java.util.Scanner ("9 9 9");
        mainClass.exec ();
        String out = mainClass.output.replace ("\n", " ").trim ();
        assertEquals (expected.replace (" ", ""), out.replace (" ", ""));
    }
}
