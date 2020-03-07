package joorei.zip4j.tree;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

/**
 * ZIP files do not have a concept of a hierarchy of entries. Instead every
 * entry can be identified by a string identifier unique in the ZIP file. To
 * keep the directory hierarchy informations when creating a ZIP archive from
 * file system folders, forward slashes are used to separate directory names in
 * the identifier to denote the path of a file archived in the ZIP archive.
 * <p>
 * As of ZIP specification version 2.0 each ZIP entry also carries the
 * information if it is a directory or a file.
 * <p>
 * This class provides basic means to re-build the directory tree hierarchy of
 * the original files and directories from {@link FileHeader}s read from a
 * {@link ZipFile}.
 *
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.17
 *      file name: (Variable)</a>
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.3.2
 *      Current minimum feature versions are as defined below</a>
 * 
 */
@SuppressWarnings("static-method")
public abstract class TreeBuilder {

	/**
	 * The default comparator used to order {@link FileHeader}.
	 *
	 * @see TreeBuilder#sort(List)
	 */
	private static final Comparator<FileHeader> FILE_NAME_COMPARATOR = new Comparator<FileHeader>() {
		@Override
		public int compare(final FileHeader arg0, final FileHeader arg1) {
			return arg0.getFileName().compareTo(arg1.getFileName());
		}
	};

	/**
	 * Structures the given fileHeaders into a tree hierarchy.
	 * <p>
	 * If the given list contains the same {@link FileHeader} multiple times or if
	 * multiple {@link FileHeader}s return the same values for the data relevant for
	 * the sorting and grouping then then these {@link FileHeader}s will be
	 * positioned as siblings in the hierarchy.
	 * <p>
	 * It is crucial that all ZIP entries that are directories and play a role (as
	 * parent or child directory) in the desired tree are present, otherwise files
	 * may be nested in the wrong directory.
	 * <p>
	 * Changing the {@link FileHeader}s after creating the tree may invalidate the
	 * tree.
	 *
	 * @param fileHeaders The {@link FileHeader}s to build a tree from.
	 * @return The root nodes (those without parent) of the tree. The returned tree
	 *         may contain {@link TreeNode#getPayload()}s with <code>null</code> as
	 *         value. This is the result from single {@link FileHeader}s that denote
	 *         multiple directories. In this case only the lowest {@link TreeNode}
	 *         will contain that {@link FileHeader}.
	 * @throws NullPointerException Neither the given {@link List} nor its
	 *                              {@link FileHeader} items must be
	 *                              <code>null</code>.
	 */
	public List<? extends TreeNode> createTreeFromUnsorted(final List<FileHeader> fileHeaders)
			throws NullPointerException {
		sort(fileHeaders);
		return createTreeFromSorted(fileHeaders);
	}

	/**
	 * Like {@link #createTreeFromUnsorted(List)} but assumes the given {@link List}
	 * was already sorted so that the direct children of a directory follow directly
	 * after that directory.
	 * <p>
	 * Using this information the list can be looped exactly one time. Each item is
	 * added to the most recent directory found that is deemed a valid parent by
	 * {@link #isValidParent}.
	 *
	 * @param fileHeaders The sorted {@link FileHeader}s.
	 *
	 * @return The root nodes (those without parent) of the tree.
	 *
	 * @see #createTreeFromUnsorted(List)
	 */
	protected List<? extends TreeNode> createTreeFromSorted(final List<FileHeader> fileHeaders) {
		final int fileHeadersCount = Objects.requireNonNull(fileHeaders).size();
		final TreeNode rootNode = new TreeNode();

		TreeNode previousNode = rootNode;
		for (int i = 0; i < fileHeadersCount; i++) {
			final FileHeader currentFileHeader = Objects.requireNonNull(fileHeaders.get(i));
			final boolean isDirectory = currentFileHeader.isDirectory();
			final String path = currentFileHeader.getFileName();
			final TreeNode currentNode = new TreeNode(currentFileHeader, isDirectory, path);
			TreeNode validParent = this.findParent(previousNode, currentNode);
			if (validParent == null) {
				validParent = rootNode;
			}
			final TreeNode directChildInValidParent = addAsChild(validParent, currentNode);
			/*
			 * Even though we add children to the rootNode, we do not set the rootNode as
			 * parent of anything, because it is only temporary (see return statement).
			 */
			if (validParent == rootNode) {
				directChildInValidParent.setParent(null);
			}
			previousNode = currentNode;
		}

		assert rootNode.getChildren().stream().allMatch(node -> node.getParent() == null);
		return rootNode.getChildren();
	}

	/**
	 * Find the nearest parent directory that is considered a parent of the
	 * childNode. Starts with the parentNode.
	 *
	 * @param parentNode The first {@link TreeNode} to consider as a parent. If it
	 *                   is not a valid parent of the childNode than the parent of
	 *                   the parentNode will be considered and so on.
	 * @param childNode  The {@link TreeNode} to find a valid parent for.
	 * @return A valid parent for the childNode or <code>null</code> if the root
	 *         level was reached without finding a valid parent.
	 */
	protected TreeNode findParent(final TreeNode parentNode, final TreeNode childNode) {
		TreeNode newParent = parentNode;
		while (newParent != null && !isValidParent(newParent, childNode)) {
			newParent = newParent.getParent();
		}
		return newParent;
	}

	/**
	 * Sorts the given list by the value returned by
	 * {@link FileHeader#getFileName()} (natural String order with the shortest
	 * values as first element and the longest as last). Because the fileName is the
	 * full path in the ZIP file, for most use cases the entries inside a directory
	 * will follow directly after that directory in the sorted list.
	 * <p>
	 * The sort algorithm used is the default of {@link List#sort(Comparator)} which
	 * is based on TimSort and well suited for this use case as it is an adaptive
	 * sorting algorithm (taking advantage of pre-sorted lists). This is because it
	 * is likely that entries were added to the backing ZIP file not randomly but by
	 * iterating through the directory tree, which should result in a (at least
	 * partially) sorted list of {@link FileHeader}s when reading the entries.
	 * 
	 * @param fileHeaders The list to sort.
	 * @throws NullPointerException Thrown if the given list is <code>null</code>,
	 *                              contains <code>null</code> items or if an item
	 *                              returns <code>null</code> for
	 *                              {@link FileHeader#getFileName()}.
	 * @see List#sort(Comparator)
	 */
	protected void sort(final List<FileHeader> fileHeaders) throws NullPointerException {
		fileHeaders.sort(FILE_NAME_COMPARATOR);
	}

	/**
	 * Tests if the given parentNode is a valid parent for the given childNode.
	 *
	 * @param parentNode The {@link TreeNode} to use as potential parent of the
	 *                   given childNode. <code>null</code> as value of
	 *                   {@link TreeNode#getParent()} indicates a root node.
	 * @param childNode  The {@link TreeNode} to use as potential child in the given
	 *                   parent.
	 * @return true if the parentNode is a valid parent for the given childNode.
	 *         false otherwise.
	 */
	protected abstract boolean isValidParent(final TreeNode parentNode, final TreeNode childNode);

	/**
	 * Adds the given childNode into the given parentNode.
	 * <p>
	 * The childNode is not necessarily added as a direct child to the parentNode.
	 * It may create additional {@link TreeNode}s if necessary to add itself as a
	 * child inside a child of the parentNode. However it will be added as a
	 * (sub)child inside the parentNode nonetheless, it will <strong>not</strong> be
	 * added into the same level as the parentNode or higher up in the hierarchy.
	 * <p>
	 * The method is also responsible to set the
	 * {@link TreeNode#setParent(TreeNode)} correctly.
	 *
	 * @param parentNode The {@link TreeNode} deemed a valid parent fort the
	 *                   childNode. Must not be <code>null</code>.
	 * @param childNode  The {@link TreeNode} deemed a valid child or sub child of
	 *                   the parentNode. Must not be <code>null</code>.
	 * @return The {@link TreeNode} that was placed directly as a child in the
	 *         parentNode.
	 */
	protected abstract TreeNode addAsChild(final TreeNode parentNode, final TreeNode childNode);

}
