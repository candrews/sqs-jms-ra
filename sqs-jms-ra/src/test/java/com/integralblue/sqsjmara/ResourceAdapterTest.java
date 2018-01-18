/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integralblue.sqsjmara;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that the Resource Adapter loads.
 * 
 * @author Craig Andrews
 *
 */
@RunWith(Arquillian.class)
//@ConnectionFactoryDefinition(
//		interfaceName = "javax.jms.ConnectionFactory",
//		resourceAdapter = "testra",
//		name = "TestConnectionFactory")
//@AdministeredObjectDefinition(
//		resourceAdapter = "testra",
//		interfaceName = "javax.jms.Queue",
//		className = "com.integralblue.sqsjmara.SQSJMSQueue",
//		name = "TestQueue",
//		properties = {"queueName=sqs-jms-ra-test-queue"
//})
@Stateless
public class ResourceAdapterTest {

	@Deployment
	public static EnterpriseArchive deployEar() throws Exception {
	    final JavaArchive rarlib = ShrinkWrap.create(JavaArchive.class, "rarlib.jar")
		        .addClasses(
		        		SQSJMSActivationSpec.class,
		        		SQSJMSConnection.class,
		        		SQSJMSConnectionFactory.class,
		        		SQSJMSManagedConnection.class,
		        		SQSJMSManagedConnectionFactory.class,
		        		SQSJMSManagedConnectionMetaData.class,
		        		SQSJMSQueue.class,
		        		SQSJMSResourceAdapter.class,
		        		SQSJMSSession.class
		        		);

	    final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "testra.rar")
	        .addAsLibrary(rarlib);
	    
	    final WebArchive war = ShrinkWrap.create(WebArchive.class, "testweb.war")
		        .addClasses(
		        		ResourceAdapterTest.class,
		        		TestMessageListener.class,
		        		TestMessageBlockingQueue.class,
		        		TestMessageBlockingQueueImpl.class
		        		)
		        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
		        .addAsWebInfResource("glassfish-resources.xml")
		        .addAsWebInfResource("glassfish-web.xml")
		        .addAsWebInfResource("resources.xml");

	    final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testear.ear")
    		.addAsModule(rar)
	    	.addAsModule(war);

       return ear;
	}
	
	@EJB
	private TestMessageBlockingQueue testMessageBlockingQueue;
	
	@Before
	public void before() {
		testMessageBlockingQueue.clear();
	}

	@Resource(name = "TestConnectionFactory")
	private ConnectionFactory connectionFactory;

	@Resource(name = "TestQueue")
	private Queue queue;

	@Test
	public void testQueueInstanceOfSQSJMSQueue() {
		assertThat(queue, instanceOf(SQSJMSQueue.class));
	}


	@Test
	public void testQueueName() throws JMSException {
		assertThat(queue.getQueueName(), equalTo("sqs-jms-ra-test-queue"));
	}

	@Test
	public void testConnectionFactoryInstanceOfSQSConnectionFactory() {
		assertThat(connectionFactory, instanceOf(SQSJMSConnectionFactory.class));
	}

	@Test
	public void testConnection() throws JMSException {
		final Connection connection = connectionFactory.createConnection();
		assertThat(connection, instanceOf(SQSJMSConnection.class));
		connection.close();
	}

	@Test
	public void testSession() throws JMSException {
		final Connection connection = connectionFactory.createConnection();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		assertThat(session, instanceOf(SQSJMSSession.class));
		session.close();
		connection.close();
	}

	@Test
	public void testSessionCreateQueue() throws JMSException {
		final Connection connection = connectionFactory.createConnection();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		final Queue queue = session.createQueue("sqs-jms-ra-test-queue");
		assertThat(queue, instanceOf(SQSJMSQueue.class));
		assertThat(queue.getQueueName(), equalTo("sqs-jms-ra-test-queue"));
		session.close();
		connection.close();
	}

	@Test
	public void testSendAndReceiveMesssage() throws JMSException, InterruptedException {
		final Connection connection = connectionFactory.createConnection();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		final MessageProducer messageProducer = session.createProducer(queue);

		final TextMessage textMessage = session.createTextMessage();
		textMessage.setText("Testing: " + UUID.randomUUID());

		messageProducer.send(textMessage);
		
		final Message receivedMessage = testMessageBlockingQueue.poll(10, TimeUnit.SECONDS);
		assertThat(receivedMessage,instanceOf(TextMessage.class));
		assertThat(((TextMessage) receivedMessage).getText(),equalTo(textMessage.getText()));

		session.close();
		connection.close();
	}

}
