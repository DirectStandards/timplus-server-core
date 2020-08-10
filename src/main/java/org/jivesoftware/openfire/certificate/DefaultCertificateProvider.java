package org.jivesoftware.openfire.certificate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.database.DbConnectionManager;

public class DefaultCertificateProvider implements CertificateProvider
{

	private static final String LOAD_CERTIFICATES = "SELECT * FROM ofCertificate";	
	
	private static final String LOAD_CERTIFICATE_BY_DOMAIN = "SELECT * FROM ofCertificate where domainAllCaps like ?";	
	
	private static final String LOAD_CERTIFICATE_BY_TP = "SELECT * FROM ofCertificate where thumbprintAllCaps = ?";	
	
    private static final String INSERT_CERTIFICATE =
            "INSERT INTO ofCertificate (id,distinguishedName,serialNumber,thumbprint,thumbprintAllCaps,validStartDate,validEndDate,"
            + "certData,domain,domainAllCaps,certStatus) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)"; 
	
    private static final String DELETE_CERTIFICATE_BY_TP = "DELETE FROM ofCertificate where thumbprintAllCaps = ?";	
    
	@Override
	public Collection<Certificate> getCertificates() throws CertificateException
	{
		   final List<Certificate> certs = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_CERTIFICATES);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            while (rs.next()) 
	            {
	            	final Certificate cert = certificateFromResultSet(rs);
	            	certs.add(cert);
	            	
	            }
		   }
	       catch (SQLException e) 
		   {
	    	   throw new CertificateException("Failed to load certificates.", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return certs;
	}

	@Override
	public Collection<Certificate> getCertificatesByDomain(String domain) throws CertificateException
	{
		final List<Certificate> certs = new ArrayList<>();
		
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CERTIFICATE_BY_DOMAIN);
            pstmt.setString(1, "%" + domain.toUpperCase() + "%");
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();

            while (rs.next()) 
            {
            	final Certificate cert = certificateFromResultSet(rs);
            	certs.add(cert);
            }
            
            if (certs.isEmpty())
            	throw new CertificateException("Could not load certificates for domain " + domain);
        }
        catch (Exception e) 
        {
            throw new CertificateNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        
        return certs;
	}

	@Override
	public Certificate getCertificateByThumbprint(String thumbprint) throws CertificateException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CERTIFICATE_BY_TP);
            pstmt.setString(1, thumbprint.toUpperCase());
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new CertificateException("Could not load certificate with thumb print " + thumbprint);
            }

            final Certificate cert = certificateFromResultSet(rs);
            
            return cert;
        }
        catch (Exception e) 
        {
            throw new CertificateNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public Certificate addCertificate(Certificate cert) throws CertificateException
	{
		if (this.isExistingCert(cert.getThumbprint()))
			throw new CertificateAlreadyExistsException("Certificate with thumbprint " + cert.getThumbprint() + "  already exists.");
		
		cert.setId(UUID.randomUUID().toString());
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            pstmt = con.prepareStatement(INSERT_CERTIFICATE);
            pstmt.setString(1, cert.getId());
            pstmt.setString(2, cert.getDistinguishedName());
            pstmt.setString(3, cert.getSerial());
            pstmt.setString(4, cert.getThumbprint());
            pstmt.setString(5, cert.getThumbprint().toUpperCase());
            pstmt.setLong(6, cert.getValidStartDate().toEpochMilli());
            pstmt.setLong(7, cert.getValidEndDate().toEpochMilli());
            pstmt.setBytes(8, cert.getCertData());            
            pstmt.setString(9, cert.getDomain());
            pstmt.setString(10, cert.getDomain().toUpperCase());
            pstmt.setInt(11, cert.getStatus().getCode());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new CertificateException("Failed to insert certificate.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return cert;	
	}

    public void deleteCertificate(String thumbprint) throws CertificateException
    {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_CERTIFICATE_BY_TP);
            pstmt.setString(1, thumbprint.toUpperCase());
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new CertificateException("Failed to delete certificate from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }   	
    }
	
	protected Certificate certificateFromResultSet(final ResultSet rs) throws SQLException
	{
		final Certificate cert = new Certificate();

		cert.setId(rs.getString(1));
		cert.setDistinguishedName(rs.getString(2));
		cert.setSerial(rs.getString(3));
		cert.setThumbprint(rs.getString(4));
		cert.setThumbprintAllCaps(rs.getString(5));
		
		if (rs.getLong(6) != 0)
			cert.setValidStartDate(Instant.ofEpochMilli(rs.getLong(6)));
		
		if (rs.getLong(7) != 0)
			cert.setValidEndDate(Instant.ofEpochMilli(rs.getLong(7)));
		
		cert.setCertData(rs.getBytes(8));
		cert.setDomain(rs.getString(9));
		cert.setDomainAllCaps(rs.getString(10));
		
		cert.setStatus(CertificateStatus.fromCode(rs.getInt(11)));

		return cert;		
	}
	
	protected boolean isExistingCert(String thumbprint)
	{
		try
		{
			return this.getCertificateByThumbprint(thumbprint) != null;
		}
		catch(CertificateException e)
		{
			return false;
		}
	}
}
