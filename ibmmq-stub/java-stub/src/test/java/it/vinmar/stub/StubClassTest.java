package it.vinmar.stub;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class StubClassTest {

	private static class UnderTest extends CoreStubClass {
		
	}
	
	@Test
	public void testTest() throws InterruptedException {
		
		CoreStubClass underTest = new UnderTest();
		
		Boolean init = underTest.initializeStubService(Arrays.asList("DEV.QUEUE.1;Input", "DEV.QUEUE.2;Output"));
		
		Assert.assertTrue(init);
		
		Thread.sleep(60_000);
		
		System.out.println("Time to live of stub is concluded!");
		
		Boolean quit = underTest.disposeStubSystem();
		
		Assert.assertTrue(quit);
	}
}
