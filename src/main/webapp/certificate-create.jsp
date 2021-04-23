<%@ page contentType="text/html; charset=UTF-8" %>
<%--
  -
  - Copyright (C) 2004-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="org.jivesoftware.util.*,
                 org.jivesoftware.openfire.certificate.*,
                 java.net.URLEncoder,
                 gnu.inet.encoding.Stringprep,
                 gnu.inet.encoding.StringprepException"
    errorPage="error.jsp"
%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.HashMap"%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager" %>
<%@ page import="org.jivesoftware.openfire.admin.AdminManager" %>
<%@ page import="org.jivesoftware.util.PrivateKeyType" %>
<%@ page import="java.security.cert.X509Certificate" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters //
    boolean another = request.getParameter("another") != null;
    boolean create = another || request.getParameter("create") != null;
    boolean cancel = request.getParameter("cancel") != null;
    boolean error = request.getParameter("error") != null;

    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    String contentType = request.getContentType();
    
    
    Map<String, String> errors = new HashMap<String, String>();
    if (create) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            create = false;
            errors.put("csrf", "CSRF Failure!");
        }
    }
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("certificate-summary.jsp");
        return;
    }

    if (error == true) 
    {
        errors.put("general","");
    }
    
    pageContext.setAttribute( "privKeyTypeList", PrivateKeyType.getPrivKeyTypeList());
    
%>

<html>
    <head>
        <title><fmt:message key="certificate.create.title"/></title>
        <meta name="pageID" content="certificate-create"/>
        <meta name="helpPage" content="add_certificate_to_the_system.html"/>
        
        
    <script type="text/javascript">
     function showHidePassPhrase(elem)
     {
    	if (elem.selectedIndex == 0 || elem.selectedIndex == 1 || elem.selectedIndex == 3 || elem.selectedIndex == 5)
        {
      	   document.getElementById('passphrase').style.display = 'none';
        }
        else
        {
      	  document.getElementById('passphrase').style.display = 'table-row';
        }
    	
    	if (elem.selectedIndex == 0 || elem.selectedIndex == 1 || elem.selectedIndex == 2)
        {
      	   document.getElementById('privKeyRow').style.display = 'none';
        }
        else
        {
      	  document.getElementById('privKeyRow').style.display = 'table-row';
        }
     }
    </script>
        
        
    </head>
    <body>


<p><fmt:message key="certificate.create.info" /></p>

<%--<c:set var="submit" value="${param.create}"/>--%>
<%--<c:set var="errors" value="${errors}"/>--%>

<%  if (!errors.isEmpty()) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">

            <% if (errors.get("general") != null) { %>
                <fmt:message key="certificate.create.error_adding_cert" />                               
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="certificate.create.created_success" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form name="f" action="certUpload.jsp" method="post" enctype="multipart/form-data">
    <input type="hidden" name="csrf" value="${csrf}">

    <div class="jive-contentBoxHeader">
        <fmt:message key="certificate.create.new_trustcert" />
    </div>
    <div class="jive-contentBox">
        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
        <tr>
            <td width="1%" nowrap><label for="privKeyTypef"><fmt:message key="certificate.create.privKeyType" />:</label> *</td>
            <td width="99%">
                 <select type="file" name="privKeyType" onchange="showHidePassPhrase(this)">
				    <% for (String type : PrivateKeyType.getPrivKeyTypeList()) { %>
				
				    <option value="<%= type %>"  <%= (type.equals(PrivateKeyType.PKCS8_PASSPHRASE.toString()) ? "selected" : "") %> >
				       <%= type %>
				    </option>
				    <% } %>
                 </select>
            </td>
        </tr>          
        <tr>
            <td width="1%" nowrap><label for="certf"><fmt:message key="certificate.create.cert" />:</label> *</td>
            <td width="99%">
                 <input type="file" name="certFile" />
            </td>
        </tr>     
        <tr id="privKeyRow">
            <td width="1%" nowrap><label for="privKeyf"><fmt:message key="certificate.create.privKey" />:</label> *</td>
            <td width="99%">
                 <input type="file" name="privKeyFile" />
            </td>
        </tr>   
        <tr id="passphrase">
            <td width="1%" nowrap><label for="passPhrasef"><fmt:message key="certificate.create.passphrase" />:</label> *</td>
            <td width="99%">
                <input type="password" name="passphrase" size="30" maxlength="75" 
                 id="passPhrasef" autocomplete="off">
            </td>
        </tr>                      
        <tr>

            <td colspan="2" style="padding-top: 10px;">
                <input type="submit" name="create" value="<fmt:message key="certificate.create.create" />">
                <input type="button" name="cancel" value="<fmt:message key="global.cancel"/>" onClick="location.href='certificate-summary.jsp'"></td>
        </tr>
        </tbody>
        </table>

    </div>



</form>

    </body>
</html>
