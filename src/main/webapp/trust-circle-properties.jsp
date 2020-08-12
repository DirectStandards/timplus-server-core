<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="org.jivesoftware.openfire.XMPPServer" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.jivesoftware.openfire.Connection" %>
<%@ page import="org.jivesoftware.openfire.spi.*" %>
<%@ page import="org.jivesoftware.openfire.trustcircle.TrustCircle" %>
<%@ page import="org.jivesoftware.openfire.trustcircle.TrustCircleManager" %>
<%@ page import="org.jivesoftware.openfire.domain.DomainManager" %>
<%@ page import="org.jivesoftware.openfire.domain.Domain" %>
<%@ page import="org.jivesoftware.openfire.trustanchor.TrustAnchorManager" %>
<%@ page import="org.jivesoftware.openfire.trustanchor.TrustAnchor" %>
<%@ page import="org.jivesoftware.openfire.trustbundle.TrustBundleManager" %>
<%@ page import="org.jivesoftware.openfire.trustbundle.TrustBundle" %>
<%@ page import="org.jivesoftware.openfire.domain.Domain" %>
<%@ page import="org.bouncycastle.asn1.x509.X509Name" %>
<%@ page import="org.bouncycastle.jce.PrincipalUtil" %>
<%@ page import="org.bouncycastle.jce.X509Principal" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.*" %>
<%@ page errorPage="error.jsp" %>

<%@ taglib uri="admin" prefix="admin" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>
<%
    boolean update = request.getParameter( "update" ) != null;
    boolean cancel = request.getParameter("cancel") != null;

    final Map<String, String> errors = new HashMap<>();
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");
    
    String circleName = ParamUtils.getParameter(request,"circleName");
    final String[] selectedBundles = ParamUtils.getParameters( request, "bundlesAssociated" );
    final String[] selectedAnchors = ParamUtils.getParameters( request, "anchorsAssociated" );
    
    // Handle a cancel
    if (cancel) {
        response.sendRedirect("trust-circle-summary.jsp");
        return;
    }
    
    if (update) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            update = false;
            errors.put("csrf", "CSRF Failure!");
        }
    }
    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);
    pageContext.setAttribute( "errors", errors );
    pageContext.setAttribute( "circleName", circleName );
    
    TrustCircle trustCircle = null;
    Collection<Domain> associatedDomains = null;
    try {
    	trustCircle = TrustCircleManager.getInstance().getTrustCircle(circleName, true, true);
    	associatedDomains = TrustCircleManager.getInstance().getTrustCircleDomains(circleName);
    }
    catch (Exception unfe) {
    }
    
    if ( update && errors.isEmpty() )
    {
    	// Update the bundles
		Collection<String> deleteBundleNames = new ArrayList<>();
		for (TrustBundle bundle : trustCircle.getTrustBundles())
			deleteBundleNames.add(bundle.getBundleName());
		
        if (deleteBundleNames.size() > 0)
        	TrustCircleManager.getInstance().deleteTrustBundlesFromCircle(trustCircle.getName(), deleteBundleNames);
        
        if (selectedBundles != null && selectedBundles.length > 0)
        {
        	for (String bundleName : selectedBundles)
        		TrustCircleManager.getInstance().addTrustBundleToCircle(trustCircle.getName(), bundleName);
        }
        
        // Update the anchors
		
        // Create a map of anchors CNs to thumbprints
        Map<String, String> anchorCNIDs = new HashMap<>();
        for (TrustAnchor anchor : TrustAnchorManager.getInstance().getAnchors())
        {
        	final X509Principal principal = PrincipalUtil.getSubjectX509Principal(anchor.asX509Certificate());
        	final Vector<?> values = principal.getValues(X509Name.CN);
        	String cn = (String) values.get(0);
        	
        	anchorCNIDs.put(cn, anchor.getThumbprint());
        }
        
        for (TrustAnchor anchor : trustCircle.getAnchors())
			TrustCircleManager.getInstance().deleteTrustAnchorFromCircle(trustCircle.getName(), anchor.getThumbprint());
        
        if (selectedAnchors != null && selectedAnchors.length > 0)
        {
        	for (String selectedAnchor : selectedAnchors)
        	{
            	String anchorId = anchorCNIDs.get(selectedAnchor);
            	if (anchorId != null)
        			TrustCircleManager.getInstance().addTrustAnchorToCircle(trustCircle.getName(), anchorId);
        	}
        }
        
        // Changes good, so redirect
        response.sendRedirect("trust-circle-summary.jsp?editsuccess=true");
        return;
    }

    if ( errors.isEmpty() ) {

    }

    // Configured and available anchors
    Collection<String> confAnchors = new ArrayList<>();
    for (TrustAnchor anchor : TrustAnchorManager.getInstance().getAnchors())
    {
    	final X509Principal principal = PrincipalUtil.getSubjectX509Principal(anchor.asX509Certificate());
    	final Vector<?> values = principal.getValues(X509Name.CN);
    	confAnchors.add((String) values.get(0));
    }
    
    Collection<String> assAnchors = new ArrayList<>();
    for (TrustAnchor anchor : trustCircle.getAnchors())
    {
    	final X509Principal principal = PrincipalUtil.getSubjectX509Principal(anchor.asX509Certificate());
    	final Vector<?> values = principal.getValues(X509Name.CN);
    	assAnchors.add((String) values.get(0));
    }
    
    // Configured and available bundles
    Collection<String> confBundles = new ArrayList<>();
    for (TrustBundle bundle : TrustBundleManager.getInstance().getTrustBundles(false))
    	confBundles.add(bundle.getBundleName());
    
    Collection<String> assBundles = new ArrayList<>();
    for (TrustBundle bundle : trustCircle.getTrustBundles())
    	assBundles.add(bundle.getBundleName());
    
    pageContext.setAttribute( "configuredDomains", DomainManager.getInstance().getDomainNames(true) );
    pageContext.setAttribute( "configuredAnchors", confAnchors);
    pageContext.setAttribute( "assocAnchors", assAnchors);
    pageContext.setAttribute( "configuredBundles", confBundles);
    pageContext.setAttribute( "assocBundles", assBundles);

%>

<html>
<head>
    <head>
        <title><fmt:message key="trustcircle.properties.title"/></title>
        <meta name="subPageID" content="trust-circle-properties"/>
        <meta name="extraParams" content="<%= "circleName="+URLEncoder.encode(circleName, "UTF-8") %>"/>
        <meta name="helpPage" content="trust_circle_properties.html"/>
    </head>
    <%--<meta name="subPageID" content="connection-settings-advanced"/>--%>
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

</head>
<body>

<!-- Display all errors -->
<c:forEach var="err" items="${errors}">
    <admin:infobox type="error">
        <c:choose>
            <c:when test="${err.key eq 'connectionType'}"><fmt:message key="connection.advanced.settings.error.connectiontype"/></c:when>
            <c:when test="${err.key eq 'connectionMode'}"><fmt:message key="connection.advanced.settings.error.connectionmode"/></c:when>
            <c:otherwise>
                <c:if test="${not empty err.value}">
                    <fmt:message key="admin.error"/>: <c:out value="${err.value}"/>
                </c:if>
                (<c:out value="${err.key}"/>)
            </c:otherwise>
        </c:choose>
    </admin:infobox>
</c:forEach>


<!-- Introduction at the top of the page -->
<p>
    <fmt:message key="trustcircle.properties.info"/>
</p>

<form action="trust-circle-properties.jsp" onsubmit="selectAllOptions('bundlesAssociated') && selectAllOptions('anchorsAssociated')" method="post">
    <input type="hidden" name="csrf" value="${csrf}">
    <input type="hidden" name="update" value="true" />
    <input type="hidden" name="circleName" value="<%= StringUtils.escapeForXML(circleName) %>">
    <!--  Trust Bundle Selection -->
    <fmt:message key="trustcircle.properties.trustbundles.boxtitle" var="associatedbundlesboxtitle"/>
    <admin:contentBox title="${associatedbundlesboxtitle}">
        <p><fmt:message key="trustcircle.properties.trustbundles.info"/></p>
        <table cellpadding="3" cellspacing="0" border="0" class="tlsconfig">
            <tr><th><fmt:message key="trustcircle.properties.trustbundles.label_associated"/></th><th></th><th><fmt:message key="trustcircle.properties.trustbundles.label_available"/></th></tr>
            <tr>
                <td>
                    <select name="bundlesAssociated" id="bundlesAssociated" size="10" multiple>
                        <c:forEach items="${assocBundles}" var="item">
                                <option><c:out value="${item}"/></option>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <input type="button" onclick="moveSelectedFromTo('bundlesAssociated','selectTrustBundle')" value="&gt;&gt;" /><br/>
                    <input type="button" onclick="moveSelectedFromTo('selectTrustBundle','bundlesAssociated')" value="&lt;&lt;" />
                </td>
                <td>
                    <select name="selectTrustBundle" id="selectTrustBundle" size="10" multiple>
                        <c:forEach items="${configuredBundles}" var="item">
                            <c:if test="${not assocBundles.contains(item)}">
                                <option><c:out value="${item}"/></option>
                            </c:if>
                        </c:forEach>
                    </select>
                </td>
            </tr>
        </table>
    </admin:contentBox>

    <!--  Trust Anchor Selection -->
    <fmt:message key="trustcircle.properties.trustanchors.boxtitle" var="associatedanchorsboxtitle"/>
    <admin:contentBox title="${associatedanchorsboxtitle}">
        <p><fmt:message key="trustcircle.properties.trustanchors.info"/></p>
        <table cellpadding="3" cellspacing="0" border="0" class="tlsconfig">
            <tr><th><fmt:message key="trustcircle.properties.trustanchors.label_associated"/></th><th></th><th><fmt:message key="trustcircle.properties.trustanchors.label_available"/></th></tr>
            <tr>
                <td>
                    <select name="anchorsAssociated" id="anchorsAssociated" size="10" multiple>
                        <c:forEach items="${assocAnchors}" var="item">
                            <c:if test="${configuredAnchors.contains(item)}">
                                <option><c:out value="${item}"/></option>
                            </c:if>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <input type="button" onclick="moveSelectedFromTo('anchorsAssociated','selectAnchors')" value="&gt;&gt;" /><br/>
                    <input type="button" onclick="moveSelectedFromTo('selectAnchors','anchorsAssociated')" value="&lt;&lt;" />
                </td>
                <td>
                    <select name="selectAnchors" id="selectAnchors" size="10" multiple>
                        <c:forEach items="${configuredAnchors}" var="item">
                            <c:if test="${not assocAnchors.contains(item)}">
                                <option><c:out value="${item}"/></option>
                            </c:if>
                        </c:forEach>
                    </select>
                </td>
            </tr>
        </table>
    </admin:contentBox>

    <input type="submit" name="update" value="<fmt:message key="global.save_settings" />">
    <input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>
</body>
</html>
