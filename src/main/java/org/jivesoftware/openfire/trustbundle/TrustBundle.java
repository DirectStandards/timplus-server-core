package org.jivesoftware.openfire.trustbundle;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertificateConversionException;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TrustBundle
{
	private String id;
	private String bundleName;
	private String bundleURL;
    private byte[] signingCertificateData;
    private Collection<TrustBundleAnchor> trustBundleAnchors;
    private int refreshInterval;
    private Instant lastRefreshAttempt;
    private BundleRefreshError lastRefreshError;
    private Instant lastSuccessfulRefresh;    
    private Instant createTime;  
    private String checkSum;
	
    /**
     * Empty constructor
     */
    public TrustBundle()
    {
    	
    }
    
    /**
     * Gets the internal system id of the trust bundle.
     * @return The internal system id of the trust bundle.
     */
    public String getId() 
    {
		return id;
	}
    
    /**
     * Sets the internal system id of the trust bundle.
     * @param the internal system id of the trust bundle.
     */
	public void setId(String id) 
	{
		this.id = id;
	}
	
	/**
	 * Gets the name of the bundle.
	 * @return The name of the bundle.
	 */
	public String getBundleName() 
	{
		return bundleName;
	}
	
	/**
	 * Sets the name of the bundle.
	 * @param bundleName The name of the bundle.
	 */
	public void setBundleName(String bundleName) 
	{
		this.bundleName = bundleName;
	}
	
	/**
	 * Gets the URL location of the bundle.
	 * @return The URL location of the bundle.
	 */
	public String getBundleURL() 
	{
		return bundleURL;
	}
	
	/**
	 * Sets the URL location of the bundle.
	 * @param bundleURL The URL location of the bundle.
	 */
	public void setBundleURL(String bundleURL) 
	{
		this.bundleURL = bundleURL;
	}
	
	/**
	 * Gets the DER encoded data of the X509 certificate that signed the bundle.
	 * @return The DER encoded data of the X509 certificate that signed the bundle.
	 */
	public byte[] getSigningCertificateData() 
	{
		return signingCertificateData;
	}
	
	/**
	 * Sets the DER encoded data of the X509 certificate that signed the bundle.
	 * @param signingCertificateData The DER encoded data of the X509 certificate that signed the bundle.
	 */
	public void setSigningCertificateData(byte[] signingCertificateData) 
	{
		this.signingCertificateData = signingCertificateData;
	}
	
	/**
	 * Gets the trust anchors in the bundle.
	 * @return The trust anchors in the bundle.
	 */
	public Collection<TrustBundleAnchor> getTrustBundleAnchors() 
	{
		if (trustBundleAnchors == null)
			trustBundleAnchors = Collections.emptyList();
		
		return Collections.unmodifiableCollection(trustBundleAnchors);
	}
	
	/**
	 * Sets the trust anchors in the bundle.
	 * @param trustBundleAnchors The trust anchors in the bundle.
	 */
	public void setTrustBundleAnchors(Collection<TrustBundleAnchor> trustBundleAnchors) 
	{
		this.trustBundleAnchors = new ArrayList<TrustBundleAnchor>(trustBundleAnchors);
	}
	
	/**
	 * Gets the refresh interval for the bundle.
	 * @return The refresh interval for the bundle.
	 */
	public int getRefreshInterval() 
	{
		return refreshInterval;
	}
	
	/**
	 * Sets the refresh interval for the bundle.
	 * @param refreshInterval The refresh interval for the bundle.
	 */
	public void setRefreshInterval(int refreshInterval) 
	{
		this.refreshInterval = refreshInterval;
	}
	
	/**
	 * Gets the date/time of the last time a refresh was attempted.
	 * @return The date/time of the last time a refresh was attempted.
	 */ 
	public Instant getLastRefreshAttempt() 
	{
		return lastRefreshAttempt;
	}
	
	/**
	 * Sets the date/time of the last time a refresh was attempted.
	 * @param lastRefreshAttempt The date/time of the last time a refresh was attempted.
	 */
	public void setLastRefreshAttempt(Instant lastRefreshAttempt) 
	{
		this.lastRefreshAttempt = lastRefreshAttempt;
	}
	
	/**
	 * Gets the status of the last refresh attempt.
	 * @return The status of the last refresh attempt.
	 */
	public BundleRefreshError getLastRefreshError() 
	{
		return lastRefreshError;
	}
	
	/**
	 * Sets the status of the last refresh attempt.
	 * @param lastRefreshError The status of the last refresh attempt.
	 */
	public void setLastRefreshError(BundleRefreshError lastRefreshError) 
	{
		this.lastRefreshError = lastRefreshError;
	}
	
	/**
	 * Gets the date/time of the last time the bundle was successfully refreshed.
	 * @return The date/time of the last time the bundle was successfully refreshed.
	 */
	public Instant getLastSuccessfulRefresh() 
	{
		return lastSuccessfulRefresh;
	}
	
	/**
	 * Sets the date/time of the last time the bundle was successfully refreshed.
	 * @param lastSuccessfulRefresh The date/time of the last time the bundle was successfully refreshed.
	 */
	public void setLastSuccessfulRefresh(Instant lastSuccessfulRefresh) 
	{
		this.lastSuccessfulRefresh = lastSuccessfulRefresh;
	}
	
	/**
	 * Gets the date/time that bundle was created in the system.
	 * @return The date/time that bundle was created in the system.
	 */
	public Instant getCreateTime()
	{
		return createTime;
	}
	
	/**
	 * Sets the date/time that bundle was created in the system.
	 * @param createTime The date/time that bundle was created in the system.
	 */
	public void setCreateTime(Instant createTime) 
	{
		this.createTime = createTime;
	}
	
	/**
	 * Gets the check sum of the bundle.  This consists of a an SHA-1 has of the bundle file.
	 * @return The check sum of the bundle.
	 */
	public String getCheckSum() 
	{
		return checkSum;
	}
	
	/**
	 * Sets the check sum of the bundle.
	 * @param checkSum The check sum of the bundle.
	 */
	public void setCheckSum(String checkSum) 
	{
		this.checkSum = checkSum;
	}
	
	@SuppressWarnings("deprecation")
	@JsonIgnore
	/**
	 * The returned value is derived from the internal byte stream representation.  This attribute is suppressed during JSON conversion.
	 */
	public X509Certificate getSigningCertificateAsX509Certificate()
	{
		
		if (signingCertificateData == null || signingCertificateData.length == 0)
			return null;
		
		ByteArrayInputStream bais = null;
        try 
        {
            bais = new ByteArrayInputStream(signingCertificateData);
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(bais);
        } 
        catch (Exception e) 
        {
            throw new CertificateConversionException("Data cannot be converted to a valid X.509 Certificate", e);
        }
        finally
        {
        	IOUtils.closeQuietly(bais);
        }
	}		
    
}
