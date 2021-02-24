package org.jivesoftware.openfire.filetransfer.proxy;

import java.io.Serializable;

import org.jivesoftware.openfire.cluster.ClusterNode;
import org.jivesoftware.openfire.filetransfer.proxy.credentials.ProxyServerCredential;

public class ClusterCrossProxyInfo implements Serializable
{
	private static final long serialVersionUID = 710669642852689812L;

	protected ClusterNode receiversClusterNode;
	
	protected ClusterNode sendersClusterNode;
	
	protected int port;
	
	protected String responseDigest;
	
	protected ProxyServerCredential proxyServiceCredential;
	
	public ClusterCrossProxyInfo()
	{
		
	}

	public ClusterNode getReceiversClusterNode() 
	{
		return receiversClusterNode;
	}

	public void setReceiversClusterNode(ClusterNode receiversClusterNode) 
	{
		this.receiversClusterNode = receiversClusterNode;
	}
	
	public ClusterNode getSendersClusterNode() 
	{
		return sendersClusterNode;
	}

	public void setSendersClusterNode(ClusterNode sendersClusterNode) 
	{
		this.sendersClusterNode = sendersClusterNode;
	}

	public int getPort() 
	{
		return port;
	}

	public void setPort(int port) 
	{
		this.port = port;
	}

	public String getResponseDigest() 
	{
		return responseDigest;
	}

	public void setResponseDigest(String responseDigest) 
	{
		this.responseDigest = responseDigest;
	}

	public ProxyServerCredential getProxyServiceCredential() 
	{
		return proxyServiceCredential;
	}

	public void setProxyServiceCredential(ProxyServerCredential proxyServiceCredential) 
	{
		this.proxyServiceCredential = proxyServiceCredential;
	}	
}
