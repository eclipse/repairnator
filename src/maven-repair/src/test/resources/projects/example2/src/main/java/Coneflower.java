
public class Coneflower {

	private String nullString = null;
	private int i = 0;

	public String methodThrowingNPE() {
		return nullString.toString();
	}

	public String intermediateMethod() {
		return "Brilliant coneflower," + methodThrowingNPE();
	}

	public String method() {
		return "Cutleaf coneflower," + intermediateMethod();
	}

	public int throwingException() {
		return Integer.parseInt("thisIsNotAnInteger");
	}

}