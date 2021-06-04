package exceptionparser;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Scanner;

/**
 * Unit test for simple App.
 */
public class StackTraceParserTest  {

	private String getResourceContent(String path) {
		StringBuilder output = new StringBuilder();
		Scanner scanner = new Scanner(getClass().getResourceAsStream(path), "UTF-8");
		while(scanner.hasNextLine()) {
			String s = scanner.nextLine();

			output.append(s + "\n");
		}
		return output.toString();
	}
	@Test
	public void stack1Test() {
		String resourceContent = getResourceContent("/stack1.txt");

		StackTraceParser stackTraceParser = new StackTraceParser(resourceContent);
		List<StackTrace> stackTraces = stackTraceParser.getStackTraces();

		Assert.assertEquals(3, stackTraces.size());
		Assert.assertEquals("java.lang.reflect.InvocationTargetException", stackTraces.get(0).getExceptionType());
		Assert.assertEquals("java.lang.instrument.IllegalClassFormatException", stackTraces.get(1).getExceptionType());
		Assert.assertEquals("java.lang.reflect.InvocationTargetException", stackTraces.get(2).getExceptionType());
	}

	@Test
	public void stack2Test() {
		String resourceContent = getResourceContent("/stack2.txt");

		StackTraceParser stackTraceParser = new StackTraceParser(resourceContent);
		List<StackTrace> stackTraces = stackTraceParser.getStackTraces();

		Assert.assertEquals(1, stackTraces.size());
		Assert.assertEquals("java.net.ConnectException", stackTraces.get(0).getExceptionType());
	}

	@Test
	public void stack3Test() {
		String resourceContent = getResourceContent("/stack2.txt");

		List<StackTrace> stackTraces = StackTraceParser.parseAll(resourceContent);

		Assert.assertEquals(1, stackTraces.size());
		Assert.assertEquals("java.net.ConnectException", stackTraces.get(0).getExceptionType());
	}
}
