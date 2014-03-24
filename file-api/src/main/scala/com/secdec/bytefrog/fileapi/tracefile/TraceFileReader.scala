/*
 * bytefrog: a tracing framework for the JVM. For more information
 * see http://code-pulse.com/bytefrog
 *
 * Copyright (C) 2014 Applied Visions - http://securedecisions.avi.com
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

package com.secdec.bytefrog.fileapi.tracefile

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import scala.util.Try

import com.secdec.bytefrog.fileapi.io.IOUtils._
import com.secdec.bytefrog.fileapi.io.zip.ZipEntryHandler
import com.secdec.bytefrog.fileapi.io.zip.ZipStreamReader
import com.secdec.bytefrog.fileapi.tracefile.entry.TraceFileEntry
import TraceFileBuilder._

object TraceFileReader {

	/** Read the given `in` file to see if it is a trace file
	  * with a trace manifest. If so, return that manifest.
	  */
	def readTraceManifest(in: File): Option[TraceManifest] = {
		val detectorHandler = new TraceFileDetector
		val reader = new ZipStreamReader(detectorHandler)
		try {
			reader.read(in)
			detectorHandler.foundTraceManifest
		} catch {
			case e: IOException => None
		}
	}

	/** Checks if the contents of the given InputStream represent a
	  * Trace File. If the data is a ZIP file, containing a "trace.manifest"
	  * entry, which contains the appropriate contents (which are
	  * auto-generated by TraceFileBuilder), this method will return `true`.
	  * In all other cases, it will return `false`.
	  *
	  * This method automatically closes `in` when it finishes.
	  */
	def isTraceFile(in: File): Boolean = readTraceManifest(in).isDefined

	/** Reads the contents of the given `in` stream, assuming that it
	  * represents a Trace File. Entries in the file will be sent to
	  * the provided `handler` for processing.
	  *
	  * This method automatically closes `in` when it finishes.
	  */
	def readTraceFile(in: File)(handler: (String, InputStream) => Unit): Unit = {
		val delegator = new TraceDelegatingHandler(handler)
		val reader = new ZipStreamReader(delegator)
		reader.read(in)
	}

	/** Read in the contents of a single entry in the trace file.
	  */
	def readTraceEntry[T](file: File, entry: TraceFileEntry[_])(doRead: InputStream => T): Option[T] = withTraceFile(file) { zip =>
		Option { zip getEntry entry.path } map { entry =>
			val stream = zip getInputStream entry
			try {
				doRead(stream)
			} finally {
				stream.close
			}
		}
	}

	/** A ZipEntryHandler that will look for a "trace manifest" entry that
	  * was auto-generated by a TraceFileBuilder.
	  */
	private class TraceFileDetector extends ZipEntryHandler {
		var foundTraceManifest: Option[TraceManifest] = None

		def handleZipEntry(entry: ZipEntry, contents: InputStream): Unit = try {
			entry.getName match {
				case `manifestFilename` => {
					for {
						manOpt <- Try { TraceManifest.readFrom(contents) }
						man <- manOpt
					} foundTraceManifest = Some(man)
				}
				case _ =>
			}
		} catch {
			case e: IOException =>
		}
	}

	/** A ZipEntryHandler that delegates to an internal handler for all entries.
	  */
	private class TraceDelegatingHandler(delegate: (String, InputStream) => Unit) extends ZipEntryHandler {

		def handleZipEntry(entry: ZipEntry, contents: InputStream): Unit = {
			delegate(entry.getName, contents)
		}
	}

	private def withTraceFile[T](file: File)(body: ZipFile => T): T = {
		val zip = new ZipFile(file)
		try {
			body(zip)
		} finally {
			zip.close
		}
	}

}