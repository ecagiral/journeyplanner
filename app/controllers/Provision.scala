package controllers

import play.api.mvc.Controller
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Action
import models._

object Provision extends Controller {
  
    ///////////////
    //Forms
    ///////////////
  
    val addLineForm = Form(
        tuple(
            "label" -> nonEmptyText(1,40),
            "period" -> number(0,120),
            "lineType" -> number(0,7)
        )
    )
    
    val editLineForm = Form(
        tuple(
            "id" -> longNumber,
            "label" -> nonEmptyText,
            "period" -> number(0,120),
            "lineType" -> number(0,7)   
        )
    )
    
    val addEdgeForm = Form(
        tuple(
            "line" -> longNumber,
            "sourceNode" -> longNumber,
            "targetNode" -> longNumber,
            "distance" -> number(0,1000),
            "duration" -> number(0,120)
        )
    )
  
    val editEdgeForm = Form(
    tuple(
        "id" -> longNumber,
        "distance" -> number(0,1000),
        "duration" -> number(0,120)     
    ))
    
    val addNodeForm = Form(
        tuple(
            "label" -> nonEmptyText,
            "lat" -> text,
            "lng" -> text
        )verifying("Invalid location", fields => fields match { 
          case (lab, lat,lng) => (lat.toDouble < 41.619481) && (lat.toDouble > 40.738656) && (lng.toDouble > 28.072815) && (lng.toDouble < 29.899292)
        })
    )
    
    val editNodeForm = Form(
        tuple(
            "id" -> longNumber,
            "label" -> nonEmptyText,
            "lat" -> nonEmptyText,
            "lng" -> nonEmptyText
        )verifying("Invalid location", fields => fields match { 
          case (id,lab, lat,lng) => (lat.toDouble < 41.619481) && (lat.toDouble > 40.738656) && (lng.toDouble > 28.072815) && (lng.toDouble < 29.899292)
        })
    )

    ///////////////
    //Lines
    ///////////////
    
    def lines = Action {
        Ok(views.html.line(LineData.all().sortBy(_.label.toLowerCase()), addLineForm))
    }
  
    def addLine = Action { implicit request =>
        addLineForm.bindFromRequest.fold(
            errors => BadRequest(views.html.line(LineData.all().sortBy(_.label.toLowerCase()), errors)),
        {
          case(label,period,lineType)=>{              
              LineData.create(label,period,lineType)
              Redirect(routes.Provision.lines)
          }
        }
      )
    }
    
    def editLine = Action { implicit request =>
        editLineForm.bindFromRequest.fold(
            errors => {
              LineData.findById(errors.get._1).map{
                line =>
                    val form = editLineForm
                    BadRequest(views.html.linedetail(line, NodeData.all.sortBy(_.label.toLowerCase()), errors,addEdgeForm))
              }.getOrElse(Redirect(routes.Provision.lines))
              
            },
            {
              case(id,label,period,lineType)=>{              
                  LineData.findById(id).map{
                    line => 
                      line.update(label, period, lineType)
                      val form = editLineForm.fill(line.id,label,period,lineType)
                      Ok(views.html.linedetail(line,NodeData.all.sortBy(_.label.toLowerCase()),form,addEdgeForm))
                  }.getOrElse(Redirect(routes.Provision.lines))
                  
              }
            }
        )
    }
  
    def deleteLine(id: Long) = Action {
        LineData.delete(id)
        Redirect(routes.Provision.lines)
    }
  
    def lineDetail(id:Long) = Action {
        LineData.findById(id).map{
          line =>
            val form = editLineForm.fill(line.id,line.label,line.period,line.lineType)
            Ok(views.html.linedetail(line, NodeData.all.sortBy(_.label.toLowerCase()),form,addEdgeForm))          
        }.getOrElse(NotFound);    
    }
    
    ///////////////
    //Nodes
    ///////////////
    

    
    def nodes = Action {
        Ok(views.html.node(NodeData.all().sortBy(_.label.toLowerCase()), addNodeForm))
    }
    
    def nodeDetail(id:Long) = Action {
      NodeData.findById(id).map{
         node => Ok(views.html.nodedetail(node,LineData.findByNode(node.id), editNodeForm.fill(node.id,node.label,node.lat.toString,node.lng.toString)))          
      }.getOrElse(NotFound);    
    }
    
      def addNode = Action { implicit request =>
      Provision.addNodeForm.bindFromRequest.fold(
        errors => BadRequest(views.html.node(NodeData.all(), errors)),
        {
          case(label,lat,lng)=>{              
              NodeData.create(label,lat.toDouble,lng.toDouble)
              Redirect(routes.Provision.nodes)
          }
        }
      )
  }
      
    def editNode = Action { implicit request =>
      
      Provision.editNodeForm.bindFromRequest.fold(
          errors => {
                  NodeData.findById(errors.get._1.toLong).map{
                      node => BadRequest(views.html.nodedetail(node, LineData.findByNode(node.id),errors))   
                  }getOrElse(Redirect(routes.Provision.nodes));
          },
          {
            case(id,label,lat,lng)=>{ 
                  NodeData.findById(id.toLong).map{node =>                  
                        node.update(label,lat.toDouble,lng.toDouble)
                        Redirect(routes.Provision.nodeDetail(node.id))
                  }.getOrElse{Redirect(routes.Provision.nodes)}                 
            }
         }      
      )
  }
  
  def deleteNode(id: Long) = Action {
      NodeData.delete(id)
      Redirect(routes.Provision.nodes)
  }
  
  
    ////////////////
    //Edge
    ///////////////
  

    
    def addEdge = Action { implicit request =>
        addEdgeForm.bindFromRequest.fold(            
            errors => {
                LineData.findById(errors.data("line").toLong).map{
                    line => BadRequest(views.html.linedetail(line, NodeData.all,Provision.editLineForm, errors))
                }.getOrElse(NotFound)              
            },
            {
              case(line,sourceEdge,targetEdge,distance,duration)=>{ 
                  EdgeData.create(line.toLong, sourceEdge.toLong,targetEdge.toLong,distance.toInt,duration.toInt)
                  Redirect(routes.Provision.lineDetail(line.toLong))
              }
            }
       )
    }
    
  def deleteEdge(id:Long) = Action {
      EdgeData.findById(id) match {
        case Some(lineEdge) =>  EdgeData.delete(id);Redirect(routes.Provision.lineDetail(lineEdge.line))
        case None =>  Redirect(routes.Provision.lines)
      }
     
     
  }
  
  def editEdge = Action { implicit request =>
      
      editEdgeForm.bindFromRequest.fold(
          errors => {                
                  EdgeData.findById(errors.get._1.toLong).map{
                      edge => BadRequest(views.html.edgedetail(edge, LineData.findById(edge.line).get,NodeData.findById(edge.sourceNode).get,NodeData.findById(edge.targetNode).get,errors))    
                  }getOrElse(Redirect(routes.Provision.lines));
          },
          {
              case(id,distance,duration)=>{ 
                  EdgeData.findById(id.toLong).map{
                    lineEdge =>
                        lineEdge.update(distance.toInt,duration.toInt)
                        Redirect(routes.Provision.edgeDetail(lineEdge.id)) 
                  }.getOrElse{Redirect(routes.Provision.lines)}                  
              }
          }
       
      )
  }
  
     def edgeDetail(id:Long) = Action {
      EdgeData.findById(id).map{
         edge => 
            val line = LineData.findById(edge.line).get
            val source = NodeData.findById(edge.sourceNode).get
            val target = NodeData.findById(edge.targetNode).get
            val form = editEdgeForm.fill(edge.id,edge.distance,edge.duration)
            Ok(views.html.edgedetail(edge,line,source,target, form))          
      }.getOrElse(NotFound);    
  }
  

}