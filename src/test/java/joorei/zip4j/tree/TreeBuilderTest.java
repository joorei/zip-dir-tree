package joorei.zip4j.tree;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import joorei.zip4j.tree.TreeBuilder;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

/**
 * Test {@link TreeBuilder}
 */
@SuppressWarnings({"static-method", "javadoc"})
public class TreeBuilderTest {
	/**
	 * Test if the ignoreLastN parameter works with 0 as value.
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_isSame() {
		final String input = "foo/";
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter(input, '/', 0);
		// String instance reusage is assumed, hence assertSame instead of assertEquals is used.
		assertSame(input, output);
	}

	/**
	 * Test empty string return if input string does not contain the search-character
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_isEmptyString() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("foo", '/', 0);
		// String instance reusage is assumed, hence assertSame instead of assertEquals is used.
		assertSame("", output);
	}

	/**
	 * Test some different values for the <code>ignoreLastN</code> parameter.
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_ignoreLastN() {
		final String input = "////////////////////";
		for (int i = 0; i < input.length(); i++) {
			final String output = TreeBuilder.getLongestSubstringEndingWithCharacter(input, '/', i);
			assertEquals(input.subSequence(0, input.length() - i), output);
		}
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_multipleBetween() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f/o/o", '/', 0);
		assertEquals("f/o/", output);
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_multipleBetweenSuccessive() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f//o//o", '/', 0);
		assertEquals("f//o//", output);
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_multipleAtEndSuccessive() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f/o/o//", '/', 0);
		assertSame("f/o/o//", output);
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_multipleAtEndSuccessiveMinus1() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f//o/o//", '/', 1);
		assertEquals("f//o/o/", output);
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_endMinus1() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f/o/o/", '/', 1);
		assertEquals("f/o/", output);
	}

	/**
	 * Testing contract
	 */
	@Test
	public void testGetLongestSubstringEndingWithCharacter_multipleBetweenSuccessiveMinus1() {
		final String output = TreeBuilder.getLongestSubstringEndingWithCharacter("f//o//o/", '/', 1);
		assertEquals("f//o//", output);
	}

	@Test
	public void testZipFileAssumptions() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		assertEquals(8, fileHeaders.size());
		checkAssumption(fileHeaders.get(0), false, ZipTestFileCreator.FILE_F0);
		checkAssumption(fileHeaders.get(1), true, ZipTestFileCreator.DIR_D1);
		checkAssumption(fileHeaders.get(2), false, ZipTestFileCreator.FILE_F1);
		checkAssumption(fileHeaders.get(3), false, ZipTestFileCreator.FILE_F2);
		checkAssumption(fileHeaders.get(4), false, ZipTestFileCreator.FILE_F3);
		checkAssumption(fileHeaders.get(5), false, ZipTestFileCreator.FILE_F4);
		checkAssumption(fileHeaders.get(6), false, ZipTestFileCreator.FILE_F5);
		checkAssumption(fileHeaders.get(7), false, ZipTestFileCreator.FILE_F6);
	}

	@Test
	public void testCreateTreeFromFileHeadersWithZip() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final List<? extends TreeNode> rootNodes = TreeBuilder.createTreeFrom(fileHeaders);
		assertEquals(5, rootNodes.size());
		final TreeNode d1 = rootNodes.get(0);
		final TreeNode f0 = d1.getChildren().get(0);
		final TreeNode f1 = rootNodes.get(1);
		final TreeNode f2 = rootNodes.get(2);
		final TreeNode f3 = d1.getChildren().get(1);
		final TreeNode f4 = d1.getChildren().get(2);
		final TreeNode f5 = rootNodes.get(3);
		final TreeNode f6 = rootNodes.get(4);
		checkAssumption(d1, true, ZipTestFileCreator.DIR_D1, 3, null);
		checkAssumption(f0, false, ZipTestFileCreator.FILE_F0, 0, d1);
		checkAssumption(f1, false, ZipTestFileCreator.FILE_F1, 0, null);
		checkAssumption(f2, false, ZipTestFileCreator.FILE_F2, 0, null);
		checkAssumption(f3, false, ZipTestFileCreator.FILE_F3, 0, d1);
		checkAssumption(f4, false, ZipTestFileCreator.FILE_F4, 0, d1);
		checkAssumption(f5, false, ZipTestFileCreator.FILE_F5, 0, null);
		checkAssumption(f6, false, ZipTestFileCreator.FILE_F6, 0, null);
	}

	/**
	 * Check for an exception when providing the same {@link FileHeader} multiple times.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateTreeFromFileHeadersWithZipB_sameFileHeader() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		fileHeaders.addAll(fileHeaders);
		TreeBuilder.createTreeFrom(fileHeaders);
		fail();
	}

	/**
	 * Check for an exception when providing {@link FileHeader} directory with <code>null</code> name.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateTreeFromFileHeadersWithZip_nullFileHeaderDirectoryName() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final FileHeader fileHeader = fileHeaders.get(1);
		assertTrue(fileHeader.isDirectory());
		fileHeader.setFileName(null);
		TreeBuilder.createTreeFrom(fileHeaders);
		fail();
	}
	
	/**
	 * Check for an exception when providing {@link FileHeader} directory with empty name.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateTreeFromFileHeadersWithZip_emptyFileHeaderDirectoryName() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final FileHeader fileHeader = fileHeaders.get(1);
		assertTrue(fileHeader.isDirectory());
		fileHeader.setFileName("");
		TreeBuilder.createTreeFrom(fileHeaders);
		fail();
	}
	
	/**
	 * Check for an exception when providing {@link FileHeader} with <code>null</code> name.
	 */
	@Test(expected=AssertionError.class)
	public void testCreateTreeFromFileHeadersWithZip_nullFileHeaderName() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final FileHeader fileHeader = fileHeaders.get(0);
		assertFalse(fileHeader.isDirectory());
		fileHeader.setFileName(null);
		TreeBuilder.createTreeFrom(fileHeaders);
		fail();
	}
	
	/**
	 * Check for an exception when providing {@link FileHeader} with empty name.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateTreeFromFileHeadersWithZip_emptyFileHeaderName() throws ZipException {
		final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final FileHeader fileHeader = fileHeaders.get(0);
		assertFalse(fileHeader.isDirectory());
		fileHeader.setFileName("");
		TreeBuilder.createTreeFrom(fileHeaders);
		fail();
	}
	
	protected static void checkAssumption(final FileHeader fileHeader, final boolean isDir, final String name) {
		assertTrue(isDir == fileHeader.isDirectory());
		assertEquals(name, fileHeader.getFileName());
	}
	
	protected static void checkAssumption(final TreeNode node, final boolean isDir, final String name, final int childrenCount, final TreeNode parent) {
		checkAssumption(node.getPayload(), isDir, name);
		assertEquals(childrenCount, node.getChildren().size());
		assertSame(parent, node.getParent());
	}
}
