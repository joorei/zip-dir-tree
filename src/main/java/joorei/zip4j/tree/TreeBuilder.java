package joorei.zip4j.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

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
 * are present, that information can be used together with the slashes in the ZIP
 * entry identifier to recreate the directory hierarchy.
 * <p>
 * This class allows to re-build the directory tree hierarchy of the original
 * files and directories from {@link FileHeader}s read from a
 * {@link ZipFile}. It will take the special cases mentioned above into account
 * and thus result in a valid directory structure even if the original files or
 * directories contain slash characters.
 *
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.17
 *      file name: (Variable)</a>
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.3.2
 *      Current minimum feature versions are as defined below</a>
 * 
 */
public final class TreeBuilder {
	/**
	 * Copied from ZIP specification: <blockquote>All slashes MUST be forward
	 * slashes '/' as opposed to backwards slashes '\' for compatibility with Amiga
	 * and UNIX file systems etc.</blockquote>
	 * 
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.4.17
	 *      file name: (Variable)</a>
	 * @see InternalZipConstants#ZIP_FILE_SEPARATOR
	 */
	private static final char ZIP_PATH_DELIMITER = '/';

	/**
	 * The root entries found to far.
	 */
	private final List<TreeNode> rootEntries = new ArrayList<>();

	/**
	 * Speed up graph building by using a mapping between entry paths and their
	 * {@link FileHeader}. The nodes will be connected during the tree creation.
	 */
	protected final NodeMapping nodeMapping;

	/**
	 * Initializes an instance using the given {@link NodeMapping}. Won't
	 * perform any grouping yet. Use {@link #addSingle(FileHeader)} to build the
	 * tree.
	 * 
	 * @param theDirectoryMapping Value to set as {@link #nodeMapping}. Must not be <code>null</code>.
	 * @throws NullPointerException Thrown if the given value is <code>null</code>.
	 */
	protected TreeBuilder(final NodeMapping theDirectoryMapping) throws NullPointerException {
		this.nodeMapping = Objects.requireNonNull(theDirectoryMapping);
	}

	/**
	 * @param fileHeaders It is crucial that all ZIP entries that are directories
	 *                    and play a role (as parent or child directory) in the
	 *                    resulting tree are present, otherwise files may be nested
	 *                    in the wrong directory. Changing the {@link FileHeader}s
	 *                    after creating the tree may invalidate the tree. The given
	 *                    value must not be <code>null</code> nor contain <code>null</code>
	 *                    as elements.
	 * @return The root nodes (those without parent) of the tree.
	 */
	public static List<? extends TreeNode> createTreeFrom(final List<FileHeader> fileHeaders) {
		Objects.requireNonNull(fileHeaders);
		final int initialDirectoryMappingSize = getDirectoryCountEstimate(fileHeaders);
		final NodeMapping nodeMapping = new NodeMapping(fileHeaders.iterator(), initialDirectoryMappingSize);
		// Shortcut if there are no directories: treat all fileHeaders as root nodes
		if (nodeMapping.isEmpty()) {
			final List<TreeNode> result = new ArrayList<>(fileHeaders.size());
			for (final FileHeader fileHeader : fileHeaders) {
				Objects.requireNonNull(fileHeader);
				result.add(new TreeNode(fileHeader));
			}
			return result;
		}
		// If there are directories then check the name of each file header to determine the directory structure
		final TreeBuilder treeBuilder = new TreeBuilder(nodeMapping);
		for (final FileHeader fileHeader : fileHeaders) {
			treeBuilder.addSingle(fileHeader);
		}
		return treeBuilder.getRootNodes();
	}

	/**
	 * Adds a single {@link FileHeader} to the tree. Previously unknown edges in the
	 * tree graph are detected automatically while adding {@link FileHeader}s.
	 * 
	 * @param fileHeader The order in which the {@link FileHeader}s are added does
	 *                   not matter for the result. However best performance is
	 *                   achieved if the {@link FileHeader}s are added while
	 *                   (partially or completely) sorted by their result of
	 *                   {@link FileHeader#getFileName()}.
	 * @return The node the given {@link FileHeader} is now wrapped in. Provides
	 *         access to its currently known parent and children.
	 * @throws NullPointerException Thrown in case of unexpected <code>null</code>
	 *                              values, eg. when the given {@link FileHeader}
	 *                              parameter or its {@link FileHeader#getFileName}
	 *                              method returns <code>null</code>, but also if
	 *                              the {@link FileHeader} is a directory but was
	 *                              not found in {@link #nodeMapping}.
	 */
	protected TreeNode addSingle(final FileHeader fileHeader) throws NullPointerException {
		Objects.requireNonNull(fileHeader);
		final String originalEntryName = fileHeader.getFileName();
		Objects.requireNonNull(originalEntryName);
		if (originalEntryName.isEmpty()) {
			throw new IllegalArgumentException("An empty path string as file name retrieved from the file header is invalid, as it is internally reserved");
		}
		/*
		 * We're searching for a directory to put the given fileHeader into. So first
		 * get the full entry name (which includes all parent directories).
		 */
		String potentialParentPath = originalEntryName;
		/*
		 * This loop does not need an end condition, however we add one to be on the
		 * safe side and avoid infinite loops in case of bugs. As end condition an
		 * iteration count greater than the number of characters in the originally given
		 * path is used as it must not take longer to find the parent because the length
		 * is reduced by at least 1 in each iteration.
		 */
		for (int i = 0; i <= originalEntryName.length(); i++) {
			/*
			 * We remove the last character and everything else before until a slash
			 * character. For directories in the first iteration this assures that a
			 * directory is not set as it's own child. For directories after the first
			 * iteration this starts the testing of the next ending slash character from the
			 * back. For files this removes the filename, even if it ends with a slash. Thus
			 * after this we have a path that ends with a slash (or is an empty string,
			 * indicating the root node) and is potentially the parent path of the given
			 * fileHeader while definitively not being the same path as the current
			 * fileHeader.
			 * The reason why we can (and in some cases must) ignore the last character is
			 * because it is either a slash character AFTER the directory name or it is a
			 * character that is part of the file/directory name, as empty names are neither
			 * allowed nor supported.
			 */
			potentialParentPath = getLongestSubstringEndingWithCharacter(potentialParentPath, ZIP_PATH_DELIMITER, 1);
			/*
			 * An empty string as potentialParentPath means we found a root node and can
			 * stop processing the given FileHeader.
			 */
			if (potentialParentPath.isEmpty()) {
				final TreeNode rootNode = getOrCreateNode(fileHeader, originalEntryName);
				this.rootEntries.add(rootNode);
				return rootNode;
			}
			/*
			 * We check if the path is the actual parent directory by checking the mapping
			 * if the path is known. If the result is not null we got the parent directory
			 * and use it, otherwise we continue the loop.
			 */
			final TreeNode foundParent = this.nodeMapping.getNodeOrNull(potentialParentPath, true);
			if (foundParent != null) {
				final TreeNode child = getOrCreateNode(fileHeader, originalEntryName);
				foundParent.addChild(child);
				return child;
			}
			/*
			 * The loop is continued until one of the two return statements is reached.
			 */
		}
		/*
		 * This exception is only thrown if the artificial end condition of the loop above was
		 * triggered, which indicates a bug.
		 */
		throw new IllegalStateException("could not find parent because of internal programming error");
	}

	/**
	 * If the given FileHeader is a directory it will be fetched from the
	 * nodeMapping (without caching, to keep the parent in the cache), as all
	 * directories should be already present there. If it is not a directory a new
	 * {@link TreeNode} will be created with the given {@link FileHeader}
	 * as payload.
	 * 
	 * @param fileHeader        Will be checked if it is a directory and if not so
	 *                          used as content of a new {@link TreeNode}.
	 * @param originalEntryName Will be used to check if a
	 *                          {@link TreeNode} already exists in
	 *                          {@link #nodeMapping} if the given {@link FileHeader}
	 *                          is not a directory.
	 * @return The existing node from {@link #nodeMapping} if the given
	 *         {@link FileHeader} is a directory or a new node if the given
	 *         {@link FileHeader} is not a directory.
	 * @throws NullPointerException Thrown if the given {@link FileHeader} is a
	 *                              directory but no {@link TreeNode}
	 *                              could be found in {@link #nodeMapping}.
	 */
	protected TreeNode getOrCreateNode(final FileHeader fileHeader, final String originalEntryName)
			throws NullPointerException {
		Objects.requireNonNull(fileHeader);
		Objects.requireNonNull(originalEntryName);
		return fileHeader.isDirectory()
				// set caching to 'false' to keep the parent in the cache
				? this.nodeMapping.getNode(originalEntryName, false)
				: new TreeNode(fileHeader);
	}

	/**
	 * @return {@link #rootEntries}
	 */
	protected List<TreeNode> getRootNodes() {
		return this.rootEntries;
	}

	/**
	 * This implementation assumes that for "large" ZIP files (more than 1024
	 * file headers) on average about 10 percent of the file headers are
	 * directories.
	 * <p>
	 * Thus for input sizes bigger than 1024 this implementation will default to returning a
	 * tenth of the given input size. For 1024 and smaller values it will return the original
	 * size instead.
	 * <p>
	 * Both the definition of "large" as well as the 10-percent assumption are complete
	 * guesswork and based on no scientific foundation. It is however hoped that the
	 * returned values result in better initial map sizes than a fixed number like
	 * the {@link HashMap}s default value of 16 would.
	 *
	 * @param fileHeaders All file headers to process (both directories and non-directories).
	 * @return The number of directory file headers assumed.
	 */
	protected static int getDirectoryCountEstimate(final List<FileHeader> fileHeaders)
	{
		final int fileHeaderCount = fileHeaders.size();
		return fileHeaderCount > 1024 ? fileHeaderCount / 10 : fileHeaderCount;
	}

	/**
	 * Returns the longest substring from the given input that ends with the given
	 * character.
	 * 
	 * @param input       The {@link String} The input to read the result from.
	 * @param character   The character the result should end at. Must not be <code>null</code>.
	 * @param ignoreLastN Handle the input as if it ends <code>n</code> characters
	 *                    before its actual end. <code>n</code> being the given
	 *                    value.
	 * @return As the longest substring is requested the substring will always start
	 *         at the beginning of the input {@link String} and end at the last
	 *         occurrence of the given character. If the given character does not
	 *         occur in the given input an empty {@link String} is returned.
	 * @throws NullPointerException Thrown if the given input string is <code>null</code>.
	 */
	protected static String getLongestSubstringEndingWithCharacter(final String input, final char character,
			final int ignoreLastN) throws NullPointerException {
		final int fromIndex = input.length() - ignoreLastN;
		final int index = input.lastIndexOf(character, fromIndex - 1);
		if (index < 0) {
			return "";
		}
		return input.substring(0, index + 1);
	}
}
