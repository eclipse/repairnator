package nopol_examples.nopol_example_1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NopolExampleTest {

	@Test
	public void test1(){
		NopolExample ex = new NopolExample();
		assertEquals('a', ex.charAt("abcd", 0));
	}
	
	@Test
	public void test2(){
		NopolExample ex = new NopolExample();
		assertEquals('d', ex.charAt("abcd", 3));
	}
	
	@Test
	public void test3(){
		NopolExample ex = new NopolExample();
		String s = "abcd";
		assertEquals('d', ex.charAt(s, s.length()-1));
	}
	
	@Test
	public void test4(){
		NopolExample ex = new NopolExample();
		String s = "abcd";
		assertEquals('d', ex.charAt(s, 12));
	}
	
	@Test
	public void test5(){
		NopolExample ex = new NopolExample();
		String s = "abcd";
		assertEquals('a', ex.charAt(s, -5));
	}
	
	@Test
	public void test6(){
		NopolExample ex = new NopolExample();
		String s = "abcd";
		assertEquals('a', ex.charAt(s, -1));
	}
	
	@Test
	public void test7(){
		NopolExample ex = new NopolExample();
		assertEquals('c', ex.charAt("abcd", 2));
	}
	
	@Test
	public void test8(){
		NopolExample ex = new NopolExample();
		assertEquals('b', ex.charAt("abcd", 1));
	}
	
	@Test
	public void test9(){
		NopolExample ex = new NopolExample();
		assertEquals('f', ex.charAt("abcdefghijklm", 5));
	}
}
