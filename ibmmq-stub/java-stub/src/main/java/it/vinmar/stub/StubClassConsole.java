package it.vinmar.stub;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.jms.JMSException;

public class StubClassConsole extends CoreStubClass {
	
	/**
	 *
	 * @param args
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JMSException
	 */
	public static void main(String... args) throws InterruptedException, ExecutionException, JMSException {

		List<String> inputTopology = Arrays.asList("DEV.QUEUE.1;Input", "DEV.QUEUE.2;Output");
		
		StubClassConsole stub = new StubClassConsole();

		Boolean init = stub.initializeStubService(inputTopology);

		if (!init) {
			logger.error("XXX Init error!");
			System.exit(1);
		}
		
		try {
			String inputLine = "";

			do {
				inputLine = stub.userInteraction().get();
			} while (!(inputLine != null && inputLine.equals("exit")));
		} catch (Exception e) {
			logger.error("XXXX Errore durante l'esecuzione del main loop : " + e.getMessage());
		} finally {
			
			Boolean quit = stub.disposeStubSystem();
			
			if (!quit) {
				logger.error("XXX Quit error!");
				System.exit(1);
			}
		}
	}
	

	/**
	 *
	 */
	private static final String exitCommand = "exit";

	/**
	 * User interaction Future that complete when user press enter
	 */
	public CompletableFuture<String> userInteraction() {

		final CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {

			final String defaultPrompt = String.format("Stub is running insert \"%s\" to stop > \n", exitCommand);

			@Override
			public String get() {

				Console console = System.console();

				System.out.println();
				System.out.print(defaultPrompt);

				return console.readLine();
			}
		});

		return future;
	}
	
}
