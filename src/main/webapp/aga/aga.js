// var webApp is assumed to be defined

var videoQueue = new Queue();
var interactionQueue = new Queue();
playlist.ready = true;
playlist.poll = 20;

var media_events = new Array();
media_events["loadstart"] = 0;
media_events["progress"] = 0;
media_events["suspend"] = 0;
media_events["emptied"] = 0;
media_events["stalled"] = 0;
media_events["play"] = 0;
media_events["pause"] = 0;
media_events["loadedmetadata"] = 0;
media_events["loadeddata"] = 0;
media_events["waiting"] = 0;
media_events["playing"] = 0;
media_events["canplay"] = 0;
media_events["canplaythrough"] = 0;
media_events["seeking"] = 0;
media_events["seeked"] = 0;
media_events["timeupdate"] = 0;
media_events["ended"] = 0;
media_events["ratechange"] = 0;
media_events["durationchange"] = 0;
media_events["volumechange"] = 0;

function agaStartEngine() {
	startEngine();
	document._video.play();	
}

function videoFailed(e) {
	// video playback failed - show a message saying why
	switch (e.target.error.code) {
		case e.target.error.MEDIA_ERR_ABORTED:
			alert('You aborted the video playback.');
			break;
		case e.target.error.MEDIA_ERR_NETWORK:
			alert('A network error caused the video download to fail part-way.');
			break;
		case e.target.error.MEDIA_ERR_DECODE:
			alert('The video playback was aborted due to a corruption problem or because the video used features your browser did not support.');
			break;
		case e.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED:
			alert('The video could not be loaded, either because the server or network failed or because the format is not supported.');
			warn("Video is being skipped...");
			playVideo();
			break;
		default:
			alert('An unknown error occurred.');
			break;
	}
}

function swapVideo() {
	var v1 = document.getElementById("video1").getAttribute("display");
	var v2 = document.getElementById("video2").getAttribute("display");
	document.getElementById("video1").setAttribute("display", v2);
	document.getElementById("video2").setAttribute("display", v1);
}

function agaLoad() {
	onLoad();
	/*
	document.getElementById("video2").setAttribute("display", "false");
	document.getElementById("video1").setAttribute("display", "true");
	*/
    document.lanterns = new Array(3);
    document.lanterns[0] = document.getElementById("btn_one");   
    document.lanterns[1] = document.getElementById("btn_two");   
    document.lanterns[2] = document.getElementById("btn_three");
	document._video = document.getElementById("video1");	
    for (key in media_events) {	
    	document._video.addEventListener(key, capture, false);
    }    
}

function illuminateLantern(n) {
	for ( var i = 0; i < document.lanterns.length; i++ )
		document.lanterns[i].setAttribute("class", "off");
	for ( var i = 0; i <= n; i++ )
		document.lanterns[i].setAttribute("class", "on");
}

function videoEnded(event) {
	document.getElementById("btn_play_video").setAttribute("value", "Play video");
	playVideo();	
}

function videoPlaying(event) {
	document.getElementById("btn_play_video").setAttribute("value", "Skip video");
}

function videoTimeUpdate(event) {/*
	if ( document._video.error == null ) 
		updateInteraction();
    if ( videoQueue.isEmpty() ) {
    	//debug("Time left: " + ( document._video.duration - document._video.currentTime ) );	    	
    	if ( document._video.duration - document._video.currentTime < playlist.poll && playlist.ready ) {
    		playlist.ready = false;
    		debug("Playlist is being updated...");
    		getPlaylist('new');
    	}
    }*/	
}

function capture(event) {
	//debug(event.type);
	if ( event.type == 'ended' ) { /*
		document.getElementById("btn_play_video").setAttribute("value", "Play video");
		playVideo();*/
	} else {/*
		if ( event.type == 'playing')
			document.getElementById("btn_play_video").setAttribute("value", "Skip video");*/
		if ( document._video.error == null ) 
			updateInteraction();
	    if ( videoQueue.isEmpty() ) {
	    	//debug("Time left: " + ( document._video.duration - document._video.currentTime ) );	    	
	    	if ( document._video.duration - document._video.currentTime < playlist.poll && playlist.ready ) {
	    		playlist.ready = false;
	    		debug("Playlist is being updated...");
	    		getPlaylist('new');
	    		playlist.poll = 5;
	    	}
	    }
	}
}

function playVideo() {
	if ( videoQueue.isEmpty() ) {
		warn("Video queue is empty.");
		return;
	}
	playlist.currentVideo = videoQueue.dequeue();
    document._video.src = playlist.currentVideo.src;
    document._video.load();
    document._video.play();
    var tr = document.getElementById(playlist.currentVideo.id);
    if ( tr !== undefined ) {
    	tr.className = "active";
    }
}

function updateInteraction() {
	if ( !interactionQueue.isEmpty() ) {
		var item = interactionQueue.peek();
		if ( item.reference == playlist.currentVideo ) {			
			item = interactionQueue.dequeue();
			if ( document._video.error == null ) {
				setTimeout('displayInteraction("' + item.text + '")', item.relStartTime * 1000);
				setTimeout(undisplayInteraction, (item.relStartTime + item.relStopTime) * 1000);
				debug(item.text + " | " + item.relStartTime + " | " + item.relStopTime);
			} else {
				alert("It should never get here!");
			}
		}
	}
}

function undisplayInteraction() {
    var ib  = document.getElementById("section_interactions");
    ib.setAttribute("display", false);
}

function displayInteraction(text) {
    var ib;
    ib = document.getElementById("p_interaction");
    ib.innerHTML = text;
    ib = document.getElementById("btn_one");
    ib.setAttribute("onclick", "javascript:submitInteraction('" + webApp + "/interaction?" + text + "=1');");
    ib = document.getElementById("btn_two");
    ib.setAttribute("onclick", "javascript:submitInteraction('" + webApp + "/interaction?" + text + "=2');");
    ib = document.getElementById("btn_three");
    ib.setAttribute("onclick", "javascript:submitInteraction('" + webApp + "/interaction?" + text + "=3');");
    ib = document.getElementById("section_interactions");
    ib.setAttribute("display", true);    
}

function customHandlePlaylistReq(response) {
	for ( var i = 0; i < response.video.length; i++ )
		videoQueue.enqueue(response.video[i]);
	for ( var i = 0; i < response.interaction.length; i++ )
			interactionQueue.enqueue(response.interaction[i]);
	// enable playlist updates 
	playlist.ready = true;			
}

function togglePlayerControls() {
	if ( document.getElementById('cb_show_video_controls').checked ) {
		document._video.setAttribute("controls","controls");
	} else {
		document._video.removeAttribute("controls");
	}	
}

function togglePlaylist() {
	document.getElementById('section_playlist').hidden = !document.getElementById('cb_show_playlist').checked;
}
