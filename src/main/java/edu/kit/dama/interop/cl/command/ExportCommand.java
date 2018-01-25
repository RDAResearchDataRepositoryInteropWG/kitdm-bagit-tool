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
package edu.kit.dama.interop.cl.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import edu.kit.dama.interop.cl.client.ExportClient;
import edu.kit.dama.interop.util.BagBuilder;
import edu.kit.jcommander.generic.parameter.CommandLineParameters;
import edu.kit.jcommander.generic.status.CommandStatus;

/**
 *
 * @author jejkal
 */
@Parameters(commandNames = "export", commandDescription = "Performs the export of a digital object from a local repository.")
public class ExportCommand extends CommandLineParameters{

  @Parameter(names = {"-i", "--objectId"}, description = "Digital object identifier of the object to export.", required = true)
  public String digitalObjectId = null;

  @Parameter(names = {"-o", "--destination"}, description = "The destination folder the bag will be stored.", required = true)
  public String destination = null;

  @Parameter(names = {"-m", "--metadataFile"}, description = "Additional metadata file in standard properties format <key>:<value> written into bag-info.txt. This file must contain all metadata fields that are required by the used profile. "
          + "If at least one required property is missing, the export will fail. If the used profile does not require any mandatory properties, this argument can be skipped. ", required = false)
  public String metadataFile = null;

  @Parameter(names = {"-f", "--force"}, description = "Force the creation of the bag at the provided destination. Otherwise, the export will not be performed if destination exists.", required = false)
  public boolean force = false;

  @Parameter(names = {"-p", "--profile"}, description = "URL of the profile used to validate the exported bag.", required = false)
  public String profileUrl = BagBuilder.BAGIT_PROFILE_LOCATION;

  @Parameter(names = {"-z", "--zip"}, description = "Serialize the bag into a zip file.", required = false)
  public boolean zipOutput = false;

  /**
   * Default constructor.
   */
  public ExportCommand(){
    super("export");
  }

  @Override
  public CommandStatus executeCommand(){
    return ExportClient.execute(this);
  }
}
