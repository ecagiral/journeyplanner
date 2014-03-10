package models

import scala.collection.mutable.ArrayBuffer
import play.api.Logger
import scala.annotation.tailrec

object RouteUtil {
    
  def solveRoute(startLat:Double,startLng:Double,endLat:Double,endLng:Double):List[Route] = {
    val startEdges = EdgeData.findNearStartEdges(startLat, startLng)
    val endEdges = EdgeData.findNearEndEdges(endLat, endLng)
    val crossData = LineCrossData.getCrossData
    val availableLines = LineCrossData.getAvailableLines(startEdges.map(_._1), endEdges.map(_._1),crossData)
    val availableEdges = EdgeData.findByLines(availableLines)
    val initialRoutes = startEdges.map(edge => new Route(List(edge._1),endEdges.map(_._1),LineData.walk.id))
    iterateRoute(availableEdges,initialRoutes)
  }
  
  def iterateRoute(edges:List[EdgeData],routes:List[Route]):List[Route] = {

       val newRoutes:List[Route] = routes.flatMap{ 
         oldRoute =>
           if(oldRoute.targetReached) {
             Seq(oldRoute) 
           }
           else{
               val candidates:List[EdgeData] = edges.filter(edge => edge.sourceNode == oldRoute.lastEdge.targetNode) 
               oldRoute.addEdges(candidates)
           }
       }

       if(newRoutes.size == 0){ 
         newRoutes
       }
       else if(newRoutes.filter(_.targetReached).size == newRoutes.size) {
         newRoutes
       }else{
         iterateRoute(edges,newRoutes)
       }
       
  }
    

}