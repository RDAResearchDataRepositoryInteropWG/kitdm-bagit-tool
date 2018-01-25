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
package edu.kit.dama.interop.cl.client;

import edu.kit.dama.authorization.entities.GroupId;
import edu.kit.dama.authorization.entities.Role;
import edu.kit.dama.authorization.entities.UserId;
import edu.kit.dama.authorization.entities.impl.AuthorizationContext;
import edu.kit.dama.authorization.exceptions.EntityAlreadyExistsException;
import edu.kit.dama.commons.types.DigitalObjectId;
import edu.kit.dama.interop.cl.command.ImportCommand;
import edu.kit.dama.interop.impl.BMDTagFileCreator;
import edu.kit.dama.interop.impl.DataCiteTagFileCreator;
import edu.kit.dama.interop.util.AnsiUtil;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.interop.util.StringUtils;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.base.Investigation;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.dama.mdm.core.exception.EntityNotFoundException;
import edu.kit.dama.mdm.dataorganization.entity.core.ICollectionNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IDataOrganizationNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IFileTree;
import edu.kit.dama.mdm.dataorganization.entity.impl.client.FileTree;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizer;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizerFactory;
import edu.kit.dama.staging.entities.TransferClientProperties;
import edu.kit.dama.staging.entities.ingest.INGEST_STATUS;
import edu.kit.dama.staging.entities.ingest.IngestInformation;
import edu.kit.dama.staging.services.impl.ingest.IngestInformationServiceLocal;
import edu.kit.dama.util.Constants;
import edu.kit.dama.util.ZipUtils;
import edu.kit.jcommander.generic.status.CommandStatus;
import edu.kit.jcommander.generic.status.Status;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.domain.Manifest;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;

/**
 *
 * @author jejkal
 */
public class ImportClient{

  private final static IMetaDataManager MDM = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.cl.client.MessageBundle");

  private static String investigationId = null;
  private static String userId = Constants.WORLD_USER_ID;
  private static String groupId = Constants.WORLD_GROUP_ID;

  private static Path source;

  private static boolean allowOverwrite = false;

  public static CommandStatus execute(ImportCommand params){
    CommandStatus status = new CommandStatus(Status.SUCCESSFUL);

    boolean finished = false;
    try{
      userId = params.userId;
      groupId = params.groupId;
      AnsiUtil.printInfo(MESSAGES.getString("init_repo_access"));

      //System.out.println(ansi().fg(Ansi.Color.GREEN).a("Initializing repository access...").reset());
      init();
      source = Paths.get(params.source);
      allowOverwrite = params.allowOverwrite;

      if(!Files.exists(source)){
        AnsiUtil.printInfo(MESSAGES.getString("source_not_exist"), source.toAbsolutePath().toString());
        status.setStatusCode(Status.FAILED);
        return status;
      }

      investigationId = params.investigationId;

      AnsiUtil.printInfo(MESSAGES.getString("starting_import"));
      doImport();
      finished = true;
    } catch(Exception ex){
      AnsiUtil.printError(MESSAGES.getString("import_failed"), ex);
      status = new CommandStatus(Status.FAILED, ex, null);
    } finally{
      destroy();
      if(!finished){
        //unhandled error
        AnsiUtil.printError(MESSAGES.getString("unhandled_error"));
        status = new CommandStatus(Status.FAILED);
      }
    }

    return status;
  }

  private static void init(){
    MDM.setAuthorizationContext(new AuthorizationContext(new UserId(userId), new GroupId(groupId), Role.MANAGER));
  }

  private static void doImport() throws Exception{
    AnsiUtil.printInfo(MESSAGES.getString("obtaining_investigation"), investigationId);
    Investigation destination = MDM.findSingleResult("SELECT o FROM Investigation o WHERE o.investigationId=?1", new Object[]{Long.parseLong(investigationId)}, Investigation.class);
    if(destination == null){
      throw new EntityNotFoundException(StringUtils.substitute(MESSAGES.getString("investigation_not_found"), investigationId));
    }

    Path bagRoot = source;

    if(!Files.isDirectory(source)){
      String bagName = source.getName(source.getNameCount() - 1).toString();
      bagName = bagName.substring(0, bagName.indexOf("."));
      AnsiUtil.printInfo(MESSAGES.getString("import_from_zip"));
      Path tempPath = Paths.get(".", "tmp");
      if(Files.exists(tempPath)){
        AnsiUtil.printWarning(MESSAGES.getString("temp_bag_location_already_exists"), tempPath.toString());
        Files.walk(tempPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      } else{
        AnsiUtil.printInfo(MESSAGES.getString("create_temp_bag_location"), tempPath.toString());
        Files.createDirectories(tempPath);
      }

      AnsiUtil.printInfo(StringUtils.substitute(MESSAGES.getString("unzip_bag"), source.toString()));
      ZipUtils.unzip(source.toFile(), tempPath.toFile());
      bagRoot = Paths.get(tempPath.toString(), bagName + "/");
    }

    AnsiUtil.printInfo(MESSAGES.getString("load_bag_from_folder"), bagRoot.toString());

    BagBuilder builder = BagBuilder.load(bagRoot.toAbsolutePath());

    //quick check profile conformance before starting to fetch anything
    builder.validateProfileConformance();

    List<FetchItem> fetchItems = builder.getBag().getItemsToFetch();
    if(!fetchItems.isEmpty()){
      AnsiUtil.printInfo(MESSAGES.getString("fetching_items"), Integer.toString(fetchItems.size()));

      for(FetchItem item : fetchItems){
        //check for URL type ... HTTP can be fetched, others might be kept as they are
        //make fetch optional? only check size?
        AnsiUtil.printInfo(MESSAGES.getString("fetching_item"), item.getPath().toString());
        URL fetchUrl = item.getUrl();
        Path fileDestination = bagRoot.toAbsolutePath().getParent().resolve(item.getPath());
        AnsiUtil.printInfo(MESSAGES.getString("creating_folder_structure"), fileDestination.getParent().toString());
        Files.createDirectories(fileDestination.getParent());
        AnsiUtil.printInfo(MESSAGES.getString("reading_data_from_url"), fetchUrl.toString());
        InputStream in = fetchUrl.openConnection().getInputStream();
        java.nio.file.Files.copy(
                in,
                fileDestination,
                StandardCopyOption.REPLACE_EXISTING);
        AnsiUtil.printInfo(MESSAGES.getString("file_fetched"));
        IOUtils.closeQuietly(in);
      }
    }

    //detailed validation of checksums after fetching all data
    AnsiUtil.printInfo(MESSAGES.getString("validating_bag"));
    builder.validateChecksums(true);
    AnsiUtil.printInfo(MESSAGES.getString("validation_successful"));

    Path bmdPath = null;
    Path datacitePath = null;
    AnsiUtil.printInfo(MESSAGES.getString("searching_consumable_metadata"));

    for(Manifest manifest : builder.getBag().getTagManifests()){
      Map<Path, String> tagfileHashes = manifest.getFileToChecksumMap();

      for(Entry<Path, String> entry : tagfileHashes.entrySet()){
        Path tagfile = bagRoot.toAbsolutePath().getParent().resolve(entry.getKey());
        if(bmdPath == null && "bmd.xml".equals(tagfile.getName(tagfile.getNameCount() - 1).toString())){
          bmdPath = tagfile;
        } else if(datacitePath == null && "datacite.xml".equals(tagfile.getName(tagfile.getNameCount() - 1).toString())){
          datacitePath = tagfile;
        }
      }
      if(bmdPath != null && datacitePath != null){
        break;
      }
    }

    boolean isKitdmBag = false;
    DigitalObject theObject = null;
    if(bmdPath != null){
      //bmd found, use it directly
      AnsiUtil.printInfo(MESSAGES.getString("kitdm_bag_detected"), bmdPath.toString());
      theObject = BMDTagFileCreator.createInstance().parseObjectFromTagFile(bmdPath, builder.getBag());
      //reset base id
      theObject.setBaseId(null);
      //set KITDM Bag flag for detailed processing
      isKitdmBag = true;
    } else if(datacitePath != null){
      //"only" datacite metadata found, create object from it
      AnsiUtil.printInfo(MESSAGES.getString("non_kitdm_bag_detected"), datacitePath.toString());
      theObject = DataCiteTagFileCreator.createInstance().parseObjectFromTagFile(datacitePath, builder.getBag());
    } else{
      //no bmd, no datacite metadata...unable to read bag.
      throw new Exception(MESSAGES.getString("no_consumable_metadata_found"));
    }

    if(theObject != null){
      //set existing object 
      if(!MDM.findResultList("SELECT o FROM DigitalObject o WHERE o.digitalObjectIdentifier=?1", new Object[]{theObject.getDigitalObjectIdentifier()}, DigitalObject.class).isEmpty()){
        if(allowOverwrite){
          AnsiUtil.printWarning(MESSAGES.getString("duplicate_object_identifier"));
          theObject.setDigitalObjectId(new DigitalObjectId(UUID.randomUUID().toString()));
          AnsiUtil.printWarning(MESSAGES.getString("alternate_object_identifier_assigned"), theObject.getDigitalObjectIdentifier());
        } else{
          throw new EntityAlreadyExistsException(StringUtils.substitute(MESSAGES.getString("identifier_overwrite_disabled"), theObject.getDigitalObjectIdentifier()));
        }
      }
      theObject.setInvestigation(destination);
      theObject.setVisible(Boolean.TRUE);
      AnsiUtil.printWarning(MESSAGES.getString("writing_digital_object_to_database"));
      DigitalObject result = MDM.save(theObject);
      if(result.getBaseId() == null){
        throw new Exception(MESSAGES.getString("failed_to_write_digital_object_to_database"));
      }
    } else{
      throw new Exception(MESSAGES.getString("failed_to_create_digital_object_from_metadata"));
    }

    AnsiUtil.printInfo(MESSAGES.getString("preparing_ingest"));
    //create ingest
    TransferClientProperties props = new TransferClientProperties();
    //@TODO configured localstaging AP necessary
    props.setStagingAccessPointId("localstaging");

    IngestInformation ingestInfo = IngestInformationServiceLocal.getSingleton().prepareIngest(theObject.getDigitalObjectId(), props, MDM.getAuthorizationContext());

    //have base Url
    String stagingUrl = ingestInfo.getStagingUrl();
    AnsiUtil.printInfo(MESSAGES.getString("ingest_data_to_url"), stagingUrl);

    Path stagingPath = Paths.get(URI.create(stagingUrl));

    Path dataPath = stagingPath.resolve("data");
    Path generatedPath = stagingPath.resolve("generated");

    Path bagPayload = bagRoot.resolve("data");
    Path bagMetadata = bagRoot.resolve("metadata");

    AnsiUtil.printInfo(MESSAGES.getString("copy_payload_to_data_location"), bagPayload.toString(), dataPath.toString());
    FileUtils.copyDirectory(bagPayload.toFile(), dataPath.toFile());
    AnsiUtil.printInfo(MESSAGES.getString("copy_metadata_to_generated_location"), bagMetadata.toString(), generatedPath.toString());
    FileUtils.copyDirectory(bagMetadata.toFile(), generatedPath.toFile());
    AnsiUtil.printInfo(MESSAGES.getString("setting_file_transfer_finished"));

    int affectedEntities = IngestInformationServiceLocal.getSingleton().updateStatus(ingestInfo.getId(), INGEST_STATUS.PRE_INGEST_FINISHED.getId(), null, MDM.getAuthorizationContext());

    if(affectedEntities != 1){
      AnsiUtil.printError(MESSAGES.getString("failed_to_update_ingest_status"));
    } else{
      AnsiUtil.printInfo(MESSAGES.getString("pre_ingest_complete"), INGEST_STATUS.PRE_INGEST_FINISHED.toString());
    }

    ingestInfo = IngestInformationServiceLocal.getSingleton().getIngestInformationById(ingestInfo.getId(), MDM.getAuthorizationContext());

    System.out.print(ansi().fg(Ansi.Color.GREEN).a(MESSAGES.getString("ingest_running")));
    long counter = 30;
    while(!ingestInfo.getStatusEnum().isFinalState()){
      try{
        Thread.sleep(1000);
      } catch(InterruptedException ex){
      }

      counter--;
      System.out.print(ansi().a("."));
      ingestInfo = IngestInformationServiceLocal.getSingleton().getIngestInformationById(ingestInfo.getId(), MDM.getAuthorizationContext());
      if(counter == 0){
        AnsiUtil.printWarning(MESSAGES.getString("long_running_ingest_detected"));
      }
    }

    System.out.println();

    if(!ingestInfo.getStatusEnum().isErrorState()){
      AnsiUtil.printInfo(MESSAGES.getString("ingest_finished"));
    } else{
      throw new Exception(StringUtils.substitute(MESSAGES.getString("ingest_failed"), ingestInfo.getErrorMessage(), ingestInfo.getStatusEnum().toString()));
    }

    if(isKitdmBag){
      AnsiUtil.printInfo(MESSAGES.getString("restoring_data_organization"));
      DataOrganizer organizer = DataOrganizerFactory.getInstance().getDataOrganizer();

      IFileTree defaultView = organizer.loadFileTree(theObject.getDigitalObjectId(), Constants.DEFAULT_VIEW);

      for(IDataOrganizationNode child : defaultView.getRootNode().getChildren()){
        AnsiUtil.printInfo(MESSAGES.getString("restoring_view"), child.getName());
        IFileTree viewTree = new FileTree();
        viewTree.setDigitalObjectId(theObject.getDigitalObjectId());
        viewTree.setViewName(child.getName());
        if(child instanceof ICollectionNode){
          ((ICollectionNode) child).getChildren().forEach((childChild) -> {
            viewTree.getRootNode().addChild(childChild);
          });
          organizer.createFileTree(viewTree);
          AnsiUtil.printInfo(MESSAGES.getString("successfully_restored_view"), child.getName());
        } else{
          AnsiUtil.printWarning(MESSAGES.getString("file_node_detected"), child.getName());
        }
      }
    }

    AnsiUtil.printInfo(MESSAGES.getString("import_completed"), theObject.getDigitalObjectIdentifier());
  }

  private static void destroy(){
    MDM.close();
  }
}
