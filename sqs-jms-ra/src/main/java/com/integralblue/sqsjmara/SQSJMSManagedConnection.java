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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 * JCA adapter for SQS to {@link ManagedConnection}.
 *
 * @author Craig Andrews
 *
 */
public class SQSJMSManagedConnection implements ManagedConnection {
	private final SQSJMSManagedConnectionFactory sqsjmsManagedConnectionFactory;
	private final List<ConnectionEventListener> listeners = new ArrayList<>();
	private PrintWriter logWriter;

	private SQSJMSConnection connection;

	public SQSJMSManagedConnection(final SQSJMSManagedConnectionFactory sqsjmsManagedConnectionFactory) {
		this.sqsjmsManagedConnectionFactory = sqsjmsManagedConnectionFactory;
	}

	@Override
	public SQSJMSConnection getConnection(final Subject subject, final ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		try {
			connection = (SQSJMSConnection) sqsjmsManagedConnectionFactory.createConnectionFactory().createQueueConnection();
			return connection;
		}
		catch (JMSException e) {
			throw new RuntimeException(e); //NOPMD
		}
	}

	@Override
	public void destroy() throws ResourceException {
		if (connection != null) {
			try {
				connection.stop();
			}
			catch (final JMSException e) {
				throw new ResourceException(e);
			}
		}
	}

	@Override
	public void cleanup() throws ResourceException {
		if (connection != null) {
			try {
				connection.stop();
			}
			catch (JMSException e) {
				throw new ResourceException(e);
			}
		}
	}

	@Override
	public void associateConnection(final Object connection) throws ResourceException {
		if (connection instanceof SQSJMSConnection) {
			this.connection = (SQSJMSConnection) connection;
		}
		else {
			throw new ResourceException("Not supported : associating connection instance of " + connection.getClass().getName());
		}
	}

	@Override
	public void addConnectionEventListener(final ConnectionEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener is null");
		}
		listeners.add(listener);
	}

	@Override
	public void removeConnectionEventListener(final ConnectionEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener is null");
		}
		listeners.remove(listener);
	}

	@Override
	public XAResource getXAResource() throws ResourceException {
		throw new NotSupportedException("GetXAResource not supported");
	}

	@Override
	public LocalTransaction getLocalTransaction() throws ResourceException {
		throw new NotSupportedException("LocalTransaction not supported");
	}

	@Override
	public ManagedConnectionMetaData getMetaData() throws ResourceException {
		return SQSJMSManagedConnectionMetaData.getInstance();
	}

	@Override
	public void setLogWriter(final PrintWriter logWriter) throws ResourceException {
		this.logWriter = logWriter;
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter;
	}

}
