package joorei.zip4j.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

import net.lingala.zip4j.model.FileHeader;

/**
 * ZIP files do not have a concept of a hierarchy of entries. Instead every
 * entry can be identified by a string identifier unique in the ZIP file. To
 * keep the directory hierarchy informations when creating a ZIP archive from
 * file system folders, forward slashes are used to separate directory names in
 * the identifier to denote the path of a file archived in the ZIP archive.
 * <p>
 * However the string identifier of an entry can contain additional slashes when
 * the original directory name or file name contains one (or more) slashes
 * itself. This may occur when ZIP files are created from a source where slashes
 * are allowed in "file" names or "directory" names (e. g. a non-file system
 * source). Using only the slashes in the string identifier to detect the
 * directory hierarchy would result in a different directory hierarchy than used
 * in the source.
 * <p>
 * As of ZIP specification version 2.0 each ZIP entry carries the information if
 * it is a directory or a file. Assuming that ZIP entries for all directories
 * are present, that information can be used together with the slashes in the
 * ZIP entry identifier to recreate the directory hierarchy.
 * <p>
 * This class allows to re-build the directory tree hierarchy of the original
 * files and directories from {@link FileHeader}s read from a {@link ZipFile}.
 * It will take the special cases mentioned above into account and thus result
 * in a valid directory structure even if the original files or directories
 * contain slash characters.
 *
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.17
 *      file name: (Variable)</a>
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.3.2
 *      Current minimum feature versions are as defined below</a>
 * 
 */
public class TreeBuilder {
	/**
	 * Structures the given fileHeaders into a tree hierarchy.
	 * <p>
	 * If the given list contains the same {@link FileHeader} multiple times or if
	 * multiple {@link FileHeader}s have the same value returned by
	 * {@link FileHeader#getFileName()} then these {@link FileHeader}s will
	 * positioned as siblings in the hierarchy.
	 * <p>
	 * If {@link FileHeader#getFileName()} returns an empty string the
	 * {@link FileHeader} will be positioned at the root level.
	 *
	 * @param fileHeaders            It is crucial that all ZIP entries that are
	 *                               directories and play a role (as parent or child
	 *                               directory) in the resulting tree are present,
	 *                               otherwise files may be nested in the wrong
	 *                               directory. Changing the {@link FileHeader}s
	 *                               after creating the tree may invalidate the
	 *                               tree.
	 * @param estimatedRootNodeCount The initial internal size of the returned
	 *                               {@link List}. Will grow if necessary.
	 * @return The root nodes (those without parent) of the tree.
	 * @throws NullPointerException Neither the given {@link List} nor its
	 *                              {@link FileHeader} items nor the values returned
	 *                              by their {@link FileHeader#getFileName()} must
	 *                              be <code>null</code>.
	 */
	public List<? extends TreeNode> createTreeFromUnsorted(final List<FileHeader> fileHeaders,
			final int estimatedRootNodeCount) throws NullPointerException {
		sort(fileHeaders);
		return createTreeFromSorted(fileHeaders, estimatedRootNodeCount);
	}

	/**
	 * Like {@link #createTreeFromUnsorted(List,int)} but assumes the given
	 * {@link List} was already sorted by the values returned by
	 * {@link FileHeader#getFileName()} (natural String order with the shortest
	 * values as first element and the longest as last).
	 *
	 * @param fileHeaders
	 * @param estimatedRootNodeCount
	 *
	 * @return
	 *
	 * @see #createTreeFromUnsorted(List,int)
	 */
	@SuppressWarnings("javadoc")
	public List<? extends TreeNode> createTreeFromSorted(final List<FileHeader> fileHeaders,
			final int estimatedRootNodeCount) {
		final int fileHeadersCount = Objects.requireNonNull(fileHeaders).size();
		final List<TreeNode> rootNodes = new ArrayList<>(estimatedRootNodeCount);
		/*
		 * The given fileHeaders list is assumed to be sorted by the fileNames of the
		 * contained FileHeaders. Because the fileName is the full path in the ZIP file,
		 * the entries inside a directory always follow directly after that directory in
		 * the list.
		 *
		 * Using this information the list can be looped exactly one time. Each item is
		 * added to the most recent directory found whose fileName matches the start of
		 * the current item.
		 */
		TreeNode currentParent = null;
		for (int i = 0; i < fileHeadersCount; i++) {
			final FileHeader currentFileHeader = Objects.requireNonNull(fileHeaders.get(i));
			final String currentFileHeaderName = Objects.requireNonNull(currentFileHeader.getFileName());
			final TreeNode currentNode = new TreeNode(currentFileHeader);

			/*
			 * Find the most recent directory whose fileName matches the start of the
			 * current item by moving up in the directory hierarchy until a match is found
			 * or the root directory (null) is reached.
			 */
			while (currentParent != null
					&& !currentFileHeaderName.startsWith(currentParent.getPayload().getFileName())) {
				currentParent = currentParent.getParent();
			}

			/*
			 * If we reached the root directory level with the previous while loop we add
			 * the current item to the list of root entries. Otherwise we add it to the
			 * parent found in the previous loop.
			 */
			if (currentParent == null) {
				rootNodes.add(currentNode);
			} else {
				currentParent.addChild(currentNode);
				currentNode.setParent(currentParent);
			}

			/*
			 * Only items that are directory entries are set as parent. It is ok to just set
			 * the current item as parent here even if we do not know if the following item
			 * will be inside the current item or a sibling of the current item. If the
			 * latter is the case the while loop will simply move up in the directory
			 * hierarchy until the correct parent directory is found again and sets it as
			 * parent.
			 */
			if (currentFileHeader.isDirectory()) {
				currentParent = currentNode;
			}
		}
		return rootNodes;
	}

	/**
	 * Sorts the given list by the value returned by
	 * {@link FileHeader#getFileName()}.
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
		fileHeaders.sort(new Comparator<FileHeader>() {
			@Override
			public int compare(final FileHeader arg0, final FileHeader arg1) {
				return arg0.getFileName().compareTo(arg1.getFileName());
			}
		});
	}
}
