/*
 * $Id: XFireNamespaceHandler.java 7167 2007-06-19 19:57:12Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jersey.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.transport.jersey.JerseyConnector;

public class JerseyNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerConnectorDefinitionParser(JerseyConnector.class);
        registerMetaTransportEndpoints("jersey");
    }
}