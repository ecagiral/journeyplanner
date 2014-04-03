import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db._
import anorm._
import models._
import play.api.Play.current

class RouteWalkSpec extends Specification{

   val fakeAppwithTestDB = FakeApplication(additionalConfiguration = Map( 
              "db.default.driver" -> "org.postgresql.Driver",
              "db.default.url" -> "jdbc:postgresql://localhost:5432/testjourney",
              "db.default.user" -> "ega",
              "db.default.password" -> "12345"
   ))
   
    "EdgeCrossData" should {
      "find routes with walk" in {
      running (fakeAppwithTestDB){
            DB.withConnection { implicit c =>
                SQL("delete from edges").executeUpdate()
                SQL("delete from nodes").executeUpdate()
                SQL("delete from lines").executeUpdate()
            }

            
          val walk = LineData.create("walk", 0, 0).get
          val line1 = LineData.create("line1", 20, 1).get
          val line2 = LineData.create("line2", 20, 1).get

          val node1 = NodeData.create("node1", 40.000, 28.000).get
          val node2 = NodeData.create("node2", 40.010, 28.010).get
          val node3 = NodeData.create("node3", 40.011, 28.011).get
          val node4 = NodeData.create("node4", 40.900, 28.900).get
          
          val edge1 = EdgeData.create(line1, node1, node2, 5, 5).get
          val edge2 = EdgeData.create(line2, node3, node4, 5, 5).get
          
          val res = RouteUtil.solveRoute(40.00, 28.00,40.90, 28.90)
          res.size must be_==(1)           
          
      }
    }
  }


}