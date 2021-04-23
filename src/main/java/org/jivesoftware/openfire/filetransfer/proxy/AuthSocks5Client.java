package org.jivesoftware.openfire.filetransfer.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;

/**
 * Used for clustered proxy to proxy connections when the sender and receiver of 
 * a file transfer are connected to different nodes in the cluser.
 * @author Greg Meyer
 *
 */
public class AuthSocks5Client
{	
    protected String proxyHost;
    
    protected int proxyPort;

    protected String digest;
	
	public AuthSocks5Client(String proxyHost, int proxyPort, String digest)
	{		
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.digest = digest;
	}
	
    public Socket getSocket(int timeout, String username, String password) throws IOException, InterruptedException,
    TimeoutException, SmackException 
    {
    	// wrap connecting in future for timeout
    	FutureTask<Socket> futureTask = new FutureTask<>(new Callable<Socket>() 
    	{

    		@Override
    		public Socket call() throws IOException, SmackException 
    		{

    			SocketAddress socketAddress = new InetSocketAddress(proxyHost,
    					proxyPort);
    			
    			SSLSocket socket = null;
    			
    			// initialize connection to SOCKS5 proxy
    			try 
    			{   		
    				// ignore certificates
    				TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() 
    		        {
    		            public java.security.cert.X509Certificate[] getAcceptedIssuers() 
    		            {
    		                return null;
    		            }
    		            
    		            public void checkClientTrusted(X509Certificate[] certs, String authType) 
    		            {
    		            }
    		            
    		            public void checkServerTrusted(X509Certificate[] certs, String authType) 
    		            {
    		            }
    		        }};
    				
	    			// initialize socket
	    			final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
	    			sslContext.init(null, trustManagers, new SecureRandom());
	    			final SSLSocketFactory socketFactory = sslContext.getSocketFactory();
	    				    			
	    			socket  = (SSLSocket) socketFactory.createSocket();
	    			socket.setSoTimeout(timeout);
	    			
	    			socket.connect(socketAddress, timeout);	
	    			
	    			
    				establish(socket, username, password);
    			}
    			catch (Exception e) 
    			{
    				e.printStackTrace();
    				if (!socket.isClosed()) 
    				{
    					try 
    					{
    						socket.close();
    					} 
    					catch (IOException e2) 
    					{
    						System.out.println("Could not close SOCKS5 socket");
    					}
    				}
    				if (e instanceof SmackException)
    					throw (SmackException)e;
    				else
    					throw new SmackException("Failed to create SOCK5 Connection", e);
    			}

    			return socket;
    		}

    	});
    	Thread executor = new Thread(futureTask);
    	executor.start();

    	// get connection to initiator with timeout
    	try 
    	{
    		return futureTask.get(timeout, TimeUnit.MILLISECONDS);
    	}
    	catch (ExecutionException e) 
    	{
    		Throwable cause = e.getCause();
    		if (cause != null) 
    		{
    			// case exceptions to comply with method signature
    			if (cause instanceof IOException) 
    			{
    				throw (IOException) cause;
    			}
    			if (cause instanceof SmackException) 
    			{
    				throw (SmackException) cause;
    			}
    		}

    		// throw generic Smack exception if unexpected exception was thrown
    		throw new SmackException("Error while connecting to SOCKS5 proxy", e);
    	}

    }

    protected void establish(Socket socket, String username, String password) throws SmackException, IOException 
    {

        byte[] connectionRequest;
        byte[] connectionResponse;
        /*
         * use DataInputStream/DataOutpuStream to assure read and write is completed in a single
         * statement
         */
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // authentication negotiation
        byte[] cmd = new byte[3];

        cmd[0] = (byte) 0x05; // protocol version 5
        cmd[1] = (byte) 0x01; // number of authentication methods supported
        cmd[2] = (byte) 0x02; // authentication method: username/password

        out.write(cmd);
        out.flush();

        byte[] response = new byte[2];
        in.readFully(response);

        // check if server responded with correct version and no-authentication method
        if (response[0] != (byte) 0x05 || response[1] != (byte) 0x02) {
        	in.close();
        	out.close();
        	socket.close();
            throw new SmackException("Remote SOCKS5 server responded with unexpected version: " + response[0] + ' ' + response[1] + ". Should be 0x05 0x02.");
        }

        // create auth request
        byte[] authRequest = createAuthRequest(username, password);
        out.write(authRequest);
        out.flush();
        
        response = new byte[2];
        in.readFully(response);
        
        // check if server responded with a success status
        if (response[1] != (byte) 0x00) 
        {
        	in.close();
        	out.close();
        	socket.close();
            throw new SmackException("Remote SOCKS5 server responded with an unsuccessful auth request: ");
        }
        
        // request SOCKS5 connection with given address/digest
        connectionRequest = createSocks5ConnectRequest();
        out.write(connectionRequest);
        out.flush();
        
        // receive response
        connectionResponse = Socks5Utils.receiveSocks5Message(in);

        // verify response
        connectionRequest[1] = (byte) 0x00; // set expected return status to 0
        if (!Arrays.equals(connectionRequest, connectionResponse)) {
        	
        	in.close();
        	out.close();
        	socket.close();
        	
            throw new SmackException(
                            "Connection request does not equal connection response. Response: "
                                            + Arrays.toString(connectionResponse) + ". Request: "
                                            + Arrays.toString(connectionRequest));
        }
    }    
    
    protected byte[] createAuthRequest(String username, String password)
    {
    	/*
    	 * The layout of user name/password request is
    	 * --------------------------------------------------------
    	 * | Ver | UName Len | UName Bytes | Pass Len | Pass Bytes|
    	 * --------------------------------------------------------
    	 */
    	
    	// create a byte array buffer
    	int nNameLen = username.length();
    	int nPassPen = password.length();
    	
    	// 3 bytes for version and length fields and bytes for username and password
    	byte[] authCmd = new byte[3 + nNameLen + nPassPen];
    	
    	int idx = 0;
    	authCmd[idx++] = (byte)0x01; // sub negotation version
    	authCmd[idx++] = (byte)nNameLen; // username length
    	System.arraycopy(username.getBytes(StandardCharsets.US_ASCII), 0, authCmd, idx, nNameLen); // username
    	idx += nNameLen;
    	authCmd[idx++] = (byte)nPassPen; // password length
    	System.arraycopy(password.getBytes(StandardCharsets.US_ASCII), 0, authCmd, idx, nPassPen); // password
    	
    	return authCmd;
    	
    }

    private byte[] createSocks5ConnectRequest() {
        byte[] addr;
        try {
            addr = digest.getBytes(StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        byte[] data = new byte[7 + addr.length];
        data[0] = (byte) 0x05; // version (SOCKS5)
        data[1] = (byte) 0x01; // command (1 - connect)
        data[2] = (byte) 0x00; // reserved byte (always 0)
        data[3] = (byte) 0x03; // address type (3 - domain name)
        data[4] = (byte) addr.length; // address length
        System.arraycopy(addr, 0, data, 5, addr.length); // address
        data[data.length - 2] = (byte) 0; // address port (2 bytes always 0)
        data[data.length - 1] = (byte) 0;

        return data;
    }
}
