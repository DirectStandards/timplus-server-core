package org.jivesoftware.openfire.certificate;

import java.security.cert.X509Certificate;
import java.time.Instant;

import org.jivesoftware.util.cert.CertUtils;
import org.jivesoftware.util.cert.Thumbprint;

public class Certificate
{
    private String id;
    private String distinguishedName;
    private String serial;
    private String thumbprint;
    private String thumbprintAllCaps;
    public Instant validStartDate;
    public Instant validEndDate;    
    private byte[] certData;
    private String domain;
    private String domainAllCaps;
    private CertificateStatus certStatus;


    
    public String getId() 
    {
        return id;
    }

    public void setId(String id) 
    {
        this.id = id;
    }
    
    public String getDomain() 
    {
        return domain;
    }

    public void setDomain(String domain) 
    {
        this.domain = domain;
    }

    public String getDomainAllCaps() 
    {
        return domainAllCaps;
    }

    public void setDomainAllCaps(String domainAllCaps) 
    {
        this.domainAllCaps = domainAllCaps;
    }
    
    public byte[] getCertData() 
    {
        return certData;
    }

    public void setCertData(byte[] certData) 
    {
        this.certData = certData;

        loadCertFromData(certData);
    }
    
    public void setThumbprint(String aThumbprint) 
    {
        thumbprint = aThumbprint;
    }

    public String getThumbprint() 
    {
        return thumbprint;
    }

    public void setThumbprintAllCaps(String thumbprintAllCaps) 
    {
    	this.thumbprintAllCaps = thumbprintAllCaps;
    }

    public String getThumbprintAllCaps() 
    {
        return thumbprintAllCaps;
    }
    
    public String getSerial()
    {
    	return serial;
    }

    public void setSerial(String serial)
    {
    	this.serial = serial;
    }
    
    public CertificateStatus getStatus() 
    {
        return certStatus;
    }

    public void setStatus(CertificateStatus certStatus) 
    {
        this.certStatus = certStatus;
    }

    public Instant getValidStartDate() 
    {
        return validEndDate;
    }

    public void setValidStartDate(Instant validStartDate) 
    {
        this.validStartDate = validStartDate;
    }
    
    public Instant getValidEndDate() 
    {
        return validEndDate;
    }

    public void setValidEndDate(Instant validEndDate) 
    {
        this.validEndDate = validEndDate;
    }
    
    public String getDistinguishedName()
    {
    	return distinguishedName;
    	
    }
    
    public void setDistinguishedName(String distinguishedName)
    {
    	this.distinguishedName = distinguishedName;
    }

	protected void loadCertFromData(byte[] certData)
    {
    	if (certData == null)
    		throw new IllegalArgumentException("Cert data cannot be null");
    	
    	X509Certificate cert = CertUtils.toCertContainer(certData).getCert();
        setThumbprint(Thumbprint.toThumbprint(cert).toString());
        setThumbprintAllCaps(Thumbprint.toThumbprint(cert).toString().toUpperCase());
        setValidEndDate(cert.getNotAfter().toInstant());
        setValidStartDate(cert.getNotBefore().toInstant());
        setSerial(cert.getSerialNumber().toString(16));
        setDistinguishedName(cert.getSubjectDN().getName());

    }
    
    public X509Certificate asX509Certificate()
    {
    	return CertUtils.toX509Certificate(certData);
    }
     
}
