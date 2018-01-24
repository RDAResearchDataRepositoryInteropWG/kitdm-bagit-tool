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

import edu.kit.dama.authorization.entities.impl.AuthorizationContext;
import edu.kit.dama.interop.cl.command.ExportCommand;
import edu.kit.dama.interop.impl.BMDTagFileCreator;
import edu.kit.dama.interop.impl.DCTagFileCreator;
import edu.kit.dama.interop.impl.DataCiteTagFileCreator;
import edu.kit.dama.interop.impl.METSTagFileCreator;
import edu.kit.dama.interop.util.AnsiUtil;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.interop.util.StringUtils;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.dama.mdm.core.exception.EntityNotFoundException;
import edu.kit.dama.util.ZipUtils;
import edu.kit.jcommander.generic.status.CommandStatus;
import edu.kit.jcommander.generic.status.Status;
import gov.loc.repository.bagit.util.PathUtils;
import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Client implementation for exporting digital objects from KIT Data
 * Manager-based repositories into BagIt bags.
 *
 * @author jejkal
 */
public class ExportClient{

  private final static IMetaDataManager MDM = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
  private static final ResourceBundle messages = ResourceBundle.getBundle("edu.kit.dama.interop.cl.client.MessageBundle");

  private static String digitalObjectId = null;
  private static String profileUrl = null;
  private static Path metadataFile = null;
  private static Path destination;
  private static boolean zipBag = false;

  public static CommandStatus execute(ExportCommand params){
    CommandStatus status = new CommandStatus(Status.SUCCESSFUL);
    boolean finished = false;
    try{
      AnsiUtil.printInfo(messages.getString("init_repo_access"));
      init();
      profileUrl = params.profileUrl;
      if(params.metadataFile != null){
        metadataFile = Paths.get(params.metadataFile);
      }
      destination = Paths.get(params.destination);
      if(Files.exists(destination) && params.force){
        AnsiUtil.printWarning(messages.getString("removing_existing_bag_root"), destination.toAbsolutePath().toString());
        Files.walk(destination, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      } else if(Files.exists(destination) && !params.force){
        AnsiUtil.printError(messages.getString("bag_root_exists_overwrite_forbidden"), destination.toAbsolutePath().toString());
        status.setStatusCode(Status.FAILED);
        return status;
      }
      AnsiUtil.printInfo(messages.getString("creating_bag_root"), destination.toAbsolutePath().toString());
      Files.createDirectories(destination);
      digitalObjectId = params.digitalObjectId;
      zipBag = params.zipOutput;
      AnsiUtil.printInfo(messages.getString("starting_export"));
      export();
      finished = true;
    } catch(Exception ex){
      AnsiUtil.printError(messages.getString("export_failed"), ex);
      status = new CommandStatus(Status.FAILED, ex, null);
    } finally{
      destroy();
      if(!finished){
        //unhandled error
        AnsiUtil.printError(messages.getString("unhandled_error"));
        status = new CommandStatus(Status.FAILED);
      }
    }

    return status;
  }

  private static void init(){
    MDM.setAuthorizationContext(AuthorizationContext.factorySystemContext());
  }

  private static void export() throws Exception{
    AnsiUtil.printInfo(messages.getString("obtaining_digital_object"), digitalObjectId);
    DigitalObject toExport = MDM.findSingleResult("SELECT o FROM DigitalObject o WHERE o.digitalObjectIdentifier=?1", new Object[]{digitalObjectId}, DigitalObject.class);
    if(toExport == null){
      throw new EntityNotFoundException(StringUtils.substitute(messages.getString("digital_object_not_found"), digitalObjectId));
    }

    //bag root preparation
    if(!Files.exists(destination)){
      AnsiUtil.printInfo(messages.getString("creating_bag_root_directory"), destination.toAbsolutePath().toString());
      Files.createDirectories(destination);
    } else{
      AnsiUtil.printWarning(messages.getString("skip_creating_bag_root_directory"), destination.toAbsolutePath().toString());
    }

    //Create bag and base properties
    AnsiUtil.printInfo(messages.getString("creating_bag_at_root"), destination.toString());
    BagBuilder builder = BagBuilder.create(destination.toAbsolutePath(), profileUrl);

    if(metadataFile != null){
      Properties props = new Properties();
      props.load(Files.newInputStream(metadataFile));
      //builder.validateAndAddMetadataProperties(props);
    }

    builder = builder.addMetadata("Bagging-Date", new SimpleDateFormat("YYYY-MM-DD").format(new Date())).
            addMetadata("Source-Organization", "Karlsruhe Institute of Technology").
            addMetadata("Contact-Email", "thomas.jejkal@kit.edu").
            addMetadata("Contact-Phone", "+49 721 608-24042").
            addMetadata("External-Identifier", digitalObjectId).
            addMetadata("External-Description", "Digital object exported from KIT Data Manager");

    //create and add tagfiles
    DCTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    BMDTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    //add mets tagfile, which also includes adding all payload files
    METSTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    //finally, create datacite metadata (must be at the end as it contains information created in beforehand)
    DataCiteTagFileCreator.createInstance(MDM.getAuthorizationContext().getUserId().toString()).createAndAddTagFile(toExport, builder);
    //add payload oxum just for verification against profile
    final String payloadOxum = PathUtils.generatePayloadOxum(PathUtils.getDataDir(builder.getBag().getVersion(), destination));
    builder.getBag().getMetadata().upsertPayloadOxum(payloadOxum);

    builder.validateProfileConformance();
    builder.validateChecksums(false);
    //write bag metadata to bag root
    AnsiUtil.printInfo(messages.getString("writing_bag_to"), destination.toString());
    builder.write(destination);

    //optional: Serialize bag to single zip file
    if(zipBag){
      Path zipDestination = Paths.get(destination.toString(), "../" + destination.getName(destination.getNameCount() - 1) + ".zip");
      AnsiUtil.printInfo(messages.getString("serializing_bag_to"), zipDestination.toAbsolutePath().toString());
      ZipUtils.zip(new File[]{destination.toFile()}, destination.toAbsolutePath().getParent().toString(), zipDestination.toFile());
    }
  }

  private static void destroy(){
    MDM.close();
  }
}
