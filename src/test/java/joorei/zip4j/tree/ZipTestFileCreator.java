package joorei.zip4j.tree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;

@SuppressWarnings("javadoc")
public class ZipTestFileCreator {
	private static final String TEST_RESOURCES_PATH = "src/test/resources/";
	private static final String TEST_DIRECTORY_PATH = TEST_RESOURCES_PATH + "directory";
	private static final String TEST_FILE_PATH = TEST_DIRECTORY_PATH + "/file.txt";

	public static final String ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES = TEST_RESOURCES_PATH + "archive_with_files_with_slashes_in_filenames.zip";
	public static final String DIR_D1 = "directory/";
	// TODO: add tests for no slashes at the end of directory names: this seems to be currently unsupported by zip4j
	// TODO: add tests for directory names containing slashes: currently it is unknown how to create ZIPs with these with zip4j
	public static final String FILE_F0 = "directory/file.txt";
	public static final String FILE_F1 = "a/directory/d";
	public static final String FILE_F2 = "a/directory/e";
	public static final String FILE_F3 = "directory/f/f";
	public static final String FILE_F4 = "directory/f/g";
	public static final String FILE_F5 = "h";
	public static final String FILE_F6 = "h///i";
	// TODO: add tests for slashes at the end of file names: this seems to be currently unsupported by zip4j

	public static void main(final String[] args) throws IOException {
		final ZipFile zipFile = new ZipFile(ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		addFolder(TEST_DIRECTORY_PATH, zipFile);
		addFile(TEST_FILE_PATH, FILE_F1, zipFile);
		addFile(TEST_FILE_PATH, FILE_F2, zipFile);
		addFile(TEST_FILE_PATH, FILE_F3, zipFile);
		addFile(TEST_FILE_PATH, FILE_F4, zipFile);
		addFile(TEST_FILE_PATH, FILE_F5, zipFile);
		addFile(TEST_FILE_PATH, FILE_F6, zipFile);
	}

	protected static void renameFile(final String oldPath, final String newPath, final ZipFile zipFile) throws IOException
	{
		final FileHeader fileHeader = zipFile.getFileHeader(oldPath);
		try (final InputStream is = zipFile.getInputStream(fileHeader);) {
			final ZipParameters parameters = new ZipParameters();
			parameters.setFileNameInZip(newPath);
			zipFile.addStream(is, parameters);
			zipFile.removeFile(fileHeader.getFileName());
		}
	}
	
	protected static void addFile(final String pathOutside, final String nameInside, final ZipFile zipFile) throws ZipException {
		final ZipParameters zipParameters = new ZipParameters();
		zipParameters.setFileNameInZip(nameInside);
		zipFile.addFile(pathOutside, zipParameters);
	}
	
	protected static void addFolder(final String pathOutside, final ZipFile zipFile) throws ZipException {
		final ZipParameters zipParameters = new ZipParameters();
		zipFile.addFolder(Paths.get(pathOutside).toFile(), zipParameters);
	}
}
