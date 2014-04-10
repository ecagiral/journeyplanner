package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import play.api.Play.current

case class Config(confName:String,confValue:String) {

}

object Config{
  
  def getConfigParm(name:String):Option[String] = DB.withConnection { implicit c =>
    SQL("select confValue from config where confName = {name} ").on('name->name).as(scalar[String].singleOpt)
  }
  
  def getAdminPassword:String = {
    getConfigParm("adminPass").getOrElse("")
  }
  
}