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

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.StringUtils;

/**
 * JCA adapter for SQS to {@link QueueConnectionFactory}.
 *
 * @author Craig Andrews
 */
public class SQSJMSConnectionFactory implements QueueConnectionFactory {
	private final ConnectionManager connectionManager;
	private final SQSJMSManagedConnectionFactory sqsJMSManagedConnectionFactory;

	public SQSJMSConnectionFactory(
			final ConnectionManager connectionManager,
			final SQSJMSManagedConnectionFactory sqsJMSManagedConnectionFactory) {
		this.connectionManager = connectionManager;
		this.sqsJMSManagedConnectionFactory = sqsJMSManagedConnectionFactory;
	}

	@Override
	public QueueConnection createConnection() throws JMSException {
		try {
			if (connectionManager == null) {
				return new SQSJMSConnection(getSQSConnectionFactory().createConnection());
			}
			else {
				return (QueueConnection) connectionManager.allocateConnection(sqsJMSManagedConnectionFactory, null);
			}
		}
		catch (final ResourceException e) {
			throw new RuntimeException(e); //NOPMD
		}
	}

	@Override
	public QueueConnection createConnection(final String userName, final String password) throws JMSException {
		return getSQSConnectionFactory().createConnection(userName, password);
	}

	private SQSConnectionFactory getSQSConnectionFactory() {
		final String region = sqsJMSManagedConnectionFactory.getAwsRegionProvider().getRegion();
		if (region == null) {
			throw new IllegalStateException("No region set. Please set a region or provide one using a method supported by com.amazonaws.regions.DefaultAwsRegionProviderChain");
		}
		if (StringUtils.isNullOrEmpty(region)) {
			throw new IllegalStateException("region must be set");
		}
		return new SQSConnectionFactory(
			new ProviderConfiguration(),
			AmazonSQSClientBuilder.standard()
				.withRegion(Regions.fromName(region))
				.withCredentials(sqsJMSManagedConnectionFactory.getAwsCredentialsProvider())
			);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		return createConnection();
	}

	@Override
	public QueueConnection createQueueConnection(final String userName, final String password) throws JMSException {
		return createConnection(userName, password);
	}

}
