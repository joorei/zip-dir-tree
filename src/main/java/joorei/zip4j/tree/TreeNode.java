package joorei.zip4j.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.lingala.zip4j.model.FileHeader;

/**
 * A node potentially connected to child nodes and a parent node of the same
 * type. Carries a {@link FileHeader} instance as {@link #payload}.
 * <p>
 * There is no special class for leaf nodes. Those are just normal nodes with no
 * children set.
 * <p>
 * While non-leaf nodes containing a {@link #payload} with the
 * {@link FileHeader#isDirectory()} flag not set (indicating a file) are not
 * valid this class implementation will not prevent creating such instance.
 * <p>
 * This implementation is mutable. {@link #children}, {@link #payload} and
 * {@link #parent} are exposed to the outside and can be changed at any time.
 */
public class TreeNode {
	/**
	 * The content wrapped by this node. Never <code>null</code> as it is set on
	 * initialization.
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
	 * @param childNode The child to add to {@link #children}. Must not be
	 *                  <code>null</code>.
	 * @throws NullPointerException Thrown if childNode is <code>null</code>.
	 */
	public void addChild(final TreeNode childNode) throws NullPointerException {
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		this.children.add(Objects.requireNonNull(childNode));
	}

	/**
	 * @param thePayload The payload to set as {@link #payload}. Must not be
	 *                   <code>null</code>.
	 */
	public TreeNode(final FileHeader thePayload) {
		this.payload = Objects.requireNonNull(thePayload);
	}

	/**
	 * @param theParent The value to set as {@link #parent}. May be
	 *                  <code>null</code> to make this node a root node.
	 */
	public void setParent(final TreeNode theParent) {
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
	 * @return The children of this node. May be empty if no children exist. Neither
	 *         the {@link List} nor its elements can be <code>null</code>.
	 */
	public List<? extends TreeNode> getChildren() {
		return this.children == null ? Collections.emptyList() : this.children;
	}
}
