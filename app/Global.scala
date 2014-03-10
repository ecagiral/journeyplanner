

import play.api._
import models.LineCrossData
import play.Logger
import models._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
      //Logger.info("app startup ")
      LineData.findByLabel("walk") match {
          case None => val id = LineData.create("walk",0, 0);Logger.info("walk line yaratildi. Id: "+id.get)
          case Some(line) => ()
      }
      //LineCrossData.init()
  }

}