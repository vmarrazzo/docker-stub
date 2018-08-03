package it.vinmar.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.common.CommonConstants;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Created by vmarrazzo on 17/07/2017.
 */
public abstract class CoreStubClass {

	/**
	 *
	 */
	protected static Logger logger = LoggerFactory.getLogger(CoreStubClass.class.getName());

	/**
	 * This thread is the core of stubbing mechanism
	 * 
	 * @author vmarrazzo
	 *
	 */
	protected static class StubThread implements Runnable {

		private static Boolean isShutdown = false;

		private static final Integer pooling = 100;
		private static final Integer timeout = 50;

		private String assignedQueue = null;
		private MessageConsumer consumer = null;

		public StubThread(String observedQueue) throws JMSException {
			assignedQueue = observedQueue;
			Queue observed = sess.createQueue(assignedQueue);
			consumer = sess.createConsumer(observed);
			
			logger.debug("Started StubThread on " + assignedQueue + " queue");
		}

		/**
		 * 
		 * @param req
		 * @param tag
		 * @return
		 */
		private static String getTag(String req, String tag) {
			String ret = "";
			if (req != null) {
				int n1 = req.indexOf("<" + tag + ">");
				int n2 = req.indexOf("</" + tag + ">", n1);
				if (n1 != -1 && n2 > n1) {
					ret = req.substring(n1 + tag.length() + 2, n2);
				} else {
					logger.error("Missing TAG {}", tag);
				}
			}
			return ret;
		}

		@Override
		public void run() {

			do {

				try {
					Thread.sleep(pooling);

					Message myMessage = consumer.receive(timeout);
					
					if (myMessage != null) {
						TextMessage textMessage = (TextMessage) myMessage;

						TextMessage sndMessage = sess.createTextMessage();
				
						sndMessage.setStringProperty("Origin", "Sample Stub VM78");
						sndMessage.setJMSCorrelationID(textMessage.getJMSCorrelationID());

						// calcolata in qualche modo
						String destQueue = getTag(textMessage.getText(), "responseOn");

						if (destMap.containsKey(destQueue)) {
							
							sndMessage.setText(String.format("<response>\r\n" + 
									"	<message>This is a response</message>\r\n" + 
									"	<requestFrom>%s</requestFrom>\r\n" + 
									"	<messageId>%s</messageId>\r\n" + 
									"</response>\r\n" + 
									"", assignedQueue, textMessage.getJMSCorrelationID()));
							
							destMap.get(destQueue).send(sndMessage);
							
							logger.info("Send a stub message to " + destQueue + " queue from " + assignedQueue);
						}
						else
							logger.info("Destination " + destQueue + " is not handled!");						
					}
					else
						logger.info("### No message");
				} catch (JMSException | InterruptedException e) {
					logger.error("XXX Error on Stub -> " + e.getMessage());
				}

			} while (!isShutdown);
		}
	}

	/**
	 * 
	 */
	private static Connection conn = null;
	
	/**
	 * 
	 */
	private static Session sess = null;

	
	/**
	 * Destination Queue mapped by them names
	 */
	private static Map<String, MessageProducer> destMap = new HashMap<>();	
	
	/**
	 * 
	 */
	private ExecutorService system = null;
	
	/**
	 * 
	 * @param topology
	 * @return
	 * @throws JMSException
	 */
	protected Boolean initializeStubService(List<String> topology) {

		class ExitStatus {
			Boolean status = true;	
		}
		
		final ExitStatus resp = new ExitStatus();
		
		/**
		 * IBM MQ Config Section
		 */
		
		String hostName = System.getProperty("IBMMQ_HOST", "localhost");
		Integer hostPort = 1415;
		String queueManagerName = "QM_WITH_TLS";
		String testChannelName = "DEV.APP.SVRCONN";

		MQQueueConnectionFactory queueConnectionFactory = null;
		
		final String keyStoreFolder = "/root";
		
		try {

			/**
			 * IBM MQ Config Section
			 */
			
			queueConnectionFactory = new MQQueueConnectionFactory();
			queueConnectionFactory.setHostName(hostName);
			queueConnectionFactory.setPort(hostPort);
			queueConnectionFactory.setQueueManager(queueManagerName);
			queueConnectionFactory.setChannel(testChannelName);

			// Connect to IBM MQ over TCP/IP
			queueConnectionFactory.setTransportType(CommonConstants.WMQ_CM_CLIENT);

			/**
			 * TLS Section
			 */
			String tlsKeystorePath = keyStoreFolder + File.separator + "my-cert.jks";
			String tlsKeystorePwd = "changeit";

			queueConnectionFactory.setStringProperty(WMQConstants.USERID, "app");
			queueConnectionFactory.setStringProperty(WMQConstants.PASSWORD, "test");

			queueConnectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "SSL_RSA_WITH_AES_256_GCM_SHA384");
			queueConnectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SPEC, "TLS_RSA_WITH_AES_256_GCM_SHA384");

			System.setProperty("com.ibm.mq.cfg.preferTLS", "true");
			System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
			System.setProperty("javax.net.ssl.keyStore", tlsKeystorePath);
			System.setProperty("javax.net.ssl.keyStorePassword", tlsKeystorePwd);
			System.setProperty("javax.net.ssl.trustStore", tlsKeystorePath);
			System.setProperty("javax.net.ssl.trustStorePassword", tlsKeystorePwd);
			/**
			 * TLS Section - END
			 */

			conn = queueConnectionFactory.createConnection();
			sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			conn.start();
			
			logger.info("Stub setup completed!");
	
		} catch (JMSException e) {
			logger.error(
					String.format("XXX Error during stub initializer -> %s", e.getMessage()));
			resp.status = false;
		}
		
		/**
		 * Filter stub threads information
		 */
		Long nrObserver = 
				topology
				.stream()
				.filter(singleString -> singleString.contains(";Input"))
				.count();

		/**
		 * Create multi thread context to handle consumers
		 */
		system = Executors.newFixedThreadPool(nrObserver.intValue());

		/**
		 * Create producer for response queue
		 */
		topology
			.stream()
			.filter(singleString -> singleString.contains(";Output"))
			.forEach(confString -> {
				String[] ans = confString.split(";");
				String queueName = ans[0];
				try {
					MessageProducer producer = sess.createProducer(sess.createQueue(queueName));
					destMap.put(queueName, producer);
				} catch (JMSException e) {
					logger.error(
							String.format("XXX Error during producer creation on %s -> %s", queueName, e.getMessage()));
					resp.status = false;
				}
			});
		
		logger.info("### Producer created");

		/**
		 * Create working thread for request queue and submit to context
		 */
		topology
			.stream()
			.filter(singleString -> singleString.contains(";Input"))
			.forEach(configString -> {
				try {
					system.submit(new StubThread(configString.replace(";Input", "")));
				} catch (JMSException e) {
					logger.error(
							String.format("XXX Error during thread creation -> %s", e.getMessage()));
					resp.status = false;
				}
			});
		
		logger.info("### Consumer threads created");

		return resp.status;
	}

	/**
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws JMSException
	 */
	protected Boolean disposeStubSystem(){
		
		Boolean resp = false;
		
		if (system != null) {
			try {
				// shutdown context
				system.shutdown();

				// use shared variable to stop consumer thread
				StubThread.isShutdown = true;

				Boolean exit = system.awaitTermination(20, TimeUnit.SECONDS);

				if (conn != null)
					conn.close();
				
				if (exit) {
					logger.info("### Stub shutdown completed");
					resp = true;
				}
				else {
					logger.error("XXX Qualcosa non va nell'uscita!");
					resp = false;
				}				
			} catch ( InterruptedException | JMSException e) {
				logger.error("XXX During dispose stub error occurs -> " + e.getMessage());
				resp = false;
			}
		}
		else
			logger.error("XXX Nothing to be disposed!!!");

		return resp;
	}
}
