<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="eng" uri="http://nsl.shapeshifting.tv/taglibs/engine"%>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="shapeshift.css" rel="stylesheet" type="text/css" />
<link rel="shortcut icon" type="image/x-icon" href="rs/img/favicon.ico" />
<title><c:out value="${eng:version()}" /></title>
</head>
<body>
	<c:if test="${requestScope['result'] != null}">
		<p class="grey16bf">Response</p>
		<pre><c:out value="${requestScope['result']}" /></pre>
	</c:if>
	<c:if test="${requestScope['error'] != null}">
		<p class="grey16bf">Error</p>
		<pre><c:out value="${requestScope['error']}" /></pre>
	</c:if>	
</body>
</html>