package joorei.zip4j.tree;

import java.util.Collection;
import java.util.List;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

@SuppressWarnings("javadoc")
public class SimpleTester {

	private static final TreeBuilder TREE_BUILDER = new TreeBuilder();

	/**
	 * Structure the file headers of a ZIP file to see if and how fast it works for
	 * the given ZIP file.
	 *
	 * @param args First parameter must be the path to the ZIP file.
	 * @throws ZipException
	 */
	public static void main(String[] args) throws ZipException {
		final ZipFile zipFile = new ZipFile(args[0]);
		final long startFileHeaderRetrieval = System.currentTimeMillis();
		final List<FileHeader> fileHeaders = zipFile.getFileHeaders();
		final long startFileHeaderSorting = System.currentTimeMillis();
		TREE_BUILDER.sort(fileHeaders);
		final long startFileHeaderStructuring = System.currentTimeMillis();
		final List<? extends TreeNode> rootNodes = TREE_BUILDER.createTreeFromSorted(fileHeaders, 1);
		final long endTime = System.currentTimeMillis();
		final long timeToSortFileHeaders = startFileHeaderStructuring - startFileHeaderSorting;
		final long timeToStructureFileHeaders = endTime - startFileHeaderStructuring;
		System.out.println("Found " + rootNodes.size() + " root node(s) for " + fileHeaders.size() + " file header(s)");
		System.out.println("Time to get file headers: " + (startFileHeaderSorting - startFileHeaderRetrieval) + " ms");
		System.out.println("Time to sort file headers: " + timeToSortFileHeaders + " ms");
		System.out.println("Time to structure file headers: " + timeToStructureFileHeaders + " ms");
		System.out.println("Time to sort and structure file headers: "
				+ (timeToSortFileHeaders + timeToStructureFileHeaders) + " ms");
		final int maxDepth = getMaxDepth(rootNodes);
		System.out.println("Number of directory levels (counting ZIP root as directory): " + maxDepth);
	}

	public static int getMaxDepth(final Collection<? extends TreeNode> nodes) {
		return nodes.stream().mapToInt(node -> getMaxDepth(node.getChildren()) + 1).max().orElse(1);
	}
}
