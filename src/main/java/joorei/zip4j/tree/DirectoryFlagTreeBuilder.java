package joorei.zip4j.tree;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * <strong>tl;dr:</strong> This class will generate a valid directory structure
 * even if the basenames of the original files or directories contain slash
 * characters as long as 1) each directory and each file is backed by a
 * {@link FileHeader} and 2) these {@link FileHeader}s have their directory flag
 * set correctly.
 * <p>
 * Extends {@link PathSeparatorTreeBuilder} by allowing basenames of
 * {@link TreeNode} paths to contain one or multiple
 * {@link InternalZipConstants#ZIP_FILE_SEPARATOR}s.
 * <p>
 * This is the case when the original name of a directory or file denoted by a
 * {@link FileHeader} contained one (or more) slashes itself. This may occur
 * when ZIP files are created from a source where slashes are allowed in "file"
 * or "directory" basenames (e. g. a non-file system source). Using only the
 * slashes in the string identifier to detect the directory hierarchy would
 * result in a different directory hierarchy than used in the source.
 * <p>
 * As of ZIP specification version 2.0 each ZIP entry carries the information if
 * it is a directory or a file. Assuming that ZIP entries for all directories
 * are present, that information can be used together with the slashes in the
 * ZIP entry identifier to recreate the directory hierarchy.
 * <p>
 * The requirement that all directories are backed by a {@link FileHeader} is
 * because we can't distinguish between a {@link FileHeader} whose basename
 * contains a slash character and a {@link FileHeader} whose basename
 * <strong>seems</strong> to contain a slash character because there was no
 * {@link FileHeader} found whose path ended with the part before the slash.
 * This class will simply assume that the latter case does not exist and all
 * {@link FileHeader}s are present.
 */
public class DirectoryFlagTreeBuilder extends PathSeparatorTreeBuilder {
	/**
	 * As it is required by this class that the directory flag is set correctly on
	 * all {@link FileHeader}s we add it as additional condition here, as a
	 * parentNode can't be a valid parent if it is not a directory.
	 */
	@Override
	public boolean isValidParent(TreeNode parentNode, TreeNode childNode) {
		return parentNode.isDirectory() && super.isValidParent(parentNode, childNode);
	}

	/**
	 * Overrides the implementation of
	 * {@link PathSeparatorTreeBuilder#addAsChild(TreeNode, TreeNode)} completely
	 * and will simply add the given childNode as direct child of the given
	 * parentNode. This is because this class can't handle missing
	 * {@link FileHeader}s (see class description).
	 */
	@Override
	public TreeNode addAsChild(final TreeNode parentNode, final TreeNode childNode) {
		parentNode.addChild(childNode);
		childNode.setParent(parentNode);
		return childNode;
	}
}
