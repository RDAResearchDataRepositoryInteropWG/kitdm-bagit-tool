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

import edu.kit.dama.util.StackTraceUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.fusesource.jansi.Ansi;
import static org.fusesource.jansi.Ansi.ansi;

/**
 *
 * @author jejkal
 */
public class AnsiUtil{

  public static void printInfo(String message, String... highlighted){
    printText(message, Ansi.Color.GREEN, highlighted);
  }

  public static void printWarning(String message, String... highlighted){
    printText(message, Ansi.Color.YELLOW, highlighted);
  }

  public static void printError(String message, String... highlighted){
    printError(message, null, highlighted);
  }

  public static void printError(String message, Throwable t, String... highlighted){
    printText(message, Ansi.Color.RED, highlighted);
    if(Objects.nonNull(t)){
      printText(StackTraceUtil.getStackTrace(t), Ansi.Color.RED);
    }
  }

  public static void printTextColored(String message, Ansi.Color foreground, String... highlighted){
    Map valuesMap = new HashMap();
    for(int i = 0; i < highlighted.length; i++){
      valuesMap.put(Integer.toString(i + 1), Ansi.ansi().bold().a(highlighted[i]).boldOff().toString());
    }
    System.out.println(ansi().fg(foreground).a(StrSubstitutor.replace(message, valuesMap)).reset());
  }

  private static void printText(String message, Ansi.Color foreground, String... highlighted){
    Map valuesMap = new HashMap();
    for(int i = 0; i < highlighted.length; i++){
      valuesMap.put(Integer.toString(i + 1), Ansi.ansi().bold().a(highlighted[i]).boldOff().toString());
    }
    System.out.println(ansi().fg(foreground).a(StrSubstitutor.replace(message, valuesMap)).reset());
  }

  public static void main(String[] args){
//    
//    Map valuesMap = new HashMap();
//    valuesMap.put("1", Ansi.ansi().bold().a("first").boldOff().toString());
//    valuesMap.put("2", Ansi.ansi().bold().a("boy").boldOff().toString());
//
//    Ansi ansiText = ansi().fg(Ansi.Color.GREEN).a(StrSubstitutor.replace(test, valuesMap)).reset();
//    System.out.println(ansiText.toString());

    String test = "This is a ${1} good test ${2}.";
    String[] rep = new String[]{"first", "boy"};

    AnsiUtil.printInfo(test, rep);

    test = "This is a ${1} good test.";
    rep = new String[]{"first", "boy"};

    AnsiUtil.printInfo(test, rep);

    test = "This is a ${1} good test ${2}.";
    rep = new String[]{"first"};

    AnsiUtil.printInfo(test, rep);

    test = "${1} ${2}";
    rep = new String[]{"first", "second"};

    AnsiUtil.printInfo(test, rep);
  }
}
