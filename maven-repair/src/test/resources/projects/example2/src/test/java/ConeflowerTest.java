import org.junit.Test;
import org.junit.Assert;

public class ConeflowerTest {

	@Test
	public void test1() throws Exception {
		Coneflower flower = new Coneflower();
		Assert.assertEquals("Cutleaf coneflower,Brilliant coneflower,", flower.method());
	}
}