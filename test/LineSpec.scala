import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db._
import anorm._
import models._
import play.api.Play.current

class LineSpec extends Specification{
  
   val fakeAppwithTestDB = FakeApplication(additionalConfiguration = Map( 
              "db.default.driver" -> "org.postgresql.Driver",
              "db.default.url" -> "jdbc:postgresql://localhost:5432/testjourney",
              "db.default.user" -> "ega",
              "db.default.password" -> "12345"
   ))
   
    "LineCrossData" should {
      "find line crossing" in {
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
          val line4 = LineData.create("line4", 20, 1).get
          val line5 = LineData.create("line5", 20, 1).get

          val node1 = NodeData.create("node1", 40.000, 28.000).get
          val node2 = NodeData.create("node2", 40.050, 28.050).get
          val node3 = NodeData.create("node3", 40.100, 28.100).get
          val node4 = NodeData.create("node4", 40.150, 28.150).get
          val node5 = NodeData.create("node5", 40.200, 28.200).get
          val node6 = NodeData.create("node6", 40.250, 28.250).get
          val node7 = NodeData.create("node7", 40.300, 28.300).get
          val node8 = NodeData.create("node8", 40.301, 28.301).get
          val node9 = NodeData.create("node9", 40.350, 28.350).get
          
          val edge1 = EdgeData.addEdge(line1, node1, node2, 5, 5).get
          val edge2 = EdgeData.addEdge(line2, node3, node4, 5, 5).get
          val edge3 = EdgeData.addEdge(line3, node4, node5, 5, 5).get
          val edge4 = EdgeData.addEdge(line4, node6, node7, 5, 5).get
          val edge5 = EdgeData.addEdge(line5, node8, node9, 5, 5).get
          
          val res = LineCrossData.getCrossData
          
          res._1.get(walk) must beNone
          res._1.get(line1) must beNone
          res._1.get(line2).get.size mustEqual 1
          res._1.get(line2).get(0) mustEqual line3
          res._1.get(line3) must beNone
          res._1.get(line4).get.size mustEqual 1
          res._1.get(line4).get(0) mustEqual line5
          
          res._2.get(walk) must beNone
          res._2.get(line1) must beNone
          res._2.get(line2) must beNone
          res._2.get(line3).get.size mustEqual 1
          res._2.get(line3).get(0) mustEqual line2
          res._2.get(line5).get.size mustEqual 1
          res._2.get(line5).get(0) mustEqual line4
          
      }
    }
  }


}