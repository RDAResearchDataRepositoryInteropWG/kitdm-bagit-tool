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
import edu.kit.dama.interop.cl.client.ImportClient;
import edu.kit.jcommander.generic.parameter.CommandLineParameters;
import edu.kit.jcommander.generic.status.CommandStatus;

/**
 *
 * @author jejkal
 */
@Parameters(commandNames = "import", commandDescription = "Performs the import of a bag into a digital object in a local repository.")
public class ImportCommand extends CommandLineParameters{

  @Parameter(names = {"-i", "--investigationId"}, description = "Parent investigation id the object will be assigned to.", required = true)
  public String investigationId = null;
  @Parameter(names = {"-u", "--userId"}, description = "The user id of the importing user.", required = true)
  public String userId = null;
  @Parameter(names = {"-g", "--groupId"}, description = "The group id the object will be associated with.", required = true)
  public String groupId = null;
  @Parameter(names = {"-s", "--source"}, description = "The source folder or file containing the bag to import.", required = true)
  public String source = null;
  @Parameter(names = {"-o", "--allowOverwrite"}, description = "Allow overwriting the digital object identifier with a new (internally) unique identifier if an object with the same identifier already exists in the repository. This feature is intended to be used only for debugging.", required = false)
  public boolean allowOverwrite = false;

  /**
   * Default constructor.
   */
  public ImportCommand(){
    super("import");
  }

  @Override
  public CommandStatus executeCommand(){
    return ImportClient.execute(this);
  }
}
