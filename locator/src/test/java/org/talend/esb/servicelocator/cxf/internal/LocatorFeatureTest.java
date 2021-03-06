/*
 * #%L
 * Service Locator Client for CXF
 * %%
 * Copyright (C) 2011 - 2012 Talend Inc.
 * %%
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
 * #L%
 */
package org.talend.esb.servicelocator.cxf.internal;

import static org.easymock.EasyMock.expect;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.managers.ClientLifeCycleManagerImpl;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ConduitSelectorHolder;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.talend.esb.servicelocator.cxf.LocatorFeature;

public class LocatorFeatureTest extends EasyMockSupport {

	Bus busMock;
	LocatorRegistrar locatorRegistrarMock;
	LocatorSelectionStrategyMap locatorSelectionStrategyMap;
	ClassLoader cll;

	@Before
	public void setUp() {
		busMock = createMock(Bus.class);

		expect(busMock.getExtension(ClassLoader.class)).andStubReturn(cll);

		locatorRegistrarMock = createMock(LocatorRegistrar.class);
		locatorRegistrarMock.startListenForServers(busMock);
		EasyMock.expectLastCall().anyTimes();
		cll = this.getClass().getClassLoader();

		locatorSelectionStrategyMap = new LocatorSelectionStrategyMap();
		locatorSelectionStrategyMap.init();
	}

	@Test
	public void initializeClient() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);
		enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");


		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		EndpointInfo ei = new EndpointInfo();
		Service service = new org.apache.cxf.service.ServiceImpl();
		Endpoint endpoint = new EndpointImpl(busMock, service, ei);
		endpoint.put(LocatorFeature.KEY_STRATEGY, "randomSelectionStrategy");
		Client client = new ClientImpl(busMock, endpoint);

		LocatorTargetSelector selector = new LocatorTargetSelector();
		selector.setEndpoint(endpoint);

		client.setConduitSelector(selector);

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
	        lf.setLocatorRegistrar(locatorRegistrarMock);
	        lf.setClientEnabler(enabler);
		lf.initialize(client, busMock);

		Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector())
				.getStrategy() instanceof RandomSelectionStrategy);

	}

	@Test
	public void initializeClientsOneWithStrategy() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);
		enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
		lf.setLocatorRegistrar(locatorRegistrarMock);
	        lf.setClientEnabler(enabler);

		Client client1 = null;
		Client client2 = null;
		{
			EndpointInfo ei = new EndpointInfo();
			Service service = new org.apache.cxf.service.ServiceImpl();
			Endpoint endpoint = new EndpointImpl(busMock, service, ei);
			endpoint.put(LocatorFeature.KEY_STRATEGY, "randomSelectionStrategy");
			client1 = new ClientImpl(busMock, endpoint);

			LocatorTargetSelector selector = new LocatorTargetSelector();
			selector.setEndpoint(endpoint);

			client1.setConduitSelector(selector);

			lf.initialize(client1, busMock);
		}
		{
			EndpointInfo ei = new EndpointInfo();
			Service service = new org.apache.cxf.service.ServiceImpl();
			Endpoint endpoint = new EndpointImpl(busMock, service, ei);
			client2 = new ClientImpl(busMock, endpoint);

			LocatorTargetSelector selector = new LocatorTargetSelector();
			selector.setEndpoint(endpoint);

			client2.setConduitSelector(selector);
			lf.initialize(client2, busMock);
		}
		Assert.assertTrue(((LocatorTargetSelector) client1.getConduitSelector())
				.getStrategy() instanceof RandomSelectionStrategy);
		Assert.assertTrue(((LocatorTargetSelector) client2.getConduitSelector())
				.getStrategy() instanceof EvenDistributionSelectionStrategy);

	}

	@Test
	public void initializeClientDefault() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);

		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		EndpointInfo ei = new EndpointInfo();
		Service service = new org.apache.cxf.service.ServiceImpl();
		Endpoint endpoint = new EndpointImpl(busMock, service, ei);
		Client client = new ClientImpl(busMock, endpoint);

		LocatorTargetSelector selector = new LocatorTargetSelector();
		selector.setEndpoint(endpoint);

		client.setConduitSelector(selector);

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
                lf.setLocatorRegistrar(locatorRegistrarMock);
	        lf.setClientEnabler(enabler);
		lf.initialize(client, busMock);

		Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector())
				.getStrategy() instanceof DefaultSelectionStrategy);

	}

	@Test
	public void initializeClientsBothWithStrategies() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);
		enabler.setDefaultLocatorSelectionStrategy("defaultSelectionStrategy");

		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
                lf.setLocatorRegistrar(locatorRegistrarMock);
                lf.setClientEnabler(enabler);

		Client client1 = null;
		Client client2 = null;

		{
			EndpointInfo ei = new EndpointInfo();
			Service service = new org.apache.cxf.service.ServiceImpl();
			Endpoint endpoint = new EndpointImpl(busMock, service, ei);
			client1 = new ClientImpl(busMock, endpoint);
			endpoint.put(LocatorFeature.KEY_STRATEGY, "randomSelectionStrategy");
			LocatorTargetSelector selector = new LocatorTargetSelector();
			selector.setEndpoint(endpoint);
			client1.setConduitSelector(selector);
			lf.initialize(client1, busMock);
		}
		{
			EndpointInfo ei = new EndpointInfo();
			Service service = new org.apache.cxf.service.ServiceImpl();
			Endpoint endpoint = new EndpointImpl(busMock, service, ei);
			client2 = new ClientImpl(busMock, endpoint);
			LocatorTargetSelector selector = new LocatorTargetSelector();
			selector.setEndpoint(endpoint);
			client2.setConduitSelector(selector);
			endpoint.put(LocatorFeature.KEY_STRATEGY, "evenDistributionSelectionStrategy");
			lf.initialize(client2, busMock);
		}
		Assert.assertTrue(((LocatorTargetSelector) client1.getConduitSelector())
				.getStrategy() instanceof RandomSelectionStrategy);
		Assert.assertTrue(((LocatorTargetSelector) client2.getConduitSelector())
				.getStrategy() instanceof EvenDistributionSelectionStrategy);
	}

	@Test
	public void initializeClientConfiguration() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);
		enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		EndpointInfo ei = new EndpointInfo();
		Service service = new org.apache.cxf.service.ServiceImpl();
		Endpoint endpoint = new EndpointImpl(busMock, service, ei);
		endpoint.put(LocatorFeature.KEY_STRATEGY, "randomSelectionStrategy");
		ClientConfiguration client = new ClientConfiguration();

		LocatorTargetSelector selector = new LocatorTargetSelector();
		selector.setEndpoint(endpoint);

		client.setConduitSelector(selector);

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
		lf.setLocatorRegistrar(locatorRegistrarMock);
	        lf.setClientEnabler(enabler);
		lf.initialize((ConduitSelectorHolder) client, busMock);

		Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector())
				.getStrategy() instanceof RandomSelectionStrategy);

	}

	@Test
	public void initializeInterceptorProvider() throws EndpointException {
		LocatorClientEnabler enabler = new LocatorClientEnabler();

		enabler.setLocatorSelectionStrategyMap(locatorSelectionStrategyMap);
		enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

		ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
		expect(busMock.getExtension(ClientLifeCycleManager.class))
				.andStubReturn(clcm);

		replayAll();

		EndpointInfo ei = new EndpointInfo();
		Service service = new org.apache.cxf.service.ServiceImpl();
		Endpoint endpoint = new EndpointImpl(busMock, service, ei);
		endpoint.put(LocatorFeature.KEY_STRATEGY, "randomSelectionStrategy");
		ClientConfiguration client = new ClientConfiguration();

		LocatorTargetSelector selector = new LocatorTargetSelector();
		selector.setEndpoint(endpoint);

		client.setConduitSelector(selector);

		LocatorFeatureImpl lf = new LocatorFeatureImpl();
		lf.setLocatorRegistrar(locatorRegistrarMock);
	        lf.setClientEnabler(enabler);
		lf.initialize((InterceptorProvider) client, busMock);

		Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector())
				.getStrategy() instanceof RandomSelectionStrategy);
	}
}
