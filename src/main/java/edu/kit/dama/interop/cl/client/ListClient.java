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
import edu.kit.dama.interop.cl.command.ListCommand;
import edu.kit.dama.interop.util.AnsiUtil;
import edu.kit.dama.interop.util.StringUtils;
import edu.kit.dama.mdm.base.DigitalObject;
import edu.kit.dama.mdm.base.Investigation;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.dama.mdm.core.exception.EntityNotFoundException;
import edu.kit.dama.mdm.dataorganization.entity.core.ICollectionNode;
import edu.kit.dama.mdm.dataorganization.entity.core.IFileTree;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizer;
import edu.kit.dama.mdm.dataorganization.service.core.DataOrganizerFactory;
import edu.kit.dama.staging.util.DataOrganizationUtils;
import edu.kit.dama.util.Constants;
import edu.kit.jcommander.generic.status.CommandStatus;
import edu.kit.jcommander.generic.status.Status;
import java.util.List;
import java.util.ResourceBundle;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;

/**
 *
 * @author jejkal
 */
public class ListClient{

  private final static IMetaDataManager MDM = MetaDataManagement.getMetaDataManagement().getMetaDataManager();
  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.cl.client.MessageBundle");

  private static String investigationId = null;
  private static String userId = Constants.WORLD_USER_ID;
  private static String groupId = Constants.WORLD_GROUP_ID;

  public static CommandStatus execute(ListCommand params){
    CommandStatus status = new CommandStatus(Status.SUCCESSFUL);

    boolean finished = false;
    try{
      userId = params.userId;
      groupId = params.groupId;
      investigationId = params.investigationId;
      AnsiUtil.printInfo(MESSAGES.getString("init_repo_access"));

      init();

      doListing();
      finished = true;
    } catch(Exception ex){
      ex.printStackTrace();
      AnsiUtil.printError(MESSAGES.getString("list_failed"), ex);
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

  private static void doListing() throws Exception{
    AnsiUtil.printInfo(MESSAGES.getString("obtaining_investigation"), investigationId);
    Investigation destination = MDM.findSingleResult("SELECT o FROM Investigation o WHERE o.investigationId=?1", new Object[]{Long.parseLong(investigationId)}, Investigation.class);
    if(destination == null){
      throw new EntityNotFoundException(StringUtils.substitute(MESSAGES.getString("investigation_not_found"), investigationId));
    }

    List<DigitalObject> listing = MDM.findResultList("SELECT o FROM DigitalObject o WHERE o.investigation.investigationId=?1", new Object[]{destination.getInvestigationId()}, DigitalObject.class);
    AnsiUtil.printTextColored(MESSAGES.getString("listing_header"), Ansi.Color.WHITE, Integer.toString(listing.size()), Long.toString(destination.getInvestigationId()));
    DataOrganizer org = DataOrganizerFactory.getInstance().getDataOrganizer();

    for(DigitalObject object : listing){
      IFileTree tree = org.loadFileTree(object.getDigitalObjectId(), Constants.DEFAULT_VIEW);
      tree.setDigitalObjectId(object.getDigitalObjectId());
      AnsiUtil.printInfo(MESSAGES.getString("object_listing_line"), Long.toString(object.getBaseId()), object.getLabel());
      System.out.println(ansi().fg(Ansi.Color.WHITE));
      if(tree != null){
        IFileTree root = DataOrganizationUtils.copyTree(tree);
        DataOrganizationUtils.printTree(root.getRootNode(), true, System.out);
      } else{
        System.out.println("- No data associated, yet - ");
      }
      System.out.println(ansi().reset());
    }

  }

  private static void destroy(){
    MDM.close();
  }

}
