/**
 *## Introduction
 *Inspired by [Docco](http://jashkenas.github.com/docco/), [Groc](http://fix.github.com/Groc/groc.groovy.html) is a *document generator* based on your comments in the source code.
 *
 *You can optionally use the [Markdown](http://en.wikipedia.org/wiki/Markdown) to enhance the formatting.
 *
 *Find the source on [Github](http://github.com/fix/Groc/).
 *
 *## Installation
 *You need to have Groovy installed on your machine.
 *
 *## Use
 *You can launch it inside a directory containing your groovy files using `groovy https://raw.github.com/fix/Groc/master/groc.groovy [extension]`. It will create a folder called `docs` with all your html files
 *
 *Only comments starting with `/**` are parsed whereas `//` or `/*` comments are left in the code.
 */
/**
 * Grab the MarkDown processor. All capabilities with extensions can be used. See [PegDown](https://github.com/sirthias/pegdown) for more information.
 */
@Grab(group='org.pegdown', module='pegdown', version='1.1.0')

/**
 * Import Template Engine, MarkupBuilder and PegDown processor
 */

import groovy.text.SimpleTemplateEngine
import groovy.xml.MarkupBuilder
import java.nio.charset.Charset
import org.pegdown.PegDownProcessor

/**
 * From args get the file type to be parse:
 * 
 * - "groovy" for `.groovy` files,
 * - "java" for `.java` files
 * - "gradle" for `.gradle files`
 * - "asfasfafasd" for `.asfasfafasd` files
 * 
 * Default is "groovy"
 */
extension = args.length==1?args[0]:"groovy"

/**
 * Set Initial parameters such as current folder to process your `.groovy` files
 */
root=new File(".")
docs=new File("docs")
docs.mkdir()
toc=[:]

/**
 *Executing for all `.groovy` files in the folder
 */
def parseFiles(File folder,String relativePath=""){
  folder.listFiles().sort{it.isDirectory()?1:-1}.each{
    if(it.name =~ ".*\\.${extension}\$"){
      File docFolder=new File(docs.path+"/"+relativePath)
      docFolder.mkdirs()
      File docSource=new File(docFolder.path+"/"+it.name+".html")
      toc[it]=[docSource,relativePath+it.name+".html"]
    }
    else if(it.isDirectory()&&it.name!="docs"&&!it.name.startsWith(".")){
      parseFiles(it,relativePath+it.name+"/")
    }
    
  }
}

parseFiles(root)
/*********************
 *## The main method
 *You can create this kind of text spanning all the page commenting "empty" code
 *********************/
/**
 * take input file and output file. Erase output if existing
 */
/*
 * Comment not parsed
 */
def createGroc(File source, File output){
/**
 *## The Parser
 */
  def parsedCode=[]
  boolean commentOn=false
  def currentCode // comment,code
  source.eachLine{
    def line=it
    if(it =~ /^\/\*\*.*/){
      if(!commentOn){
        if(currentCode)parsedCode<<currentCode
        currentCode=["", ""]
        line=it.replaceFirst("/\\**","")
        commentOn=true
      }
    }

    if(commentOn){
      currentCode[0]+=((line =~ ".?\\*\\**/")
      .replaceFirst("") =~ ".?\\*")
      .replaceFirst("")+"\n"
    }
    else{
      currentCode[1]+=line+"\n"
    }

    if(it =~ /.*\*\//){
      commentOn=false
    }
  }
  parsedCode << currentCode

  PegDownProcessor m=new PegDownProcessor()

  parsedCode.each{
    it[0] = m.markdownToHtml(it[0])
  }


  FileWriter fw=new FileWriter(output)
/**
* 
*## The HTML template, thanks to MarkupBuilder
* 
*/
/**
* Inlining all the javascript and css
*/
  def tl=new MarkupBuilder(fw).html{
    head{
      title("Grocs "+source.name)
      meta("http-equiv":"content-type", content:"text/html; charset=UTF8")
      style(media:"all"){
        mkp.yieldUnescaped("https://raw.github.com/fix/Groc/master/groc.css".toURL().text)
      }
      style(media:"all"){
        mkp.yieldUnescaped("http://alexgorbatchev.com/pub/sh/current/styles/shCore.css".toURL().text)
      }
      script(type:"text/javascript"){
        mkp.yieldUnescaped("http://alexgorbatchev.com/pub/sh/current/scripts/shCore.js".toURL().text)
      }
      script(type:"text/javascript"){
        mkp.yieldUnescaped("http://alexgorbatchev.com/pub/sh/current/scripts/shBrushGroovy.js".toURL().text)
      }
    }
    body{
      div(id:"content"){
        if(toc.size()>1){
          div(id:"jump_to"){
            mkp.yieldUnescaped("Jump To &hellip;")
            div(id:"jump_wrapper"){
              div(id:"jump_page"){
                toc.entrySet().each{
                  a("class":"source",
                    href:docs.absolutePath+"/${it.value[1]}",
                    style:(it.key==source?"background-color:#aaffaa;":""),
                    it.key.path.replaceFirst("./","")
                    )
                }
              }
            }
          }
        }
        table(cellpadding:"0", cellspacing:"0"){
          thead{
            th("class":"docs"){ h1(source.name) }
            th()
          }
          tbody{
            parsedCode.eachWithIndex {code,i->
              tr(id:"section-"+i){
                td("class":"docs"+(code[1].size()>0?"":" main"), colspan:code[1].size()>0?"1":"2"){
                  div("class":"pilwrap"){
                    a("class":"pilcrow", href:"#section-"+i, "#")
                  }

                  mkp.yieldUnescaped(code[0])
                }
                if(code[1].size()>0){
                  td("class":"codes"){
                    pre("class":"brush: groovy; gutter: false; toolbar: false;"){
                      mkp.yield(code[1])
                    }
                  }
                }
              }
            }
          }
        }
      }
      script(type:"text/javascript"){ mkp.yield("SyntaxHighlighter.all()") }
    }
  }
}

/**
 *## Do It!
 */
/** */
toc.entrySet().each{
  createGroc(it.key,it.value[0])
}