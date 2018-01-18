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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.jms.Message;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TestMessageBlockingQueueImpl implements TestMessageBlockingQueue  {
	private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
	
	/* (non-Javadoc)
	 * @see com.integralblue.sqsjmara.TestMessageBlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Message poll(final long timeout, final TimeUnit unit) throws InterruptedException {
		return queue.poll(timeout, unit);
	}
	
	/* (non-Javadoc)
	 * @see com.integralblue.sqsjmara.TestMessageBlockingQueue#clear()
	 */
	@Override
	public void clear() {
		queue.clear();
	}

	@Override
	public void add(final Message message) {
		queue.add(message);
	}
}
