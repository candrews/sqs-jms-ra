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

import javax.jms.Queue;
import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.AwsRegionProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.StringUtils;

/**
 * {@link ActivationSpec} for SQS.
 *
 * @author Craig Andrews
 *
 */
@SuppressWarnings("serial")
@Activation(messageListeners = { javax.jms.MessageListener.class })
public class SQSJMSActivationSpec implements ActivationSpec, Serializable, AWSCredentialsProvider {

	private ResourceAdapter adapter;

	@ConfigProperty(description = "AWS Access Key. If not set, defers to com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
	private String awsAccessKeyId;

	@ConfigProperty(description = "AWS Secret Key. If not set, defers to com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
	private String awsSecretKey;

	@ConfigProperty(description = "Region of the SQS queue. If not set, defers to com.amazonaws.regions.DefaultAwsRegionProviderChain")
	private String region;

	@ConfigProperty(description = "Values are Auto-acknowledge, Dups-ok-acknowledge, and Client-acknowledge")
	@NotNull
	private String acknowledgeMode;

	@ConfigProperty(description = "Use JNDI to look up the destination queue. Otherwise, use destination as the name of the queue.", defaultValue = "true")
	@NotNull
	private Boolean useJndi = true;

	@ConfigProperty(description = "Destination (queue) from which messages are consumed. If useJndi, then treated as a JNDI name. Otherwise, treated as the name of a queue.")
	@NotNull
	private String destination;

	@ConfigProperty(description = "Destination type. Must be 'javax.jms.Queue'.", defaultValue = "javax.jms.Queue")
	@NotNull
	@Pattern(regexp = "^javax\\.jms\\.Queue$")
	private String destinationType = Queue.class.getName();

	public SQSJMSActivationSpec() {
		final Region currentRegion = Regions.getCurrentRegion();
		if (currentRegion != null)  {
			region = currentRegion.getName();
		}
	}

	private final AWSCredentials awsCredentials = new AWSCredentials() {
		@Override
		public String getAWSAccessKeyId() {
			if (StringUtils.isNullOrEmpty(awsAccessKeyId)) {
				throw new SdkClientException("awsAccessKeyId not set on " + this.getClass().getName());
			}
			else {
				return awsAccessKeyId;
			}
		}

		@Override
		public String getAWSSecretKey() {
			if (StringUtils.isNullOrEmpty(awsSecretKey)) {
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
	public ResourceAdapter getResourceAdapter() {
		return adapter;
	}

	@Override
	public void setResourceAdapter(final ResourceAdapter ra) throws ResourceException {
		// spec section 5.3.3
		if (adapter != null) {
			throw new ResourceException("ResourceAdapter already set");
		}
		if (!(ra instanceof SQSJMSResourceAdapter)) {
			throw new ResourceException("ResourceAdapter is not of type: " + SQSJMSResourceAdapter.class.getName());
		}
		adapter = ra;
	}

	@Override
	public void validate() throws InvalidPropertyException {
		if (!Queue.class.getName().equals(destinationType)) {
			throw new InvalidPropertyException("'destinationType' must be '" + Queue.class.getName() + "'.");
		}
		try {
			SQSJMSResourceAdapter.acknowledgeModeStringToInt(acknowledgeMode);
		}
		catch (final IllegalArgumentException e) {
			throw new InvalidPropertyException(e.getMessage(), e);
		}
		if (StringUtils.isNullOrEmpty(getAwsRegionProvider().getRegion())) {
			throw new InvalidPropertyException("Must set the 'region' property or provide the region to use via one of the com.amazonaws.regions.DefaultAwsRegionProviderChain supported mechanisms");
		}
		try {
			getCredentials();
		}
		catch (final Exception e) { //NOPMD
			throw new InvalidPropertyException("Must set awsAccessKeyId and awsSecretKey or provide the credentials to use via one of the com.amazonaws.auth.DefaultAWSCredentialsProviderChain supported mechanisms", e);
		}
	}

	public String getAwsAccessKeyId() {
		return awsAccessKeyId;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	@Override
	public AWSCredentials getCredentials() {
		return awsCredentialsProvider.getCredentials();
	}

	public String getAcknowledgeMode() {
		return acknowledgeMode;
	}

	public void setAcknowledgeMode(final String acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public void setDestination(final String destination) {
		this.destination = destination;
	}

	public String getDestination() {
		return destination;
	}

	public Boolean getUseJndi() {
		return useJndi;
	}

	public void setUseJndi(final Boolean useJndi) {
		this.useJndi = useJndi;
	}

	public String getRegion() {
		return region;
	}

	@Override
	public void refresh() {
		awsCredentialsProvider.refresh();
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
