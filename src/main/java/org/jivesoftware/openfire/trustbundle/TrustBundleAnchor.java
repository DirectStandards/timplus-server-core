package org.jivesoftware.openfire.trustbundle;

import java.security.cert.X509Certificate;
import java.time.Instant;

import org.jivesoftware.util.cert.CertUtils;
import org.jivesoftware.util.cert.Thumbprint;

public class TrustBundleAnchor
{
    private String id;
    private String thumbprint;
    private byte[] anchorData;
    private Instant validStartDate;
    private Instant validEndDate;
    private String serial;
    private String distinguishedName;
    private String trustBundleId;
    
    public static final byte[] NULL_CERT = new byte[] {};
    
    /**
     * Get the value of id.
     * 
     * @return the value of id.
     */
    public String getId() 
    {
        return id;
    }

    /**
     * Set the value of id.
     * 
     * @param id
     *            The value of id.
     */
    public void setId(String id) 
    {
        this.id = id;
    }
    

    /**
     * Get the value of the certificate data.
     * 
     * @return the value of the certificate data.
     */
    public byte[] getAnchorData() 
    {
        return anchorData;
    }

    /**
     * Set the value of the certificate data.
     * 
     * @param data
     *            The value of the certificate data.
     * @throws CertificateException
     */
    public void setAnchorData(byte[] anchorData) 
    {
        this.anchorData = anchorData;
        if (anchorData == NULL_CERT) 
        {
            setThumbprint("");
            setSerial("");
        } 
        else 
        {
            loadAnchorFromData(anchorData);
        }
    }

    
    /*
     * Set the certificate thumb print
     */
    protected void setThumbprint(String aThumbprint) 
    {
        thumbprint = aThumbprint;
    }

    /**
     * Get the value of thumbprint.
     * 
     * @return the value of thumbprint.
     */
    public String getThumbprint() 
    {
        return thumbprint;
    }

    /**
     * Gets the serial number of the certificate.
     * @return The serial number of the certificate.
     */
    public String getSerial()
    {
    	return serial;
    }

    /*
     * Sets the serial number of the certificate.
    */
    protected void setSerial(String serial)
    {
    	this.serial = serial;
    }
    

    /**
     * Get the value of validEndDate.
     * 
     * @return the value of validEndDate.
     */
    public Instant getValidEndDate() 
    {
        return validEndDate;
    }

    /**
     * Set the value of validEndDate.
     */
    protected void setValidEndDate(Instant validEndDate) 
    {
        this.validEndDate = validEndDate;
    }
    
    /**
     * Get the value of validStartDate.
     * 
     * @return the value of validStartDate.
     */
    public Instant getValidStartDate() 
    {
        return validStartDate;
    }

    /**
     * Set the value of validStartDate.
     */
    protected void setValidStartDate(Instant validStartDate) 
    {
        this.validStartDate = validStartDate;
    }
    
    
    
    /**
     * Gets the anchor distinguished name
     * @return The anchor distinguished name
     */
    public String getDistinguishedName()
    {
    	return distinguishedName;
    	
    }
    
    /**
     * Sets the anchor distinguished name
     * @param distinguishedName The anchor distinguished name
     */
    public void setDistinguishedName(String distinguishedName)
    {
    	this.distinguishedName = distinguishedName;
    }
    
    
    
    /**
     * Gets the id of the trust bundle that this anchor is associated with
     * @return The id of the trust bundle that this anchor is associated with
     */
    public String getTrustBundleId()
	{
		return trustBundleId;
	}

    /**
     * Sets the id of the trust bundle that this anchor is associated with
     * @param trustBundleId The id of the trust bundle that this anchor is associated with
     */
	public void setTrustBundleId(String trustBundleId)
	{
		this.trustBundleId = trustBundleId;
	}

	/*
     * load data from a the anchor data and populates the non-settable fields
     */
    protected void loadAnchorFromData(byte[] anchor)
    {
    	if (anchor == null)
    		throw new IllegalArgumentException("Anchor data cannot be null");
    	
        try 
        {     
        	X509Certificate cert = CertUtils.toX509Certificate(anchor);
            setThumbprint(Thumbprint.toThumbprint(cert).toString());
            setValidEndDate(cert.getNotAfter().toInstant());
            setValidStartDate(cert.getNotBefore().toInstant());
            setSerial(cert.getSerialNumber().toString(16));
            setDistinguishedName(cert.getSubjectDN().getName());
        }
    	catch (Exception e) 
        {
            setAnchorData(NULL_CERT);
        }
    }
    
    /**
     * Gets the anchor as an X509Certificate object
     * @return The anchor as an X509Certificate object
     */
    public X509Certificate asX509Certificate()
    {
    	return CertUtils.toX509Certificate(anchorData);
    }
    
}
