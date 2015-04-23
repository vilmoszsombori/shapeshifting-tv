// var webApp is assumed to be defined

var receiveReq = getXmlHttpRequestObject();
var engine = {};
var playlist = {};

//Gets the browser specific XmlHttpRequest Object
function getXmlHttpRequestObject() {
	if (window.XMLHttpRequest) {
		return new XMLHttpRequest();
	} else if(window.ActiveXObject) {
		return new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		error('Status: Cound not create XmlHttpRequest Object.' +
				'Consider upgrading your browser.');
	}
}

function submitJsonReq(url, handler) {
	if (receiveReq.readyState == 4 || receiveReq.readyState == 0) {
		receiveReq.open("GET", url, true);
		receiveReq.onreadystatechange = handler; 
		receiveReq.send(null);
	}				
}

function updateButtons(response) {
	var value = ( response === undefined ) ? true : (response.interpreter === undefined);
	var elem = document.getElementById('btn_start_engine');
	if ( elem !== undefined )
		elem.disabled = !value;
	var elem = document.getElementById('btn_stop_engine');
	if ( elem !== undefined )
		elem.disabled = value;
	var elem = document.getElementById('btn_get_playlist');
	if ( elem !== undefined )
		elem.disabled = ( response === undefined ) ? true : ((response.interpreter === undefined) ? true : !(response.interpreter === 'INITIALIZED' || response.interpreter === 'INTERPRETING'));
	var elem = document.getElementById('select_owl');
	if ( elem !== undefined )
		elem.disabled = !value;
}

function engineStatus() {
	submitJsonReq(webApp + '/engine', handleStatusReq);	
}

function handleStatusReq() {
	if (receiveReq.readyState == 4) {
		debug();
		try {
			var response = eval("(" + receiveReq.responseText + ")");			
			updateButtons(response);
			if ( response.error === undefined ) {
				engine.status = response.interpreter;
				engine.sessionid = response.sessionid;
				debug('Session id [' + response.sessionid + ']');
				debug('Interpreter state [' + response.interpreter + ']');
				debug('Version: ' + response.version);
				debug('OWL size: ' + response.owl);
			} else {
				engine.error = response.error;
				error(response.error);
			}
		} catch ( SyntaxError ) {
			debug(receiveReq.responseText);
			error("Syntax error: failed to parse JSON response.");
		}
	}
}

function startEngine() {
	document.getElementById('btn_start_engine').disabled = true;
	document.getElementById('btn_stop_engine').disabled = true;
	document.getElementById('select_owl').disabled = true;
	var select_owl = document.getElementById('select_owl');
	debug('OWL = ' + select_owl.options[select_owl.selectedIndex].text);
	debug('Staring engine...');
	submitJsonReq(webApp +'/engine?command=start&' + select_owl.options[select_owl.selectedIndex].value, handleStartEngineReq);
}

function handleStartEngineReq() {	
	if (receiveReq.readyState == 4) {
		debug('Start engine returned.');
		engineStatus();
	}	
}

function stopEngine() {
	debug('Stopping engine...');	
	submitJsonReq(webApp + '/engine?command=stop', handleStopEngineReq);
}

function handleStopEngineReq() {	
	if (receiveReq.readyState == 4) {
		debug('Stop engine returned.');
		engineStatus();
	}	
}

function debug(text) {
	var p_debug = document.getElementById('p_debug');
	if ( text === undefined ) {
		p_debug.innerHTML = '';
	} else {
		p_debug.innerHTML += text + '<br/>';		
	}
}

function warn(text) {
	var p_warn = document.getElementById('p_warn');
	p_warn.innerHTML = text;		
}

function error(text) {
	var p_error = document.getElementById('p_error');
	p_error.innerHTML = text;		
}

function onLoad() {
	toggleDebugMessages();
	engineStatus();
	getPlaylist('old');
}

//Gets the last playlist fragment from the narrative engine
function getPlaylist(command) {
	if ( command === undefined )
		command = 'new';
	if (receiveReq.readyState == 4 || receiveReq.readyState == 0) {
		debug(webApp + '/playlist?command=' + command);
		receiveReq.open("GET", webApp + '/playlist?command=' + command, true);
		receiveReq.onreadystatechange = handlePlaylistReq;
		receiveReq.command = command;
		receiveReq.send(null);
	}			
} 

function handlePlaylistReq() {
	if (receiveReq.readyState == 4) {
		try {
			var response = eval("(" + receiveReq.responseText + ")");
			
			playlist.video = undefined;
			playlist.audio = undefined;
			playlist.interaction = undefined;
			playlist.items = new Array();
			
			if ( response.video !== undefined ) {
				playlist.video = response.video;
				for ( var i = 0; i < playlist.video.length; i++ ) {
					playlist.items[playlist.video[i].id] = playlist.video[i];
					playlist.video[i].interactions = new Array();
				}								
			}

			if ( response.audio !== undefined ) {
				playlist.audio = response.audio;
				for ( var i = 0; i < playlist.audio.length; i++ ) {
					playlist.items[playlist.audio[i].id] = playlist.audio[i];
					playlist.audio[i].interactions = new Array();
				}								
			}
			
			if ( response.interaction !== undefined ) {
				playlist.interaction = response.interaction;
				for ( var i = 0; i < playlist.interaction.length; i++ ) {					
					var reference = playlist.items[playlist.interaction[i].begin.substring(0, playlist.interaction[i].begin.indexOf("."))];
					var relStartTime = new Number(playlist.interaction[i].begin.substring(playlist.interaction[i].begin.indexOf("+"), playlist.interaction[i].begin.length - 1));
					var relStopTime = new Number(playlist.interaction[i].dur.substring(0, playlist.interaction[i].dur.length - 1));
					// TODO unsafe to use relStartTime as index. Multiple interactions with the same relStartTime won't work.
					reference.interactions[playlist.interaction[i].id] = playlist.interaction[i];				
					playlist.interaction[i].reference = reference;
					playlist.interaction[i].relStartTime = relStartTime;
					playlist.interaction[i].relStopTime = relStopTime;
				}
			}
			
			if ( typeof customHandlePlaylistReq === 'function' && receiveReq.command === 'new' ) {
				warn('Calling out to a customHandlePlaylistReq...');
				customHandlePlaylistReq(response);				
			}
			
			// update the UI
			var playlistTbody = document.getElementById('playlist');
			while ( playlistTbody.childNodes.length > 0 )
				playlistTbody.removeChild(playlistTbody.lastChild);			
			var tr, td;
			
			// update the videos
			if ( playlist.video !== undefined ) {
				tr = document.createElement('tr');
				td = document.createElement('th');
				td.setAttribute("colspan", 3);
				td.setAttribute("align", "middle");
				td.innerHTML = 'video';			
				tr.appendChild(td);
				playlistTbody.appendChild(tr);								
				for ( var i = 0; i < playlist.video.length; i++ ) {
					tr = document.createElement('tr');
					tr.setAttribute("id", playlist.video[i].id);
					tr.className = "true";
					//td.setAttribute("colspan", 3);
					td = document.createElement('td');
					td.innerHTML = '<a href="' + playlist.video[i].src + '?start=' + playlist.video[i].clipBegin + '&end=' + playlist.video[i].clipEnd + '" target="_blank">' + playlist.video[i].src + '</a>';			
					tr.appendChild(td);
					td = document.createElement('td');
					td.setAttribute("align", "right");
					td.innerHTML = playlist.video[i].clipBegin;			
					tr.appendChild(td);
					td = document.createElement('td');
					td.setAttribute("align", "right");
					td.innerHTML = playlist.video[i].clipEnd;			
					tr.appendChild(td);
					playlistTbody.appendChild(tr);
					for ( var iKey in playlist.video[i].interactions ) {
						var interaction = playlist.video[i].interactions[iKey];
						tr = document.createElement('tr');
						tr.className = "false";
						td = document.createElement('td');
						td.setAttribute("align", "right");
						td.innerHTML = '<a href="' + interaction.url +'" target="_blank">' + interaction.text + '</a>';			
						tr.appendChild(td);
						td = document.createElement('td');
						td.setAttribute("align", "right");
						td.innerHTML = 'begin=' + interaction.relStartTime + 's';
						tr.appendChild(td);					
						td = document.createElement('td');
						td.setAttribute("align", "right");					
						td.innerHTML = 'dur=' + interaction.relStopTime + 's';
						tr.appendChild(td);
						playlistTbody.appendChild(tr);										
					}					
				}								
			}

			// update the audios
			if ( response.audio !== undefined ) {
				tr = document.createElement('tr');
				td = document.createElement('th');
				td.setAttribute("colspan", 3);
				td.setAttribute("align", "middle");
				td.innerHTML = 'audio';			
				tr.appendChild(td);
				playlistTbody.appendChild(tr);								
				for ( var i = 0; i < playlist.audio.length; i++ ) {
					tr = document.createElement('tr');
					tr.setAttribute("id", playlist.audio[i].id);
					tr.className = "true";
					td = document.createElement('td');
					//td.setAttribute("colspan", 3);
					td.innerHTML = '<a href="' + playlist.audio[i].src + '" target="_blank">' + playlist.audio[i].src + '</a>';			
					tr.appendChild(td);
					td = document.createElement('td');
					td.setAttribute("align", "right");
					td.innerHTML = playlist.audio[i].clipBegin;			
					tr.appendChild(td);
					td = document.createElement('td');
					td.setAttribute("align", "right");
					td.innerHTML = playlist.audio[i].clipEnd;			
					tr.appendChild(td);
					playlistTbody.appendChild(tr);
					for ( var iKey in playlist.audio[i].interactions ) {
						var interaction = playlist.audio[i].interactions[iKey];
						tr = document.createElement('tr');
						tr.className = "false";
						td = document.createElement('td');
						td.setAttribute("align", "right");
						td.innerHTML = '<a href="' + interaction.url +'" target="_blank">' + interaction.text + '</a>';			
						tr.appendChild(td);
						td = document.createElement('td');
						td.setAttribute("align", "right");
						td.innerHTML = 'begin=' + interaction.relStartTime + 's';
						tr.appendChild(td);					
						td = document.createElement('td');
						td.setAttribute("align", "right");					
						td.innerHTML = 'dur=' + interaction.relStopTime + 's';
						tr.appendChild(td);
						playlistTbody.appendChild(tr);										
					}					
				}														
			}			
		} catch(SyntaxError) {
			error("Syntax error: failed to parse JSON response.");
			debug(receiveReq.responseText);
		}
		engineStatus();
	}
}

function toggleDebugMessages() {
	document.getElementById('section_debug').hidden = !document.getElementById('cb_show_debug').checked;
}

function submitInteraction(url) {
	if (receiveReq.readyState == 4 || receiveReq.readyState == 0) {
		undisplayInteraction();
		receiveReq.open("GET", url, true);
		receiveReq.onreadystatechange = handleInteractionReq; 
		receiveReq.send(null);
	}				
}

function handleInteractionReq() {
	if (receiveReq.readyState == 4) {
		debug(receiveReq.responseText);
	}
}