package net.yourhome.server.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

public class ZipFile {

	private FileOutputStream fos;
	private ZipOutputStream zos;
	private List<String> zipFileList = new ArrayList<String>();
	private static Logger log = Logger.getLogger("net.yourhome.server.base.ZIPFILE");

	public ZipFile(String fileName) throws FileNotFoundException {
		File f = new File(fileName);
		f.delete();

		fos = new FileOutputStream(fileName);
		zos = new ZipOutputStream(fos);
	}

	public void addTextFile(String fileName, String textContent) throws IOException {
		ZipEntry zipEntry = new ZipEntry(fileName);
		zos.putNextEntry(zipEntry);
		zos.write(textContent.getBytes());
		zos.closeEntry();
	}

	public void addFile(String relativefileName, String baseFolder) throws IOException {
		String fileName = baseFolder + "/" + relativefileName;
		if (!zipFileList.contains(fileName)) {
			zipFileList.add(fileName);
			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			ZipEntry zipEntry = new ZipEntry(relativefileName);
			zos.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}
			zos.closeEntry();
			fis.close();
		}
	}

	public void close() {
		if (zos != null)
			try {
				zos.finish();
				zos.close();
				zipFileList.clear();
			} catch (IOException e) {
				log.error("Exception occured: ", e);
			}
	}
}
