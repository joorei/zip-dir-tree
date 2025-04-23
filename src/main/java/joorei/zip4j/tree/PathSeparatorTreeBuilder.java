package joorei.zip4j.tree;

import java.util.Comparator;
import java.util.List;

import org.codeturnery.tree.TreeBuilder;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * ZIP files do not have a concept of a hierarchy of entries. Instead every
 * entry can be identified by a string identifier unique in the ZIP file. To
 * keep the directory hierarchy information when creating a ZIP archive from
 * file system folders, forward slashes are used to separate directory names in
 * the identifier to denote the path of a file archived in the ZIP archive.
 * <p>
 * As of ZIP specification version 2.0 each ZIP entry also carries the
 * information if it is a directory or a file.
 * <p>
 * This class provides basic means to re-build the directory tree hierarchy of
 * the original files and directories from {@link FileHeader}s read from a
 * {@link ZipFile}.
 * <p>
 * It is solely based on the path of the {@link FileHeader} read from
 * {@link FileHeader#getFileName()}. It will <strong>not</strong> consider
 * directory flags and has thus no support for directories or files whose
 * basenames contain the {@link InternalZipConstants#ZIP_FILE_SEPARATOR} one or
 * multiple times.
 * <p>
 * It is crucial that all ZIP entries that are directories and play a role (as
 * parent or child directory) in the desired tree are present, otherwise files
 * may be nested in the wrong directory.
 * <p>
 * The returned tree may contain {@link FileHeaderTreeNode#getPayload()}s with
 * <code>null</code> as value. This is the result from single
 * {@link FileHeader}s that denote multiple directories. In this case only the
 * lowest {@link FileHeaderTreeNode} will contain that {@link FileHeader}.
 *
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.17
 *      file name: (Variable)</a>
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.3.2
 *      Current minimum feature versions are as defined below</a>
 */
public class PathSeparatorTreeBuilder extends TreeBuilder<FileHeader, FileHeaderTreeNode> {
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
	 * The given parentNode is deemed a valid parent of the given childNode if and
	 * only if the path of the child node starts with the path of the parent node
	 * <strong>and</strong> the path of the parentNode ends with
	 * {@link InternalZipConstants#ZIP_FILE_SEPARATOR}.
	 */
	@Override
	public boolean isValidParent(final FileHeaderTreeNode parentNode, final FileHeaderTreeNode childNode) {
		return parentNode.getPath().endsWith(InternalZipConstants.ZIP_FILE_SEPARATOR)
				&& childNode.getPath().startsWith(parentNode.getPath());
	}

	/**
	 * This method assumes that childNodes that contains
	 * {@link InternalZipConstants#ZIP_FILE_SEPARATOR} in their "filename" actually
	 * denote additional directories, even if no {@link FileHeader} exists for
	 * these. It will thus create additional {@link FileHeaderTreeNode} to reflect
	 * the full path and connects the highest one as child inside the given
	 * parentNode while positioning itself at the lowest level of these new
	 * {@link FileHeaderTreeNode}s.
	 */
	@Override
	public FileHeaderTreeNode addAsChild(final FileHeaderTreeNode parentNode, final FileHeaderTreeNode childNode)
			throws NullPointerException {
		FileHeaderTreeNode currentChild = childNode;
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
			final FileHeaderTreeNode newParent = new FileHeaderTreeNode(null, true, parentNode.getPath() + childPath);
			newParent.addChild(currentChild);
			currentChild.setParent(newParent);
			currentChild = newParent;
		}
		parentNode.addChild(currentChild);
		currentChild.setParent(parentNode);
		return currentChild;
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
	@Override
	protected void sort(final List<FileHeader> fileHeaders) throws NullPointerException {
		fileHeaders.sort(FILE_NAME_COMPARATOR);
	}

	@Override
	protected FileHeaderTreeNode createTreeNode(final FileHeader payload) {
		return new FileHeaderTreeNode(payload);
	}

	@Override
	protected FileHeaderTreeNode getRootNode() {
		return new FileHeaderTreeNode();
	}
}
