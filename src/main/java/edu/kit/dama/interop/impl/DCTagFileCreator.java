/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.dama.interop.impl;

import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.content.util.DublinCoreHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;
import org.w3c.dom.Document;

/**
 *
 * @author jejkal
 */
public class DCTagFileCreator extends AbstractTagFileCreator{

  public static DCTagFileCreator createInstance(){
    return new DCTagFileCreator();
  }

  @Override
  String getMetadataType(){
    return "DublinCore";
  }

  @Override
  Path createTagFile(DigitalObject theObject, BagBuilder theBagBuilder) throws Exception{
    try{
      //Create and store dublin core metadata
      Document doc = DublinCoreHelper.createDublinCoreDocument(theObject, theObject.getUploader());
      Path dcOutputPath = Paths.get(getMetadataPath(theBagBuilder.getBag()).toString(), "dc.xml");
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      Result output = new StreamResult(Files.newOutputStream(dcOutputPath));
      Source input = new DOMSource(doc);
      transformer.transform(input, output);
      return dcOutputPath;
    } catch(ParserConfigurationException | TransformerException | IOException ex){
      throw new Exception("Failed to generate metadata tagfile.", ex);
    }
  }

  @Override
  DigitalObject createDigitalObject(Path tagFile) throws Exception{
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
