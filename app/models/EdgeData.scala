package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.Logger
import scala.util.Random
import AnormImplicits._

case class EdgeData(id:Long,line:Long,sourceNode:Long,targetNode:Long,distance:Integer,duration:Integer){
    
  def update(newDistance:Integer,newDuration:Integer){

        DB.withConnection { implicit c =>
            SQL("update edges set (distance,duration) = ({distance},{duration}) where id = {id}").on(
                'id -> id,
                'distance -> newDistance,
                'duration -> newDuration
            ).executeUpdate()
        }
        
    }
}

object EdgeData {

    def all(): List[EdgeData] = DB.withConnection { implicit c =>
        SQL("select * from edges").as(edge *)
    }
    
    def findById(id: Long): Option[EdgeData] = {
        DB.withConnection { implicit connection =>
            SQL("select * from edges where id = {id}").on(
                'id -> id
            ).as(edge.singleOpt)
        } 
    }
       
    def findByLines(lines: List[Long]):List[EdgeData] = {
        if(lines.size==0)List[EdgeData]()
        else{
            DB.withConnection { implicit connection =>
               val sqlText = RichSQL(""" SELECT * FROM edges WHERE line IN ({lineIds}) """).onList("lineIds" -> lines).toSQL
               sqlText.as(edge *)(connection)
            }
        }
    }
    def getNodes(line_id:Long): List[NodeData] = DB.withConnection { implicit c =>
        SQL("select distinct(e.*) from edges le join nodes e on e.id = le.sourceNode where le.line = {line}").on(
            'line->line_id
            ).as(NodeData.node *)
    }
    
    def findByLine(line_id:Long): List[EdgeData] = DB.withConnection { implicit c =>
        SQL("select * from edges where line = {line}").on(
            'line->line_id
            ).as(edge *)
    }
    
    def hasEdge(line:Long,sourceNode:Long,targetNode:Long): Boolean = DB.withConnection { implicit c =>
        SQL("select count(*)=1 from edges  where line = {line} and sourceNode = {sourceNode} and targetNode = {targetNode} ").on(
            'line->line,
            'sourceNode->sourceNode,
            'targetNode->targetNode
            ).as(scalar[Boolean].single)
    }

    def delete(id:Long) {
        DB.withConnection { implicit c =>
            SQL("delete from edges where id = {id}").on(
                'id -> id
            ).executeUpdate()
            
        }
    }
   
    
    def create(line:Long,sourceNode:Long,targetNode:Long, distance:Integer, duration:Integer):Option[Long] = {
        DB.withConnection { implicit c =>
            SQL("insert into edges (line,sourceNode,targetNode,distance,duration) values ({line},{sourceNode},{targetNode},{distance},{duration})").on(
                    'line -> line,
                    'sourceNode -> sourceNode,
                    'targetNode -> targetNode,
                    'distance -> distance,
                    'duration -> duration
            ).executeInsert()
        }
    }
    
    def getWalkDistance(from:NodeData,to:NodeData):Option[Int] = {
        DB.withConnection { implicit c =>
            SQL("select el.distance from edges el join lines li on li.id = el.line where el.sourceNode = {sourceNode} and el.targetNode = {targetNode} and li.linetype = 0 order by el.distance asc limit 1").on(
                    'sourceNode -> from.id,
                    'targetNode -> to.id
            ).as(scalar[Int].singleOpt)
        }
    }
    
//    def findNearStartEdges(lat:Double,lng:Double):List[(EdgeData,Int)]={
//      DB.withConnection { implicit c =>
//        val edge_tDistance = "select edge.*, 1000*sqrt(pow({lat}-node.lat,2)+pow({lng}-node.lng,2)) as tDistance " +
//                "from edges edge join nodes node on edge.sourceNode = node.id " +
//                "where node.lat < ({lat}+0.005) and node.lat > ({lat}-0.005) and node.lng < ({lng}+0.005) and node.lng > ({lng}-0.005) and edge.line != {walk}";
//        val edge_rank = "select *,rank() OVER (PARTITION BY line ORDER BY tDistance,id DESC) from ("+edge_tDistance+") as subq"
//        SQL("select * from ("+edge_rank+") as subq2 where rank = 1").on(
//            'lat -> lat,
//            'lng -> lng,
//            'walk -> LineData.walk.id
//        ).as(edgeDistance *)
//      }
//    }
    
    def findNearStartEdges(lat:Double,lng:Double):List[(EdgeData,Int)]={
      DB.withConnection { implicit c =>
        SQL("select edge.*, 1000*sqrt(pow({lat}-node.lat,2)+pow({lng}-node.lng,2)) as tDistance " +
                "from edges edge join nodes node on edge.sourceNode = node.id " +
                "where node.lat < ({lat}+0.005) and node.lat > ({lat}-0.005) and node.lng < ({lng}+0.005) and node.lng > ({lng}-0.005) and edge.line != {walk}").on(
            'lat -> lat,
            'lng -> lng,
            'walk -> LineData.walk.id
        ).as(edgeDistance *)
      }
    }
    
    def findNearEndEdges(lat:Double,lng:Double):List[(EdgeData,Int)]={
      DB.withConnection { implicit c =>
        SQL("select edge.*, 1000*sqrt(pow({lat}-node.lat,2)+pow({lng}-node.lng,2)) as tDistance " +
                "from edges edge join nodes node on edge.targetNode = node.id " +
                "where node.lat < ({lat}+0.005) and node.lat > ({lat}-0.005) and node.lng < ({lng}+0.005) and node.lng > ({lng}-0.005) and edge.line != {walk}").on(
            'lat -> lat,
            'lng -> lng,
            'walk -> LineData.walk.id
        ).as(edgeDistance *)
      }
    }
    
    val edgeDistance = {
        get[Long]("id") ~ 
        get[Long]("line") ~ 
        get[Long]("sourceNode") ~
        get[Long]("targetNode") ~
        get[Int]("distance") ~
        get[Int]("duration") ~ 
        get[Double]("tDistance") map { 
          case id~line~sourceNode~targetNode~distance~duration~tDistance => 
            (EdgeData(id,line, sourceNode,targetNode,distance,duration),tDistance.toInt)
        }
    }
    
    val edge = { 
        get[Long]("id") ~ 
        get[Long]("line") ~ 
        get[Long]("sourceNode") ~
        get[Long]("targetNode") ~
        get[Int]("distance") ~
        get[Int]("duration") map {
            case id~line~sourceNode~targetNode~distance~duration => EdgeData(id,line, sourceNode,targetNode,distance,duration)
        }
    }

}