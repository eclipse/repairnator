package introclassJava;

class IntObj {
    public int value;
    public IntObj () {
    } public IntObj (int i) {
        value = i;
    }
}

class FloatObj {
    public float value;
    public FloatObj () {
    } public FloatObj (float i) {
        value = i;
    }
}

class LongObj {
    public long value;
    public LongObj () {
    } public LongObj (long i) {
        value = i;
    }
}

class DoubleObj {
    public double value;
    public DoubleObj () {
    } public DoubleObj (double i) {
        value = i;
    }
}

class CharObj {
    public char value;
    public CharObj () {
    } public CharObj (char i) {
        value = i;
    }
}

public class median_3b2376ab_003 {
    public java.util.Scanner scanner;
    public String output = "";

    public static void main (String[]args) throws Exception {
        median_3b2376ab_003 mainClass = new median_3b2376ab_003 ();
        String output;
        if (args.length > 0) {
            mainClass.scanner = new java.util.Scanner (args[0]);
        } else {
            mainClass.scanner = new java.util.Scanner (System.in);
        }
        mainClass.exec ();
        System.out.println (mainClass.output);
    }

    public void exec () throws Exception {
        IntObj n1 = new IntObj (), n2 = new IntObj (), n3 = new IntObj (), small =
            new IntObj ();
        output +=
            (String.format ("Please enter 3 numbers separated by spaces > "));
        n1.value = scanner.nextInt ();
        n2.value = scanner.nextInt ();
        n3.value = scanner.nextInt ();
        if (n1.value < n2.value) {
            small.value = n1.value;
            if (small.value > n3.value) {
                output += (String.format ("%d is the median\n", n1.value));
            } else if (n3.value > n2.value) {
                output += (String.format ("%d is the median\n", n2.value));
            } else {
                output += (String.format ("%d is the median\n", n3.value));
            }
        } else {
            small.value = n2.value;
            if (small.value > n3.value) {
                output += (String.format ("%d is the median\n", n2.value));
            } else if (n3.value > n1.value) {
                output += (String.format ("%d is the median\n", n3.value));
            } else {
                output += (String.format ("%d is the median\n", n1.value));
            }
        }
    }
}
