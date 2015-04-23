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
	<link rel="shortcut icon" type="image/x-icon" href="./../rs/img/favicon.ico" />
	<link href="./../shapeshift.css" rel="stylesheet" type="text/css" />
	<link href="aga.css" rel="stylesheet" type="text/css" />
	<script type="text/javascript">
		var webApp = "${pageContext.request.contextPath}";
	</script>		
	<script src="./../shapeshift.js" type="text/javascript"></script>
	<script src="queue.js" type="text/javascript"></script>		
	<script src="aga.js" type="text/javascript"></script>	
	<title>ShapeShifting TV (A Golden Age)</title>
</head>
<body onload="javascript:agaLoad();" >
	<h2>ShapeShifting TV (A Golden Age)</h2>

	<section id='section_engine'><hr size="1" />
	<h3>Engine</h3>
	<form id="form_engine" name="form_engine" onsubmit="">
		<select name="owl" id="select_owl" hidden="true">
			<option value="owl=${webPath}/ontology/production/nm2.aga.nsl.ttl&rules=${webPath}/ontology/production/nm2.aga.static.rule&rules=${webPath}/ontology/production/nm2.aga.dynamic.rule">A Golden Age</option>
		</select>
		<input type="button" name="btn_start_engine" id="btn_start_engine" value="Start engine" onclick="javascript:agaStartEngine();" />
		<input type="button" name="btn_stop_engine" id="btn_stop_engine" value="Stop engine" onclick="javascript:stopEngine();" />
		<input type="button" name="btn_get_playlist" id="btn_get_playlist" value="Get playlist" onclick="javascript:getPlaylist('new');" />
		<input type="button" name="btn_play_video" id="btn_play_video" value="Play video" onclick="javascript:playVideo();" />							
		<input type="checkbox" name="cb_show_video_controls" id="cb_show_video_controls" value="Player controls" onclick="javascript:togglePlayerControls();" />
		Player controls
		<input type="checkbox" name="cb_show_playlist" id="cb_show_playlist" value="Playlist" onclick="javascript:togglePlaylist();" />
		Playlist				
		<input type="checkbox" name="cb_show_debug" id="cb_show_debug" value="Debug messages" onclick="javascript:toggleDebugMessages();" />
		Debug messages					
		<input type="button" name="btn_swap_video" id="btn_swap_video" value="Swap video" onclick="javascript:swapVideo();" hidden="true"/>							
	</form>
	</section>
	<section id="section_player"><hr size="1" />
	<h3>Player</h3>
	<section id="section_videos">
		<video id='video1' width="768" height="432" onerror="videoFailed(event)" <% out.print("display=\"true\""); %> onended="videoEnded(event)" onplaying="videoPlaying(event)">
			<source src="http://nim.goldsmiths.ac.uk/Media/AGA/MP4/905 NEW TYPE OF TELEVISION.mp4" type="video/mp4">
			Your browser does not support the video tag.
			</source>
		</video>
		<video id='video2' width="768" height="432"
			onerror="videoFailed(event)" controls <% out.print("display=\"false\""); %> >
			<!--
				<source src="http://nim.goldsmiths.ac.uk/video/902 NEW TYPE OF TELEVISION.mov" type="video/mp4">
				Your browser does not support the video tag.
				</source>
			-->
		</video>
	</section>	
	<section id="section_interactions">
		<table id="table_interactions">
			<tr>
				<td align="center">
					<button id="btn_one" class="off" onmouseover="JavaScript:illuminateLantern(0);" onmouseout="JavaScript:illuminateLantern(-1);"></button>
					<button id="btn_two" class="off" onmouseover="JavaScript:illuminateLantern(1);" onmouseout="JavaScript:illuminateLantern(-1);"></button>
					<button id="btn_three" class="off" onmouseover="JavaScript:illuminateLantern(2);" onmouseout="JavaScript:illuminateLantern(-1);"></button>
				</td>
			</tr>
			<tr>
				<td align="center">
					<span id="p_interaction" ></span>
				</td>
			</tr>
		</table>			
	</section>				  			
	</section>
    	
	<section id="section_playlist" hidden="true"><hr size="1" />
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
		
	<!--  
	<footer><hr size="1" />
		<c:out value="${eng:version()}" /><br/>
		<c:out value="${eng:revision()}"></c:out><br/>
		<c:out value="${eng:date()}"></c:out><br/>
		<c:out value="${eng:author()}"></c:out><br/>
	</footer> -->			
</body>
</html>