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

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.amazon.sqs.javamessaging.SQSQueueDestination;
import com.amazon.sqs.javamessaging.SQSSession;

/**
 * JCA adapter for {@link SQSSession}.
 *
 * @author Craig Andrews
 *
 */
@SuppressWarnings({"PMD.TooManyMethods"})
public class SQSJMSSession implements Session, QueueSession {
	private final SQSSession delegate;

	public SQSJMSSession(final SQSSession delegate) {
		this.delegate = delegate;
	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		return delegate.createBytesMessage();
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		return delegate.createMapMessage();
	}

	@Override
	public Message createMessage() throws JMSException {
		return delegate.createMessage();
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		return delegate.createObjectMessage();
	}

	@Override
	public ObjectMessage createObjectMessage(final Serializable object) throws JMSException {
		return delegate.createObjectMessage(object);
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		return delegate.createStreamMessage();
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		return delegate.createTextMessage();
	}

	@Override
	public TextMessage createTextMessage(final String text) throws JMSException {
		return delegate.createTextMessage(text);
	}

	@Override
	public boolean getTransacted() throws JMSException {
		return delegate.getTransacted();
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		return delegate.getAcknowledgeMode();
	}

	@Override
	public void commit() throws JMSException {
		delegate.commit();
	}

	@Override
	public void rollback() throws JMSException {
		delegate.rollback();
	}

	@Override
	public void close() throws JMSException {
		delegate.close();
	}

	@Override
	public void recover() throws JMSException {
		delegate.recover();
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return delegate.getMessageListener();
	}

	@Override
	public void setMessageListener(final MessageListener listener) throws JMSException {
		delegate.setMessageListener(listener);
	}

	@Override
	public void run() {
		delegate.run();
	}

	@Override
	public MessageProducer createProducer(final Destination destination) throws JMSException {
		return delegate.createProducer(toSQSQueueDestination(destination));
	}

	@Override
	public MessageConsumer createConsumer(final Destination destination) throws JMSException {
		return delegate.createConsumer(toSQSQueueDestination(destination));
	}

	@Override
	public MessageConsumer createConsumer(final Destination destination, final String messageSelector) throws JMSException {
		return delegate.createConsumer(toSQSQueueDestination(destination), messageSelector);
	}

	@Override
	public MessageConsumer createConsumer(final Destination destination, final String messageSelector, final boolean noLocal)
			throws JMSException {
		return delegate.createConsumer(toSQSQueueDestination(destination), messageSelector, noLocal);
	}

	@Override
	public Queue createQueue(final String queueName) throws JMSException {
		return new SQSJMSQueue(queueName);
	}

	@Override
	public Topic createTopic(final String topicName) throws JMSException {
		return delegate.createTopic(topicName);
	}

	@Override
	public TopicSubscriber createDurableSubscriber(final Topic topic, final String name) throws JMSException {
		return delegate.createDurableSubscriber(topic, name);
	}

	@Override
	public TopicSubscriber createDurableSubscriber(final Topic topic, final String name, final String messageSelector, final boolean noLocal)
			throws JMSException {
		return delegate.createDurableSubscriber(topic, name, messageSelector, noLocal);
	}

	@Override
	public QueueBrowser createBrowser(final Queue queue) throws JMSException {
		return delegate.createBrowser(toSQSQueueDestination(queue));
	}

	@Override
	public QueueBrowser createBrowser(final Queue queue, final String messageSelector) throws JMSException {
		return delegate.createBrowser(toSQSQueueDestination(queue), messageSelector);
	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return delegate.createTemporaryQueue();
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return delegate.createTemporaryTopic();
	}

	@Override
	public void unsubscribe(final String name) throws JMSException {
		delegate.unsubscribe(name);
	}

	@Override
	public QueueReceiver createReceiver(final Queue queue) throws JMSException {
		return delegate.createReceiver(toSQSQueueDestination(queue));
	}

	@Override
	public QueueReceiver createReceiver(final Queue queue, final String messageSelector) throws JMSException {
		return delegate.createReceiver(toSQSQueueDestination(queue), messageSelector);
	}

	@Override
	public QueueSender createSender(final Queue queue) throws JMSException {
		return delegate.createSender(toSQSQueueDestination(queue));
	}

	private SQSQueueDestination toSQSQueueDestination(final Destination queue) throws JMSException {
		if (!(queue instanceof SQSJMSQueue)) {
			throw new JMSException("Actual type of Destination/Queue has to be " + SQSJMSQueue.class.getName());
		}
		return (SQSQueueDestination) delegate.createQueue(((SQSJMSQueue) queue).getQueueName());
	}

}
