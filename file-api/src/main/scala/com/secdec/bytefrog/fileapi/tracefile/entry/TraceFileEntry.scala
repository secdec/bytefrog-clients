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

package com.secdec.bytefrog.fileapi.tracefile.entry

import java.io.InputStream

import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder

trait TraceFileEntry[Item] {
	def path: String
	def reader: TraceFileEntryReader[Item]
	def writer(builder: TraceFileBuilder): TraceFileEntryWriter[Item]
}

trait TraceFileEntryReader[Item] {
	def read(content: InputStream)(callback: Item => Unit): Unit
}

trait TraceFileEntryWriter[Item] {
	def write(item: Item): Unit
	def finish(): Unit
}