package models

import play.Logger
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.BitSet
import scala.collection.mutable.HashSet

object LineCrossData {

  def getCrossData:(Map[Long,List[Long]],Map[Long,List[Long]]) = {
    val forwList = LineData.getIntersectForw ::: LineData.getIntersectWalkForw
    val backList = LineData.getIntersectBack ::: LineData.getIntersectWalkBack
    (forwList.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))},
        backList.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))})
  }
  
  def getAvailableLines(startEdges:List[EdgeData],endEdges:List[EdgeData],lineCrossData:(Map[Long,List[Long]],Map[Long,List[Long]])):List[Long]={
    val result:HashSet[Long] = HashSet(LineData.walk.id);
    //single access
    for(lineA <- startEdges.map(_.line);lineB <- endEdges.map(_.line)){
      if(lineA == lineB){
        result.add(lineA)
      }
    }
    
    //double access
    for(lineA <- startEdges.map(_.line);lineB <- endEdges.map(_.line)){
      lineCrossData._1.getOrElse(lineA, List[Long]()).foreach{ mLine =>
        if(mLine==lineB)
          result.add(lineA)
          result.add(lineB)
      }
    } 
    //triple access
    for(lineA <- startEdges.map(_.line);lineB <- endEdges.map(_.line)){
      lineCrossData._1.getOrElse[List[Long]](lineA, List[Long](0)).foreach{
        lineX => if(lineCrossData._2.getOrElse[List[Long]](lineB, List[Long](0)).contains(lineX)){
          result.add(lineA)
          result.add(lineB)
          result.add(lineX)
        }
      }
    }
    return result.toList
    
  }
  
}