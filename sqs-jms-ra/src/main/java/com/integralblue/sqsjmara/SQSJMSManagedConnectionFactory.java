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
import java.util.Set;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.AwsRegionProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.util.StringUtils;

/**
 * {@link ManagedConnection} for SQS.
 *
 * @author Craig Andrews
 *
 */
@SuppressWarnings("serial")
@ConnectionDefinition(
	connection = QueueConnection.class,
	connectionFactory = QueueConnectionFactory.class,
	connectionFactoryImpl = SQSJMSConnectionFactory.class,
	connectionImpl = SQSConnection.class
)
public class SQSJMSManagedConnectionFactory implements ManagedConnectionFactory {
	private PrintWriter logger;

	@ConfigProperty(description = "AWS Access Key. If not set, defers to com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
	private String awsAccessKeyId;

	@ConfigProperty(description = "AWS Secret Key. If not set, defers to com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
	private String awsSecretKey;

	@ConfigProperty(description = "Region of the SQS queue. If not set, defers to com.amazonaws.regions.DefaultAwsRegionProviderChain.DefaultAwsRegionProviderChain().")
	private String region;

	private final AWSCredentials awsCredentials = new AWSCredentials() {
		@Override
		public String getAWSAccessKeyId() {
			if (StringUtils.isNullOrEmpty(awsAccessKeyId)
					|| StringUtils.isNullOrEmpty(awsAccessKeyId)) {
				throw new SdkClientException("awsAccessKeyId not set on " + this.getClass().getName());
			}
			else {
				return awsAccessKeyId;
			}
		}

		@Override
		public String getAWSSecretKey() {
			if (StringUtils.isNullOrEmpty(awsSecretKey) || StringUtils.isNullOrEmpty(awsSecretKey)) {
				throw new SdkClientException("awsSecretKey not set on " + this.getClass().getName());
			}
			else {
				return awsSecretKey;
			}
		}
	};

	private final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProviderChain(
			new AWSStaticCredentialsProvider(awsCredentials),
			DefaultAWSCredentialsProviderChain.getInstance());

	@Override
	public QueueConnectionFactory createConnectionFactory(final ConnectionManager cxManager) throws ResourceException {
		return new SQSJMSConnectionFactory(cxManager, this);
	}

	@Override
	public QueueConnectionFactory createConnectionFactory() throws ResourceException {
		return createConnectionFactory(null);
	}

	@Override
	public ManagedConnection createManagedConnection(final Subject subject, final ConnectionRequestInfo cxRequestInfo)
			throws ResourceException {
		return new SQSJMSManagedConnection(this);
	}

	@Override
	public ManagedConnection matchManagedConnections(
			final @SuppressWarnings("rawtypes") Set connectionSet,
			final Subject subject,
			final ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		return (ManagedConnection) connectionSet.toArray()[0];
	}

	@Override
	public void setLogWriter(final PrintWriter logger) throws ResourceException {
		this.logger = logger;
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logger;
	}

	public AWSCredentialsProvider getAwsCredentialsProvider() {
		return awsCredentialsProvider;
	}

	public AWSCredentials getAwsCredentials() {
		return awsCredentials;
	}

	public String getAwsAccessKeyId() {
		return awsAccessKeyId;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public String getRegion() {
		return region;
	}

	@SuppressWarnings("PMD.NullAssignment")
	public AwsRegionProvider getAwsRegionProvider() {
		return new AwsRegionProviderChain(
				new AwsRegionProvider() {
					@Override
					public String getRegion() throws SdkClientException {
						return StringUtils.isNullOrEmpty(region) ? null : region;
					}
				},
				new DefaultAwsRegionProviderChain());
	}

}
