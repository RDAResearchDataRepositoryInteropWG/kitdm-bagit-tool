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

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import edu.kit.dama.entities.dc40.DataResource;
import edu.kit.dama.entities.dc40.ResourceType;
import edu.kit.dama.entities.dc40.Title;
import edu.kit.dama.entities.dc40.User;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.interop.util.DataCiteResourceHelper;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.base.UserData;
import edu.kit.dama.util.Constants;
import edu.kit.dama.util.DCTransformationHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.datacite.schema.kernel_4.Resource;
import org.datacite.schema.kernel_4.Resource.Creators.Creator;

/**
 *
 * @author jejkal
 */
public class DataCiteTagFileCreator extends AbstractTagFileCreator{

  private String creatorId;

  DataCiteTagFileCreator(){
    this.creatorId = Constants.WORLD_USER_ID;
  }

  DataCiteTagFileCreator(String creatorId){
    this.creatorId = creatorId;
  }

  public static DataCiteTagFileCreator createInstance(String creatorId){
    return new DataCiteTagFileCreator(creatorId);
  }

  public static DataCiteTagFileCreator createInstance(){
    return new DataCiteTagFileCreator();
  }

  @Override
  String getMetadataType(){
    return "DataCite 4.0";
  }

  @Override
  Path createTagFile(DigitalObject theObject, BagBuilder theBagBuilder) throws Exception{
    //Create and store datacite metadata
    User creator = User.createUser();
    creator.setIdentifier(theObject.getUploader().getDistinguishedName());
    creator.setName(theObject.getUploader().getLastName() + ", " + theObject.getUploader().getFirstName());
    creator.setGivenName(theObject.getUploader().getFirstName());
    creator.setFamilyName(theObject.getUploader().getLastName());
    DataResource dataResource = DataResource.factoryNewDataResource(theObject.getDigitalObjectIdentifier());
    dataResource.setTitle(new HashSet<>(Arrays.asList(Title.createTitle(theObject.getLabel(), Title.TYPE.TRANSLATED_TITLE))));
    dataResource.setCreator(new HashSet<>(Arrays.asList(creator)));
    if(theObject.getUploadDate() != null){
      edu.kit.dama.entities.dc40.Date creationDate = new edu.kit.dama.entities.dc40.Date();
      creationDate.setType(edu.kit.dama.entities.dc40.Date.DATE_TYPE.CREATED);
      creationDate.setDate(theObject.getUploadDate());
      dataResource.addDate(creationDate);
    }
    dataResource.setPublisher(creatorId);
    dataResource.setPublicationYear(new SimpleDateFormat("YYYY").format(new Date()));
    dataResource.setResourceType(ResourceType.createResourceType("Digital Object v1.6", ResourceType.TYPE_GENERAL.DATASET));
    dataResource.getSize().add(FileUtils.byteCountToDisplaySize(theBagBuilder.getPayloadSize()));

    Resource dcResource = DCTransformationHelper.toDataCite(dataResource);
    Marshaller marshaller = org.eclipse.persistence.jaxb.JAXBContext.newInstance(Resource.class).createMarshaller();
    marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper(){
      @Override
      public String getPreferredPrefix(String uri, String arg1, boolean arg2){
        return "";//return uri.equals("http://datacite.org/schema/kernel-4") ? "" : "";
      }
    });
    marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
    Path dataciteOutputPath = Paths.get(getMetadataPath(theBagBuilder.getBag()).toString(), "datacite.xml");
    marshaller.marshal(dcResource, Files.newOutputStream(dataciteOutputPath));
    return dataciteOutputPath;
  }

  @Override
  DigitalObject createDigitalObject(Path tagFile) throws Exception{
    Unmarshaller unmarshaller = org.eclipse.persistence.jaxb.JAXBContext.newInstance(Resource.class).createUnmarshaller();
    Resource resource = (Resource) unmarshaller.unmarshal(tagFile.toFile());

    String identifier = DataCiteResourceHelper.getIdentifier(resource);
    if(identifier == null){
      throw new Exception("No valid resource identifier found. Neither the primary identifier not an alternate identifier are assigned.");
    }

    DigitalObject object = DigitalObject.factoryNewDigitalObject(identifier);
    object.setLabel(DataCiteResourceHelper.getTitle(resource));

    if(object.getLabel() == null){
      object.setLabel("Created from DataCite metadata.");
    }

    List<Creator> creators = resource.getCreators().getCreator();
    if(!creators.isEmpty()){
      Creator creator = creators.get(0);
      if(!creator.getNameIdentifier().isEmpty()){
        String creatorId = creator.getNameIdentifier().get(0).getValue();
        UserData user = new UserData();
        user.setDistinguishedName(creatorId);
        object.setUploader(user);
        object.addExperimenter(user);
      } else{
        object.setUploader(UserData.WORLD_USER);
      }
    }

    return object;
  }
}
