/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jersey;

import java.util.ArrayList;
import java.util.List;

import org.mule.api.MuleException;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultJavaComponent;
import org.mule.context.notification.MuleContextNotification;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.AbstractConnector;

/**
 * 
 */
public class JerseyConnector extends AbstractConnector implements MuleContextNotificationListener {
    private List<SedaService> services = new ArrayList<SedaService>();

    public JerseyConnector() {
        super();
    }
    
    public boolean supportsProtocol(String protocol)
    {
        // we can listen on any protocol provided that the necessary 
        // http headers are there.
        return protocol.startsWith("jersey:");
    }
    
    protected void registerReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep)
        throws MuleException {
        JerseyMessageReceiver jReceiver = (JerseyMessageReceiver)receiver;
        // best I can come up with for now
        String name = new Integer(jReceiver.hashCode()).toString();
        
        // TODO MULE-2228 Simplify this API
        SedaService c = new SedaService();
        c.setName("_jerseyConnector" + name + jReceiver.hashCode());
        c.setModel(muleContext.getRegistry().lookupSystemModel());

        c.setComponent(new DefaultJavaComponent(new SingletonObjectFactory(jReceiver)));

        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if the port is different
        String endpoint = receiver.getEndpointURI().getAddress();

        boolean sync = receiver.getEndpoint().isSynchronous();

        EndpointBuilder serviceEndpointbuilder = new EndpointURIEndpointBuilder(endpoint,
                                                                                muleContext);
        serviceEndpointbuilder.setSynchronous(sync);
        serviceEndpointbuilder.setName(ep.getScheme() + ":" + name);
        // Set the transformers on the endpoint too
        serviceEndpointbuilder.setTransformers(receiver.getEndpoint().getTransformers().isEmpty() ? null
                                                                                                  : receiver.getEndpoint().getTransformers());
        serviceEndpointbuilder.setResponseTransformers(receiver.getEndpoint().getResponseTransformers().isEmpty() ? null
                                                                                                                 : receiver.getEndpoint().getResponseTransformers());
        // set the filter on the axis endpoint on the real receiver endpoint
        serviceEndpointbuilder.setFilter(receiver.getEndpoint().getFilter());
        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        serviceEndpointbuilder.setSecurityFilter(receiver.getEndpoint().getSecurityFilter());
    
        // TODO Do we really need to modify the existing receiver endpoint? What happnes if we don't security,
        // filters and transformers will get invoked twice?
        EndpointBuilder receiverEndpointBuilder = new EndpointURIEndpointBuilder(receiver.getEndpoint(),
            muleContext);
        // Remove the Axis filter now
        receiverEndpointBuilder.setFilter(null);
        // Remove the Axis Receiver Security filter now
        receiverEndpointBuilder.setSecurityFilter(null);
    
        InboundEndpoint serviceEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(serviceEndpointbuilder);
    
        InboundEndpoint receiverEndpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint(receiverEndpointBuilder);
    
        receiver.setEndpoint(receiverEndpoint);
    
        c.setInboundRouter(new DefaultInboundRouterCollection());
        c.getInboundRouter().addEndpoint(serviceEndpoint);
        
        services.add(c);
    }

    public void onNotification(ServerNotification event) {
        // We need to register the CXF service service once the model
        // starts because
        // when the model starts listeners on components are started, thus
        // all listener
        // need to be registered for this connector before the CXF service
        // service is registered. The implication of this is that to add a
        // new service and a
        // different http port the model needs to be restarted before the
        // listener is available
        if (event.getAction() == MuleContextNotification.CONTEXT_STARTED) {
            for (Service c : services) {
                try {
                    muleContext.getRegistry().registerService(c);
                } catch (MuleException e) {
                    handleException(e);
                }
            }
        }
    }

    @Override
    protected void doConnect() throws Exception {
    }

    @Override
    protected void doDisconnect() throws Exception {
    }

    @Override
    protected void doDispose() {
    }

    @Override
    protected void doInitialise() throws InitialisationException {
        // Registers the listener
        try {
            muleContext.registerListener(this);
        } catch (Exception e) {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    protected void doStart() throws MuleException {
    }

    @Override
    protected void doStop() throws MuleException {
    }

    public String getProtocol() {
        return "jersey";
    }

}
