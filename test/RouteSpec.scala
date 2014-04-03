import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db._
import anorm._
import models._
import play.api.Play.current

class RouteSpec extends Specification{

   val fakeAppwithTestDB = FakeApplication(additionalConfiguration = Map( 
              "db.default.driver" -> "org.postgresql.Driver",
              "db.default.url" -> "jdbc:postgresql://localhost:5432/testjourney",
              "db.default.user" -> "ega",
              "db.default.password" -> "12345"
   ))
   
    "RouteUtil" should {
      "find routes without walk" in {
      running (fakeAppwithTestDB){
            DB.withConnection { implicit c =>
                SQL("delete from edges").executeUpdate()
                SQL("delete from nodes").executeUpdate()
                SQL("delete from lines").executeUpdate()
            }
        
          // N1S -> line1 -> N1E   = one line no station (1 solution)
            
          // N2S -> line2 -> N21 -> line2 -> N2E = one line one station (1 solution)
            
          // N3S -> line3 -> N31 -> line4 -> N3E = two serial lines one station (1 solution)
          
          // N4S -> line5 -> N41 -> line5 -> N42 -> line5 -> N4E = two parallel lines two stations (6 solutions)
          // N4S -> line6 -> N41 -> line6 -> N42 -> line6 -> N4E
            
          // N5S -> line7 -> N5E = independent line (0 solution)
            
          val walk = LineData.create("walk", 0, 0).get
          val line1 = LineData.create("line1", 20, 1).get
          val line2 = LineData.create("line2", 20, 1).get
          val line3 = LineData.create("line3", 20, 1).get
          val line4 = LineData.create("line4", 20, 1).get
          val line5 = LineData.create("line5", 20, 1).get
          val line6 = LineData.create("line6", 20, 1).get
          val line7 = LineData.create("line7", 20, 1).get
          
          //start nodes
          val node1S = NodeData.create("node1S", 40.000, 28.000).get
          val node2S = NodeData.create("node2S", 40.001, 28.001).get
          val node3S = NodeData.create("node3S", 40.002, 28.002).get
          val node4S = NodeData.create("node4S", 40.003, 28.003).get
          val node5S = NodeData.create("node5S", 40.500, 28.500).get
          
          //middle nodes
          val node21 = NodeData.create("node21", 40.010, 28.010).get
          val node31 = NodeData.create("node31", 40.020, 28.020).get
          val node41 = NodeData.create("node41", 40.030, 28.030).get
          val node42 = NodeData.create("node42", 40.040, 28.040).get
          
          //end nodes
          val node1E = NodeData.create("node1E", 40.900, 28.900).get
          val node2E = NodeData.create("node2E", 40.901, 28.901).get
          val node3E = NodeData.create("node3E", 40.902, 28.902).get
          val node4E = NodeData.create("node4E", 40.903, 28.903).get
          val node5E = NodeData.create("node5E", 40.999, 28.999).get
          
          val edgeC1_1 = EdgeData.create(line1, node1S, node1E, 5, 5).get
          val edgeC2_1 = EdgeData.create(line2, node2S, node21, 5, 5).get
          val edgeC2_2 = EdgeData.create(line2, node21, node2E, 5, 5).get
          val edgeC3_1 = EdgeData.create(line3, node3S, node31, 5, 5).get
          val edgeC3_2 = EdgeData.create(line4, node31, node3E, 5, 5).get
          val edgeC4_1 = EdgeData.create(line5, node4S, node41, 5, 5).get
          val edgeC4_2 = EdgeData.create(line5, node41, node42, 5, 5).get
          val edgeC4_3 = EdgeData.create(line5, node42, node4E, 5, 5).get
          val edgeC4_4 = EdgeData.create(line6, node4S, node41, 5, 5).get
          val edgeC4_5 = EdgeData.create(line6, node41, node42, 5, 5).get
          val edgeC4_6 = EdgeData.create(line6, node42, node4E, 5, 5).get
          val edgeC7_1 = EdgeData.create(line7, node5S, node5E, 5, 5).get
          
          val res = RouteUtil.solveRoute(40.00, 28.00,40.90, 28.90)
          //res.foreach(route=>println("route : "+route.toString))
          res.size must be_==(7)           
          
      }
    }
  }


}