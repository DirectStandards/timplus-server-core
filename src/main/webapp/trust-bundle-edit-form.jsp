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

<%@ page import="org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.util.StringUtils,
                 org.jivesoftware.util.CookieUtils,
                 org.jivesoftware.util.DomainResolver,
                 org.jivesoftware.openfire.trustbundle.*,
                 java.net.URLEncoder"
    errorPage="error.jsp"
%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.jivesoftware.openfire.admin.AdminManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = ParamUtils.getBooleanParameter(request,"save");
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    String trustBundleName = ParamUtils.getParameter(request,"bundleName");
    String trustBundleURL = ParamUtils.getParameter(request,"bundleURL");
    String refreshInterval = ParamUtils.getParameter(request,"refreshInterval");
    Map<String, String> errors = new HashMap<String, String>();
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");
    if (save) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            save = false;
            errors.put("csrf", "CSRF Failure");
        }
    }
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    // Handle a cancel
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("trust-bundle-properties.jsp?bundleName=" + URLEncoder.encode(trustBundleName, "UTF-8"));
        return;
    }

    // Load the trust bundle object
    TrustBundle bundle = TrustBundleManager.getInstance().getTrustBundle(trustBundleName);

    // Handle a save
    if (save) {


        if (trustBundleURL == null || trustBundleURL.trim().equals("")) 
        {
            errors.put("trustBundleURL","");
        }

        if (refreshInterval == null || refreshInterval.trim().equals("")) 
        {
            errors.put("refreshInterval","");
        }    	
    	
        if (errors.size() == 0) 
        {
        	
        	bundle.setBundleName(trustBundleName);
        	bundle.setBundleURL(trustBundleURL);
        	bundle.setRefreshInterval(Integer.parseInt(refreshInterval));
        	
        	TrustBundleManager.getInstance().updateTrustBundleAttributes(trustBundleName, bundle, true);

            if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
                // Log the event
                webManager.logEvent("edited trust bundle "+ trustBundleName, "");
            }

            // Changes good, so redirect
            response.sendRedirect("trust-bundle-properties.jsp?editsuccess=true&bundleName=" + URLEncoder.encode(trustBundleName, "UTF-8"));
            return;
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="trustbundle.edit.form.title"/></title>
        <meta name="subPageID" content="trust-bundle-properties"/>
        <meta name="extraParams" content="<%= "bundleName="+URLEncoder.encode(trustBundleName, "UTF-8") %>"/>
    </head>
    <body>
<%  if (!errors.isEmpty()) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">

            <% if (errors.get("csrf") != null) { %>
                CSRF Failure!
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="trustbundle.edit.form.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="trustbundle.edit.form.info" />
</p>

<form action="trust-bundle-edit-form.jsp">

<input type="hidden" name="csrf" value="${csrf}">
<input type="hidden" name="bundleName" value="<%= StringUtils.escapeForXML(trustBundleName) %>">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="trustbundle.edit.form.property" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td class="c1">
                <fmt:message key="trustbundle.create.bundle" />:
            </td>
            <td>
                <%= StringUtils.escapeHTMLTags(trustBundleName) %>
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="trustbundle.create.bundleURL" />
            </td>
            <td width="99%">
                <input type="text" name="bundleURL" size="30" maxlength="75" value="<%= bundle.getBundleURL() %>"
                 id="bundleURLtf" autocomplete="off">
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="trustbundle.create.bundle_refresh_interval" />
            </td>
            <td width="99%">
                <input type="text" name="refreshInterval" size="30" maxlength="75" value="<%= bundle.getRefreshInterval() %>"
                 id="bundleURLtf" autocomplete="off">
            </td>
        </tr>        
    </tbody>
    </table>
    </div>

</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="global.save_properties" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">

</form>

<br/>

<span class="jive-description">
* <fmt:message key="trustbundle.create.requied" />
</span>

    </body>
</html>
