package yetanothertf2configgenerator

import org.fusesource.scalate._
import java.io._
import settings._
import scala.swing.Component
import java.nio.ByteBuffer

object ConfigGenerator {
  val templateDir = "templates"
  val cacheCopy = "cache"
  val cacheDir = System.getProperty("user.home") + File.separator + "yetanothertf2configgenerator-cache"
  val configExtension = ".cfg.ssp"
  val tmpExtension = ".tmp"

  val engine = new TemplateEngine(List(new File(cacheDir)), java.lang.System.getProperty("scalate.mode", "production"))
  engine.escapeMarkup = false
  engine.workingDirectory = new File(cacheDir)
  engine.allowReload = false

  val configNames = List(
    "autoexec",
    "reset",
    "scout",
    "engineer",
    "pyro",
    "soldier",
    "heavyweapons",
    "sniper",
    "spy",
    "medic",
    "demoman",
    "dx9frames",
    "highframes",
    "maxframes",
    "highquality",
    "maxquality",
    "net")
  
  def bindif(key : String, to : String) = if(key == "nothing") "" else "bind " + key + " " + to   
  
  def unbindif(key : String) = if(key == "all") "unbindall" else if(key == "nothing") "" else "unbind " + key
  
  def generateTemplatesWithVariableDeclarations {
    Util.copyDirectory(new File(cacheCopy), new File(cacheDir))
      
    configNames.foreach(configName => {
      val varDeclBuff = ByteBuffer.wrap(Setting.templateVariableDeclarations.getBytes)
      val from = new File(templateDir + File.separator + configName + configExtension)
      val to = new File(cacheDir + File.separator + configName + tmpExtension + configExtension)
      
      if(to.exists)
        to.delete
        
      try {
        val fromStream = new FileInputStream(from)
        val toStream = new FileOutputStream(to, true)
        val fromChannel = fromStream.getChannel
        val toChannel = toStream.getChannel
        
        try {        
          toChannel.write(varDeclBuff)
          fromChannel.transferTo(0, Long.MaxValue, toChannel)
        } finally {
          fromChannel.close
          toChannel.close
          fromStream.close
          toStream.close        
        }
      } catch {
        case e: Exception => {
          Util.handleException(e)
        }
      }
    })
  }

  /*
   * Filter the resulting configs so they look neat and tidy
   */
  def filterResult(result : String) = {
    val deIndented = """(?m)^ *(.*)$""".r.replaceAllIn(result, "$1")
    val withoutEmptyLines = """[\r\n]{2,}""".r.replaceAllIn(deIndented, sys.props("line.separator"))
    withoutEmptyLines
  }

  /*
   * render the templates
   */
  def render = {
    val templateData = Setting.getTemplateData
    configNames.map((configname: String) => {
      val result = engine.layout(configname + tmpExtension + configExtension, templateData + (("currentConfig", configname)), List())
      (configname, filterResult(result))
    }).toMap
  }

  /*
   * Write output templates to a directory
   */
  def writeToDirectory(directory: String) {
    render.foreach {
      case (filename, rendered) => {
        val out = new OutputStreamWriter(new FileOutputStream(directory + File.separator + filename + ".cfg"))
        out.write(rendered)
        out.close
      }
    }
  }
}
