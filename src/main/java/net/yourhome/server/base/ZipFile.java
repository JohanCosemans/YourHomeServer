/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.yourhome.server.base;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

	private FileOutputStream fos;
	private ZipOutputStream zos;
	private List<String> zipFileList = new ArrayList<String>();
	private static Logger log = Logger.getLogger("net.yourhome.server.base.ZIPFILE");

	public ZipFile(String fileName) throws FileNotFoundException {
		File f = new File(fileName);
		f.delete();

		this.fos = new FileOutputStream(fileName);
		this.zos = new ZipOutputStream(this.fos);
	}

	public void addTextFile(String fileName, String textContent) throws IOException {
		ZipEntry zipEntry = new ZipEntry(fileName);
		this.zos.putNextEntry(zipEntry);
		this.zos.write(textContent.getBytes());
		this.zos.closeEntry();
	}

	public void addFile(String relativefileName, String baseFolder) throws IOException {
		String fileName = baseFolder + "/" + relativefileName;
		if (!this.zipFileList.contains(fileName)) {
			this.zipFileList.add(fileName);
			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			ZipEntry zipEntry = new ZipEntry(relativefileName);
			this.zos.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				this.zos.write(bytes, 0, length);
			}
			this.zos.closeEntry();
			fis.close();
		}
	}

	public void close() {
		if (this.zos != null) {
			try {
				this.zos.finish();
				this.zos.close();
				this.zipFileList.clear();
			} catch (IOException e) {
				ZipFile.log.error("Exception occured: ", e);
			}
		}
	}
}
