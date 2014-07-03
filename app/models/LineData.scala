package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.Logger
import scala.util.Random
import play.api.Application
import play.Application
import play.Play
import play.Play
import play.Application
import play.api.GlobalSettings
import models.AnormImplicits.RichSQL
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class LineData(id:Long,label:String,period:Int,lineType:Int){
    //line types
    // 0: walk
    // 1: bus
    // 2: subway
    // 3: ferry
    // 4: metrobus
    // 5: tram
    // 6: minibus
    // 7: train
    def getNodes: List[NodeData] = {
        EdgeData.getNodes(id) 
    }
    
    def getEdgeData: List[EdgeData] = {
        EdgeData.findByLine(id) 
    }
    
    def update(label: String,period:Int,lineType:Int) {
        DB.withConnection { implicit c =>
            SQL("update lines set (label,period,lineType) = ({label},{period},{lineType}) where id = {id}").on(
                'id -> id,
                'label -> label,
                'period -> lineType,
                'lineType -> lineType
            ).executeUpdate()
        }
    }
    
    def toJson: JsValue = {
       val edges:Seq[JsValue] = getEdgeData.map{_.toJson}
       Json.obj("edges" -> edges,"label" -> label,"id"-> id)
    }

}

object LineData {
  
    def walk(): LineData = DB.withConnection { implicit c =>
        SQL("select * from lines where label = 'walk' limit 1").as(line.single)
    }
  
    def all(): List[LineData] = DB.withConnection { implicit c =>
        SQL("select * from lines").as(line *)
    }
    
    def create(label: String,period:Int,lineType:Int):Option[Long]= {
        DB.withConnection { implicit c =>
            SQL("insert into lines (label,period,lineType) values ({label},{period},{lineType})").on(
                    'label -> label,
                    'period -> period,
                    'lineType -> lineType
            ).executeInsert()
        }
    }
    
    def dummy(id:Long):LineData = {
      //val id = Random.nextInt(Integer.MAX_VALUE)
      val period = Random.nextInt;
      new LineData(id,"line"+id,period,0)
    }

    def delete(id: Long) {
        DB.withConnection { implicit c =>
            SQL("delete from lines where id = {id}").on(
                'id -> id
            ).executeUpdate()
        }
    }
    
    val line = {
        get[Long]("id") ~ 
        get[String]("label") ~ 
        get[Int]("period") ~ 
        get[Int]("lineType") map {
            case id~label~period~lineType => LineData(id, label,period,lineType)
        }
    }
    
    
    def findById(id: Long): Option[LineData] = {
        DB.withConnection { implicit connection =>
            SQL("select * from lines where id = {id}").on(
                'id -> id
            ).as(line.singleOpt)
        } 
    }
    
    def findByIds(lines: List[Long]):List[LineData] = {
        if(lines.size==0)List[LineData]()
        else{
            DB.withConnection { implicit connection =>
               val sqlText = RichSQL(""" SELECT * FROM lines WHERE id IN ({lineIds}) """).onList("lineIds" -> lines).toSQL
               sqlText.as(line *)(connection)
            }
        }
    }
    
    def findByLabel(label: String): Option[LineData] = {
        DB.withConnection { implicit connection =>
            SQL("select * from lines where label = {label}").on(
                'label -> label
            ).as(line.singleOpt)
        } 
    }  

    def findByNode(nodeId:Long):List[LineData] = DB.withConnection{ implicit c =>
        SQL("select distinct li.* from edges le join lines li on li.id = le.line where (le.sourceNode = {node} or le.targetNode = {node})").on(
           'node -> nodeId
        ).as(line *)     
    }
    
    val intersect = {
        get[Long]("fromline") ~ 
        get[Long]("toline") map {
            case fromline~toline => (fromline,toline)
        }
    }
        
    def getIntersectForw:List[(Long,Long)] = DB.withConnection{ implicit c =>
        SQL("select edgea.line as fromline,edgeb.line as toline from edges edgea join edges edgeb on (edgea.targetnode = edgeb.sourcenode and edgea.line != edgeb.line) where edgea.line != {walkId} and edgeb.line != {walkId}")
        .on('walkId -> LineData.walk.id).as(intersect *)
    }
    
    def getIntersectWalkForw:List[(Long,Long)] = DB.withConnection{ implicit c =>
        SQL("select edgea.line as fromline,edgec.line as toline from edges edgea join edges edgeb on (edgea.targetnode=edgeb.sourcenode and edgeb.line = {walkId}) join edges edgec on (edgeb.targetnode = edgec.sourcenode and edgea.line != edgec.line and edgec.line != {walkId}) where edgea.line != {walkId}")
        .on('walkId -> LineData.walk.id).as(intersect *)
    }
    
    def getIntersectBack:List[(Long,Long)] = DB.withConnection{ implicit c =>
        SQL("select edgea.line as fromline,edgeb.line as toline from edges edgea join edges edgeb on (edgea.sourcenode = edgeb.targetnode and edgea.line != edgeb.line) where edgea.line != {walkId} and edgeb.line != {walkId}")
        .on('walkId -> LineData.walk.id).as(intersect *)
    }
    
    def getIntersectWalkBack:List[(Long,Long)] = DB.withConnection{ implicit c =>
        SQL("select edgea.line as fromline,edgec.line as toline from edges edgea join edges edgeb on (edgea.sourcenode=edgeb.targetnode and edgeb.line = {walkId})  join edges edgec on (edgeb.sourcenode = edgec.targetnode and edgea.line != edgec.line and edgec.line != {walkId}) where edgea.line != {walkId} ")
        .on('walkId -> LineData.walk.id).as(intersect *)
    }

}