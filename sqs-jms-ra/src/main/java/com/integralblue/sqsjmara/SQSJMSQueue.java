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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

import com.amazon.sqs.javamessaging.SQSQueueDestination;

/**
 * Abstraction over {@link Queue} for JCA.
 *
 * {@link SQSQueueDestination} cannot be used because its constructor is package scope and users wouldn't know all the necessary information to instantiate it even if the constructor was available.
 *
 * @author Craig Andrews
 *
 */
@AdministeredObject(adminObjectInterfaces = {Queue.class, Destination.class})
public class SQSJMSQueue implements Queue {
	@ConfigProperty
	private String queueName;

	public SQSJMSQueue() {

	}

	public SQSJMSQueue(final String queueName) {
		this.queueName = queueName;
	}

	@Override
	public String getQueueName() throws JMSException {
		return queueName;
	}

}
