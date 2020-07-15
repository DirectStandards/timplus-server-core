/*
 * Copyright (C) 1999-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.filetransfer.proxy;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.PacketException;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.RoutableChannelHandler;
import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.DiscoItem;
import org.jivesoftware.openfire.disco.DiscoItemsProvider;
import org.jivesoftware.openfire.disco.DiscoServerItem;
import org.jivesoftware.openfire.disco.ServerItemsProvider;
import org.jivesoftware.openfire.domain.DomainManager;
import org.jivesoftware.openfire.filetransfer.FileTransferManager;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyServerCredential;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyServerCredentialManager;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.openfire.spi.ConnectionListener;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.openfire.spi.ConnectionType;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

/**
 * Manages the transfering of files between two remote entities on the jabber network.
 * This class acts independtly as a Jabber component from the rest of the server, according to
 * the Jabber <a href="http://www.jabber.org/jeps/jep-0065.html">SOCKS5 bytestreams protocol</a>.
 *
 * @author Alexander Wenckus
 */
public class FileTransferProxy extends BasicModule
        implements ServerItemsProvider, DiscoInfoProvider, DiscoItemsProvider,
        RoutableChannelHandler {

	private static final int DNSName_TYPE = 2;
	
    private static final Logger Log = LoggerFactory.getLogger( FileTransferProxy.class);

    /**
     * The JiveProperty relating to whether or not the file treansfer proxy is enabled.
     */
    public static final String JIVEPROPERTY_PROXY_ENABLED = "xmpp.proxy.enabled";

    /**
     * The JiveProperty relating to the port the proxy is operating on. Changing this value requires a restart of the
     * proxy.
     */
    public static final String JIVEPROPERTY_PORT = "xmpp.proxy.port";

    /**
     * Name of the property that hardcodes the external IP that is being listened on.
     */
    public static final String PROPERTY_EXTERNALIP = "xmpp.proxy.externalip";

    /**
     * Whether or not the file transfer proxy is enabled by default.
     */
    public static final boolean DEFAULT_IS_PROXY_ENABLED = true;

    /**
     * The default port of the file transfer proxy
     */
    public static final int DEFAULT_PORT = 7777;

    /**
     * The namespace for requesting single username/passwords to the proxy service.
     */
    public static final String UP_REQUEST_NAMESPACE = "http://standards.directtrust.org/DS2019-02/filetransfer/proxy/auth#onetimeup";
    
    /**
     * The element for the credential request.
     */
    public static final String UP_REQUEST_ELEMENT = "credRequest";
    
    private String proxyServiceName;

    private IQHandlerInfo info;
    private RoutingTable routingTable;
    private PacketRouter router;
    private ProxyConnectionManager connectionManager;

    // The address to operate on. Null for any address.
    private InetAddress bindInterface;

    private ConnectionConfiguration configuration;

    public FileTransferProxy() 
    {
        super("SOCKS5 file transfer proxy");

        info = new IQHandlerInfo("query", FileTransferManager.NAMESPACE_BYTESTREAMS);
        
        PropertyEventDispatcher.addListener(new FileTransferPropertyListener());
    }

    public boolean handleIQ(IQ packet) throws UnauthorizedException {
        Element childElement = packet.getChildElement();
        String namespace = null;

        // ignore errors
        if (packet.getType() == IQ.Type.error) {
            return true;
        }
        if (childElement != null) {
            namespace = childElement.getNamespaceURI();
        }

        if ("http://jabber.org/protocol/disco#info".equals(namespace)) {
            IQ reply = XMPPServer.getInstance().getIQDiscoInfoHandler().handleIQ(packet);
            router.route(reply);
            return true;
        }
        else if ("http://jabber.org/protocol/disco#items".equals(namespace)) {
            // a component
            IQ reply = XMPPServer.getInstance().getIQDiscoItemsHandler().handleIQ(packet);
            router.route(reply);
            return true;
        }
        else if (FileTransferManager.NAMESPACE_BYTESTREAMS.equals(namespace)) {
            if (packet.getType() == IQ.Type.get) {
                IQ reply = IQ.createResultIQ(packet);
                Element newChild = reply.setChildElement("query", FileTransferManager.NAMESPACE_BYTESTREAMS);

                final String externalIPs = JiveGlobals.getProperty( PROPERTY_EXTERNALIP );
                if ( externalIPs != null && !externalIPs.isEmpty() )
                {
                	String streamHosts[] = externalIPs.split(",");
                	
                	for (String streamHost : streamHosts)
                	{
	                	
	                    // OF-512: Override the automatic detection with a specific address (useful for NATs, proxies, etc)
	                    final Element response = newChild.addElement( "streamhost" );
	                    response.addAttribute( "jid", packet.getTo().toString());
	                    response.addAttribute( "host", streamHost );
	                    response.addAttribute( "port", String.valueOf( connectionManager.getProxyPort() ) );
                	}
                	
                }
                else
                {
                    // Report all network addresses that we know that we're servicing.
                    for ( final InetAddress address : getAddresses() )
                    {
                        final Element response = newChild.addElement( "streamhost" );
                        response.addAttribute( "jid", packet.getTo().toString() );
                        response.addAttribute( "host", address.getHostAddress() );
                        response.addAttribute( "port", String.valueOf( connectionManager.getProxyPort() ) );
                    }
                }
                router.route(reply);
                return true;
            }
            else if (packet.getType() == IQ.Type.set) {
                String sid = childElement.attributeValue("sid");
                JID from = packet.getFrom();
                JID to = new JID(childElement.elementTextTrim("activate"));

                IQ reply = IQ.createResultIQ(packet);
                try {
                    connectionManager.activate(from, to, sid);
                }
                catch (IllegalArgumentException ie) {
                    Log.error("Error activating connection", ie);
                    reply.setType(IQ.Type.error);
                    reply.setError(new PacketError(PacketError.Condition.not_allowed));
                }

                router.route(reply);
                return true;
            }
        }
        else if (UP_REQUEST_NAMESPACE.equals(namespace))
        {
        	IQ reply = IQ.createResultIQ(packet);
        	try
        	{
	        	final ProxyServerCredential cred = ProxyServerCredentialManager.getInstance().createCredential();
	        	
	        	Element credExt = reply.setChildElement(UP_REQUEST_ELEMENT, UP_REQUEST_NAMESPACE);
	        	
	        	final Element credElement = credExt.addElement("credentials");
	        	credElement.addAttribute("subject", cred.getSubject());
	        	credElement.addAttribute("secret", cred.getSecret());
	        	
	        	final Element caElement = credExt.addElement("ca");
	        	caElement.setText(getProxyCA(packet.getTo()));    	

        	}
        	catch (Exception e)
        	{
        		Log.error("Error creating one time username/password for proxy connection", e);
        		reply = IQ.createResultIQ(packet);
                reply.setType(IQ.Type.error);
                reply.setError(new PacketError(PacketError.Condition.internal_server_error));
        	}
        	
            router.route(reply);
            return true;
        }
        return false;
    }

    public IQHandlerInfo getInfo() {
        return info;
    }

    @Override
    public void initialize( XMPPServer server )
    {
        super.initialize(server);

        proxyServiceName = JiveGlobals.getProperty("xmpp.proxy.service", "ftproxystream");
        routingTable = server.getRoutingTable();
        router = server.getPacketRouter();

        connectionManager = new ProxyConnectionManager(getFileTransferManager(server));
    }

    /**
     * Returns the IP address(es) that the proxy connection manager is servicing.
     */
    private Set<InetAddress> getAddresses()
    {
        final String interfaceName = JiveGlobals.getXMLProperty( "network.interface" );

        final Set<InetAddress> result = new HashSet<>();

        // Let's see if we hardcoded a specific interface, then use its address.
        if ( interfaceName != null && !interfaceName.trim().isEmpty() )
        {
            try
            {
                bindInterface = InetAddress.getByName( interfaceName.trim() );
                result.add( bindInterface );
                return result;
            }
            catch ( UnknownHostException e )
            {
                Log.error( "Error binding to network.interface '{}'", interfaceName, e );
            }
        }

        // When there's no specific address configured, return all available (non-loopback) addresses.
        try
        {
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while ( networkInterfaces.hasMoreElements() )
            {
                final NetworkInterface networkInterface = networkInterfaces.nextElement();
                if ( networkInterface.isLoopback() )
                {
                    continue;
                }
                final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while ( inetAddresses.hasMoreElements() )
                {
                    result.add( inetAddresses.nextElement() );
                }
            }
        }
        catch ( SocketException e )
        {
            Log.error( "Error determining all addresses for this server", e );
        }
        return result;
    }

    private FileTransferManager getFileTransferManager(XMPPServer server) {
        return server.getFileTransferManager();
    }

    @Override
    public void start() {
        super.start();

        if (isEnabled()) {
            startProxy();
        }
        else {
            XMPPServer.getInstance().getIQDiscoItemsHandler().removeServerItemsProvider(this);
        }
    }

    private void startProxy() {
        connectionManager.processConnections(bindInterface, getProxyPort());
        
        for (JID serviceAddress : getServiceAddresses())
        	routingTable.addComponentRoute(serviceAddress, this);
        
        XMPPServer server = XMPPServer.getInstance();

        server.getIQDiscoItemsHandler().addServerItemsProvider(this);
    }

    @Override
    public void stop() {
        super.stop();

        for (JID serviceAddress : getServiceAddresses())
        {
        	XMPPServer.getInstance().getIQDiscoItemsHandler()
                .removeComponentItem(serviceAddress.toString());
        
        	routingTable.removeComponentRoute(serviceAddress);
        }

        connectionManager.disable();
    }

    @Override
    public void destroy() {
        super.destroy();

        connectionManager.shutdown();
    }

    public void enableFileTransferProxy(boolean isEnabled) {
        JiveGlobals.setProperty(FileTransferProxy.JIVEPROPERTY_PROXY_ENABLED,
                                Boolean.toString(isEnabled));
        setEnabled( isEnabled );
    }

    private void setEnabled(boolean isEnabled) {
        if (isEnabled) {
            startProxy();
        }
        else {
            stop();
        }
    }

    /**
     * Returns true if the file transfer proxy is currently enabled and false if it is not.
     *
     * @return Returns true if the file transfer proxy is currently enabled and false if it is not.
     */
    public boolean isProxyEnabled() {
        return connectionManager.isRunning() &&
                JiveGlobals.getBooleanProperty(JIVEPROPERTY_PROXY_ENABLED, DEFAULT_IS_PROXY_ENABLED);
    }

    private boolean isEnabled() {
        return JiveGlobals.getBooleanProperty(JIVEPROPERTY_PROXY_ENABLED, DEFAULT_IS_PROXY_ENABLED);
    }

    /**
     * Sets the port that the proxy operates on. This requires a restart of the file transfer proxy.
     *
     * @param port The port.
     */
    public void setProxyPort(int port) {
        JiveGlobals.setProperty(JIVEPROPERTY_PORT, Integer.toString(port));
    }

    /**
     * Returns the port that the file transfer proxy is opertating on.
     *
     * @return Returns the port that the file transfer proxy is opertating on.
     */
    public int getProxyPort() {
        return JiveGlobals.getIntProperty(JIVEPROPERTY_PORT, DEFAULT_PORT);
    }

    /**
     * Returns the fully-qualifed domain name of this chat service.
     * The domain is composed by the service name and the
     * name of the XMPP server where the service is running.
     *
     * @return the file transfer server domain (service name + host name).
     */
    public Collection<String> getServiceDomains() 
    {
    	final Collection<String> serviceDomains = new ArrayList<>();
    	for (String domainName : DomainManager.getInstance().getDomainNames(true))
    		serviceDomains.add(proxyServiceName + "." + domainName);
    	
    	return serviceDomains;
    }

    @Override
	public JID getAddress()
	{
    	/* not used */
		return null;
	}

    public Collection<JID> getServiceAddresses() 
    {
    	final Collection<JID> retVal = new ArrayList<>();
    	
        for (String serviceDomain : this.getServiceDomains())
        	retVal.add(new JID(null, serviceDomain, null));
        	
        return retVal;
    }

    @Override
    public Iterator<DiscoServerItem> getItems() 
    {

    	final Collection<DiscoServerItem> retVal = new ArrayList<>();
        
    	for (String serviceDomain : this.getServiceDomains())
    	{
    	
    		final DiscoServerItem item = new DiscoServerItem(new JID(
    				serviceDomain), "Socks 5 Bytestreams Proxy", null, null, this,
                                                         this);
    		
    		retVal.add(item);
    	}
    	
        return retVal.iterator();
    }

    @Override
    public Iterator<Element> getIdentities(String name, String node, JID senderJID, String domain) {
        // Answer the identity of the proxy
        Element identity = DocumentHelper.createElement("identity");
        identity.addAttribute("category", "proxy");
        identity.addAttribute("name", "SOCKS5 Bytestreams Service");
        identity.addAttribute("type", "bytestreams");

        return Collections.singleton(identity).iterator();
    }

    @Override
    public Iterator<String> getFeatures(String name, String node, JID senderJID, String domain) {
        return Arrays.asList(FileTransferManager.NAMESPACE_BYTESTREAMS,
                             "http://jabber.org/protocol/disco#info").iterator();
    }

    @Override
    public DataForm getExtendedInfo(String name, String node, JID senderJID, String domain) {
        return null;
    }

    @Override
    public Set<DataForm> getExtendedInfos(String name, String node, JID senderJID, String domain) {
        return new HashSet<DataForm>();
    }

    @Override
    public boolean hasInfo(String name, String node, JID senderJID, String domain) {
        return true;
    }

    @Override
    public Iterator<DiscoItem> getItems(String name, String node, JID senderJID, String domain) {
        // A proxy server has no items
        return new ArrayList<DiscoItem>().iterator();
    }

    @Override
    public void process(Packet packet) throws UnauthorizedException, PacketException {
        // Check if the packet is a disco request or a packet with namespace iq:register
        if (packet instanceof IQ) {
            if (handleIQ((IQ) packet)) {
                // Do nothing
            }
            else {
                IQ reply = IQ.createResultIQ((IQ) packet);
                reply.setChildElement(((IQ) packet).getChildElement().createCopy());
                reply.setError(PacketError.Condition.feature_not_implemented);
                router.route(reply);
            }
        }
    }

    private class FileTransferPropertyListener implements PropertyEventListener {
        @Override
        public void propertySet(String property, Map params)
        {
            if ( isEnabled() )
            {
                // Restart when configuration changed.
                if (JIVEPROPERTY_PORT.equalsIgnoreCase( property ))
                {
                    setEnabled( false );
                    setEnabled( true );
                }
            }

            if(JIVEPROPERTY_PROXY_ENABLED.equalsIgnoreCase(property)) {
                Object value = params.get("value");
                boolean isEnabled = (value != null ? Boolean.parseBoolean(value.toString()) : DEFAULT_IS_PROXY_ENABLED);
                setEnabled(isEnabled);
            }
        }

        @Override
        public void propertyDeleted(String property, Map params) {
            if(JIVEPROPERTY_PROXY_ENABLED.equalsIgnoreCase(property)) {
                setEnabled(DEFAULT_IS_PROXY_ENABLED);
            }

            if ( isEnabled() )
            {
                // Restart when configuration changed.
                if (JIVEPROPERTY_PORT.equalsIgnoreCase( property ) )
                {
                    setEnabled( false );
                    setEnabled( true );
                }
            }
        }

        @Override
        public void xmlPropertySet(String property, Map params) {
        }

        @Override
        public void xmlPropertyDeleted(String property, Map params) {
        }
    }
    
    protected String getProxyCA(JID proxyJID)
    {
    	if (configuration == null)
    	{
    		synchronized(this)
    		{
	    		final ConnectionManagerImpl connectionManager = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager());
	    		final ConnectionListener listener = connectionManager.getListener( ConnectionType.SOCKET_S2S, false);
	        
	    		this.configuration = listener.generateConnectionConfiguration();
    		}
    	}
    	
        Builder builder = Builder.newInstance(configuration.getIdentityStore().getStore(),
                new PasswordProtection(configuration.getIdentityStoreConfiguration().getPassword()));
        
        X509Certificate trustCACert = null;
        
        try
		{
        	// find a certificate in the identity store that has an SAN that
        	// matches the JID of the file transfer service
        	X509Certificate proxyServerCert = null;
        	
			final KeyStore identityKs = builder.getKeyStore();
			if (identityKs != null)
			{
		    	for (Enumeration<String> e = identityKs.aliases(); e.hasMoreElements(); )
		    	{
		    		final String alias = e.nextElement();
		            if (identityKs.isKeyEntry(alias) == false) 
		            {
		                continue;
		            }
		            
		            final Certificate possibleCert = identityKs.getCertificate(alias);
		            if (possibleCert != null && possibleCert instanceof X509Certificate)
		            {
		            	final X509Certificate xCert = X509Certificate.class.cast(possibleCert);
		            	
			            final Collection<List<?>> subjAltNames = xCert.getSubjectAlternativeNames();
			            if (subjAltNames != null) 
			            {
			                for ( List<?> next : subjAltNames) 
			                {
			                    if (((Integer)next.get(0)).intValue() == DNSName_TYPE) 
			                    {
			                        String dnsName = (String)next.get(1);
			                        if (proxyJID.getDomain().toLowerCase().equals(dnsName.toLowerCase())) 
			                        {
			                        	// found
			                        	proxyServerCert = xCert;
			                        	break;
			                        }
			                    }
			                }
			                if (proxyServerCert != null)
			                	break;
			            }
		            }
		    	}
		    	
			}
			
			if (proxyServerCert != null)
			{
		        builder = Builder.newInstance(configuration.getTrustStore().getStore(),
		                new PasswordProtection(configuration.getTrustStoreConfiguration().getPassword()));
				// find the anchor that this chains to
		        
				final KeyStore trustKS = builder.getKeyStore();
				if (trustKS != null)
				{
			    	for (Enumeration<String> e = trustKS.aliases(); e.hasMoreElements(); )
			    	{
			    		final String alias = e.nextElement();

			            final Certificate possibleCACert = trustKS.getCertificate(alias);
			            if (possibleCACert != null && possibleCACert instanceof X509Certificate)
			            {
			            	final X509Certificate xCACert = X509Certificate.class.cast(possibleCACert);
			            	
			        		CertPath certPath = null;
			            	CertificateFactory factory = CertificateFactory.getInstance("X509");
			            	
			            	List<Certificate> certs = new ArrayList<Certificate>();
			            	certs.add(proxyServerCert);
			            	
			            	final Set<TrustAnchor> trustAnchorSet = new HashSet<TrustAnchor>();
			        		
			            	
			            	trustAnchorSet.add(new TrustAnchor(xCACert, null));
			            	
			                final PKIXParameters params = new PKIXParameters(trustAnchorSet); 
			                
			                params.setRevocationEnabled(false);
			                
			            	certPath = factory.generateCertPath(certs);
			            	final CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX", "BC");    		
			        		
			            	try
			            	{
			            		pathValidator.validate(certPath, params);
			            		
			            		trustCACert = xCACert;
			            		break;
			            	}
			            	catch (Exception v)
			            	{
			            		/* no-op, not in the trust chain */
			            	}
			            }
			    	}
			    	
				}		        
			}
		} 
        catch (Exception e)
		{
        	throw new IllegalStateException("Error searching for proxy server CA certificate", e);
		}
        
        if (trustCACert != null)
        {
        	try
			{
				return Base64.getEncoder().encodeToString(trustCACert.getEncoded());
			} 
        	catch (CertificateEncodingException e)
			{
        		throw new IllegalStateException("Error encoding proxy CA certificate to PEM format", e);
			}
        }
        else
        {
        	throw new IllegalStateException("Could not find a CA certificate for the domain " + proxyJID.getDomain());
        }
    }
}
