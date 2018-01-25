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

import edu.kit.dama.commons.types.DigitalObjectId;
import edu.kit.dama.interop.util.AnsiUtil;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.base.UserData;
import edu.kit.dama.mdm.content.mets.util.MetsBuilder;
import edu.kit.dama.mdm.dataorganization.entity.core.ICollectionNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IDataOrganizationNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IFileNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IFileTree;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizer;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizerFactory;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author jejkal
 */
public class METSTagFileCreator extends AbstractTagFileCreator{

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.impl.MessageBundle");

  public static METSTagFileCreator createInstance(){
    return new METSTagFileCreator();
  }

  @Override
  String getMetadataType(){
    return "METS";
  }

  @Override
  Path createTagFile(DigitalObject theObject, BagBuilder theBagBuilder) throws Exception{
    final List<PayloadElement> payloadElements = new ArrayList<>();

    final Map<String, URI> fetchMap = new HashMap<>();
    MetsBuilder metsBuilder = MetsBuilder.init(theObject).
            createBMDSection(true).
            createDCSection(UserData.WORLD_USER).
            createDOSection((t) -> {
              if(t instanceof IFileNode){
                String lfn = ((IFileNode) t).getLogicalFileName().getStringRepresentation();
                URI fileUri = URI.create(lfn);
                if("file".equals(fileUri.getScheme())){
                  String baseUri = lfn.substring(0, lfn.indexOf("data") + 5);
                  String dataLocation = t.getViewName() + "/" + lfn.substring(baseUri.length());
                  payloadElements.add(new PayloadElement(lfn, baseUri, dataLocation));
                  return "bag:///data/" + dataLocation;
                } else{
                  String dataLocation = t.getViewName() + "/" + t.getName();
                  try{
                    dataLocation = getNodePath(theObject.getDigitalObjectId(), fileUri.toURL().toString(), t.getViewName());
                  } catch(Exception e){
                    AnsiUtil.printError(MESSAGES.getString("failed_to_determine_data_location"), e, theObject.getDigitalObjectIdentifier(), t.getName(), t.getViewName());
                  }
                  fetchMap.put("data/" + dataLocation, fileUri);
                  return lfn;
                }
              }
              return null;
            });

    Path metsOutputPath = Paths.get(getMetadataPath(theBagBuilder.getBag()).toString(), "mets.xml");
    metsBuilder.write(Files.newOutputStream(metsOutputPath));

    //Adding collected payload elements to bag
    AnsiUtil.printInfo(MESSAGES.getString("adding_payload_elements"), Integer.toString(payloadElements.size()));
    for(PayloadElement element : payloadElements){
      AnsiUtil.printInfo(MESSAGES.getString("adding_payload_element"), element.getPayloadPath());

      theBagBuilder = theBagBuilder.addPayload(Paths.get(URI.create(element.getBasePath())), URI.create(element.getPayloadPath()), element.getBagDataPath());
    }

    //adding created fetch elements to bag
    Set<Map.Entry<String, URI>> fetchEntries = fetchMap.entrySet();
    AnsiUtil.printInfo(MESSAGES.getString("adding_fetch_items"), Integer.toString(fetchEntries.size()));
    for(Map.Entry<String, URI> entry : fetchEntries){
      URL fileUrl = entry.getValue().toURL();
      AnsiUtil.printInfo(MESSAGES.getString("adding_fetch_item"), entry.getKey(), fileUrl.toString());
      URLConnection connection = fileUrl.openConnection();
      long contentLength = Long.parseLong(connection.getHeaderField("Content-Length"));

      theBagBuilder = theBagBuilder.addFetchItem(new FetchItem(fileUrl, contentLength, Paths.get(entry.getKey())));
    }
    return metsOutputPath;
  }

  @Override
  DigitalObject createDigitalObject(Path tagFile, Bag theBag) throws Exception{
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  private String getNodePath(DigitalObjectId doi, String lfn, String viewName) throws Exception{
    DataOrganizer organizer = DataOrganizerFactory.getInstance().getDataOrganizer();
    IFileTree tree = organizer.loadFileTree(doi);
    Stack<String> path = new Stack<>();
    checkNode(tree.getRootNode(), lfn, path);
    StringBuilder builder = new StringBuilder();
    while(!path.isEmpty()){
      String element = path.pop();
      if(element == null){
        //element is null, so it is the tree root...thus, use the bag data folder as root
        element = viewName;
      }
      if(builder.length() != 0){
        builder = builder.insert(0, element + "/");
      } else{
        builder = builder.append(element);
      }
    }
    return builder.toString();
  }

  private boolean checkNode(IDataOrganizationNode node, String lfn, Stack<String> path){
    boolean result = false;
    if(node instanceof ICollectionNode){
      path.push(node.getName());
      for(IDataOrganizationNode child : ((ICollectionNode) node).getChildren()){
        result = checkNode(child, lfn, path);

        if(result){
          break;
        }
      }
      if(!result){
        path.pop();
      }
    } else if(node instanceof IFileNode){
      if(((IFileNode) node).getLogicalFileName().getStringRepresentation().equals(lfn)){
        path.push(node.getName());
        result = true;
      }
    }
    return result;
  }

}

class PayloadElement{

  private final String payloadPath;
  private final String basePath;
  private final String bagDataPath;

  public PayloadElement(String payloadPath, String basePath, String bagDataPath){
    this.payloadPath = payloadPath;
    this.basePath = basePath;
    this.bagDataPath = bagDataPath;
  }

  public String getPayloadPath(){
    return payloadPath;
  }

  public String getBagDataPath(){
    return bagDataPath;
  }

  public String getBasePath(){
    return basePath;
  }

}
