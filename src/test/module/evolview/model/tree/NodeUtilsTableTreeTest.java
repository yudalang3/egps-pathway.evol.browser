package test.module.evolview.model.tree;

import evoltree.struct.EvolNode;
import module.evolview.model.tree.NodeUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for NodeUtils.parseTableTree method.
 * Run with: java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" test.module.evolview.model.tree.NodeUtilsTableTreeTest
 */
public class NodeUtilsTableTreeTest {

	public static void main(String[] args) {
		System.out.println("=== Testing NodeUtils.parseTableTree ===\n");

		testExample1_SimpleTree();
		testExample2_TreeWithBranchLengths();
		testExample3_SpeciesTree();

		System.out.println("\n=== All tests completed ===");
	}

	/**
	 * Example 1 - Simple tree without branch lengths:
	 * root    A,B
	 * A    C,D
	 * B    E,F
	 * C
	 * D
	 * E
	 * F
	 *
	 * Expected Newick: ((C,D)A,(E,F)B)root;
	 */
	private static void testExample1_SimpleTree() {
		System.out.println("Test 1: Simple tree without branch lengths");

		List<String> lines = Arrays.asList(
				"root\tA,B",
				"A\tC,D",
				"B\tE,F",
				"C",
				"D",
				"E",
				"F"
		);

		try {
			EvolNode root = NodeUtils.parseTableTree(lines);
			System.out.println("  Root name: " + root.getName());
			System.out.println("  Child count: " + root.getChildCount());

			// Verify structure
			if (root.getChildCount() != 2) {
				System.out.println("  FAIL: Expected 2 children, got " + root.getChildCount());
				return;
			}

			EvolNode childA = root.getChildAt(0);
			EvolNode childB = root.getChildAt(1);

			System.out.println("  Child A: " + childA.getName() + " (children: " + childA.getChildCount() + ")");
			System.out.println("  Child B: " + childB.getName() + " (children: " + childB.getChildCount() + ")");

			if (childA.getChildCount() == 2 && childB.getChildCount() == 2) {
				System.out.println("  PASS\n");
			} else {
				System.out.println("  FAIL: Expected 2 grandchildren for each child\n");
			}

		} catch (Exception e) {
			System.out.println("  FAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Example 2 - Tree with branch lengths:
	 * root:0    A:0.1,B:0.2
	 * A:0.1    C:0.3,D:0.4
	 * B:0.2    E:0.15,F:0.25
	 * C:0.3
	 * D:0.4
	 * E:0.15
	 * F:0.25
	 */
	private static void testExample2_TreeWithBranchLengths() {
		System.out.println("Test 2: Tree with branch lengths");

		List<String> lines = Arrays.asList(
				"root:0\tA:0.1,B:0.2",
				"A:0.1\tC:0.3,D:0.4",
				"B:0.2\tE:0.15,F:0.25",
				"C:0.3",
				"D:0.4",
				"E:0.15",
				"F:0.25"
		);

		try {
			EvolNode root = NodeUtils.parseTableTree(lines);
			System.out.println("  Root name: " + root.getName() + ", length: " + root.getLength());

			EvolNode childA = root.getChildAt(0);
			System.out.println("  Child A: " + childA.getName() + ", length: " + childA.getLength());

			if (Math.abs(childA.getLength() - 0.1) < 0.001) {
				System.out.println("  PASS\n");
			} else {
				System.out.println("  FAIL: Expected length 0.1 for A\n");
			}

		} catch (Exception e) {
			System.out.println("  FAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Example 3 - Species tree:
	 * Hominidae:0    Hominini:0.2,GreatApes:0.3
	 * Hominini:0.2    Human:0.1,Chimp:0.08
	 * GreatApes:0.3    Gorilla:0.15,Orangutan:0.25
	 * Human:0.1
	 * Chimp:0.08
	 * Gorilla:0.15
	 * Orangutan:0.25
	 */
	private static void testExample3_SpeciesTree() {
		System.out.println("Test 3: Species tree");

		List<String> lines = Arrays.asList(
				"Hominidae:0\tHominini:0.2,GreatApes:0.3",
				"Hominini:0.2\tHuman:0.1,Chimp:0.08",
				"GreatApes:0.3\tGorilla:0.15,Orangutan:0.25",
				"Human:0.1",
				"Chimp:0.08",
				"Gorilla:0.15",
				"Orangutan:0.25"
		);

		try {
			EvolNode root = NodeUtils.parseTableTree(lines);
			System.out.println("  Root name: " + root.getName());

			// Count total leaves
			int leafCount = countLeaves(root);
			System.out.println("  Total leaves: " + leafCount);

			if (leafCount == 4 && "Hominidae".equals(root.getName())) {
				System.out.println("  PASS\n");
			} else {
				System.out.println("  FAIL: Expected 4 leaves and root 'Hominidae'\n");
			}

		} catch (Exception e) {
			System.out.println("  FAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static int countLeaves(EvolNode node) {
		if (node.getChildCount() == 0) {
			return 1;
		}
		int count = 0;
		for (int i = 0; i < node.getChildCount(); i++) {
			count += countLeaves(node.getChildAt(i));
		}
		return count;
	}
}
