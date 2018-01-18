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

import static org.junit.Assert.assertNotNull;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven(
		messageListenerInterface=javax.jms.MessageListener.class,
		activationConfig = {
			@ActivationConfigProperty(propertyName = "queueName", propertyValue = "sqs-jms-ra-test-queue"),
			@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "1") })
// activation config properties are from com.integralblue.sqsjmara.SQSJMSActivationSpec
public class TestMessageListener implements MessageListener {
	@EJB
	private TestMessageBlockingQueue testMessageBlockingQueue;

	@Override
	public void onMessage(final Message message) {
		assertNotNull(message);
		testMessageBlockingQueue.add(message);
	}

}