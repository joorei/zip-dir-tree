package joorei.zip4j.tree;

import java.util.Comparator;
import java.util.List;

/**
 * This class provides an algorithm to build a tree hierarchy from elements
 * resulting in structured {@link TreeNodeInterface}s.
 * 
 * @param <E> The type of the elements to structure.
 * @param <N> The type of the {@link TreeNodeInterface}s to use to structure the
 *            instances.
 */
public abstract class TreeBuilder<E, N extends TreeNodeInterface<N>> {
	/**
	 * Structures the given elements into a tree hierarchy.
	 * <p>
	 * If the given list contains the same element multiple times or if multiple
	 * elements return the same values for the data relevant for the sorting and
	 * grouping then these elements will be positioned as siblings in the hierarchy.
	 * <p>
	 * Changing the state of elements after creating the tree may invalidate the
	 * tree.
	 *
	 * @param elements The elements to build a tree from.
	 * @return The root nodes (those without parent) of the tree.
	 * @throws NullPointerException Neither the given {@link List} nor its elements
	 *                              must be <code>null</code>.
	 */
	public N createTreeFromUnsorted(final List<E> elements) throws NullPointerException {
		sort(elements);
		return createTreeFromSorted(elements);
	}

	/**
	 * Like {@link #createTreeFromUnsorted(List)} but assumes the given {@link List}
	 * was already sorted following the requirements named in {@link #sort(List)}.
	 * <p>
	 * Using the sorting the list can be looped exactly one time. Each element is
	 * added as child to the most recent element found that is deemed a valid parent
	 * by {@link #isValidParent}.
	 *
	 * @param elements The sorted elements to be structured in a tree hierarchy.
	 *
	 * @return The root node of the tree.
	 *
	 * @see #createTreeFromUnsorted(List)
	 */
	protected N createTreeFromSorted(final Iterable<E> elements) {
		final N rootNode = getRootNode();

		N previousNode = rootNode;
		for (E currentElement : elements) {
			final N currentNode = createTreeNode(currentElement);
			N validParent = this.findParent(previousNode, currentNode);
			if (validParent == null) {
				validParent = rootNode;
			}
			addAsChild(validParent, currentNode);
			previousNode = currentNode;
		}

		return rootNode;
	}

	/**
	 * @param element The element this node is created for. Must not be
	 *                <code>null</code>.
	 * @return The created node. Must not be <code>null</code>.
	 */
	protected abstract N createTreeNode(E element);

	/**
	 * Creates a root node that will be used as the topmost parent of all other
	 * nodes and returned by {@link #createTreeFromSorted(Iterable)} and
	 * {@link #createTreeFromUnsorted(List)}.
	 *
	 * @return @The created root node.
	 */
	protected abstract N getRootNode();

	/**
	 * Find the nearest parent element that is considered a parent of the child
	 * node. Starts with the given parent node.
	 *
	 * @param parentNode The first {@link FileHeaderTreeNode} to consider as a
	 *                   parent. If it is not a valid parent of the child node than
	 *                   its parent node will be considered and so on.
	 * @param childNode  The {@link FileHeaderTreeNode} to find a valid parent for.
	 * @return A parent node deemed valid for the child node by
	 *         {@link #isValidParent(TreeNodeInterface, TreeNodeInterface)} or the
	 *         root node (the node without a parent node) if none other was valid.
	 */
	protected N findParent(final N parentNode, final N childNode) {
		N newParent = parentNode;
		while (newParent.getParent() != null && !isValidParent(newParent, childNode)) {
			newParent = newParent.getParent();
		}
		return newParent;
	}

	/**
	 * Sorts the given elements so that the direct children of an element follow
	 * directly after that element.
	 *
	 * @param elements The list to sort.
	 * @throws NullPointerException Thrown if the given list is <code>null</code>,
	 *                              contains <code>null</code> items or if an item
	 *                              returns <code>null</code> for data necessary to
	 *                              compare the elements.
	 * @see List#sort(Comparator)
	 */
	protected abstract void sort(final List<E> elements) throws NullPointerException;

	/**
	 * Tests if the given parent node is a valid parent for the given child node.
	 *
	 * @param parentNode The node to use as potential parent of the given child
	 *                   node. Will never be a root node (<code>null</code> as value
	 *                   of {@link TreeNodeInterface#getParent()}) as such is always
	 *                   considered a valid parent.
	 * @param childNode  The node to use as potential child in the given parent.
	 * @return true if the parentNode is a valid parent for the given child node.
	 *         false otherwise.
	 */
	protected abstract boolean isValidParent(final N parentNode, final N childNode);

	/**
	 * Adds the given child node into the given parent node.
	 * <p>
	 * The childNode is not necessarily added as a direct child to the parent node.
	 * It may create additional {@link TreeNodeInterface}s if necessary to add
	 * itself as a child inside a child of the parentNode. However it will be added
	 * as a (sub)child inside the parentNode nonetheless, it will
	 * <strong>not</strong> be added into the same level as the parent node or
	 * higher up in the hierarchy.
	 * <p>
	 * The method is also responsible to set the parent of the child correctly if
	 * such thing is necessary.
	 *
	 * @param parentNode The {@link FileHeaderTreeNode} deemed a valid parent fort
	 *                   the childNode. Must not be <code>null</code>.
	 * @param childNode  The {@link FileHeaderTreeNode} deemed a valid child or sub
	 *                   child of the parentNode. Must not be <code>null</code>.
	 * @return The {@link FileHeaderTreeNode} that was placed directly as a child in
	 *         the parentNode.
	 */
	protected abstract N addAsChild(final N parentNode, final N childNode);
}
