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
import gov.loc.repository.bagit.domain.Bag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.eclipse.persistence.jaxb.MarshallerProperties;

/**
 *
 * @author jejkal
 */
public class BMDTagFileCreator extends AbstractTagFileCreator{

  public static BMDTagFileCreator createInstance(){
    return new BMDTagFileCreator();
  }

  @Override
  String getMetadataType(){
    return "BaseMetadata";
  }

  @Override
  Path createTagFile(DigitalObject theObject, BagBuilder theBagBuilder) throws Exception{
    Marshaller marshaller = org.eclipse.persistence.jaxb.JAXBContext.newInstance(DigitalObject.class).createMarshaller();
    marshaller.setProperty(MarshallerProperties.OBJECT_GRAPH, "default");
    Path bmdOutputPath = Paths.get(getMetadataPath(theBagBuilder.getBag()).toString(), "bmd.xml");
    marshaller.marshal(theObject, Files.newOutputStream(bmdOutputPath));
    theBagBuilder.addTagfile(bmdOutputPath.toUri());
    return bmdOutputPath;
  }

  @Override
  DigitalObject createDigitalObject(Path tagFile, Bag theBag) throws Exception{
    Unmarshaller unmarshaller = org.eclipse.persistence.jaxb.JAXBContext.newInstance(DigitalObject.class).createUnmarshaller();
    return (DigitalObject) unmarshaller.unmarshal(Files.newInputStream(tagFile));
  }

}
