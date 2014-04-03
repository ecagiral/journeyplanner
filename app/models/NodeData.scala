package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.Logger
import scala.util.Random
import models.AnormImplicits.RichSQL

case class NodeData(id:Long,label:String,lat:Double,lng:Double){
    
    def update(label: String,lat:Double,lng:Double) {
        DB.withConnection { implicit c =>
            SQL("update nodes set (label,lat,lng) = ({label},{lat},{lng}) where id = {id}").on(
                'id -> id,
                'label -> label,
                'lat -> lat,
                'lng -> lng
            ).executeUpdate()
        }
    }

}

object NodeData {
  
    def all(): List[NodeData] = DB.withConnection { implicit c =>
        SQL("select * from nodes").as(node *)
    }
    
    def findByIds(nodes: List[Long]):List[NodeData] = {
        if(nodes.size==0)List[NodeData]()
        else{
            DB.withConnection { implicit connection =>
               val sqlText = RichSQL(""" SELECT * FROM nodes WHERE id IN ({nodeIds}) """).onList("nodeIds" -> nodes).toSQL
               sqlText.as(node *)(connection)
            }
        }
    }
    
    def findByLoc(lat:Double,lng:Double):Option[NodeData] = DB.withConnection{ implicit c =>
        SQL("select * from nodes where lat = {lat} and lng = {lng}").on(
           'lat -> lat,
           'lng -> lng
        ).as(node.singleOpt)     
    }
    
    def findNear(lat:Double,lng:Double,distance:Integer):List[(Long,String,Long)] = DB.withConnection{ implicit c =>
        SQL("select distinct on (id) id,label,distance::bigint from (select id,label,(1609*SQRT(POW(69.1 * (lat - {lat}), 2) + POW(69.1 * ({lng} - lng) * COS(lat / 57.3), 2))) as distance FROM nodes) as subq where distance < {distance}  ").on(
           'lat -> lat,
           'lng -> lng,
           'distance -> distance
        ).parse( 
            long("id") ~ str("label") ~ long("distance") map{
              case id~label~dist => (id,label,dist)
            }*
        )  
        
    }
    
    def create(label: String,lat:Double,lng:Double):Option[Long]= {
        val rLat =((lat*1000).toInt).toDouble/1000
        val rLng =((lng*1000).toInt).toDouble/1000
        NodeData.findByLoc(rLat, rLng) match {
          case Some(node) => if(node.label!=label){Logger.warn("node "+label+" saved as "+node.label)};Some(node.id)
          case None => {
            NodeData.findByLabel(label) match {
                case Some(node) =>{ 
                  val dist = (1000*Math.sqrt(Math.pow(rLat-node.lat,2)+Math.pow(rLng-node.lng,2))).toInt
                  if(dist>2){
                      Logger.warn("node "+label+" saved "+dist+" away");
                      None
                  }else{
                      Some(node.id)
                  }
                  
                }
                case None => create_db(label,rLat,rLng)
            }
          }
        }
        
    }
    
    def create_db(label: String,lat:Double,lng:Double):Option[Long] = DB.withConnection { implicit c =>
        SQL("insert into nodes (label,lat,lng) values ({label},{lat},{lng})").on(
                'label -> label,
                'lat -> lat,
                'lng -> lng
        ).executeInsert().map{
          id => 
            findNear(lat,lng,500).foreach{
              node =>
                if(node._1 != id){
                    EdgeData.create(LineData.walk.id, node._1, id, node._3.toInt, 0)
                    EdgeData.create(LineData.walk.id, id, node._1,node._3.toInt, 0)
                }
            }               
            id
        }                
    }
    
    def dummy(id:Long):NodeData = {
      val lat = Random.nextDouble+40.0;
      val lng = Random.nextDouble+28.0;
      new NodeData(id,"node"+id,lat,lng)
    }

    def delete(id: Long) {
        DB.withConnection { implicit c =>
            SQL("delete from nodes where id = {id}").on(
                'id -> id
            ).executeUpdate()
        }
    }
    
    val node = {
        get[Long]("id") ~ 
        get[String]("label") ~
        get[Double]("lat") ~
        get[Double]("lng") map {
            case id~label~lat~lng => NodeData(id, label,lat,lng)
        }
    }
    
    def findById(id: Long): Option[NodeData] = {
        DB.withConnection { implicit connection =>
            SQL("select * from nodes where id = {id}").on(
                'id -> id
            ).as(node.singleOpt)
        } 
    }
    
    def findByLabel(label: String): Option[NodeData] = {
        DB.withConnection { implicit connection =>
            SQL("select * from nodes where label = {label}").on(
                'label -> label
            ).as(node.singleOpt)
        } 
    }
       
}