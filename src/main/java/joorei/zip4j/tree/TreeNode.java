package joorei.zip4j.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.lingala.zip4j.model.FileHeader;

/**
 *  A node potentially connected to child nodes of the same type carrying a
 * {@link FileHeader} instance as payload.
 * <p>
 * This implementation is mutable. Children can be changed at any time.
 */
class TreeNode {
	/**
	 * The content wrapped by this node. Never <code>null</code> as it is set on initialization.
	 */
	private final FileHeader payload;

	/**
	 * The children of this node. <code>null</code> if no children were added yet.
	 * Contained elements must not be <code>null</code>.
	 */
	private List<TreeNode> children = null;

	/**
	 * @param childNode The child to add to {@link #children}. Must not be <code>null</code>.
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
		if (this.children == null) {
			return Collections.emptyList();
		}
		return this.children;
	}
}
