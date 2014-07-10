package controllers

import play.api.mvc.Controller
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Action
import models._
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.Logger
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.mvc.Security
import play.api.libs.json.Json
import play.api.libs.json.JsNull
import play.api.libs.ws.Response
import play.api.libs.json.JsNumber

object Provision extends Controller with Secured{
  
    // -- Authentication

  val loginForm = Form(
    single(
      "password" -> text
    ) verifying ("Invalid password", result => result match {
      case (password) => password == Config.getAdminPassword
    })
  )
  
  def isAjax[A](implicit request : Request[A]) = {
      request.headers.get("X-Requested-With") == Some("XMLHttpRequest")
  }

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.provision.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.provision.login(formWithErrors)),
      user => Redirect(routes.Provision.lines).withSession("admin" -> "true")
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }
  
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
    //Index
    ///////////////
    
    def index = withAuth { username => implicit request =>
        Ok(views.html.provision.index(LineData.all().sortBy(_.label.toLowerCase())))
    }

    ///////////////
    //Lines
    ///////////////
    
    def lines = withAuth { username => implicit request =>
        Ok(views.html.provision.line(LineData.all().sortBy(_.label.toLowerCase()), addLineForm))
    }
  
    def addLine = withAuth { username => implicit request =>
        addLineForm.bindFromRequest.fold(
            errors => {
              if(isAjax(request)){
                  BadRequest("Bad Request: " + errors);
              }else{
                  BadRequest(views.html.provision.line(LineData.all().sortBy(_.label.toLowerCase()), errors))
              }
            },
        {
          case(label,period,lineType)=>{              
              LineData.create(label,period,lineType)
              if(isAjax(request)){
                  Ok(label);
              }else{
                  Redirect(routes.Provision.lines) 
              }
              
          }
        }
      )
    }
    
    def editLine = withAuth { username => implicit request =>
        editLineForm.bindFromRequest.fold(
            errors => {
              LineData.findById(errors.get._1).map{
                line =>
                    val form = editLineForm
                    BadRequest(views.html.provision.linedetail(line, NodeData.all.sortBy(_.label.toLowerCase()), errors,addEdgeForm))
              }.getOrElse(Redirect(routes.Provision.lines))
              
            },
            {
              case(id,label,period,lineType)=>{              
                  LineData.findById(id).map{
                    line => 
                      line.update(label, period, lineType)
                      val form = editLineForm.fill(line.id,label,period,lineType)
                      Ok(views.html.provision.linedetail(line,NodeData.all.sortBy(_.label.toLowerCase()),form,addEdgeForm))
                  }.getOrElse(Redirect(routes.Provision.lines))
                  
              }
            }
        )
    }
  
    def deleteLine(id: Long) = withAuth { username => implicit request =>
        LineData.delete(id)
        Redirect(routes.Provision.lines)
    }
  
    def lineDetail(id:Long) = withAuth { username => implicit request =>
      
        LineData.findById(id).map{
          line =>
            if(isAjax(request)){
                Ok(line.toJson)
            }else{
                val form = editLineForm.fill(line.id,line.label,line.period,line.lineType)
                Ok(views.html.provision.linedetail(line, NodeData.all.sortBy(_.label.toLowerCase()),form,addEdgeForm)) 
            }
        }.getOrElse(NotFound);    
    }
    
    ///////////////
    //Nodes
    ///////////////
    

    
    def nodes = withAuth { username => implicit request =>
        Ok(views.html.provision.node(NodeData.all().sortBy(_.label.toLowerCase()), addNodeForm))       
    }
    
    def nodeDetail(id:Long) = withAuth { username => implicit request =>
      NodeData.findById(id).map{
         node => Ok(views.html.provision.nodedetail(node,LineData.findByNode(node.id), editNodeForm.fill(node.id,node.label,node.lat.toString,node.lng.toString)))          
      }.getOrElse(NotFound);    
    }
    
      def addNode = withAuth { username => implicit request =>

      Provision.addNodeForm.bindFromRequest.fold(
        {
            errors => 
              if(isAjax(request)){
                  BadRequest("Bad Request: " + errors);
              }else{
                  Redirect(routes.Provision.nodes)
              }
              BadRequest(views.html.provision.node(NodeData.all(), errors))
        },
        {
          case(label,lat,lng)=>{ 
              val id:Long = NodeData.create(label,lat.toDouble,lng.toDouble).getOrElse(0)
              if(isAjax(request)){
                  NodeData.findById(id) match {
                    case Some(node) => Ok(node.toJson)
                    case None => BadRequest("Unable to add");
                  }                                                       
              }else{
                  Redirect(routes.Provision.nodes)
              }
          }
        }
      )
  }
      
    def editNode = withAuth { username => implicit request =>
      
      Provision.editNodeForm.bindFromRequest.fold(
          errors => {
                  NodeData.findById(errors.get._1.toLong).map{
                      node => BadRequest(views.html.provision.nodedetail(node, LineData.findByNode(node.id),errors))   
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
  
  def deleteNode(id: Long) = withAuth { username => implicit request =>
      NodeData.delete(id)
      if(isAjax(request)){
          Ok("");
      }else{
          Redirect(routes.Provision.nodes)
      }
  }
  
  
    ////////////////
    //Edge
    ///////////////
  

    
    def addEdge = withAuth { username => implicit request =>
        addEdgeForm.bindFromRequest.fold(            
            errors => {
              if(isAjax(request)){
                  Logger.error("hata"+errors.toString())
                  BadRequest("Bad Request: " + errors)
              }else{
                LineData.findById(errors.data("line").toLong).map{
                    line => BadRequest(views.html.provision.linedetail(line, NodeData.all,Provision.editLineForm, errors))
                }.getOrElse(NotFound)    
              }
            },
            {
              case(line,sourceEdge,targetEdge,distance,duration)=>{ 
                  EdgeData.create(line.toLong, sourceEdge.toLong,targetEdge.toLong,distance.toInt,duration.toInt)
                  if(isAjax(request)){
                    LineData.findById(line.toLong).map{
                        lineData =>  Ok(lineData.label);
                    }.getOrElse(BadRequest("Line not found"))  
                     
                  }else{
                      Redirect(routes.Provision.lineDetail(line.toLong))
                  }
              }
            }
       )
    }
    
  def deleteEdge(id:Long)= withAuth { username => implicit request =>
      EdgeData.findById(id) match {
        case Some(lineEdge) =>  {
          EdgeData.delete(id);
          if(isAjax(request)){
            Ok("")
          }else{
            Redirect(routes.Provision.lineDetail(lineEdge.line))    
          }
             
        }
        case None =>  {
           if(isAjax(request)){
            BadRequest("edge not found")
          }else{
              Redirect(routes.Provision.lines)
          }
        }
      }
     
     
  }
  
  def editEdge = withAuth { username => implicit request =>
      
      editEdgeForm.bindFromRequest.fold(
          errors => {                
                  EdgeData.findById(errors.get._1.toLong).map{
                      edge => BadRequest(views.html.provision.edgedetail(edge, LineData.findById(edge.line).get,NodeData.findById(edge.sourceNode).get,NodeData.findById(edge.targetNode).get,errors))    
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
  
     def edgeDetail(id:Long) = withAuth { username => implicit request =>
      EdgeData.findById(id).map{
         edge => 
            val line = LineData.findById(edge.line).get
            val source = NodeData.findById(edge.sourceNode).get
            val target = NodeData.findById(edge.targetNode).get
            val form = editEdgeForm.fill(edge.id,edge.distance,edge.duration)
            Ok(views.html.provision.edgedetail(edge,line,source,target, form))          
      }.getOrElse(NotFound);    
  }
  
     def dummyResponse(id:Long) = Action{ implicit request =>
       val res = Json.obj("id" -> id,"name" -> "dummy","price" -> 1234567890,"image"-> "http://www.ofix.com/UserFiles/Document/S12828_8b9_c60.jpg","url"->"http://www.ofix.com/Urunler/020922/Scrikss-F-108-Tukenmez-Kalem/S12828")
       Ok(Json.toJson(res))       
     }
}

trait Secured {
  
  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("admin")
  
  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = {
    Logger.info("unauth");
    Results.Redirect(routes.Provision.login)
  }
}