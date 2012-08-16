package edu.toronto.cs.ontools.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class LocalFileUtils {
	public static File getInputFileHandler(String inputLocation,
			boolean forceUpdate) {
		try {
			File result = new File(inputLocation);
			if (!result.exists()) {
				String name = inputLocation.substring(inputLocation
						.lastIndexOf('/') + 1);
				result = getTemporaryFile(name);
				if (!result.exists()) {
					result.createNewFile();
					BufferedInputStream in = new BufferedInputStream((new URL(
							inputLocation)).openStream());
					OutputStream out = new FileOutputStream(result);
					IOUtils.copy(in, out);
					out.flush();
					out.close();
				}
			}
			return result;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static File getTemporaryFile(String name) {
		return getInternalFile(name, "tmp");
	}

	public static File getInternalFile(String name, String dir) {
		File parent = new File("", dir);
		if (!parent.exists()) {
			parent.mkdirs();
		}
		return new File(parent, name);
	}
}
