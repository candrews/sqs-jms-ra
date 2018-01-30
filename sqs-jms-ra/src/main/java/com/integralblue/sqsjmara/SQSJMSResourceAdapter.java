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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

/**
 * {@link ResourceAdapter} for SQS.
 *
 * @author Craig Andrews
 *
 */
@Connector(
		displayName = "Amazon SQS JMS Resource Adapter"
		)
@SuppressWarnings("serial")
public class SQSJMSResourceAdapter implements ResourceAdapter, Serializable {
	private static final Logger LOGGER = Logger.getLogger(SQSJMSResourceAdapter.class.getName());

	private final Map<MessageEndpointFactory, SQSConnection> registeredConnections = new ConcurrentHashMap<>();

	private static final Method ON_MESSAGE_METHOD;

	static {
			try {
				ON_MESSAGE_METHOD = MessageListener.class.getMethod("onMessage", Message.class);
			}
			catch (final NoSuchMethodException | SecurityException e) {
				// this should never happen
				throw new ExceptionInInitializerError(e);
			}
	}

	@Override
	public void start(final BootstrapContext ctx) throws ResourceAdapterInternalException {
		LOGGER.info("Amazon SQS Resource Adapter Started...");
	}

	@Override
	public void stop() {
		LOGGER.info("Amazon SQS Resource Adapter Stopped");
		// go through all the registered factories and stop
		for (final SQSConnection value : registeredConnections.values()) {
			try {
				value.close();
			}
			catch (JMSException e) {
				LOGGER.log(Level.SEVERE, "Failed to close connection", e);
			}
		}
		registeredConnections.clear();
	}

	private SQSJMSQueue getSQSJMSQueue(final SQSJMSActivationSpec sqsSpec) throws ResourceException {
		final SQSJMSQueue sqsjmsQueue;
		if (sqsSpec.getUseJndi()) {
			try {
				sqsjmsQueue = (SQSJMSQueue) InitialContext.doLookup(sqsSpec.getDestination());
			}
			catch (final NamingException e) {
				throw new ResourceException("JNDI lookup failed for "
						+ sqsSpec.getDestination(), e);
			}
		}
		else {
			sqsjmsQueue = new SQSJMSQueue(sqsSpec.getDestination());
		}
		return sqsjmsQueue;
	}

	@Override
	@SuppressWarnings({"PMD.NcssCount", "PMD.CyclomaticComplexity"})
	public void endpointActivation(final MessageEndpointFactory endpointFactory, final ActivationSpec spec)
			throws ResourceException {

		if (!(spec instanceof SQSJMSActivationSpec)) {
			throw new NotSupportedException("Got endpoint activation for an ActivationSpec of unknown class " + spec.getClass().getName());
		}

		final SQSJMSActivationSpec sqsSpec = (SQSJMSActivationSpec) spec;

		final String region = sqsSpec.getAwsRegionProvider().getRegion();
		if (region == null) {
			throw new IllegalStateException("No region set. Please set a region or provide one using a method supported by com.amazonaws.regions.DefaultAwsRegionProviderChain");
		}
		final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
			new ProviderConfiguration(),
			AmazonSQSClientBuilder.standard()
			.withRegion(Regions.fromName(region))
			.withCredentials(sqsSpec)
			);

		try {
			final SQSJMSQueue sqsjmsQueue = getSQSJMSQueue(sqsSpec);

			final SQSConnection connection = connectionFactory.createConnection();
			registeredConnections.put(endpointFactory, connection);
			final QueueSession session = connection.createQueueSession(false, acknowledgeModeStringToInt(sqsSpec.getAcknowledgeMode()));

			final Queue queue;
			try {
				queue = session.createQueue(sqsjmsQueue.getQueueName());
			}
			catch (final InvalidDestinationException | QueueDoesNotExistException e) {
				throw new ResourceException("Queue with name '" + sqsjmsQueue.getQueueName() + "' does not exist", e);
			}

			final MessageConsumer messageConsumer = session.createConsumer(queue);
			messageConsumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(final Message message) {
					MessageEndpoint messageEndpoint = null;
					try {
						messageEndpoint = endpointFactory.createEndpoint(null);
						messageEndpoint.beforeDelivery(ON_MESSAGE_METHOD);
						ON_MESSAGE_METHOD.invoke(messageEndpoint, message);
						messageEndpoint.afterDelivery();
					}
					catch (final NoSuchMethodException | ResourceException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException("Exception delivering message", e); //NOPMD
					}
					finally {
						if (messageEndpoint != null) {
							messageEndpoint.release();
						}
					}
				}
			});
			connection.start();
		}
		catch (final JMSException e) {
			throw new ResourceException(e);
		}
	}

	@SuppressWarnings({"PMD.DefaultPackage"})
	static int acknowledgeModeStringToInt(final String acknowledgeMode) {
		if ("auto-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
			return Session.AUTO_ACKNOWLEDGE;
		}
		else if ("client-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
			return Session.CLIENT_ACKNOWLEDGE;
		}
		else if ("dups-ok-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
			return Session.DUPS_OK_ACKNOWLEDGE;
		}
		else {
			throw new IllegalArgumentException("valid acknowledgeModes are: auto-acknowledge, client-acknowledge, and dups-ok-acknowledge");
		}
	}

	@Override
	public void endpointDeactivation(final MessageEndpointFactory endpointFactory, final ActivationSpec spec) {
		final SQSConnection connection = registeredConnections.get(endpointFactory);
		try {
			connection.close();
		}
		catch (final JMSException e) {
			throw new RuntimeException(e); //NOPMD
		}
	}

	@Override
	public XAResource[] getXAResources(final ActivationSpec[] specs) throws ResourceException {
		return null;
	}

}
