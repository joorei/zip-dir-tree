package joorei.zip4j.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.lingala.zip4j.model.FileHeader;

/**
 * A node potentially connected to child nodes and a parent node of the same
 * type. May carry a {@link FileHeader} instance as {@link #payload}.
 * <p>
 * There is no special class for leaf nodes. Those are just normal nodes with no
 * children set.
 * <p>
 * While non-leaf nodes containing a {@link #payload} with the
 * {@link FileHeader#isDirectory()} flag not set (indicating a file) are not
 * valid, this class implementation will not prevent creating such instance.
 * <p>
 * This implementation is mutable. {@link #children}, {@link #payload} and
 * {@link #parent} are exposed to the outside and can be changed at any time.
 */
public class TreeNode {
	/**
	 * The content wrapped by this node. May be <code>null</code>.
	 */
	private final FileHeader payload;

	/**
	 * The children of this node. <code>null</code> if no children were added yet.
	 * May be as well non-<code>null</code> and empty. Contained elements must not
	 * be <code>null</code> but the class will not prevent adding
	 * <code>null</code>-values.
	 */
	private List<TreeNode> children = null;

	/**
	 * The parent node of this node. Will be <code>null</code> if no parent was
	 * found yet or this is a root node.
	 */
	private TreeNode parent = null;

	/**
	 * true if this instance is a directory and thus may contain children. false
	 * otherwise, meaning that no children can be added or retrieved from this
	 * instance.
	 */
	private final boolean directory;

	/**
	 * The path of this instance in the tree hierarchy.
	 */
	private final String path;

	/**
	 * @param thePayload  The payload to set as {@link #payload}. May be
	 *                    <code>null</code>.
	 * @param isDirectory Value to set as {@link #directory}.
	 * @param thePath     Value to set as {@link #path}. Must not be
	 *                    <code>null</code>.
	 */
	public TreeNode(final FileHeader thePayload, final boolean isDirectory, final String thePath) {
		this.payload = thePayload;
		this.directory = isDirectory;
		this.path = Objects.requireNonNull(thePath);
		assert this.path.length() != 0;
	}

	/**
	 * Create a root node (directory) without payload and with an empty string as
	 * path.
	 */
	public TreeNode() {
		this.payload = null;
		this.directory = true;
		this.path = "";
	}

	/**
	 * @return The value set as {@link #path}. Never <code>null</code>.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @param childNode The child to add to {@link #children}. Must not be
	 *                  <code>null</code>.
	 * @throws NullPointerException Thrown if childNode is <code>null</code>.
	 */
	public void addChild(final TreeNode childNode) throws NullPointerException {
		if (!this.directory) {
			throw new UnsupportedOperationException("non directories can't have children");
		}
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		this.children.add(Objects.requireNonNull(childNode));
	}

	/**
	 * @param theParent The value to set as {@link #parent}. May be
	 *                  <code>null</code> to make this node a root node.
	 */
	public void setParent(final TreeNode theParent) {
		assert theParent != this;
		assert theParent == null || this.path.startsWith(theParent.getPath()) : theParent.getPath() + " | " + this.path;
		this.parent = theParent;
	}

	/**
	 * @return The value set as {@link #parent}. May be <code>null</code> if this
	 *         node is a root node.
	 */
	public TreeNode getParent() {
		return this.parent;
	}

	/**
	 * @return The payload wrapped by this node. Never <code>null</code>.
	 */
	public FileHeader getPayload() {
		return this.payload;
	}

	/**
	 * @return The value of {@link #directory}.
	 */
	public boolean isDirectory() {
		return this.directory;
	}

	/**
	 * @return The children of this node. May be empty if no children exist. Neither
	 *         the {@link List} nor its elements can be <code>null</code>.
	 */
	public List<? extends TreeNode> getChildren() {
		assert this.directory || (this.children == null || this.children.size() == 0) : this.children.size();
		return this.children == null ? Collections.emptyList() : this.children;
	}

	/**
	 * @return The part of the {@link #path} without the {@link #path} of the parent
	 *         and without the trailing '/' if {@link #directory} is true.
	 */
	public String getBasename() {
		return getBasename(this.parent == null ? "" : this.parent.getPath());
	}

	/**
	 * @param parentPath The parent to use instead of the one internally set by this
	 *                   instance in {@link #parent}.
	 * @return The part of the {@link #path} without the given parent path and
	 *         without the trailing '/' if {@link #directory} is true.
	 */
	public String getBasename(String parentPath) {
		return this.path.substring(parentPath.length(), this.path.length() - (this.directory ? 1 : 0));
	}
}
