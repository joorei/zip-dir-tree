package joorei.zip4j.tree;

/**
 * A node in a tree. Provides a parent node.
 *
 * @param <S> The type of the parent node.
 */
public interface TreeNodeInterface<S extends TreeNodeInterface<S>> {

	/**
	 * @return The parent node if this node. May be <code>null</code> if this node
	 *         has no parent (and is thus a root node).
	 */
	public S getParent();
}
