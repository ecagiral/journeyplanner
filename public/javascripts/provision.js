var map;
var lineSymbol = {path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW};
var nodeMarkers = {};
var edgeLines = [];
var startNode;
var endNode;
var tmpLine;
var mapCenter;
var mapStatus = "nodes";

//Responsibilities of Provisioning.js
// - Left click on empty map => Draw nodes inside the circle where center is click location
// - Right click on empty map => Add new node
// - Left click on nodeA => List lines passing through nodeA
// - Right click on node => Delete node

function initialize() {
	  var myLatlng = new google.maps.LatLng(41.023557,29.002018);
	  var mapOptions = {
	    zoom: 12,
	    center: myLatlng
	  }
	  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
	  google.maps.event.addListener(map, 'click', function(event){leftClickMap(event)});
	  google.maps.event.addListener(map, 'rightclick', function(event){rightClickMap(event)});
	  google.maps.event.addListener(map, 'idle', function(){idleMap(event)});
	  mapCenter = map.getCenter();
	  getNodes(mapCenter,drawNodes);	
}

function idleMap(){
	if(mapStatus == "nodes"){
		if(mapCenter.lat() != map.getCenter().lat() || mapCenter.lng() != map.getCenter().lng() ){
			mapCenter = map.getCenter();
			clearEdges();
			getNodes(mapCenter,drawNodes)	
		}
	}
}

function rightClickMap(event){
	var latSh = String(event.latLng.lat().toFixed(3));
	var lngSh = String(event.latLng.lng().toFixed(3));
	var nodeLabel = prompt("Durak Ekle",latSh+"-"+lngSh);
	if(nodeLabel){
		addNode(nodeLabel,event.latLng);
	}
}

function leftClickNode(marker){
	if(startNode){
		if(startNode.nodeId == marker.nodeId){
			startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
			nodeMarkers[startNode.nodeId] = startNode;
			startNode = null;
			if(tmpLine){
				tmpLine.setMap(null);
			}			
		}else{
			if(endNode){
				if(endNode.nodeId == marker.nodeId){
					endNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
					nodeMarkers[endNode.nodeId] = endNode;
					endNode = null;
					if(tmpLine){
						tmpLine.setMap(null);
					}				
				}else{
					endNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
					nodeMarkers[endNode.nodeId] = endNode;
					endNode = null;
					startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
					nodeMarkers[startNode.nodeId] = startNode;
					startNode = marker;
					startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
					if(tmpLine){
						tmpLine.setMap(null);
					}
					delete nodeMarkers[marker.nodeId];
				}
			}else{
				endNode = marker;
				endNode.setIcon('http://maps.google.com/mapfiles/ms/icons/blue-dot.png');	     		
				tmpLine = new google.maps.Polyline({
					label:"ehe",
					strokeWeight:5,
	     		    path: [startNode.getPosition(),endNode.getPosition()],
	     		    icons: [{icon: lineSymbol,offset: '50%'}],
	     		    map: map
	     		});
				delete nodeMarkers[marker.nodeId];
			}
		}

	}else{

		if(endNode){
			if(endNode.nodeId == marker.nodeId){
				endNode = null;
				startNode = marker;
				startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
				if(tmpLine){
					tmpLine.setMap(null);
				}
			}else{
				startNode = marker;
				startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
				if(tmpLine){
					tmpLine.setMap(null);
				}
				delete nodeMarkers[marker.nodeId];		
				tmpLine = new google.maps.Polyline({
					label:"ehe",
					strokeWeight:5,
	     		    path: [startNode.getPosition(),endNode.getPosition()],
	     		    icons: [{icon: lineSymbol,offset: '50%'}],
	     		    map: map
	     		});
				
			}


		}else{
			startNode = marker;
			startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
			if(tmpLine){
				tmpLine.setMap(null);
			}
			delete nodeMarkers[marker.nodeId];			
		}
		
	}
	getLines(startNode,endNode);
}

function rightClickNode(nodeCursor){
	var check = confirm(nodeCursor.title+" duragini sil?");
	if(check){
		deleteNode(nodeCursor.nodeId,function(){
			nodeCursor.setMap(null);
		})
	}
}

function leftClickLine(lineElem){

	var lineId = lineElem.data("line");
	if(lineElem.hasClass("selectedItem")){
		lineElem.removeClass("selectedItem");
		$("#editLineContainer").hide();
		$("#addLineContainer").show();
		mapStatus = "nodes";
		  mapCenter = map.getCenter();
		  clearEdges();
		  getNodes(mapCenter,drawNodes);	
		return;
	}
	lineElem.siblings().removeClass("selectedItem");
	getLine(lineId,function(line){
		  lineElem.addClass("selectedItem");
		  $("#editLineForm input[name='id']").val(line.id);
		  $("#editLineForm input[name='label']").val(line.label);
		  $("#editLineForm input[name='period']").val(line.period);
		  $("#editLineForm select[name='lineType']").val(line.type);
		  $("#editLineForm input[name='delete']").data("line",line.id);
		  $("#addLineContainer").hide();
		  $("#editLineContainer").show();
		  if(line.edges.length==0){
			  alert("no edge found");
			  return;
		  }
		  mapStatus = "line";
		  drawEdges(line.edges);
	})
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
		      nodeId:data.id,
		      icon:'http://maps.google.com/mapfiles/ms/icons/red-dot.png'
		 });
		 google.maps.event.addListener(marker, 'click', function() {
		 	leftClickNode(this);
		 });
		 google.maps.event.addListener(marker, 'rightclick', function() {
			rightClickNode(this);
		 });
		 nodeMarkers[data.id] = marker;
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

function drawNodes(data,fitBound){

		clearNodes();
		
		var bounds = new google.maps.LatLngBounds();		
		for (var i = 0; i < data.length; i++) {	
			if(startNode && data[i].id == startNode.nodeId){
				bounds.extend(startNode.getPosition());
				continue;
			}
			if(endNode && data[i].id == endNode.nodeId){
				bounds.extend(endNode.getPosition());
				continue;
			}
			var marker = new google.maps.Marker({
				position: new google.maps.LatLng(data[i].lat,data[i].lng),
				map: map,
				title: data[i].label,
				nodeId:data[i].id,
				icon:'http://maps.google.com/mapfiles/ms/icons/red-dot.png'
			});
			bounds.extend(marker.getPosition());
			google.maps.event.addListener(marker, 'click', function() {
				leftClickNode(this);
			});
			google.maps.event.addListener(marker, 'rightclick', function() {
				rightClickNode(this);
			});
			nodeMarkers[marker.nodeId] = marker;
		}
		if(fitBound){
			map.fitBounds(bounds);
		}
}

function getLine(lineId,callback){

	  var jqxhr = $.ajax( {
	        type : 'GET',
	        contentType:'text/plain',
	        url : '/linedetail/'+lineId
	  })
	  .done(function(data) {
		  callback(data);
	  })
	  .fail(function() {
	    alert( "error" );
	  });	
}

function getLines(nodeA,nodeB){

	if(nodeA){
		if(nodeB){
		  var jqxhr = $.ajax( {
		        type : 'GET',
		        contentType:'text/plain',
		        url : '/findLines',
		        data : {
		            'nodeA': nodeA.nodeId,
		            'nodeB': nodeB.nodeId,
		        }
		  })
		  .done(function(data) {
			  listEdgeLines(data,nodeA,nodeB);
		  })
		  .fail(function() {
		    alert( "error" );
		  });
		}else{
		  var jqxhr = $.ajax( {
		        type : 'GET',
		        contentType:'text/plain',
		        url : '/findLines',
		        data : {
		        	'nodeA': nodeA.nodeId
		        }
		  })
		  .done(function(data) {
			  listNodeLines(data,nodeA);
		  })
		  .fail(function() {
		    alert( "error" );
		  });
		}
	}else{
		if(nodeB){
		  var jqxhr = $.ajax( {
		        type : 'GET',
		        contentType:'text/plain',
		        url : '/findLines',
		        data : {
		        	'nodeA': nodeB.nodeId
		        }
		  })
		  .done(function(data) {
			  listNodeLines(data,nodeB);
		  })
		  .fail(function() {
		    alert( "error" );
		  });
		}else{
			listAllLines();
		}
	}


}

function drawEdges(data){
	if(startNode){
		startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');	
		nodeMarkers[startNode.nodeId] = startNode;
		startNode = null;
	}
	if(endNode){
		endNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
		nodeMarkers[endNode.nodeId] = endNode;
		endNode = null;
	}

	if(tmpLine){
		tmpLine.setMap(null);
	}	
	clearEdges();
	var nodes = [];
	for (var i = 0; i < data.length; i++) {
		var sourceAdded = false;
		for (var j = 0; j < nodes.length; j++) {	
			if(nodes[j].id == data[i].source.id){
				sourceAdded = true;
			}
		}

		if(!sourceAdded){
			nodes.push(data[i].source);
		}
		
		var targetAdded = false;
		for (var j = 0; j < nodes.length; j++) {	
			if(nodes[j].id == data[i].target.id){
				targetAdded = true;
			}
		}
		
		if(!targetAdded){
			nodes.push(data[i].target);
		}
		var rndLat = 0;
		if(sourceAdded && targetAdded){
			rndLat = 0.001;
		}
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
	drawNodes(nodes,true);
}

function edgeAdded(){
	startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/red-dot.png');
	nodeMarkers[startNode.nodeId] = startNode;
	startNode = endNode;
	startNode.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
	endNode = null;
	if(tmpLine){
		tmpLine.setMap(null);
	}	
	  var jqxhr = $.ajax( {
	        type : 'GET',
	        contentType:'text/plain',
	        url : '/findLines',
	        data : {
	        	'nodeA': startNode.nodeId
	        }
	  })
	  .done(function(data) {
		  listNodeLines(data,startNode);
	  })
	  .fail(function() {
	    alert( "error" );
	  });
}

function listAllLines(){
	$('#nodeLineList').hide();
	$('#edgeLineList').hide();
	$('#allLineList').show();	
}

function listNodeLines(lines,node){
	$('#allLineList').hide();
	$('#edgeLineList').hide();
	$('#nodeLineList span').text(node.title);
	$('#nodeLineList ul li').remove();
	for (var i = 0; i < lines.length; i++) {
		$('#nodeLineList ul').append('<li style="cursor:pointer" data-line="'+lines[i].id+'"><span onclick="leftClickLine($(this).parent())">'+lines[i].label+'</span></li>')
	}
	$('#nodeLineList').show();	
}

function listEdgeLines(edges,nodeA,nodeB){
	$('#nodeLineList').hide();
	$('#allLineList').hide();
	var headerText = nodeA.title+" to "+nodeB.title;
	$('#edgeLineList span').text(headerText);
	$("#edgeLineList input[name='sourceNode']").val(nodeA.nodeId);
	$("#edgeLineList input[name='targetNode']").val(nodeB.nodeId);
	$('#edgeLineList ul li').remove();
	for (var i = 0; i < edges.length; i++) {
		$('#edgeLineList ul').append('<li>'+edges[i].lineLabel+'<a data-edge="'+edges[i].id+'" href="javascript:void(0)" onclick ="deleteEdge(this)">X</a></li>')
	}
	$('#edgeLineList').show();	
}

function addLine(data){

	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/lines',
        data : data
    }).done(function(data) {
    	$('#allLineList ul').append('<li>'+data+'</li>')
	}).fail(function(data) {
	    alert( "add node error",data );
	});
}

function editLine(data){

	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/editLine',
        data : data
    }).done(function(data) {
    	console.log(data);
    	$("#allLineList ul").find("[data-line='" + data.id + "'] > span").html(data.label);
	}).fail(function(data) {
	    alert( "add node error",data );
	});
}

function deleteLine(lineElem){
	var lineId = $(lineElem).data("line");
	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/lines/'+lineId+'/delete',
    }).done(function(data) {
		$("#editLineContainer").hide();
		$("#addLineContainer").show();
		$("#allLineList ul").find("[data-line='" + lineId + "']").remove();
	}).fail(function(data) {
	    alert( "delete line error",data );
	});
}


function addEdge(data){
	console.log(data);
	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/addEdge',
        data : data
    }).done(function(data) {
    	edgeAdded();
	}).fail(function(data) {
	    alert( "add node error",data );
	});
}

function deleteEdge(edgeElem){
	var edgeId = $(edgeElem).data("edge");
	var jqxhr = $.ajax( {
        type : 'POST',
        url : '/deleteEdge/'+edgeId+'/delete',
    }).done(function(data) {
    	$(edgeElem).parent().remove();
	}).fail(function(data) {
	    alert( "delete edge error",data );
	});
}

function clearEdges(){
	for (var i = 0; i < edgeLines.length; i++) {
		edgeLines[i].setMap(null);
	}
	edgeLines = [];
}
function clearNodes(){
	for (var m in nodeMarkers) {
		nodeMarkers[m].setMap(null);
	}
	nodeMarkers = {};
}

$.fn.serializeObject = function()
{
   var o = {};
   var a = this.serializeArray();
   $.each(a, function() {
       if (o[this.name]) {
           if (!o[this.name].push) {
               o[this.name] = [o[this.name]];
           }
           o[this.name].push(this.value || '');
       } else {
           o[this.name] = this.value || '';
       }
   });
   return o;
};

google.maps.event.addDomListener(window, 'load', initialize);