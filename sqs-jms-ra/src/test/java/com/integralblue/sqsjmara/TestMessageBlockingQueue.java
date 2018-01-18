package com.integralblue.sqsjmara;

import java.util.concurrent.TimeUnit;

import javax.jms.Message;

public interface TestMessageBlockingQueue {

	Message poll(long timeout, TimeUnit unit) throws InterruptedException;

	void clear();

	void add(Message message);

}