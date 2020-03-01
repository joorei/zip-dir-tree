package joorei.zip4j.tree;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * Implements {@link TreeBuilder} by creating the tree structure solely based on
 * the path of the {@link FileHeader} read from
 * {@link FileHeader#getFileName()}. It will <strong>not</strong> consider
 * directory flags and has thus no support for directories or files whose
 * basenames contain the {@link InternalZipConstants#ZIP_FILE_SEPARATOR} one or
 * multiple times.
 */
public class PathSeparatorTreeBuilder extends TreeBuilder {
	/**
	 * The given parentNode is deemed a valid parent of the given childNode if and
	 * only if the path of the child node starts with the path of the parent node
	 * <strong>and</strong> the path of the parentNode ends with
	 * {@link InternalZipConstants#ZIP_FILE_SEPARATOR}.
	 */
	@Override
	public boolean isValidParent(final TreeNode parentNode, final TreeNode childNode) {
		return parentNode.getPath().endsWith(InternalZipConstants.ZIP_FILE_SEPARATOR)
				&& childNode.getPath().startsWith(parentNode.getPath());
	}

	/**
	 * This method assumes that childNodes that contains
	 * {@link InternalZipConstants#ZIP_FILE_SEPARATOR} in their "filename" actually
	 * denote additional directories, even if no {@link FileHeader} exists for
	 * these. It will thus create additional {@link TreeNode} to reflect the full
	 * path and connects the highest one as child inside the given parentNode while
	 * positioning itself at the lowest level of these new {@link TreeNode}s.
	 */
	@Override
	public TreeNode addAsChild(final TreeNode parentNode, final TreeNode childNode) {
		TreeNode currentChild = childNode;
		// get the path of the childNode without the stuff that is already in the
		// parentNode and re-add a trailing slash
		String childPath = childNode.getBasename(parentNode.getPath()).concat(InternalZipConstants.ZIP_FILE_SEPARATOR);
		// iterate through the path segments from the right to the left, ignoring the
		// trailing slashes
		int slashIndex = -1;
		while ((slashIndex = childPath.lastIndexOf(InternalZipConstants.ZIP_FILE_SEPARATOR,
				childPath.length() - 2)) != -1) {
			// get the path without the basename of the previous childPath but with a
			// trailing '/'
			childPath = childPath.substring(0, slashIndex + 1);
			// create a new node and set its values
			final TreeNode newParent = new TreeNode(null, true, parentNode.getPath() + childPath);
			newParent.addChild(currentChild);
			currentChild.setParent(newParent);
			currentChild = newParent;
		}
		parentNode.addChild(currentChild);
		currentChild.setParent(parentNode);
		return currentChild;
	}
}
