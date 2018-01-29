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

import edu.kit.dama.interop.util.AnsiUtil;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.dama.mdm.base.DigitalObject;
import gov.loc.repository.bagit.domain.Bag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import org.apache.commons.io.FilenameUtils;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;

/**
 *
 * @author jejkal
 */
public abstract class AbstractTagFileCreator{

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.impl.MessageBundle");

  private Path metadataPath = null;

  public void createAndAddTagFile(DigitalObject theObject, BagBuilder theBagBuilder) throws Exception{
    AnsiUtil.printInfo(MESSAGES.getString("creating_metadata"), getMetadataType());
    Path tagFile = createTagFile(theObject, theBagBuilder);
    AnsiUtil.printInfo(MESSAGES.getString("adding_tag_file"), tagFile.toString());
    theBagBuilder.addTagfile(tagFile.toUri());
  }

  public DigitalObject parseObjectFromTagFile(Path tagFile, Bag theBag) throws Exception{
    AnsiUtil.printInfo(MESSAGES.getString("reading_digital_object_from_tag_file"), tagFile.toString());
    return createDigitalObject(tagFile, theBag);
  }

  abstract Path createTagFile(DigitalObject theObject, BagBuilder theBag) throws Exception;

  abstract DigitalObject createDigitalObject(Path tagFile, Bag theBag) throws Exception;

  abstract String getMetadataType();

  public String normalizePath(Path path){
    return FilenameUtils.normalize(path.toAbsolutePath().toString());
  }

  public Path getMetadataPath(Bag theBag) throws IOException{
    if(metadataPath == null){
      metadataPath = Paths.get(theBag.getRootDir().toAbsolutePath().toString(), "metadata");
      if(!Files.exists(metadataPath)){
        AnsiUtil.printInfo(MESSAGES.getString("creating_metadata_directory"), metadataPath.toAbsolutePath().toString());
        Files.createDirectory(metadataPath);
      } else{
        AnsiUtil.printInfo(MESSAGES.getString("skip_creating_metadata_directory"), metadataPath.toAbsolutePath().toString());
      }
    }
    return metadataPath;
  }

}
