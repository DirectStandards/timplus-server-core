<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    <%@ page import="java.io.*,java.util.*, javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="org.apache.commons.fileupload.*" %>
<%@ page import="org.apache.commons.fileupload.disk.*" %>
<%@ page import="org.apache.commons.fileupload.servlet.*" %>
<%@ page import="org.apache.commons.io.output.*" %>
<%@ page import="org.jivesoftware.openfire.trustanchor.TrustAnchorManager" %>
<%@ page import="java.security.cert.CertificateFactory" %>
<%@ page import="java.security.cert.X509Certificate" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Anchor File Upload</title>
</head>
<body>
<%
   boolean cancel = request.getParameter("cancel") != null;  

   // Handle a cancel
   if (cancel) 
   {
	   response.sendRedirect("trust-anchor-summary.jsp");
	   return;
   }

   System.out.println("Received file upload request");
   File file ;
   int maxFileSize = 5000 * 1024;
   int maxMemSize = 5000 * 1024;
 
   String contentType = request.getContentType();
   if ((contentType.indexOf("multipart/form-data") >= 0)) {
 
      DiskFileItemFactory factory = new DiskFileItemFactory();
      factory.setSizeThreshold(maxMemSize);
      factory.setRepository(new File("c:\\temp"));
      ServletFileUpload upload = new ServletFileUpload(factory);
      upload.setSizeMax( maxFileSize );
      try{ 
         List fileItems = upload.parseRequest(request);
         Iterator i = fileItems.iterator();
         while ( i.hasNext () ) 
         {
            FileItem fi = (FileItem)i.next();
            if ( !fi.isFormField () )  
            {
                String fieldName = fi.getFieldName();
                String fileName = fi.getName();
                boolean isInMemory = fi.isInMemory();
                long sizeInBytes = fi.getSize();
				System.out.println("Writing anchor");
				try
				{
					X509Certificate anchorCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fi.getInputStream());
					TrustAnchorManager.getInstance().addTrustAnchor(anchorCert);
					response.sendRedirect("trust-anchor-summary.jsp?success=true");
				}
				catch (Exception e)
				{
					response.sendRedirect("trust-anchor-create.jsp?error=true");
					
				}
            }
         }

      }catch(Exception ex) {
         System.out.println(ex);
      }
   }else{

   }
%>
</body>
</html>