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
                 org.jivesoftware.openfire.domain.*,
                 java.net.URLEncoder"
    errorPage="error.jsp"
%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.jivesoftware.openfire.security.SecurityAuditManager" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.jivesoftware.openfire.trustcircle.TrustCircle" %>
<%@ page import="org.jivesoftware.openfire.trustcircle.TrustCircleManager" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jivesoftware.openfire.admin.AdminManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = ParamUtils.getBooleanParameter(request,"save");
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    String domainName = ParamUtils.getParameter(request,"domain");
    boolean isEnabled = ParamUtils.getBooleanParameter(request,"isenabled");
    final String[] selectedCircles = ParamUtils.getParameters( request, "circlesAssociated" );
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
        response.sendRedirect("domain-properties.jsp?domain=" + URLEncoder.encode(domainName, "UTF-8"));
        return;
    }

    // Load the domain object
    Domain domain = webManager.getDomainManager().getDomain(domainName);

    // Handle a save
    if (save) {


        if (errors.size() == 0) 
        {
        	webManager.getDomainManager().enableDomain(domainName, isEnabled);

        	// Update the trust circles
    		Collection<String> trustCicleNames = new ArrayList<>();
    		for (TrustCircle circle : TrustCircleManager.getInstance().getCirclesByDomain(domainName, false, false))
    			TrustCircleManager.getInstance().deleteCirclesFromDomain(domainName, Collections.singletonList(circle.getName()));
            
            if (selectedCircles != null && selectedCircles.length > 0)
            {
            	for (String circleName : selectedCircles)
            		TrustCircleManager.getInstance().addCirclesToDomain(domainName, Collections.singletonList(circleName));
            }        	
        	
            if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
                // Log the event
                webManager.logEvent("edited domain "+domainName, "set enabled = " + isEnabled);
            }

            // Changes good, so redirect
            response.sendRedirect("domain-properties.jsp?editsuccess=true&domain=" + URLEncoder.encode(domainName, "UTF-8"));
            return;
        }
    }
    
    // Configured and available circles
    Collection<String> confCircles = new ArrayList<>();
    for (TrustCircle circle : TrustCircleManager.getInstance().getTrustCircles(false, false))
       confCircles.add(circle.getName());
    
    Collection<String> assCircles = new ArrayList<>();
    for (TrustCircle circle : TrustCircleManager.getInstance().getCirclesByDomain(domainName, false, false))
    	assCircles.add(circle.getName());
    
    pageContext.setAttribute( "configuredCircles", confCircles);
    pageContext.setAttribute( "assocCircles", assCircles);   
    
%>

<html>
    <head>
        <title><fmt:message key="domain.edit.form.title"/></title>
        <meta name="subPageID" content="domain-properties"/>
        <meta name="extraParams" content="<%= "domain="+URLEncoder.encode(domainName, "UTF-8") %>"/>
    </head>
    <script type="text/javascript">
        // Displays or hides the configuration blocks, based on the status of selected settings.
        function applyDisplayable()
        {
            var tlsConfigs, displayValue, i, len;

            displayValue = ( document.getElementById( "tlspolicy-disabled" ).checked ? "none" : "block" );

            // Select the right configuration block and enable or disable it as defined by the the corresponding checkbox.
            tlsConfigs = document.getElementsByClassName( "tlsconfig" );
            for ( i = 0, len = tlsConfigs.length; i < len; i++ )
            {
                // Hide or show the info block (as well as it's title, which is the previous sibling element)
                tlsConfigs[ i ].parentElement.style.display = displayValue;
                tlsConfigs[ i ].parentElement.previousSibling.style.display = displayValue;
            }
        }

        // Marks all options in a select element as 'selected' (useful prior to form submission)
        function selectAllOptions( selectedId )
        {
            var select, i, len;

            select = document.getElementById( selectedId );

            for ( i = 0, len = select.options.length; i < len; i++ )
            {
                select.options[ i ].selected = true;
            }
        }

        // Moves selected option values from one select element to another.
        function moveSelectedFromTo( from, to )
        {
            var selected, i, len;

            selected = getSelectValues( document.getElementById( from ) );

            for ( i = 0, len = selected.length; i < len; i++ )
            {
                document.getElementById( to ).appendChild( selected[ i ] );
            }
        }

        // Return an array of the selected options. argument is an HTML select element
        function getSelectValues( select )
        {
            var i, len, result;

            result = [];

            for ( i = 0, len = select.options.length; i < len; i++ )
            {
                if ( select.options[ i ].selected )
                {
                    result.push( select.options[ i ] );
                }
            }
            return result;
        }

        // Ensure that the various elements are set properly when the page is loaded.
        window.onload = function()
        {
            applyDisplayable();
        };
    </script>    
    
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
        <fmt:message key="domain.edit.form.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="domain.edit.form.info" />
</p>

<form action="domain-edit-form.jsp" onsubmit="selectAllOptions('circlesAssociated')">

<input type="hidden" name="csrf" value="${csrf}">
<input type="hidden" name="domain" value="<%= StringUtils.escapeForXML(domainName) %>">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="domain.edit.form.property" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td class="c1">
                <fmt:message key="domain.create.domain" />:
            </td>
            <td>
                <%= StringUtils.escapeHTMLTags(JID.unescapeNode(domain.getDomainName())) %>
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="domain.create.isenabled" />
            </td>
            <td>
                <input type="checkbox" name="isenabled"<%= domain.isEnabled() ? " checked='checked'" : "" %>>
                (<fmt:message key="domain.create.enabled_info"/>)
            </td>
        </tr>
    </tbody>
    </table>
    </div>

</fieldset>

<br><br>

    <!--  Trust Circle Selection -->
    <fmt:message key="domain.edit.form.boxtitle" var="associatedcirclesboxtitle"/>
    <admin:contentBox title="${associatedcirclesboxtitle}">
        <p><fmt:message key="domain.edit.form.boxtitleinfo"/></p>
        <table cellpadding="3" cellspacing="0" border="0" class="tlsconfig">
            <tr><th><fmt:message key="domain.edit.form.label_associated"/></th><th></th><th><fmt:message key="domain.edit.form.label_available"/></th></tr>
            <tr>
                <td>
                    <select name="circlesAssociated" id="circlesAssociated" size="10" multiple>
                        <c:forEach items="${assocCircles}" var="item">
                                <option><c:out value="${item}"/></option>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <input type="button" onclick="moveSelectedFromTo('circlesAssociated','selectTrustCircle')" value="&gt;&gt;" /><br/>
                    <input type="button" onclick="moveSelectedFromTo('selectTrustCircle','circlesAssociated')" value="&lt;&lt;" />
                </td>
                <td>
                    <select name="selectTrustCircle" id="selectTrustCircle" size="10" multiple>
                        <c:forEach items="${configuredCircles}" var="item">
                            <c:if test="${not assocCircles.contains(item)}">
                                <option><c:out value="${item}"/></option>
                            </c:if>
                        </c:forEach>
                    </select>
                </td>
            </tr>
        </table>
    </admin:contentBox>

<br><br>

<input type="submit" value="<fmt:message key="global.save_properties" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">

</form>

<br/>

<span class="jive-description">
* <fmt:message key="domain.create.requied" />
</span>

    </body>
</html>
