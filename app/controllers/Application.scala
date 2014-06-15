package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.libs.json._

object Application extends Controller {
  
  def index = Action {
      Ok(views.html.application.index())
  }

    def findNearestNode(lat:Double,lng:Double) = Action{ request =>
      
      val resJsonArray:Seq[JsValue] = NodeData.findNear(lat, lng,1500).map{
        node => 
          NodeData.findById(node._1) match {
            case Some(nodeData) => Json.obj("id" -> nodeData.id,"lat" -> nodeData.lat,"lng" -> nodeData.lng, "label" -> node._2)
            case None => Json.obj("error" -> "unknown")        
          }          
      }
        Ok(Json.toJson(resJsonArray))
    }
    
    def findLines(nodeId:Long) = Action{ request =>   
      val walkId = LineData.walk.id;
      val resJsonArray:Seq[JsValue] = LineData.findByNode(nodeId).filterNot(_.id==walkId).map{
        line => line.toJson          
      }
      Ok(Json.toJson(resJsonArray))
    }
    
    def findPath(fromLat:Double,fromLng:Double,toLat:Double,toLng:Double) = Action{ request =>
      val start:Long = System.currentTimeMillis();
      val routes = RouteUtil.solveRoute(fromLat, fromLng, toLat, toLng)  
      val duration:Long = System.currentTimeMillis() - start;
      Logger.info("find path takes "+duration+" miliseconds");
      
      val resJsonArray:List[JsValue] = routes.map{_.toJson}
      Ok(Json.toJson(resJsonArray))
    }
  
}