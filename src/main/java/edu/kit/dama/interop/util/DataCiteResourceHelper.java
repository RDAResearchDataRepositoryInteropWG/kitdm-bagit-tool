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
import edu.kit.dama.mdm.base.DigitalObject;
import java.util.Arrays;
import java.util.List;
import org.datacite.schema.kernel_4.Resource;
import org.datacite.schema.kernel_4.TitleType;

/**
 *
 * @author jejkal
 */
public final class DataCiteResourceHelper{

  public static String getIdentifier(Resource resource){
    String resourceIdentifier = resource.getIdentifier().getValue();
    try{
      UnknownInformationConstants.valueOf(resourceIdentifier);
      resourceIdentifier = null;
    } catch(IllegalArgumentException ex){
      //unknown value, identifier can be used
    }

    if(resourceIdentifier == null){

      for(Resource.AlternateIdentifiers.AlternateIdentifier id : resource.getAlternateIdentifiers().getAlternateIdentifier()){
        if(Identifier.IDENTIFIER_TYPE.INTERNAL.toString().equals(id.getAlternateIdentifierType())){
          resourceIdentifier = id.getValue();
          break;
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
      if(titleType.equals(title.getTitleType())){
        return title.getValue();
      }
    }
    return null;
  }

}
