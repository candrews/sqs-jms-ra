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

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * {@link ManagedConnectionMetaData} implementation for SQS.
 *
 * @author Craig Andrews
 *
 */
public class SQSJMSManagedConnectionMetaData implements ManagedConnectionMetaData {
	private static final SQSJMSManagedConnectionMetaData INSTANCE = new SQSJMSManagedConnectionMetaData();

	public static SQSJMSManagedConnectionMetaData getInstance() {
		return INSTANCE;
	}

	@Override
	public String getEISProductName() throws ResourceException {
		return getClass().getPackage().getImplementationTitle();
	}

	@Override
	public String getEISProductVersion() throws ResourceException {
		return getClass().getPackage().getImplementationVersion();
	}

	@Override
	public int getMaxConnections() throws ResourceException {
		return 0;
	}

	@Override
	public String getUserName() throws ResourceException {
		return null;
	}

}
