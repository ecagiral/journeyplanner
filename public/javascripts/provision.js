var map;
var lineSymbol = {path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW};
var nodeMarkers = [];
var edgeLines = [];
var nodeCircle;

//Responsibilities of Provisioning.js
// - Left click on empty map => Draw nodes inside the circle where center is click location
// - right click on empty map => Add new node
// - Left click on node => Draw lines passing through clicked node
// - Left click on node => Delete node

function initialize() {
	  var myLatlng = new google.maps.LatLng(41.023557,29.002018);
	  var mapOptions = {
	    zoom: 12,
	    center: myLatlng
	  }
	  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
	  google.maps.event.addListener(map, 'click', function(event){leftClickMap(event)});
	  google.maps.event.addListener(map, 'rightclick', function(event){rightClickMap(event)});
}

function leftClickMap(event){
	clearEdges();
	var circleOptions = {
  	      strokeColor: '#FF0000',
  	      strokeOpacity: 0.8,
  	      strokeWeight: 2,
  	      fillOpacity: 0,
  	      map: map,
  	      center: event.latLng,
  	      radius: 1500
  	};
	if(nodeCircle){
		nodeCircle.setMap(null);
	}	
	nodeCircle = new google.maps.Circle(circleOptions);
	google.maps.event.addListener(nodeCircle, "click", function(event){
		google.maps.event.trigger(map, 'click', event);
	});
	getNodes(event.latLng,drawNodes)	
}

function rightClickMap(event){
	var nodeLabel = prompt("Durak Ekle","Isim");
	if(nodeLabel){
		addNode(nodeLabel,event.latLng);
	}
}

function leftClickNode(nodeId){
	getLines(nodeId);
}

function rightClickNode(nodeCursor){
	var check = confirm(nodeCursor.title+" duragini sil?");
	if(check){
		deleteNode(nodeCursor.nodeId,function(){
			nodeCursor.setMap(null);
		})
	}
}

function addNode(label,latLng){
	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/nodes',
        data : {
            'label': label,'lat': latLng.lat(),'lng': latLng.lng()
        }
    }).done(function(data) {
    	 var marker = new google.maps.Marker({
		      position: new google.maps.LatLng(data.lat,data.lng),
		      map: map,
		      title: data.label,
		      icon:'http://maps.google.com/mapfiles/ms/icons/green-dot.png'
		 });
    	 nodeMarkers.push(marker);
	}).fail(function(data) {
	    alert( "add node error",data );
	});
}

function getNodes(latlng,callback){

	var jqxhr = $.ajax( {
	        type : 'GET',
	        contentType:'text/plain',
	        url : '/findNearestNode',
	        data : {
	            'lat': latlng.lat(),'lng': latlng.lng()
	        }
	  })
	  .done(function(data) {
		  callback(data);
	  })
	  .fail(function() {
	    alert( "error" );
	  });
}

function deleteNode(nodeId,callback){
	
	var jqxhr = $.ajax( {
	        type : 'GET',
	        contentType:'text/plain',
	        url : '/nodes/'+nodeId+'/delete'
	  })
	  .done(function() {
		  callback();
	  })
	  .fail(function() {
	    alert( "error delete node" );
	  });
}

function drawNodes(data){
		clearNodes();
		nodeMarkers = [];
		for (var i = 0; i < data.length; i++) {			  
		  var marker = new google.maps.Marker({
		      position: new google.maps.LatLng(data[i].lat,data[i].lng),
		      map: map,
		      title: data[i].label,
		      nodeId:data[i].id
		  });
		  google.maps.event.addListener(marker, 'click', function() {
			  leftClickNode(this.nodeId);
		  });
		  google.maps.event.addListener(marker, 'rightclick', function() {
			  rightClickNode(this);
		  });
		  nodeMarkers.push(marker);
		}
}

function getLines(nodeId){
    if(nodeCircle){
    	nodeCircle.setMap(null);
    }
	var jqxhr = $.ajax( {
	        type : 'GET',
	        contentType:'text/plain',
	        url : '/findLines',
	        data : {
	            'nodeId': nodeId
	        }
	  })
	  .done(function(data) {
		  drawEdges(data[0].edges);
	  })
	  .fail(function() {
	    alert( "error" );
	  });
}

function drawEdges(data){
	
	clearEdges();
	edgeLines = [];
	var nodes = [];
	for (var i = 0; i < data.length; i++) {
		var sourceAdded = false;
		for (var j = 0; j < nodes.length; j++) {	
			if(nodes[j].id == data[i].source.id){
				sourceAdded = true;
			}
		}

		if(!sourceAdded){
			console.log("source not added");
			nodes.push(data[i].source);
		}
		
		var targetAdded = false;
		for (var j = 0; j < nodes.length; j++) {	
			if(nodes[j].id == data[i].target.id){
				targetAdded = true;
			}
		}
		
		if(!targetAdded){
			console.log("target not added");
			nodes.push(data[i].target);
		}
		var rndLat = 0;
		if(sourceAdded && targetAdded){
			console.log("target source added");
			rndLat = 0.001;
		}
		console.log("rndlat "+rndLat);
		  var lineCoordinates = [		    
		    new google.maps.LatLng(data[i].source.lat+rndLat, data[i].source.lng+rndLat),
		    new google.maps.LatLng(data[i].target.lat+rndLat, data[i].target.lng+rndLat)
		  ];
		
		  var line = new google.maps.Polyline({			
			label:data[i].source.label+"-"+data[i].target.label,
		    path: lineCoordinates,
		    icons: [{icon: lineSymbol,offset: '100%'}],
		    map: map
		  });
		  google.maps.event.addListener(line, 'click', function() {
			  alert(this.label);
		  });
		  edgeLines.push(line);
	}
	drawNodes(nodes);
}

function clearEdges(){
	for (var i = 0; i < edgeLines.length; i++) {
		edgeLines[i].setMap(null);
	}
}
function clearNodes(){
	for (var i = 0; i < nodeMarkers.length; i++) {
		nodeMarkers[i].setMap(null);
	}
}



google.maps.event.addDomListener(window, 'load', initialize);