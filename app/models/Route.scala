package models

import play.api.libs.json._

class Route(edges: List[EdgeData], targets: List[EdgeData], walkId:Long) {

  val targetReached = if (targets.find(_.id == edges.last.id).isDefined) true else false

  def duration = edges.foldLeft((0, 0L))((time_line, edge) =>
    if (time_line._2 != edge.line) {
      //line changed
      (time_line._1 + edge.duration + LineData.findById(edge.line).map{_.period}.
          getOrElse(0), edge.line)
    } else {
      (time_line._1 + edge.duration, edge.line)
    })._1

  def lines: List[Long] = edges.foldLeft(List[Long]()) {
    (lines, edge) => if (lines.contains(edge.line) || edge.line == walkId) lines else lines :+ edge.line
  }

  def lastEdge: EdgeData = edges.last

  def getEdges = edges

  def addEdges(nextEdges: List[EdgeData]): Seq[Route] = {
    nextEdges.filter(isValidEdge(_)).map { edge => new Route(edges :+ edge, targets,walkId) }
  }

  def isValidEdge(candidate: EdgeData): Boolean = {
    //do not walk twice
    if(candidate.line == walkId && edges.last.line == walkId) 
      false
    //do not go same node
    else if(edges.map(_.sourceNode).contains(candidate.targetNode))
      false
    else{
        val lineChanged = candidate.line != edges.last.line
        //do not allow if node or line already passed
        
        edges.find(edge => edge.targetNode == candidate.targetNode || (lineChanged && edge.line == candidate.line)) match {
          case Some(edge) => false
          case None => true
        }
    }
  }

  def nextEdge(smallRoute: Route): Option[EdgeData] = {
    if (edges.startsWith(smallRoute.getEdges)) {
      Some(edges(smallRoute.getEdges.length))
    } else {
      None
    }
  }
  
  override def toString:String = {
    edges.map{
      edge => 
        val line = LineData.findById(edge.line).getOrElse(LineData.dummy(0))
        val sourceNode=NodeData.findById(edge.sourceNode).getOrElse(NodeData.dummy(0))
        val targetNode = NodeData.findById(edge.targetNode).getOrElse(NodeData.dummy(0))
        sourceNode.label+" "+line.label+" "+targetNode.label
    }.mkString("","-->"," "+duration+" mins")
  }
  
  def toJson = {
       val jsonRouteSeq:Seq[JsObject] = edges.map{edge =>
           val sourceNode:NodeData = NodeData.findById(edge.sourceNode).get
           val sourceJson = Json.obj("id" -> sourceNode.id,"lat" -> sourceNode.lat,"lng" -> sourceNode.lng, "label" -> sourceNode.label)
           val targetNode:NodeData = NodeData.findById(edge.targetNode).get
           val targetJson = Json.obj("id" -> targetNode.id,"lat" -> targetNode.lat,"lng" -> targetNode.lng, "label" -> targetNode.label)
           val line:LineData = LineData.findById(edge.line).get
           val lineJson = Json.obj("label" -> line.label)
           Json.obj("source" -> sourceJson,"target" -> targetJson, "line" -> lineJson)
       }
       val finalJson = Json.obj("edges"->jsonRouteSeq,"duration"->duration)
       Json.toJson(finalJson)
  }
}