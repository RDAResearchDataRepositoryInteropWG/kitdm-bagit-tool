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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.commons.io.FileUtils;

/**
 * Client implementation for exporting digital objects from KIT Data
 * Manager-based repositories into BagIt bags.
 *
 * @author jejkal
 */
public class ExportClient{

  private final static IMetaDataManager MDM = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.cl.client.MessageBundle");

  private static String digitalObjectId = null;
  private static String profileUrl = null;
  private static Path metadataFile = null;
  private static Path destination;
  private static boolean zipBag = false;

  public static CommandStatus execute(ExportCommand params){
    CommandStatus status = new CommandStatus(Status.SUCCESSFUL);
    boolean finished = false;
    try{
      AnsiUtil.printInfo(MESSAGES.getString("init_repo_access"));
      init();
      profileUrl = params.profileUrl;
      if(params.metadataFile != null){
        metadataFile = Paths.get(params.metadataFile);
      }

      AnsiUtil.printInfo(MESSAGES.getString("checking_profile_url"), profileUrl);
      AnsiUtil.printInfo(MESSAGES.getString("profile_url_valid"), new URL(profileUrl).toString());

      if(!Files.exists(metadataFile) || !Files.isReadable(metadataFile)){
        AnsiUtil.printError(MESSAGES.getString("metadata_not_found"), metadataFile.toAbsolutePath().toString());
        status.setStatusCode(Status.FAILED);
        return status;
      }

      destination = Paths.get(params.destination);
      if(Files.exists(destination) && params.force){
        AnsiUtil.printWarning(MESSAGES.getString("removing_existing_bag_root"), destination.toAbsolutePath().toString());
        Files.walk(destination, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      } else if(Files.exists(destination) && !params.force){
        AnsiUtil.printError(MESSAGES.getString("bag_root_exists_overwrite_forbidden"), destination.toAbsolutePath().toString());
        status.setStatusCode(Status.FAILED);
        return status;
      }
      AnsiUtil.printInfo(MESSAGES.getString("creating_bag_root"), destination.toAbsolutePath().toString());
      Files.createDirectories(destination);
      digitalObjectId = params.digitalObjectId;
      zipBag = params.zipOutput;
      AnsiUtil.printInfo(MESSAGES.getString("starting_export"));
      export();
      finished = true;
    } catch(Exception ex){
      AnsiUtil.printError(MESSAGES.getString("export_failed"), ex);
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
    MDM.setAuthorizationContext(AuthorizationContext.factorySystemContext());
  }

  private static void export() throws Exception{
    AnsiUtil.printInfo(MESSAGES.getString("obtaining_digital_object"), digitalObjectId);
    DigitalObject toExport = MDM.findSingleResult("SELECT o FROM DigitalObject o WHERE o.digitalObjectIdentifier=?1", new Object[]{digitalObjectId}, DigitalObject.class);
    if(toExport == null){
      throw new EntityNotFoundException(StringUtils.substitute(MESSAGES.getString("digital_object_not_found"), digitalObjectId));
    }

    //bag root preparation
    if(!Files.exists(destination)){
      AnsiUtil.printInfo(MESSAGES.getString("creating_bag_root_directory"), destination.toAbsolutePath().toString());
      Files.createDirectories(destination);
    } else{
      AnsiUtil.printWarning(MESSAGES.getString("skip_creating_bag_root_directory"), destination.toAbsolutePath().toString());
    }

    //Create bag and base properties
    AnsiUtil.printInfo(MESSAGES.getString("creating_bag_at_root"), destination.toString());
    BagBuilder builder = BagBuilder.create(destination.toAbsolutePath(), profileUrl);

    //add only external identifier as all other metadata elements must be added from the properties files provided via --metadata argument
    builder = builder.addMetadata("External-Identifier", digitalObjectId);

    //create and add metadata tagfiles
    DCTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    BMDTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    //add mets tagfile, which also includes adding all payload files
    METSTagFileCreator.createInstance().createAndAddTagFile(toExport, builder);
    //finally, create datacite metadata (must be at the end as it contains information created in beforehand)
    DataCiteTagFileCreator.createInstance(MDM.getAuthorizationContext().getUserId().toString()).createAndAddTagFile(toExport, builder);

    //store profile to bag    
    InputStream profileStream = new URL(profileUrl).openConnection().getInputStream();
    Path profilePath = Paths.get(builder.getBag().getRootDir().toAbsolutePath().toString(), "metadata", "profile", "profile.json");
    AnsiUtil.printInfo(MESSAGES.getString("writing_profile"), profileUrl, profilePath.toString());
    FileUtils.copyInputStreamToFile(profileStream, profilePath.toFile());
    builder.addTagfile(profilePath.toUri());

    //add payload oxum just for verification against profile
    final String payloadOxum = PathUtils.generatePayloadOxum(PathUtils.getDataDir(builder.getBag().getVersion(), destination));
    builder.getBag().getMetadata().upsertPayloadOxum(payloadOxum);

    if(metadataFile != null){
      Properties props = new Properties();
      props.load(Files.newInputStream(metadataFile));
      builder.validateAndAddMetadataProperties(props);
    }

    builder.validateProfileConformance();
    builder.validateChecksums(false);
    //write bag metadata to bag root
    AnsiUtil.printInfo(MESSAGES.getString("writing_bag_to"), destination.toString());
    builder.write(destination);

    //optional: Serialize bag to single zip file
    if(zipBag){
      Path zipDestination = Paths.get(destination.toString(), "../" + destination.getName(destination.getNameCount() - 1) + ".zip");
      AnsiUtil.printInfo(MESSAGES.getString("serializing_bag_to"), zipDestination.toAbsolutePath().toString());
      ZipUtils.zip(new File[]{destination.toFile()}, destination.toAbsolutePath().getParent().toString(), zipDestination.toFile());
    }
  }

  private static void destroy(){
    MDM.close();
  }
}
