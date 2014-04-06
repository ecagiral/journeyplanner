package controllers

import play.api.data.Forms._
import play.api.data.Form
import play.api.mvc._
import models._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.matching.Regex
import play.api.Logger

object Import extends Controller{
    val lineParserForm = Form(
        tuple(
            "label" -> nonEmptyText,
            "url" -> text
        )
    )
    
    def parseLinePage() = Provision.withAuth { username => implicit request =>
      Ok(views.html.parseline(lineParserForm,List[NodeData]()))
    }
  
  def parseLineData() = Provision.withAuth { username => implicit request =>
      lineParserForm.bindFromRequest.fold(
        errors => BadRequest(views.html.parseline(errors,List[NodeData]())),
        {
          case(lineName,url)=>{ 
              
              //NodeData.create(label,lat.toDouble,lng.toDouble)
                 Async{
                    WS.url(url).get().map { response =>
                      val line:LineData = LineData.findByLabel(lineName).getOrElse{
                          val lineId:Long = LineData.create(lineName, 20,1).getOrElse(0)
                          new LineData(lineId,lineName,20, 1)
                      }                      
                      if(line.id == 0){
                        Ok(views.html.parseline(lineParserForm,List[NodeData]()))
                      }
                      val pattern = new Regex("<td>(.*?)</td>")
                      val markers:String= response.body.split("addMarker").drop(1).mkString("")
                      val forwards:List[String] = markers.split("forwardStops-istanbul").drop(1).map{
                        text => text.substring(text.indexOf(",")+1, text.indexOf("'<div"));
                      }.toList
                      var previousNode:Option[NodeData] = None
                      val forwardNodes:List[NodeData] = forwards.flatMap{
                        text => 
                          val labLatLng = text.split(",") 
                          val label = labLatLng(0).trim.stripPrefix("'").stripSuffix("'")
                          
                          NodeData.create(label, labLatLng(1).toDouble, labLatLng(2).toDouble) match {
                              case Some(id) => {
                               previousNode match {
                                   case None => ()
                                   case Some(previous) => {
                                       if(!EdgeData.hasEdge(line.id, previous.id,id)){
                                           EdgeData.create(line.id, previous.id,id,0,5)
                                       }                             
                                   }
                               }
                               val node = NodeData.findById(id)
                               previousNode = node
                               node
                              }
                              case None => None
                          }
                          
                      }
                      previousNode = None
                      val backwards:List[String] = markers.split("backwardStops-istanbul").drop(1).map{
                        text =>                         
                          text.substring(text.indexOf(",")+1, text.indexOf("'<div"));
                      }.toList
                      val backwardNodes:List[NodeData] = backwards.flatMap{
                        text => 

                          val labLatLng = text.split(",") 
                          val label = labLatLng(0).trim.stripPrefix("'").stripSuffix("'")

                          NodeData.create(label, labLatLng(1).toDouble, labLatLng(2).toDouble) match {
                              case Some(id) => {
                               previousNode match {
                                   case None => ()
                                   case Some(previous) => {
                                       if(!EdgeData.hasEdge(line.id, previous.id,id)){
                                           EdgeData.create(line.id, previous.id,id,0,5)
                                       }                             
                                   }
                               }
                               val node = NodeData.findById(id)
                               previousNode = node
                               node
                              }
                              case None => None
                          }
                      }
                      Logger.info(LineCrossData.toString)
                      Ok(views.html.parseline(lineParserForm,forwardNodes:::backwardNodes))
                    }
                    
               }
          }
        }
        
      )
      
  }
}