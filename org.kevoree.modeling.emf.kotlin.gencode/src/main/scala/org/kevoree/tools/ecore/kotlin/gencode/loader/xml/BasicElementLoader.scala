/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * 	Fouquet Francois
 * 	Nain Gregory
 */


package org.kevoree.tools.ecore.kotlin.gencode.loader.xml

import org.eclipse.emf.ecore.{EPackage, EClass}
import java.io.{PrintWriter, File}
import scala.collection.JavaConversions._
import org.kevoree.tools.ecore.kotlin.gencode.{GenerationContext, ProcessorHelper}

/**
 * Created by IntelliJ IDEA.
 * User: Gregory NAIN
 * Date: 29/09/11
 * Time: 17:24
 */

class BasicElementLoader(ctx : GenerationContext, genDir: String, genPackage: String, elementType: EClass, context: String, modelingPackage: EPackage, modelPackage: String) {

  def generateLoader() {
    //Creation of the generation dir
    ProcessorHelper.checkOrCreateFolder(genDir)
    val file = new File(genDir + "/" + elementType.getName + "Loader.kt")

    if (!file.exists()) {
      //Does not override existing file. Should have been removed before if required.
      val subLoaders = generateSubs(elementType)
      val pr = new PrintWriter(file,"utf-8")
      pr.println("package " + genPackage + ";" )
      pr.println()
      pr.println("import " + modelPackage + ".*")
      pr.println("import " + genPackage.substring(0, genPackage.lastIndexOf(".")) + ".*")
      pr.println("import javax.xml.stream.XMLStreamConstants")

      pr.println()

      //Generates the Trait
      pr.print("trait " + elementType.getName + "Loader")
      if (subLoaders.size > 0) {
        var stringListSubLoaders = List[String]()
        subLoaders.foreach(sub => stringListSubLoaders = stringListSubLoaders ++ List(sub.getName + "Loader"))
        pr.println(stringListSubLoaders.mkString(" : ", ", ", " {"))
      } else {
        pr.println("{")
      }

      pr.println()
      generateElementLoadingMethod(pr)
      pr.println("")
      pr.println("}")
      pr.flush()
      pr.close()
    }
  }

  private def generateSubs(currentType: EClass): List[EClass] = {
    var listContainedElementsTypes = List[EClass]()

    currentType.getEAllContainments.foreach {
      ref =>
        if(ref.getEReferenceType != currentType) { //avoid looping in self-containment
          if (!ref.getEReferenceType.isInterface) {
            //Generates loaders for simple elements
            val el = new BasicElementLoader(ctx, genDir, genPackage, ref.getEReferenceType, context, modelingPackage, modelPackage)
            el.generateLoader()

          } else {
            //System.out.println("ReferenceType of " + ref.getName + " is an interface. Not supported yet.")
            val el = new InterfaceElementLoader(ctx, genDir + "/sub/", genPackage + ".sub", ref.getEReferenceType, context, modelingPackage, modelPackage)
            el.generateLoader()
          }
          if (!listContainedElementsTypes.contains(ref.getEReferenceType)) {
            listContainedElementsTypes = listContainedElementsTypes ++ List(ref.getEReferenceType)
          }
        }
    }

    listContainedElementsTypes
  }


  private def generateElementLoadingMethod(pr: PrintWriter) {
    pr.println("fun load" + elementType.getName + "Element(elementId: String, context : " + context + ") : " + ProcessorHelper.fqn(ctx,elementType) + " {")
    pr.println("")

    val ePackageName = elementType.getEPackage.getName
    val factory = ProcessorHelper.fqn(ctx,elementType.getEPackage) + "." + ePackageName.substring(0, 1).toUpperCase + ePackageName.substring(1) + "Factory"

    pr.println("val elementTagName = context.xmiReader?.getLocalName()")
    pr.println("val modelElem = " + factory + ".create" + elementType.getName + "()")
    pr.println("context.map.put(elementId, modelElem)")
    //pr.println("System.out.println(\"Stored:\" + elementId +\"->\"+ modelElem.class.getSimpleName)")
    pr.println("")



    val references = elementType.getEAllReferences.filter(ref => !ref.isContainment)
    if(elementType.getEAllAttributes.size()>0 || references.size > 0) {

      pr.println("for(i in 0.rangeTo(context.xmiReader?.getAttributeCount() as Int)) {")
      pr.println("val prefix = context.xmiReader?.getAttributePrefix(i)")
      pr.println("if(prefix==null || prefix.equals(\"\")) {")
      pr.println("val attrName = context.xmiReader?.getAttributeLocalName(i)")
      pr.println("val value = context.xmiReader?.getAttributeValue(i)")
      pr.println("when(attrName){")
      pr.println("")
      elementType.getEAllAttributes.foreach { att =>

        val methName = "set" + att.getName.substring(0, 1).toUpperCase + att.getName.substring(1)
        pr.println("\""+att.getName+"\" -> {")
        //pr.println("val value = context.xmiReader?.getAttributeValue(i)")
       // pr.println("modelElem." + methName + "(" + ProcessorHelper.convertType(att.getEAttributeType.getInstanceClassName) + ".valueOf(value))")

        val attTypeName = ProcessorHelper.convertType(att.getEAttributeType.getName)
        if(attTypeName.equals("String")) {
          pr.println("modelElem." + methName + "(value as String)")
        } else {
          pr.println("modelElem." + methName + "(value?.to"+ attTypeName +"() as "+attTypeName+")")
        }

        pr.println("}")
        pr.println("")

      }
      references.foreach {
        ref =>

          var methName: String = ""
          if (ref.getUpperBound == 1) {
            methName = "set"
          } else {
            methName = "add"
          }
          methName += ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)

          pr.println("\""+ref.getName+"\" -> {")

          pr.println("value?.split(\" \")?.forEach{ xmiRef ->")
          pr.println("val ref = context.map.get(xmiRef)")
          pr.println("if( ref != null) {")
         // if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
          //    pr.println("case Some(s: " + ProcessorHelper.fqn(ctx,ref.getEReferenceType) + ") => modelElem." + methName + "(Some(s))")
         pr.println("modelElem." + methName + "(ref as "+ref.getEReferenceType.getName+")")          //} else {
          //  pr.println("case Some(s: " + ProcessorHelper.fqn(ctx, ref.getEReferenceType) + ") => modelElem." + methName + "(s)")
         // }
          pr.println("} else {")
          pr.println("context.resolvers.add({()->{")
          pr.println("val ref = context.map.get(xmiRef)")
          pr.println("if(ref != null) {")
          //if (ref.getUpperBound == 1 && ref.getLowerBound == 0) {
            pr.println("modelElem." + methName + "(ref as "+ref.getEReferenceType.getName+")")
          //} else {
          //  pr.println("case Some(s: " + ProcessorHelper.fqn(ctx, ref.getEReferenceType) + ") => modelElem." + methName + "(s)")
          //}
          pr.println("} else { throw Exception(\"KMF Load error : " + ref.getEReferenceType.getName + " not found in map ! xmiRef:\" + xmiRef)")
          pr.println("}")//match
          pr.println("}})")//append
          pr.println("}")
          pr.println("}")
          pr.println("}")
         // pr.println("}")
      }
      pr.println("else->{System.out.println(\""+elementType.getName+">>AttributeName not in cases:\" + attrName);null }")
      pr.println("}")
      pr.println(" }")
      pr.println(" }")

      pr.println("")

    }

    if(elementType.getEAllContainments.size() > 0) {
      pr.println("var done = false")
      pr.println("while(!done) {")
      pr.println("when(context.xmiReader?.nextTag()){")
      pr.println("XMLStreamConstants.START_ELEMENT -> {")
      pr.println("when(context.xmiReader?.getLocalName()){")
      pr.println("")

      elementType.getEAllContainments.foreach {refa =>
        pr.println("\""+refa.getName+"\" -> {")

        val formattedReferenceName = refa.getName.substring(0, 1).toUpperCase + refa.getName.substring(1)

        //pr.println("val " + refa.getName + " = ")

        if(!refa.isMany) {
          pr.println("val " + refa.getName + "ElementId = elementId + \"/@" + refa.getName+"\"")
          //if (!refa.isRequired) {
          //  pr.println("modelElem.set" + formattedReferenceName + "(Some(load" + refa.getEReferenceType.getName + "Element(" + refa.getName + "ElementId, context)))")
          //} else {
            pr.println("modelElem.set" + formattedReferenceName + "(load" + refa.getEReferenceType.getName + "Element(" + refa.getName + "ElementId, context))")
          //}
        } else {
          pr.println("val i = context.elementsCount.get(elementId + \"/@" + refa.getName + "\") ?: 0")
          pr.println("val " + refa.getName + "ElementId = elementId + \"/@" + refa.getName + ".\" + i")
          pr.println("modelElem.add" + formattedReferenceName + "(load" + refa.getEReferenceType.getName + "Element(" + refa.getName + "ElementId, context))")
          pr.println("context.elementsCount.put(elementId + \"/@" + refa.getName + "\",i+1)")
        }

        pr.println("}") //case
      }

      pr.println("")
      pr.println("else -> {}")
      pr.println("}")//Match
      pr.println("}")//Case Start
      pr.println("XMLStreamConstants.END_ELEMENT -> {")
      pr.println("if(context.xmiReader?.getLocalName().equals(elementTagName) as Boolean){done = true}")
      pr.println("}") //case END
      pr.println("else -> {}")
      pr.println("}")//Match
      pr.println("}")//while
      pr.println("")
    }
    pr.println("return modelElem")
    pr.println("}")
  }

}
