package it.vinmar.stub;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.jms.JMSException;

public class DockerStubClass extends CoreStubClass {
	
	public static void main(String... args) throws InterruptedException, ExecutionException, JMSException {

		String arg = System.getProperty("STUB_MINUTES", "10");
		long duration = 60 * 1000 * Long.parseLong(arg);
		
		CoreStubClass underTest = new DockerStubClass();
		
		Boolean init = underTest.initializeStubService(Arrays.asList("DEV.QUEUE.1;Input", "DEV.QUEUE.2;Output"));
		
		if (!init) {
			logger.error("XXX Init error!");
			System.exit(1);
		}
		
		Thread.sleep(duration);
		
		System.out.println("Time to live of stub is concluded!");
		
		Boolean quit = underTest.disposeStubSystem();
		
		if (!quit) {
			logger.error("XXX Quit error!");
			System.exit(1);
		}
	}
}
