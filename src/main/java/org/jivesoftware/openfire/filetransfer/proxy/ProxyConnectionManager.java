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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.StandardConstants;

import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.ssl.SSLObjectFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.cluster.ClusterException;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNode;
import org.jivesoftware.openfire.filetransfer.FileTransferManager;
import org.jivesoftware.openfire.filetransfer.FileTransferRejectedException;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyCredentialException;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyServerCredential;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyServerCredentialManager;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.openfire.spi.ConnectionType;
import org.jivesoftware.openfire.spi.EncryptionArtifactFactory;
import org.jivesoftware.openfire.stats.Statistic;
import org.jivesoftware.openfire.stats.StatisticsManager;
import org.jivesoftware.openfire.stats.i18nStatistic;
import org.jivesoftware.util.ClassUtils;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * Manages the connections to the proxy server. The connections go through two stages before
 * file transfer begins. The first stage is when the file transfer target initiates a connection
 * to this manager. Stage two is when the initiator connects, the manager will then match the two
 * connections using the unique SHA-1 hash defined in the SOCKS5 protocol.
 *
 * @author Alexander Wenckus
 */
public class ProxyConnectionManager {

	public static final String FILE_TRANSFER_CACHE_NAME = "File Transfer";

	public static final String CLUSTER_CROSS_PROXY_MAP_CACHE_NAME = "Cluster Cross Proxy Map";
	
    private static final Logger Log = LoggerFactory.getLogger(ProxyConnectionManager.class);

    private static final String proxyTransferRate = "proxyTransferRate";

    private Cache<String, ProxyTransfer> connectionMap;

    private Cache<String, ClusterCrossProxyInfo> clusterCrossProxyMap;
    
    private final Object connectionLock = new Object();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private Future<?> socketProcess;

    private ServerSocket serverSocket;

    private int proxyPort;

    private FileTransferManager transferManager;

    private String className;

    public ProxyConnectionManager(FileTransferManager manager) {
        
        connectionMap = CacheFactory.createCache(FILE_TRANSFER_CACHE_NAME);

        clusterCrossProxyMap = CacheFactory.createCache(CLUSTER_CROSS_PROXY_MAP_CACHE_NAME);
        
        className = JiveGlobals.getProperty("provider.transfer.proxy",
                "org.jivesoftware.openfire.filetransfer.proxy.DefaultProxyTransfer");

        transferManager = manager;
        StatisticsManager.getInstance().addStatistic(proxyTransferRate, new ProxyTracker());
    }

    /*
    * Processes the clients connecting to the proxy matching the initiator and target together.
    * This is the main loop of the manager which will run until the process is canceled.
    */
    synchronized void processConnections(final InetAddress bindInterface, final int port) {
        if (socketProcess != null) {
            if (proxyPort == port) {
                return;
            }
        }
        reset();
        socketProcess = executor.submit(new Runnable() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    final ConnectionManagerImpl connectionManager = ( (ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager() );
                    final ConnectionConfiguration configuration = connectionManager.getListener( ConnectionType.SOCKET_S2S, true ).generateConnectionConfiguration();

                	final SslContextFactory sslContextFactory = new EncryptionArtifactFactory( configuration ).getSslContextFactory();
                    
                    sslContextFactory.start();
                    
                	final ServerSocketFactory socketFactory = sslContextFactory.getSslContext().getServerSocketFactory();

                    serverSocket = socketFactory.createServerSocket(port, -1, bindInterface);

                    if (serverSocket instanceof SSLServerSocket)
                    {
                    	final SSLServerSocket sslServerSocket = SSLServerSocket.class.cast(serverSocket);
                    	final SSLParameters params = sslServerSocket.getSSLParameters();
                    	
                    	final SNIMatcher matcher = SSLObjectFactory.createAliasMatcher();
                    	
                    	final Collection<SNIMatcher> existingMatchers = params.getSNIMatchers();
                    	final List<SNIMatcher> matchers = 
                    			(existingMatchers != null && !existingMatchers.isEmpty()) ? new ArrayList<>(params.getSNIMatchers()) :
                    				new ArrayList<>();
                    			
                    	matchers.add(matcher);
                    	
                    	params.setSNIMatchers(matchers);
                    	
                    	sslServerSocket.setSSLParameters(params);
                    }
                    
                }
                catch (Exception e) 
                {
                    Log.error("Error creating server socket", e);
                    return;
                }
                
                while (serverSocket.isBound()) 
                {
                    final Socket socket;
                    try 
                    {
                        socket = serverSocket.accept();
                    }
                    catch (IOException e) 
                    {
                        if (!serverSocket.isClosed()) 
                        {
                            Log.error("Error accepting proxy connection", e);
                            continue;
                        }
                        else 
                        {
                        	Log.error("Proxy conenction server socket has been closed and will not accept any more connections.", e);
                            break;
                        }
                    }
                    Log.info("Accepted proxy socket connection from " + socket.getInetAddress().getHostAddress());
                    executor.submit(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            try 
                            {
                                processConnection(socket);
                            }
                            catch (IOException ie) 
                            {
                                Log.error("Error processing file transfer proxy connection", ie);
                                try 
                                {
                                    socket.close();
                                }
                                catch (IOException e) 
                                {
                                    /* Do Nothing */
                                }
                            }
                        }
                    });
                }
            }
        });
        proxyPort = port;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    private void processConnection(Socket connection) throws IOException {
        OutputStream out = new DataOutputStream(connection.getOutputStream());
        InputStream in = new DataInputStream(connection.getInputStream());

        // first byte is version should be 5
        int b = in.read();
        if (b != 5) {
            throw new IOException("Only SOCKS5 supported");
        }

        // second byte number of authentication methods supported
        b = in.read();
        int[] auth = new int[b];
        for (int i = 0; i < b; i++) {
            auth[i] = in.read();
        }

        int authMethod = -1;
        for (int anAuth : auth) 
        {
            authMethod = (anAuth == 2 ? 0 : -1); // only auth method
            // 2, username/passwowrd
            if (authMethod == 0) 
            {
                break;
            }
        }
        if (authMethod != 0) {
            throw new IOException("Authentication method not supported");
        }

        // Username/password auth method so respond with success
        byte[] cmd = new byte[2];
        cmd[0] = (byte) 0x05;
        cmd[1] = (byte) 0x02;
        out.write(cmd);
        out.flush();
        
        // authenticate
        
        // read the subnegotiation version
        b = in.read();
        if (b != 1) {
            throw new IOException("Subnegotiation version must be 1");
        }
        
        // read the username length and username
        b = in.read();
        byte[] usernameBytes = new byte[b];
        for (int i = 0; i < b; i++) 
        {
        	usernameBytes[i] = (byte)in.read();
        }
        
        // read the password length and password
        b = in.read();
        byte[] passwordBytes = new byte[b];
        for (int i = 0; i < b; i++) 
        {
        	passwordBytes[i] = (byte)in.read();
        }
        
        byte[] authResult = new byte[2];
        authResult[0] = (byte) 0x01;
        if (!ProxyServerCredentialManager.getInstance().validateCredential(new String(usernameBytes, StandardCharsets.US_ASCII), new String(passwordBytes, StandardCharsets.US_ASCII)))
        {
        	// anything other than 0 is failure
        	authResult[1] = (byte) 0x01;
            out.write(authResult);
            out.flush();
        	throw new IOException("Invalid credentials");
        }
        
        // 0 = successful
    	authResult[1] = (byte) 0x00;
        out.write(authResult);
        
        String responseDigest = processIncomingSocks5Message(in);
        
        try
        {
        	setupAndUpateProxyTranfer(responseDigest, connection);
            cmd = createOutgoingSocks5Message(0, responseDigest);
            out.write(cmd);
        }
        catch (UnauthorizedException eu) 
        {
            cmd = createOutgoingSocks5Message(2, responseDigest);
            out.write(cmd);
            throw new IOException("Illegal proxy transfer");
        }

    }

    
    private void setupAndUpateProxyTranfer(String responseDigest, Socket connection) throws IOException, UnauthorizedException
    {
    	ClusterCrossProxyInfo crossProxyInfo = null;
    	

        synchronized (connectionLock) 
        {
        	// check if a clustered connection (i.e. the receiver's connection) has already been established by another 
        	// node in the cluster
            if (ClusterManager.isClusteringStarted())
            {
            	crossProxyInfo = clusterCrossProxyMap.get(responseDigest);
            }
        	
            ProxyTransfer transfer = connectionMap.get(responseDigest);
            if (transfer != null)
            {
        		/*
            	 *  This connection is coming from the sender.  It might be the actual sender's edge client
            	 *  or it might be from another proxy server that the sender's edge client is connected to.
            	 *  Either way, we know that this node has been connected to by the receiver and the output
            	 *  stream to the receiver has already been captured.
            	 */
         		transfer.setInputStream(connection.getInputStream());
         		transfer.setSessionID(responseDigest);
         		
            	if (crossProxyInfo != null)
            	{
            		/*
            		 * This is a clustered connection.  In clustered proxy mode, we will auto activate the 
            		 * the proxy because we can't gaurantee that we will receive the ACTIVATE message.
            		 */
            		doTransfer(responseDigest, transfer);
            	}
            	
            }
            else if (transfer == null && crossProxyInfo == null) 
            {
            	/*
            	 * This connection is coming from the receiver. Bby TIM+ spec, the receiver establishes
            	 * a connection to the proxy server first.
            	 */
                
            	transfer = createProxyTransfer(responseDigest, connection);
                if (ClusterManager.isClusteringStarted())
                {
                	try
                	{
                		crossProxyInfo = createClusterCrossProxyInfo(responseDigest);
                		clusterCrossProxyMap.put(responseDigest, crossProxyInfo);
                	}
                	catch (Exception e)
                	{
                		Log.error("Failed to get cross proxy info information.  File transfer digest {} may fail.", responseDigest, e);
                	}
                }
                transferManager.registerProxyTransfer(responseDigest, transfer);
 
                connectionMap.put(responseDigest, transfer);
            }
            else 
            {
            	/*
            	 * This connection is coming from the sender's edge client, but the receiver is connection
            	 * to another node.
            	 * This condition should only happen when in a clustered mode, so we 
            	 * should have a crossProxyInfo structure available.
            	 */

            	
            	/*
            	 *  We need to connect to the peer proxy clustered node using information in the 
            	 *  crossProxyInfo structure.
            	 */
            	final AuthSocks5Client socks5Client = new AuthSocks5Client(crossProxyInfo.getReceiversClusterNode().getNodeIP(), 
            			crossProxyInfo.getPort(), responseDigest);
            	
            	try
            	{
            		final Socket proxySocket = socks5Client.getSocket(10000, crossProxyInfo.getProxyServiceCredential().getSubject(), 
            			crossProxyInfo.getProxyServiceCredential().getSecret());
            	
            		transfer = createProxyTransfer(responseDigest, proxySocket);
            		transferManager.registerProxyTransfer(responseDigest, transfer);
            		
            		transfer.setInputStream(connection.getInputStream());
                    transfer.setSessionID(responseDigest);
            		
            		connectionMap.put(responseDigest, transfer);
            		
            		// This is a clustered config, so auto ACTIVATE that transfer
            		doTransfer(responseDigest, transfer);
            	}
            	catch (Exception e)
            	{
            		throw new IOException("Failed to establish SOCKS5 proxy to proxy connection.", e);
            	}
            	
            }
        }

    }
    
    private ProxyTransfer createProxyTransfer(String transferDigest, Socket targetSocket)
            throws IOException {
        ProxyTransfer provider;
        try {
            Class c = ClassUtils.forName(className);
            provider = (ProxyTransfer) c.newInstance();
        }
        catch (Exception e) {
            Log.error("Error loading proxy transfer provider: " + className, e);
            provider = new DefaultProxyTransfer();
        }

        provider.setTransferDigest(transferDigest);
        provider.setOutputStream(targetSocket.getOutputStream());
        return provider;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    private static String processIncomingSocks5Message(InputStream in)
            throws IOException {
        // read the version and command
        byte[] cmd = new byte[5];
        int read = in.read(cmd, 0, 5);
        if (read != 5) {
            throw new IOException("Error reading Socks5 version and command");
        }

        // read the digest
        byte[] addr = new byte[cmd[4]];
        read = in.read(addr, 0, addr.length);
        if (read != addr.length) {
            throw new IOException("Error reading provided address");
        }
        String digest = new String(addr);

        in.read();
        in.read();

        return digest;
    }

    private static byte[] createOutgoingSocks5Message(int cmd, String digest) {
        byte addr[] = digest.getBytes();

        byte[] data = new byte[7 + addr.length];
        data[0] = (byte) 5;
        data[1] = (byte) cmd;
        data[2] = (byte) 0;
        data[3] = (byte) 0x3;
        data[4] = (byte) addr.length;

        System.arraycopy(addr, 0, data, 5, addr.length);
        data[data.length - 2] = (byte) 0;
        data[data.length - 1] = (byte) 0;

        return data;
    }

    synchronized void shutdown() {
        disable();
        executor.shutdown();
        StatisticsManager.getInstance().removeStatistic(proxyTransferRate);
    }

    /**
     * Activates the stream, this method should be called when the initiator sends the activate
     * packet after both parties have connected to the proxy.
     *
     * @param initiator The initiator or sender of the file transfer.
     * @param target The target or receiver of the file transfer.
     * @param sid The session id that uniquely identifies the transfer between the two participants.
     * @throws IllegalArgumentException This exception is thrown when the activated transfer does
     *                                  not exist or is missing one or both of the sockets.
     */
    void activate(JID initiator, JID target, String sid) {
        final String digest = createDigest(sid, initiator, target);

        ProxyTransfer temp;
        synchronized (connectionLock) {
            temp = connectionMap.get(digest);
        }
        if (temp != null)
        {
	        final ProxyTransfer transfer = temp;
	        // check to make sure we have all the required
	        // information to start the transfer
	        if (transfer == null || !transfer.isActivatable()) {
	            throw new IllegalArgumentException("Transfer doesn't exist or is missing parameters");
	        }
	
	        transfer.setInitiator(initiator.toString());
	        transfer.setTarget(target.toString());
	        transfer.setSessionID(sid);
	        
	        // In clustered mode, the activation has already been started
	        if (!ClusterManager.isClusteringStarted())
	        	doTransfer(digest, transfer);
        }
        
    }

    private void doTransfer(String digest, ProxyTransfer transfer)
    {
        transfer.setTransferFuture(executor.submit(new Runnable() {
            @Override
            public void run() 
            {
                try
                {
	            	Log.info("Strarting file transfer transfer.");
	            	
	            	try {
	                    transferManager.fireFileTransferStart( transfer.getSessionID(), true );
	                }
	                catch (FileTransferRejectedException e) {
	                    notifyFailure(transfer, e);
	                    return;
	                }
	                try {
	                    transfer.doTransfer();
	                    transferManager.fireFileTransferCompleted( transfer.getSessionID(), true );
	                }
	                catch (IOException e) {
	                    Log.error("Error during file transfer", e);
	                    transferManager.fireFileTransferCompleted( transfer.getSessionID(), false );
	                }
	                finally {
	                    connectionMap.remove(digest);
	                    clusterCrossProxyMap.remove(digest);
	                }
                }
                catch (Exception e)
                {
                	Log.error("Error during file transfer", e);
                }
            }
        }));    	
    }
    
    private void notifyFailure(ProxyTransfer transfer, FileTransferRejectedException e) {

    }

    /**
     * Creates the digest needed for a byte stream. It is the SHA1(sessionID +
     * initiator + target).
     *
     * @param sessionID The sessionID of the stream negotiation
     * @param initiator The initiator of the stream negotiation
     * @param target The target of the stream negotiation
     * @return SHA-1 hash of the three parameters
     */
    public static String createDigest(final String sessionID, final JID initiator,
                                      final JID target) {
        return StringUtils.hash(sessionID + initiator.getNode()
                + "@" + initiator.getDomain() + "/"
                + initiator.getResource()
                + target.getNode() + "@"
                + target.getDomain() + "/"
                + target.getResource(), "SHA-1");
    }

    public boolean isRunning() {
        return socketProcess != null && !socketProcess.isDone();
    }

    public void disable() {
        reset();
    }

    private void reset() {
        if (socketProcess != null) {
            socketProcess.cancel(true);
            socketProcess = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            }
            catch (IOException e) {
                Log.warn("Error closing proxy listening socket", e);
            }
        }
    }

    private static class ProxyTracker extends i18nStatistic {
        public ProxyTracker() {
            super("filetransferproxy.transfered", Statistic.Type.rate);
        }

        @Override
        public double sample() {
            return (ProxyOutputStream.amountTransferred.getAndSet(0) / 1000d);
        }

        @Override
        public boolean isPartialSample() {
            return true;
        }
    }
    
    class AliasSNIMatcher extends SNIMatcher
    {
        private String _host;

        AliasSNIMatcher()
        {
            super(StandardConstants.SNI_HOST_NAME);
        }

        @Override
        public boolean matches(SNIServerName serverName)
        {

            if (serverName instanceof SNIHostName)
            {
                _host = StringUtil.asciiToLowerCase(((SNIHostName)serverName).getAsciiName());
            }
            else
            {
            }

            // Return true and allow the KeyManager to accept or reject when choosing a certificate.
            // If we don't have a SNI host, or didn't see any certificate aliases,
            // just say true as it will either somehow work or fail elsewhere.
            return true;
        }

        public String getHost()
        {
            return _host;
        }
    }

    protected ClusterCrossProxyInfo createClusterCrossProxyInfo(String responseDigest) throws ClusterException, ProxyCredentialException
    {
    	final ClusterCrossProxyInfo retVal = new ClusterCrossProxyInfo();
    	
    	final ClusterNode clusterNode = 
    			ClusterManager.getClusterNodeProvider().getClusterMember(XMPPServer.getInstance().getNodeID());
    	
    	retVal.setReceiversClusterNode(clusterNode);
    	retVal.setPort(proxyPort);
    	retVal.setResponseDigest(responseDigest);
    	
    	final ProxyServerCredential cred = ProxyServerCredentialManager.getInstance().createCredential();
    	
    	retVal.setProxyServiceCredential(cred);
    	
    	return retVal;
    }
}
