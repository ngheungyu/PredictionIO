/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.tools.imprt

import io.prediction.controller.Utils
import io.prediction.data.storage.Event
import io.prediction.data.storage.EventJson4sSupport
import io.prediction.data.storage.Storage
import io.prediction.tools.Runner
import io.prediction.workflow.WorkflowContext
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging
import org.json4s.native.Serialization._

case class FileToEventsArgs(
  env: String = "",
  logFile: String = "",
  appId: Int = 0,
  inputPath: String = "",
  verbose: Boolean = false,
  debug: Boolean = false)

object FileToEvents extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[FileToEventsArgs]("FileToEvents") {
      opt[String]("env") action { (x, c) =>
        c.copy(env = x)
      }
      opt[String]("log-file") action { (x, c) =>
        c.copy(logFile = x)
      }
      opt[Int]("appid") action { (x, c) =>
        c.copy(appId = x)
      }
      opt[String]("input") action { (x, c) =>
        c.copy(inputPath = x)
      }
      opt[Unit]("verbose") action { (x, c) =>
        c.copy(verbose = true)
      }
      opt[Unit]("debug") action { (x, c) =>
        c.copy(debug = true)
      }
    }
    parser.parse(args, FileToEventsArgs()) map { args =>
      WorkflowUtils.modifyLogging(verbose = args.verbose)
      @transient lazy implicit val formats = Utils.json4sDefaultFormats +
        new EventJson4sSupport.APISerializer
      val sc = WorkflowContext(
        mode = "Import",
        batch = "App ID " + args.appId,
        executorEnv = Runner.envStringToMap(args.env))
      val rdd = sc.textFile(args.inputPath)
      val events = Storage.getPEvents()
      events.write(rdd.map(read[Event](_)), args.appId)(sc)
    }
  }
}
