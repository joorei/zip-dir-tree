package joorei.zip4j.tree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import net.lingala.zip4j.model.FileHeader;

/**
 * Provides a mapping between ZIP entry identifiers and ZIP entry directory
 * nodes.
 * <p>
 * A minimal cache is used to cache a single value with its key to avoid
 * accessing the backing {@link #nodes}-{@link Map} if this value is provided again.
 */
final class NodeMapping {
	/**
	 * The mapping from ZIP entry path strings to ZIP entries.
	 */
	private final Map<String, TreeNode> nodes;
	/**
	 * The key of the value currently stored in the cache. Kept in sync with
	 * {@link #cachedValue}.
	 */
	private String cachedKey;
	/**
	 * The value currently stored in the cache. Kept in sync with
	 * {@link #cachedKey}.
	 */
	private TreeNode cachedValue;

	/**
	 * Builds a mapping between the string identifier of directory entries and new
	 * {@link TreeNode}s wrapping the entries.
	 * <p>
	 * Changes to the given entries that result in different string identifiers or
	 * directory/non-directory states will invalidate the mapping.
	 * 
	 * @param entries     Directories that should be present in the resulting graph
	 *                    and directories whose children/sub-children should be present in the
	 *                    resulting graph. Non-directories are ignored.
	 * @param initialSize The initial size of the backing {@link Map}.
	 * @throws IllegalArgumentException Thrown if the entry key was already added to
	 *                                  the backing {@link Map}. Note that an empty
	 *                                  string as file name is used internally and
	 *                                  must not be provided by the given
	 *                                  {@link Iterator}.
	 * @throws NullPointerException     Thrown if the given Iterator contained a
	 *                                  <code>null</code> value.
	 */
	protected NodeMapping(final Iterator<FileHeader> entries, final int initialSize) {
		Objects.requireNonNull(entries);
		this.nodes = new HashMap<>(initialSize);
		// create an entry for each directory
		while (entries.hasNext()) {
			final FileHeader fileHeader = entries.next();
			if (fileHeader.isDirectory()) {
				final String fileName = fileHeader.getFileName();
				if (fileName == null) {
					throw new IllegalArgumentException("fileName in fileHeader must not be null");
				}
				if (fileName.isEmpty()) {
					throw new IllegalArgumentException("fileName in fileHeader must not be empty");
				}
				final TreeNode previousValue = this.nodes.put(fileName, new TreeNode(fileHeader));
				if (previousValue != null) {
					throw new IllegalArgumentException(
							"Duplicated entry identifier in given entries: '" + fileHeader.getFileName() + "'");
				}
			}
		}
	}
	
	/**
	 * @return <code>true</code> if this instance does not contain any mappings. <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return this.nodes.size() == 0;
	}

	/**
	 * This method will always access the cache first to check if the
	 * {@link #cachedKey} is the same as the one used on the previous access (when
	 * caching was allowed). Using the cache resulted in a (slight but measurable)
	 * performance improvement.
	 * 
	 * 
	 * @param key          The key to search for.
	 * @param allowCaching If the key and value should be cached if a value was
	 *                     found. While they are cached, values can be returned
	 *                     without accessing the backing {@link Map}. Only one value
	 *                     can be cached at the same time.
	 * @return The found node or <code>null</code>.
	 */
	protected TreeNode getNodeOrNull(final String key, final boolean allowCaching) {
		TreeNode node = this.getCacheValue(key);
		if (node != null) {
			return node;
		}
		node = this.nodes.get(key);
		// node can be null if not in map
		if (node != null && allowCaching) {
			this.updateCache(key, node);
		}
		return node;
	}

	/**
	 * @param key          The key to search for.
	 * @param allowCaching If the key and value should be cached if a value was
	 *                     found. While cached values can be returned without
	 *                     accessing the backing {@link Map}. Only one value can be
	 *                     cached at the same time.
	 * @return The found node.
	 * @throws NullPointerException Thrown if the given key was null or no node for the given key was found.
	 */
	protected TreeNode getNode(final String key, final boolean allowCaching) throws NullPointerException {
		Objects.requireNonNull(key);
		final TreeNode node = getNodeOrNull(key, allowCaching);
		return Objects.requireNonNull(node, "No non-null value found for given key: " + key);
	}
	
	/**
	 * @param newKey  Will be set as {@link #cachedKey}.
	 * @param newNode Will be set as {@link #cachedValue}.
	 */
	protected void updateCache(final String newKey, final TreeNode newNode) {
		this.cachedValue = Objects.requireNonNull(newNode);
		this.cachedKey = Objects.requireNonNull(newKey);
	}

	/**
	 * @param key The key to get the value for. Must not be <code>null</code>.
	 * @return The value currently cached. May be <code>null</code>.
	 * @throws NullPointerException Thrown if the given key is <code>null</code>.
	 */
	protected TreeNode getCacheValue(final String key) throws NullPointerException {
		return key.equals(this.cachedKey) ? this.cachedValue : null;
	}
}
