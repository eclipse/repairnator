package nopol_examples.nopol_example_1;

public class NopolExample {

	/*
	 * Return the index char of s 
	 * or the last if index > s.length
	 * or the first if index < 0			
	 */
	public char charAt(String s, int index){
		
		if ( index == 0 ) // Fix index <= 0
			return s.charAt(0);
		
		if ( index < s.length() )
			return s.charAt(index);
		
		return s.charAt(s.length()-1);
	}
	
	public NopolExample() {
		int variableInsideConstructor;
		variableInsideConstructor = 15; 
		index = 2 * variableInsideConstructor;
	}
	
	private int index = 419382;
	private static String s = "Overloading field name with parameter name";
}
