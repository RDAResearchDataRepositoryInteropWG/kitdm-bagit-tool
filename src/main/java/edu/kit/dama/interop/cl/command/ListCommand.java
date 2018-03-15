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
import edu.kit.dama.interop.cl.client.ListClient;
import edu.kit.jcommander.generic.parameter.CommandLineParameters;
import edu.kit.jcommander.generic.status.CommandStatus;

/**
 *
 * @author jejkal
 */
@Parameters(commandNames = "list", commandDescription = "Performs a listing of all digital object fin one investigation.")
public class ListCommand extends CommandLineParameters{

  @Parameter(names = {"-i", "--investigationId"}, description = "Parent investigation id the objects will be listed from.", required = true)
  public String investigationId = null;
  @Parameter(names = {"-u", "--userId"}, description = "The user id of the accessing user.", required = true)
  public String userId = null;
  @Parameter(names = {"-g", "--groupId"}, description = "The group id used for access.", required = true)
  public String groupId = null;

  /**
   * Default constructor.
   */
  public ListCommand(){
    super("list");
  }

  @Override
  public CommandStatus executeCommand(){
    return ListClient.execute(this);
  }
}
