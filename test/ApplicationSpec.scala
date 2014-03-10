package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models._
import scala.util.Random
import models.Route
import play.api.Logger
import play.api.db._
import anorm._
import play.api.Play.current


class ApplicationSpec extends Specification {
  val fakeAppwithTestDB = FakeApplication(additionalConfiguration = Map( 
              "db.default.driver" -> "org.postgresql.Driver",
              "db.default.url" -> "jdbc:postgresql://localhost:5432/testjourney",
              "db.default.user" -> "ega",
              "db.default.password" -> "12345"
         ))
         
  
  "RouteUtil" should {
    "find a route in single unidirectional line" in {
      
      val nodes = List.tabulate(4)(NodeData.dummy(_))
      val lines = List.tabulate(1)(LineData.dummy(_))
      //Line1
      val edge1 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0)
      val edge2 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0)
      val edge3 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(2).id,nodes(3).id,5,0) 

      val edges = List(edge1,edge2,edge3)
      //val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edge1),List(edge3),-1))    
             
      val result = RouteUtil.iterateRoute(edges, routes)
      result.size must be_==(1)
      
    }
    
    "find no route in single unidirectional line" in {
      
      val nodes = List.tabulate(4)(NodeData.dummy(_))
      val lines = List.tabulate(1)(LineData.dummy(_))
      //Line1
      val edge1 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0)
      val edge2 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0)
      val edge3 = new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(2).id,nodes(3).id,5,0) 

      val edges = List(edge1,edge2,edge3)
      //val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edge3), List(edge1),-1))    
             
      val result = RouteUtil.iterateRoute(edges, routes)  
      result.size must be_==(0)      
    }
        
    "find a route in two serial lines" in {
      
      val nodes = List.tabulate(5)(NodeData.dummy(_))
      val lines = List.tabulate(2)(LineData.dummy(_))
      //Line1
      val edges = List(
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(2).id,nodes(3).id,5,0), 
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(3).id,nodes(4).id,5,0))

      //val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edges(0)), List(edges(3)),-1))    
             
      val result = RouteUtil.iterateRoute(edges, routes)
      result.size must be_==(1)
      
    }
    
    "find two routes in two parallel lines" in {
      
      val nodes = List.tabulate(4)(NodeData.dummy(_))
      val lines = List.tabulate(2)(LineData.dummy(_))
      //Line1
      val edges = List(
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(1).id,nodes(2).id,5,0), 
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(2).id,nodes(3).id,5,0))

      //val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edges(0)), List(edges(3)),-1))    
             
      val result = RouteUtil.iterateRoute(edges, routes)
      result.size must be_==(2)
      
    }
    
    "find one route in two independent lines" in {
      
      val nodes = List.tabulate(6)(NodeData.dummy(_))
      val lines = List.tabulate(2)(index => LineData.dummy(index))
      //Line1
      val edges = List(
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(2).id,nodes(3).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(0).id,nodes(1).id,5,0), 
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(1).id,nodes(4).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(4).id,nodes(5).id,5,0))

      //val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edges(0)), List(edges(2)),-1))    
           
      val result = RouteUtil.iterateRoute(edges, routes)
      result.size must be_==(1)
      
    }
    
    "find 4 routes in one serial one parallel bidirectional lines" in {
      
      
      val nodes = List.tabulate(8)(NodeData.dummy(_))
      val lines = List.tabulate(3)(LineData.dummy(_))
      val edges = List(
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(0).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(0).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(1).id,nodes(2).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(2).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(2).id,nodes(3).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(0).id,nodes(3).id,nodes(2).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(1).id,nodes(4).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(4).id,nodes(1).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(4).id,nodes(6).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(6).id,nodes(4).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(6).id,nodes(7).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(1).id,nodes(7).id,nodes(6).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(3).id,nodes(5).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(5).id,nodes(3).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(5).id,nodes(6).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(6).id,nodes(5).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(6).id,nodes(7).id,5,0),
        new EdgeData(Random.nextInt(Integer.MAX_VALUE) ,lines(2).id,nodes(7).id,nodes(6).id,5,0))

      val validEdges = edges.filter(edge => lines.map(_.id).contains(edge.line))
      val routes = List(new Route(List(edges(0)), List(edges(10)),-1))    
             
      val result = RouteUtil.iterateRoute(validEdges, routes)

      result.size must be_==(2)
      
      
    }
  }
  
  "EdgeData" should {
      "find 5 start edges in 3 lines" in {
      running (fakeAppwithTestDB){
            DB.withConnection { implicit c =>
                SQL("delete from edges").executeUpdate()
                SQL("delete from nodes").executeUpdate()
                SQL("delete from lines").executeUpdate()
            }
        
          val walk = LineData.create("walk", 0, 0).get
          val line1 = LineData.create("line1", 20, 1).get
          val line2 = LineData.create("line2", 20, 1).get
          val line3 = LineData.create("line3", 20, 1).get
          val node1 = NodeData.create("node1", 40.002, 28.002).get
          val node2 = NodeData.create("node2", 40.003, 28.003).get
          val node3 = NodeData.create("node3", 40.004, 28.004).get
          val node4 = NodeData.create("node4", 40.007, 28.007).get
          val node5 = NodeData.create("node5", 40.010, 28.010).get
          
          val edge1 = EdgeData.addEdge(line1, node1, node2, 5, 5)
          val edge2 = EdgeData.addEdge(line1, node2, node3, 5, 5)
          val edge3 = EdgeData.addEdge(line2, node2, node3, 5, 5)
          val edge4 = EdgeData.addEdge(line2, node3, node4, 5, 5)
          val edge5 = EdgeData.addEdge(line3, node3, node4, 5, 5)
          val edge6 = EdgeData.addEdge(line3, node4, node5, 5, 5)
          
          val start_edges = EdgeData.findNearStartEdges(40.000, 28.000).sortBy(_._2)          
          start_edges.size must be_==(5)
          
          val end_edges = EdgeData.findNearEndEdges(40.000, 28.000).sortBy(_._2)         
          end_edges.size must be_==(3)
          
      }
    }

  }
}