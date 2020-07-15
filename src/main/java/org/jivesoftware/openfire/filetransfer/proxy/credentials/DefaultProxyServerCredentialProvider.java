package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.domain.DomainNotFoundException;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProxyServerCredentialProvider implements ProxyServerCredentialProvider
{
	private static final Logger Log = LoggerFactory.getLogger(DefaultProxyServerCredentialProvider.class);
	
    private static final String INSERT_CREDENTIAL =
            "INSERT INTO ofProxyCredentials (subject,secretHash,creationDate)  VALUES (?,?,?)"; 
	
    private static final String LOAD_CREDENTIAL ="SELECT * FROM ofProxyCredentials WHERE subject=?";
    
    private static final String DELETE_CREDENTIAL = "DELETE FROM ofProxyCredentials WHERE subject=?";
    
    private static final String ALL_CREDENTIALS = "SELECT * FROM ofProxyCredentials";
    
	public DefaultProxyServerCredentialProvider()
	{
		
	}

	protected ProxyServerCredential newCredential() throws ProxyCredentialException
	{
		// create the credential set
		final Random random = new SecureRandom();
		final String subject = RandomStringUtils.random(26, 0, 0, true, true, null, random);
		final String secret = RandomStringUtils.random(26, 0, 0, true, true, null, random);

		byte[] hash = generateSecretHash(secret);
		
		return new ProxyServerCredential(subject, secret, hash, Calendar.getInstance().getTime());
	}
	
	@Override
	public ProxyServerCredential createCredential() throws ProxyCredentialException
	{

		final ProxyServerCredential cred = newCredential();

        // Add The new credential
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_CREDENTIAL);
            pstmt.setString(1, cred.getSubject());
            pstmt.setBytes(2, cred.getSecretHash());
            pstmt.setString(3, StringUtils.dateToMillis(cred.getCreationDate()));
            pstmt.execute();
        }
        catch (SQLException e) 
        {
            throw new RuntimeException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
		return cred;
		
	}

	@Override
	public ProxyServerCredential getCredential(String subject) throws ProxyCredentialNotFoundException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CREDENTIAL);
            pstmt.setString(1, subject);
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new DomainNotFoundException();
            }

            return credFromResultSet(rs);
        }
        catch (Exception e) 
        {
            throw new ProxyCredentialNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public void deleteCredential(String subject)
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_CREDENTIAL);
            pstmt.setString(1, subject);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            Log.error(e.getMessage(), e);
            abortTransaction = true;
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
	}

	@Override
	public void deleteExpiredCredentials(Date expirationTime)
	{
		final Collection<ProxyServerCredential> creds = getCredentials();
		
		for (ProxyServerCredential cred : creds)
		{
			if (cred.getCreationDate().getTime() < expirationTime.getTime())
				deleteCredential(cred.getSubject());
		}
	}

	@Override
	public byte[] generateSecretHash(String secret) throws ProxyCredentialException
	{
		try
		{
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
		} 
		catch (NoSuchAlgorithmException e)
		{
			throw new ProxyCredentialException("Could not create credential secret hash.", e);
		}
	}
	
	protected ProxyServerCredential credFromResultSet(final ResultSet rs) throws SQLException
	{
        final String subject = rs.getString(1);
        final byte[] secretHash = rs.getBytes(2);
        final Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));

        return new ProxyServerCredential(subject, "", secretHash, creationDate);
	}
	
	public Collection<ProxyServerCredential> getCredentials()
	{
	   final List<ProxyServerCredential> creds = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(ALL_CREDENTIALS);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            	creds.add(credFromResultSet(rs));
	   }
       catch (SQLException e) 
	   {
    	   Log.error(e.getMessage(), e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return creds;
	}
}
