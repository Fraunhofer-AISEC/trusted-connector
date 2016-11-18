/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.both;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.jsse.ClientAuthentication;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SSLContextServerParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fhg.camel.ids.client.TestServletFactory;
import de.fhg.camel.ids.client.WsComponent;
import de.fhg.camel.ids.server.WebsocketComponent;

/**
 *
 */
public class WssProducerConsumerTest extends CamelTestSupport {
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final int PORT = AvailablePortFinder.getNextAvailable();
    protected Server server;
    private Process tpm2dc = null;
    private Process tpm2ds = null;
    private Process ttp = null;
    private File socketServer;
    private File socketClient;
    protected List<Object> messages;
	private static String PWD = "password";
	private String dockerName = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dmock:latest";
	private String sockets = "tpm2ds.sock";
	private String socketc = "tpm2dc.sock";	
	
    @Before
    public void initMockServer() throws IOException, InterruptedException {
		socketServer = new File("mock/socket/"+sockets);
		socketClient = new File("mock/socket/"+socketc);
		
		String folder = socketServer.getAbsolutePath().substring(0, socketServer.getAbsolutePath().length() - sockets.length());

		// build a docker imagess
    	ProcessBuilder image = new ProcessBuilder("docker", "build", "-t", dockerName, "./mock");
    	
    	// then start the docker image as tpm2d for the server
    	ProcessBuilder tpm2dsService = new ProcessBuilder("docker", 
    			"run", 
    			"--rm",
    			"-v",
    			folder +":/socket/",
    			dockerName,
    			"su -m tpm2d -c '/tpm2d/tpm2d.py /socket/tpm2ds.sock'");
    	
    	// then start the docker image as tpm2d for the client
    	ProcessBuilder tpm2dcService = new ProcessBuilder("docker", 
    			"run", 
    			"--rm",
    			"-v",
    			folder +":/socket/",
    			dockerName,
    			"su -m tpm2d -c '/tpm2d/tpm2d.py /socket/tpm2dc.sock'");
    	
    	// then start a ttp with a port
    	ProcessBuilder ttpService = new ProcessBuilder("docker", "run", "--rm","-P", dockerName, "/tpm2d/ttp.py");

    	// start build process
    	Process generator = image.start();
    	
    	// start tpm2d services
    	tpm2ds = tpm2dsService.start();
    	tpm2dc = tpm2dcService.start();
    	
    	// start ttp service
    	ttp = ttpService.start();
    	
    	String stdOut2 = getInputAsString(ttp.getInputStream());
    	String stdErr2 = getInputAsString(ttp.getErrorStream());
    	String stdOut3 = getInputAsString(tpm2ds.getInputStream());
    	String stdErr3 = getInputAsString(tpm2ds.getErrorStream());
    	String stdOut4 = getInputAsString(tpm2dc.getInputStream());
    	String stdErr4 = getInputAsString(tpm2dc.getErrorStream());

    	log.debug("---------------------------------------->" + stdOut2);
    	log.debug("---------------------------------------->" + stdErr2);
    	log.debug("---------------------------------------->" + stdOut3);
    	log.debug("---------------------------------------->" + stdErr3);
    	log.debug("---------------------------------------->" + stdOut4);
    	log.debug("---------------------------------------->" + stdErr4);		
    }
    
    @After
    public void teardownMockServer() throws Exception {
    	if(tpm2dc != null && tpm2dc.isAlive()) {
    		tpm2dc.destroy();
    		socketClient.delete();
    	}
    	if(tpm2ds != null && tpm2ds.isAlive()) {
    		tpm2ds.destroy();
    		socketServer.delete();
    	}
    	if(ttp != null && ttp.isAlive()) {
    		ttp.destroy();
    	}
    	server.stop();
        server.destroy();    	
    }
    
    private String getInputAsString(InputStream is) {
        try(java.util.Scanner s = new java.util.Scanner(is))  { 
            return s.useDelimiter("\\A").hasNext() ? s.next() : ""; 
        }
     }    
    
    public void startTestServer() throws Exception {
        // start a simple websocket echo service
        server = new Server(PORT);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");
        ctx.addServlet(TestServletFactory.class.getName(), "/*");

        server.setHandler(ctx);
        
        server.start();
        assertTrue(server.isStarted());      
    }
    
    public void stopTestServer() throws Exception {
        server.stop();
        server.destroy();
    }

    @Override
    public void setUp() throws Exception {
        
        ClassLoader classLoader = getClass().getClassLoader();
        URL trustStoreURL = classLoader.getResource("jsse/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStore", trustStoreURL.getFile());
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        startTestServer();
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        stopTestServer();
    }

    @Test
    public void testTwoRoutes() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();
    }

    @Test
    public void testTwoRoutesRestartConsumer() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();

        resetMocks();

        log.info("Restarting bar route");
        context.stopRoute("bar");
        Thread.sleep(500);
        context.startRoute("bar");

        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();
    }
    
    private static SSLContextParameters defineClientSSLContextClientParameters() {

        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(Thread.currentThread().getContextClassLoader().getResource("jsse/client-keystore.jks").toString());
        ksp.setPassword(PWD);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(PWD);
        kmp.setKeyStore(ksp);

        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(Thread.currentThread().getContextClassLoader().getResource("jsse/client-truststore.jks").toString());
        
        tsp.setPassword(PWD);
        
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);

        SSLContextServerParameters scsp = new SSLContextServerParameters();
        //scsp.setClientAuthentication(ClientAuthentication.REQUIRE.name());
        scsp.setClientAuthentication(ClientAuthentication.NONE.name());

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        sslContextParameters.setTrustManagers(tmp);
        sslContextParameters.setServerParameters(scsp);
        

        return sslContextParameters;
    }
    
    private static SSLContextParameters defineServerSSLContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(Thread.currentThread().getContextClassLoader().getResource("jsse/server-keystore.jks").toString());
        ksp.setPassword(PWD);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(PWD);
        kmp.setKeyStore(ksp);

        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(Thread.currentThread().getContextClassLoader().getResource("jsse/server-truststore.jks").toString());
        tsp.setPassword(PWD);
        
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);

        SSLContextServerParameters scsp = new SSLContextServerParameters();
        //scsp.setClientAuthentication(ClientAuthentication.REQUIRE.name());
        scsp.setClientAuthentication(ClientAuthentication.NONE.name());
	
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        sslContextParameters.setTrustManagers(tmp);
        sslContextParameters.setServerParameters(scsp);
	   
	
	   return sslContextParameters;
    }
    
    @Override
    protected RouteBuilder[] createRouteBuilders() throws Exception {
        RouteBuilder[] rbs = new RouteBuilder[2];
        
        // An IDS consumer
        rbs[0] = new RouteBuilder() {
            public void configure() {
        		
            	// Needed to configure TLS on the client side
		        WsComponent wsComponent = (WsComponent) context.getComponent("idsclient");
		        wsComponent.setSslContextParameters(defineClientSSLContextClientParameters());
	        
		        from("direct:input").routeId("foo")
                	.log(">>> Message from direct to WebSocket Client : ${body}")
                	.to("idsclient://localhost:9292/echo")
                    .log(">>> Message from WebSocket Client to server: ${body}");
                }
        };
        
        // An IDS provider
        rbs[1] = new RouteBuilder() {
            public void configure() {
            	
            		// Needed to configure TLS on the server side
            		WebsocketComponent websocketComponent = (WebsocketComponent) context.getComponent("idsserver");
					websocketComponent.setSslContextParameters(defineServerSSLContextParameters());

					// This route is set to use TLS, referring to the parameters set above
                    from("idsserver:localhost:9292/echo")
                    .log(">>> Message from WebSocket Server to mock: ${body}")
                	.to("mock:result");
            }
        };
        return rbs;
    }
}
