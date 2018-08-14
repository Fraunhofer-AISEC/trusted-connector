/*-
 * ========================LICENSE_START=================================
 * rat-repository
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
 * %%
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
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.attestation;

public class Start {
  private static RemoteAttestationServer ratServer;
  private static String version = "1.1.0-SNAPSHOT";
  private static String host = "127.0.0.1";
  private static String url = "configurations/check";
  private static int port = 31337;

  public static void main(String[] args) throws Exception {
    switch (args.length) {
      case 2:
        firstArg(args);
        run();
        break;
      case 4:
        firstArg(args);
        secondArg(args);
        run();
        break;
      default:
        System.out.println("IDS: Remote Attestation Repository v" + version);
        System.out.println("-----------------------------------------");
        System.out.println(
            "usage:\n\tjava -jar rat-repository-1.1.0-SNAPSHOT.jar [-p Port] [-h Host]");
        System.out.println(
            "example:\n\tjava -jar rat-repository-1.1.0-SNAPSHOT.jar -p 31337 -h 127.0.0.1");
        System.out.println("");
        break;
    }
  }

  private static void firstArg(String[] args) {
    if (args[0].equals("-p")) {
      if (isInteger(args[1])) {
        port = Integer.parseInt(args[1]);
      } else {
        System.out.println("error: " + args[1] + " is not a port number!");
      }
    } else if (args[0].equals("-h")) {
      host = args[1];
    } else {
      System.out.println("error: " + args[1] + " is not a valid option!");
    }
  }

  private static void secondArg(String[] args) {
    if (args[2].equals("-p")) {
      if (isInteger(args[3])) {
        port = Integer.parseInt(args[3]);
      } else {
        System.out.println("error: " + args[3] + " is not a port number!");
      }
    } else if (args[2].equals("-h")) {
      host = args[3];
    } else {
      System.out.println("error: " + args[3] + " is not a valid option!");
    }
  }

  private static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    } catch (NullPointerException e) {
      return false;
    }
    return true;
  }

  private static void run() {
    ratServer = new RemoteAttestationServer(host, url, port);
    try {
      ratServer.start();
      ratServer.join();
    } finally {
      ratServer.stop();
    }
  }
}
