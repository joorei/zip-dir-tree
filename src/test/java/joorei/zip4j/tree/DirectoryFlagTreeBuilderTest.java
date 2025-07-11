package joorei.zip4j.tree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

/**
 * Test {@link TreeBuilder}
 */
@SuppressWarnings({ "static-method", "javadoc" })
public class DirectoryFlagTreeBuilderTest {

	private static final DirectoryFlagTreeBuilder TREE_BUILDER = new DirectoryFlagTreeBuilder();

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
		final FileHeaderTreeNode rootNode = TREE_BUILDER.createTreeFromUnsorted(fileHeaders);
		assertSame(null, rootNode.getParent());
		assertSame("", rootNode.getPath());
		assertSame(null, rootNode.getPayload());
		final List<? extends FileHeaderTreeNode> rootNodes = rootNode.getChildren();
		assertEquals(5, rootNodes.size());
		final FileHeaderTreeNode d1 = rootNodes.get(2);
		final FileHeaderTreeNode f3 = d1.getChildren().get(0);
		final FileHeaderTreeNode f2 = rootNodes.get(1);
		final FileHeaderTreeNode f1 = rootNodes.get(0);
		final FileHeaderTreeNode f4 = d1.getChildren().get(1);
		final FileHeaderTreeNode f0 = d1.getChildren().get(2);
		final FileHeaderTreeNode f5 = rootNodes.get(3);
		final FileHeaderTreeNode f6 = rootNodes.get(4);
		checkAssumption(d1, true, ZipTestFileCreator.DIR_D1, 3, rootNode);
		checkAssumption(f0, false, ZipTestFileCreator.FILE_F0, 0, d1);
		checkAssumption(f1, false, ZipTestFileCreator.FILE_F1, 0, rootNode);
		checkAssumption(f2, false, ZipTestFileCreator.FILE_F2, 0, rootNode);
		checkAssumption(f3, false, ZipTestFileCreator.FILE_F3, 0, d1);
		checkAssumption(f4, false, ZipTestFileCreator.FILE_F4, 0, d1);
		checkAssumption(f5, false, ZipTestFileCreator.FILE_F5, 0, rootNode);
		checkAssumption(f6, false, ZipTestFileCreator.FILE_F6, 0, rootNode);
	}

	/**
	 * Check for an exception when providing {@link FileHeader} directory with
	 * <code>null</code> name.
	 */
	@Test
	public void testCreateTreeFromFileHeadersWithZip_nullFileHeaderDirectoryName() throws ZipException {
		assertThrows(NullPointerException.class, () -> {
			final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
			final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			final FileHeader fileHeader = fileHeaders.get(1);
			assertTrue(fileHeader.isDirectory());
			fileHeader.setFileName(null);
			TREE_BUILDER.createTreeFromUnsorted(fileHeaders);
		});
	}

	/**
	 * Check for an exception when providing {@link FileHeader} with
	 * <code>null</code> name.
	 */
	@Test
	public void testCreateTreeFromFileHeadersWithZip_nullFileHeaderName() throws ZipException {
		assertThrows(NullPointerException.class, () -> {
			final ZipFile zipFile = new ZipFile(ZipTestFileCreator.ARCHIVE_WITH_SLASHED_FILE_AND_DIR_NAMES);
			final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			final FileHeader fileHeader = fileHeaders.get(0);
			assertFalse(fileHeader.isDirectory());
			fileHeader.setFileName(null);
			TREE_BUILDER.createTreeFromUnsorted(fileHeaders);
		});
	}

	protected static void checkAssumption(final FileHeader fileHeader, final boolean isDir, final String name) {
		assertTrue(isDir == fileHeader.isDirectory());
		assertEquals(name, fileHeader.getFileName());
	}

	protected static void checkAssumption(final FileHeaderTreeNode node, final boolean isDir, final String name,
			final int childrenCount, final FileHeaderTreeNode parent) {
		checkAssumption(node.getPayload(), isDir, name);
		assertTrue(isDir == node.isDirectory());
		assertEquals(childrenCount, node.getChildren().size());
		assertSame(parent, node.getParent());
	}
}
