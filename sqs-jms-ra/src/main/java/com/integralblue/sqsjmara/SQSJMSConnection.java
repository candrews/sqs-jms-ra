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

import java.util.List;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ManagedConnection;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSMessagingClientConstants;
import com.amazon.sqs.javamessaging.SQSSession;

/**
 * JCA adapter for SQS {@link Connection}.
 *
 * @author Craig Andrews
 *
 */
public class SQSJMSConnection implements QueueConnection {
	private final SQSConnection delegate;
	private final ManagedConnection managedConnection;
	private final List<ConnectionEventListener> listeners;

	@SuppressWarnings("PMD.NullAssignment")
	public SQSJMSConnection(
			final SQSConnection delegate) {
		this.managedConnection = null;
		this.delegate = delegate;
		this.listeners = null;
	}

	public SQSJMSConnection(
			final ManagedConnection managedConnection,
			final SQSConnection delegate,
			final List<ConnectionEventListener> listeners) {
		this.managedConnection = managedConnection;
		this.delegate = delegate;
		this.listeners = listeners;
	}

	@Override
	public Session createSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
		final SQSSession sqsSession = (SQSSession) delegate.createSession(transacted, acknowledgeMode);
		return new SQSJMSSession(sqsSession);
	}

	@Override
	public String getClientID() throws JMSException {
		return delegate.getClientID();
	}

	@Override
	public void setClientID(final String clientID) throws JMSException {
		delegate.setClientID(clientID);
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return delegate.getMetaData();
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return delegate.getExceptionListener();
	}

	@Override
	public void setExceptionListener(final ExceptionListener listener) throws JMSException {
		delegate.setExceptionListener(listener);
	}

	@Override
	public void start() throws JMSException {
		delegate.start();
	}

	@Override
	public void stop() throws JMSException {
		delegate.stop();
	}

	@Override
	public void close() throws JMSException {
		try {
			if (managedConnection != null) {
				final ConnectionEvent event = new ConnectionEvent(managedConnection, ConnectionEvent.CONNECTION_CLOSED);
				event.setConnectionHandle(this);

				for (final ConnectionEventListener cel : listeners) {
					cel.connectionClosed(event);
				}
			}
		}
		finally {
			delegate.close();
		}
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(
			final Destination destination,
			final String messageSelector,
			final ServerSessionPool sessionPool,
			final int maxMessages) throws JMSException {
		throw new JMSException(SQSMessagingClientConstants.UNSUPPORTED_METHOD);
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(
			final Topic topic,
			final String subscriptionName,
			final String messageSelector,
			final ServerSessionPool sessionPool,
			final int maxMessages) throws JMSException {
		throw new JMSException(SQSMessagingClientConstants.UNSUPPORTED_METHOD);
	}

	@Override
	public QueueSession createQueueSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
		final SQSSession sqsSession = (SQSSession) delegate.createQueueSession(transacted, acknowledgeMode);
		return new SQSJMSSession(sqsSession);
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(
			final Queue queue,
			final String messageSelector,
			final ServerSessionPool sessionPool,
			final int maxMessages) throws JMSException {
		throw new JMSException(SQSMessagingClientConstants.UNSUPPORTED_METHOD);
	}
}
