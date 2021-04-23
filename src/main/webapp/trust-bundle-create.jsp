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
                 org.jivesoftware.openfire.trustbundle.*,
                 java.net.URLEncoder,
                 gnu.inet.encoding.Stringprep,
                 gnu.inet.encoding.StringprepException"
    errorPage="error.jsp"
%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.HashMap"%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager" %>
<%@ page import="org.jivesoftware.openfire.admin.AdminManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters //
    boolean another = request.getParameter("another") != null;
    boolean create = another || request.getParameter("create") != null;
    boolean cancel = request.getParameter("cancel") != null;
    String trustBundleName = ParamUtils.getParameter(request,"bundleName");
    String trustBundleURL = ParamUtils.getParameter(request,"bundleURL");
    String refreshInterval = ParamUtils.getParameter(request,"refreshInterval");
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

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
        response.sendRedirect("trust-bundle-summary.jsp");
        return;
    }

    // Handle a request to create a domain:
    if (create) {
        // Validate
        if (trustBundleName == null || trustBundleName.trim().equals("")) 
        {
            errors.put("trustBundleName","");
        }
        else
        {
        	if (TrustBundleManager.getInstance().isRegisteredTrustBundle(trustBundleName))
        	{
        		errors.put("bundleExists","");
        	}
        }
        if (trustBundleURL == null || trustBundleURL.trim().equals("")) 
        {
            errors.put("trustBundleURL","");
        }

        if (refreshInterval == null || refreshInterval.trim().equals("")) 
        {
            errors.put("refreshInterval","");
        }
        
        // do a create if there were no errors
        if (errors.size() == 0) {
            try {
                TrustBundle newBundle = new TrustBundle();
                newBundle.setBundleName(trustBundleName);
                newBundle.setBundleURL(trustBundleURL);
                newBundle.setRefreshInterval(Integer.parseInt(refreshInterval));
                
                TrustBundleManager.getInstance().addTrustBundle(newBundle);

                if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) 
                {
                    // Log the event
                    webManager.logEvent("created new trust bundle " + trustBundleName, "");
                }

                // Successful, so redirect
                if (another) {
                    response.sendRedirect("trust-bundle-create.jsp?success=true");
                }
                else {
                    response.sendRedirect("trust-bundle-properties.jsp?success=true&bundleName=" +
                            URLEncoder.encode(trustBundleName, "UTF-8"));
                }
                return;
            }
            catch (TrustBundleAlreadyExistsException e) {
                errors.put("bundleExists","");
            }
            catch (Exception e) {
                errors.put("general","");
                Log.error(e);
            }
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="trustbundle.create.title"/></title>
        <meta name="pageID" content="trust-bundle-create"/>
        <meta name="helpPage" content="add_trust_bundle_to_the_system.html"/>
    </head>
    <body>


<p><fmt:message key="trustbundle.create.info" /></p>

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
                <fmt:message key="trustbundle.create.error_creating_bundle" />
            <% } else if (errors.get("trustBundleName") != null) { %>
                <fmt:message key="trustbundle.create.invalid_bundle" />  
            <% } else if (errors.get("trustBundleURL") != null) { %>
                <fmt:message key="trustbundle.create.invalid_url" />   
            <% } else if (errors.get("refreshInterval") != null) { %>
                <fmt:message key="trustbundle.create.invalid_refresh_interval" />                                                                 
            <% } else if (errors.get("bundleExists") != null) { %>
                <fmt:message key="trustbundle.create.bundle_exist" />
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
        <fmt:message key="trustbundle.create.created_success" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form name="f" action="trust-bundle-create.jsp" method="get">
    <input type="hidden" name="csrf" value="${csrf}">

    <div class="jive-contentBoxHeader">
        <fmt:message key="trustbundle.create.new_trustbundle" />
    </div>
    <div class="jive-contentBox">
        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
        <tr>
            <td width="1%" nowrap><label for="bundletf"><fmt:message key="trustbundle.create.bundle" />:</label> *</td>
            <td width="99%">
                <input type="text" name="bundleName" size="30" maxlength="75" value="<%= ((trustBundleName!=null) ? StringUtils.escapeForXML(trustBundleName) : "") %>"
                 id="bundletf" autocomplete="off">
            </td>
        </tr>  
        <tr>
            <td width="1%" nowrap><label for="bundleURLtf"><fmt:message key="trustbundle.create.bundleURL" />:</label> *</td>
            <td width="99%">
                <input type="text" name="bundleURL" size="30" maxlength="75" value="<%= ((trustBundleURL!=null) ? StringUtils.escapeForXML(trustBundleURL) : "") %>"
                 id="bundleURLtf" autocomplete="off">
            </td>
        </tr>  
        <tr>
            <td width="1%" nowrap><label for="bundleRefreshtf"><fmt:message key="trustbundle.create.bundle_refresh_interval" />:</label> *</td>
            <td width="99%">
                <input type="text" name="refreshInterval" size="30" maxlength="75" value="<%= ((refreshInterval!=null) ? StringUtils.escapeForXML(refreshInterval) : "") %>"
                 id="bundleRefreshtf" autocomplete="off">
            </td>
        </tr>                           
        <tr>

            <td colspan="2" style="padding-top: 10px;">
                <input type="submit" name="create" value="<fmt:message key="trustbundle.create.create" />">
                <input type="submit" name="another" value="<fmt:message key="trustbundle.create.create_another" />">
                <input type="submit" name="cancel" value="<fmt:message key="global.cancel" />"></td>
        </tr>
        </tbody>
        </table>

    </div>

    <span class="jive-description">
    * <fmt:message key="trustbundle.create.requied" />
    </span>


</form>

<script language="JavaScript" type="text/javascript">
document.f.bundleName.focus();
</script>


    </body>
</html>
