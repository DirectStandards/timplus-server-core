package org.jivesoftware.openfire.certificate;

import java.util.Collection;

public interface CertificateProvider
{
	public Collection<Certificate> getCertificates() throws CertificateException;
	
    public Collection<Certificate> getCertificatesByDomain(String domain) throws CertificateException;
    
    public Certificate getCertificateByThumbprint(String thumbprint) throws CertificateException;
    
    public Certificate addCertificate(Certificate cert) throws CertificateException;
    
    public void deleteCertificate(String thumbprint) throws CertificateException;
}
