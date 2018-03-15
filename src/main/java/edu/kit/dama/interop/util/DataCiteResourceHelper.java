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
package edu.kit.dama.interop.util;

import edu.kit.dama.entities.dc40.Identifier;
import edu.kit.dama.entities.dc40.UnknownInformationConstants;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import org.datacite.schema.kernel_4.Resource;
import org.datacite.schema.kernel_4.TitleType;

/**
 *
 * @author jejkal
 */
public final class DataCiteResourceHelper{

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("edu.kit.dama.interop.util.MessageBundle");

  public static String getIdentifier(Resource resource){
    String resourceIdentifier = resource.getIdentifier().getValue();
    AnsiUtil.printInfo(MESSAGES.getString("checking_for_placeholder_identifier"), resourceIdentifier);
    if(resourceIdentifier != null){
      for(UnknownInformationConstants constant : UnknownInformationConstants.values()){
        if(constant.getValue().equals(resourceIdentifier)){
          AnsiUtil.printInfo(MESSAGES.getString("placeholder_identifier_found"), resourceIdentifier);
          resourceIdentifier = null;
          break;
        }
      }
    } else{
      AnsiUtil.printInfo(MESSAGES.getString("no_identifier_found"), resourceIdentifier);
    }

    if(resourceIdentifier == null){
      AnsiUtil.printInfo(MESSAGES.getString("checking_for_internal_identifier"));
      if(resource.getAlternateIdentifiers() != null){
        for(Resource.AlternateIdentifiers.AlternateIdentifier id : resource.getAlternateIdentifiers().getAlternateIdentifier()){
          if(Identifier.IDENTIFIER_TYPE.INTERNAL.toString().equals(id.getAlternateIdentifierType())){
            resourceIdentifier = id.getValue();
            break;
          }
        }
      }
    }
    return resourceIdentifier;
  }

  public static String getTitle(Resource resource){
    return getTitle(resource, Arrays.asList(TitleType.TRANSLATED_TITLE, TitleType.SUBTITLE, TitleType.ALTERNATIVE_TITLE, TitleType.OTHER));
  }

  public static String getTitle(Resource resource, List<TitleType> titlePriority){
    for(TitleType type : titlePriority){
      String titleOfType = getTitle(resource, type);
      if(titleOfType != null){
        return titleOfType;
      }
    }
    return null;
  }

  public static String getTitle(Resource resource, TitleType titleType){
    for(Resource.Titles.Title title : resource.getTitles().getTitle()){
      //if type is OTHER allow to return title without provided type
      if(title.getTitleType() == null && titleType.equals(TitleType.OTHER)){
        return title.getValue();
      }
      if(titleType.equals(title.getTitleType())){
        return title.getValue();
      }
    }
    return null;
  }

}
