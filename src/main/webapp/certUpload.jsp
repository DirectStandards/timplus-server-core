<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    <%@ page import="java.io.*,java.util.*, javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="org.apache.commons.fileupload.*" %>
<%@ page import="org.apache.commons.fileupload.disk.*" %>
<%@ page import="org.apache.commons.fileupload.servlet.*" %>
<%@ page import="org.apache.commons.io.output.*" %>
<%@ page import="org.jivesoftware.openfire.certificate.Certificate" %>
<%@ page import="org.jivesoftware.openfire.certificate.CertificateManager" %>
<%@ page import="org.jivesoftware.util.PrivateKeyType" %>
<%@ page import="java.security.cert.CertificateFactory" %>
<%@ page import="java.security.cert.X509Certificate" %>
<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Certificate File Upload</title>
</head>
<body>
<%
   boolean cancel = request.getParameter("cancel") != null;  

   System.out.println("Received file upload request");

   Part privKeyTypePart = request.getPart("privKeyType");
   Part passphrasePart = request.getPart("passphrase");
   Part certFilePart = request.getPart("certFile");
   Part privKeyFile = request.getPart("certFile");
   
   final PrivateKeyType privKeyType = PrivateKeyType.fromString(IOUtils.toString(privKeyTypePart.getInputStream()));
   final String passphrase = IOUtils.toString(passphrasePart.getInputStream());
   final InputStream certFileIS = certFilePart.getInputStream();
   final InputStream privKeyFileIS = privKeyFile.getInputStream();

	if ((privKeyType == PrivateKeyType.PKCS8_PASSPHRASE || privKeyType == PrivateKeyType.PKCS_12_PASSPHRASE) && 
			StringUtils.isEmpty(passphrase))
	{
		// can't move on if a passphrase is required and one is not supplied
		response.sendRedirect("certificate-create.jsp?error=true");
		return;
	}   

	try
	{
		//X509Certificate anchorCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fi.getInputStream());
		//TrustAnchorManager.getInstance().addTrustAnchor(anchorCert);
		Certificate cert = CertificateManager.getInstance().certFromUpdloadRequest(privKeyType, passphrase, certFileIS, privKeyFileIS);
		CertificateManager.getInstance().addCertificate(cert);
		
		response.sendRedirect("certificate-summary.jsp?success=true");
	}
	catch (Exception e)
	{
		response.sendRedirect("certificate-create.jsp?error=true");
		
	}
%>
</body>
</html>