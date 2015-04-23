<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="eng" uri="http://nsl.shapeshifting.tv/taglibs/engine"%>

<c:set var="webPath" value="http://${header.host}${pageContext.request.contextPath}"></c:set>
    
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<link rel="shortcut icon" type="image/x-icon" href="rs/img/favicon.ico" />
	<link href="shapeshift.css" rel="stylesheet" type="text/css" />
	<script type="text/javascript">
		var webApp = "${pageContext.request.contextPath}";
	</script>
	<script src="shapeshift.js" type="text/javascript"></script>	
	<title>ShapeShifting TV</title>
</head>
<body onload="javascript:onLoad();">
	<h2>ShapeShifting TV</h2>

	<section id='section_engine'><hr size="1" />
	<h3>Engine</h3>
	<form id="form_engine" name="form_engine" onsubmit="">
		Narrative: 
		<select name="owl" id="select_owl">
			<option value="owl=${webPath}/ontology/production/nm2.aga.nsl.ttl&rules=${webPath}/ontology/production/nm2.aga.static.rule&rules=${webPath}/ontology/production/nm2.aga.dynamic.rule">A Golden Age</option>
			<!-- <option value="owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos.nsl.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos.content.ttl&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/myvideos.static.rule">MyVideos</option> -->
			<option value="owl=${webPath}/ontology/production/ta2.myvideos2.static.nsl.ttl&owl=${webPath}/ontology/production/ta2.myvideos2.content.ttl&owl=${webPath}/ontology/production/ta2.myvideos2.annotations.ttl&rules=${webPath}/ontology/production/ta2.myvideos2.static.rule">MyVideos2.static</option>
			<option value="owl=${webPath}/ontology/production/ta2.myvideos2.dynamic.nsl.ttl&owl=${webPath}/ontology/production/ta2.myvideos2.content.ttl&owl=${webPath}/ontology/production/ta2.myvideos2.annotations.ttl&rules=${webPath}/ontology/production/ta2.myvideos2.static.rule">MyVideos2.dynamic</option>
		<!--  
			<option value="owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/nm2aga.production.1.owl&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/aga.static.rule&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/aga.dynamic.rule">A Golden Age</option>
			<option value="owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos.nsl.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos.content.ttl&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/myvideos.static.rule">MyVideos</option>
			<option value="owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.nsl.static.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.content.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.annotations.ttl&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/myvideos.static.rule">MyVideos2.static</option>
			<option value="owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.nsl.dynamic.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.content.ttl&owl=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/owl/ta2myvideos2.annotations.ttl&rules=http://nim.goldsmiths.ac.uk/shapeshifting-tv/rs/rule/myvideos.static.rule">MyVideos2.dynamic</option>
		-->
		</select>							
		<input type="button" name="btn_start_engine" id="btn_start_engine" value="Start" onclick="javascript:startEngine();" />
		<input type="button" name="btn_stop_engine" id="btn_stop_engine" value="Stop" onclick="javascript:stopEngine();" />
		<input type="button" name="btn_get_playlist" id="btn_get_playlist" value="Get playlist" onclick="javascript:getPlaylist('new');" />	
		<a href="Share?dummy" target="_blank">Edit</a>						
		<input type="checkbox" name="cb_show_debug" id="cb_show_debug" value="Debug messages" onclick="javascript:toggleDebugMessages();" />		
		Debug messages		
	</form>
	</section>		      
	
	<section id="section_playlist"><hr size="1" />
	<h3>Playlist</h3>
	<table summary='Playlist'> 
		<tbody id='playlist'></tbody> 
	</table>   						
	</section>	

	<section id="section_debug"><hr size="1" />
	<h3>Debug</h3>	
	<p id="p_error" />		
	<p id="p_warn" />		
	<p id="p_debug" />	
	</section>
	
	<footer><hr size="1" />
		<c:out value="${eng:version()}" /><br/>
		<c:out value="${eng:revision()}" /><br/>
		<c:out value="${eng:date()}" /><br/>
		<c:out value="${eng:author()}" /><br/>
	</footer>	
</body>
</html>