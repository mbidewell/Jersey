package org.mule.transport.jersey;

import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.servlet.MuleReceiverServlet;

public abstract class AbstractServletTest extends FunctionalTestCase {
    public static final int HTTP_PORT = 63088;

    private Server httpServer;
    private String context;
    
    
    public AbstractServletTest(String context) {
        super();
        this.context = context;
    }

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();
        
        httpServer = new Server();
        SelectChannelConnector conn = new SelectChannelConnector();
        conn.setPort(HTTP_PORT);
        httpServer.addConnector(conn);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(MuleReceiverServlet.class, context);
        httpServer.addHandler(handler);
        
        httpServer.start();
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }
    }
    
    public void testBasic(String root) throws Exception
    {
        MuleClient client = new MuleClient();
        
        MuleMessage result = client.send(root + "/helloworld", "", null);
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello World", result.getPayloadAsString());
        
        result = client.send(root + "/hello", "", null);
        assertEquals(404, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        result = client.send(root + "/helloworld", "", props);
        assertEquals(405, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_DELETE);
        result = client.send(root + "/helloworld", "", props);
        assertEquals("Hello World Delete", result.getPayloadAsString());
        assertEquals(200, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
    
}
