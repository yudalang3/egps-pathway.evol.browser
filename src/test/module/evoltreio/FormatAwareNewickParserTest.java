package test.module.evoltreio;

import evoltree.phylogeny.DefaultPhyNode;
import evoltree.struct.EvolNode;
import module.evoltreio.NewickFormatConfig;
import module.evoltreio.exception.NewickParseException;
import module.evoltreio.exception.NewickSyntaxException;
import module.evoltreio.parser.FormatAwareNewickParser;

/**
 * Comprehensive test for FormatAwareNewickParser covering all 10 formats.
 * Run with: java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" test.module.evoltreio.FormatAwareNewickParserTest
 */
public class FormatAwareNewickParserTest {

	private static int passCount = 0;
	private static int failCount = 0;

	public static void main(String[] args) {
		System.out.println("=== FormatAwareNewickParser Comprehensive Test ===\n");

		testBasicParsing();
		testFormat0();
		testFormat1();
		testFormat2_Strict();
		testFormat3_Strict();
		testFormat4();
		testFormat5();
		testFormat6();
		testFormat7();
		testFormat8();
		testFormat9();
		testSyntaxErrors();
		testComplexTrees();

		System.out.println("\n=== Test Summary ===");
		System.out.println("Passed: " + passCount);
		System.out.println("Failed: " + failCount);
		System.out.println("Total: " + (passCount + failCount));

		if (failCount > 0) {
			System.exit(1);
		}
	}

	private static void testBasicParsing() {
		System.out.println("Test: Basic Parsing");

		// Simple tree
		testParse(0, "(A,B);", 2, "Simple two-leaf tree");
		testParse(0, "((A,B),C);", 3, "Three-leaf tree");
		testParse(0, "((A,B),(C,D));", 4, "Balanced four-leaf tree");
		testParse(0, "(A);", 1, "Single leaf in parens");
	}

	private static void testFormat0() {
		System.out.println("\nTest: Format 0 - Flexible with support values");

		// leaf: name:dist, internal: support:dist
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(0);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)90:0.5;");

			// Check leaf A
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");
			assertEqual(0.1, leafA.getLength(), 0.001, "Leaf A distance");

			// Check leaf B
			EvolNode leafB = root.getChildAt(1);
			assertEqual("B", leafB.getName(), "Leaf B name");
			assertEqual(0.2, leafB.getLength(), 0.001, "Leaf B distance");

			// Check root (internal) - support stored in name
			assertEqual("90", root.getName(), "Root support (stored as name)");
			assertEqual(0.5, root.getLength(), 0.001, "Root distance");

			pass("Format 0 parsing correct");
		} catch (Exception e) {
			fail("Format 0 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat1() {
		System.out.println("\nTest: Format 1 - Internal node names");

		// leaf: name:dist, internal: name:dist
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(1);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)Clade1:0.5;");

			// Check leaf A
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");

			// Check root (internal) - name is clade name
			assertEqual("Clade1", root.getName(), "Root name");
			assertEqual(0.5, root.getLength(), 0.001, "Root distance");

			pass("Format 1 parsing correct");
		} catch (Exception e) {
			fail("Format 1 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat2_Strict() {
		System.out.println("\nTest: Format 2 - Strict format 0");

		// Should fail if missing fields
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(2);
			// Missing distance on leaf - should fail
			parser.parse("(A,B:0.2)90:0.5;");
			fail("Format 2 should reject missing leaf distance");
		} catch (NewickParseException e) {
			pass("Format 2 correctly rejected missing leaf distance");
		}

		// Valid strict format
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(2);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)90:0.5;");
			pass("Format 2 accepted valid tree");
		} catch (Exception e) {
			fail("Format 2 rejected valid tree: " + e.getMessage());
		}
	}

	private static void testFormat3_Strict() {
		System.out.println("\nTest: Format 3 - Strict format 1");

		// Should fail if missing fields
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(3);
			// Missing distance on internal - should fail
			parser.parse("(A:0.1,B:0.2)Clade1;");
			fail("Format 3 should reject missing internal distance");
		} catch (NewickParseException e) {
			pass("Format 3 correctly rejected missing internal distance");
		}

		// Valid strict format
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(3);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)Clade1:0.5;");
			assertEqual("Clade1", root.getName(), "Format 3 root name");
			pass("Format 3 accepted valid tree");
		} catch (Exception e) {
			fail("Format 3 rejected valid tree: " + e.getMessage());
		}
	}

	private static void testFormat4() {
		System.out.println("\nTest: Format 4 - Leaf only");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(4);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)ignored:0.5;");

			// Check leaf A
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");
			assertEqual(0.1, leafA.getLength(), 0.001, "Leaf A distance");

			// Internal node label should be ignored
			// (root name should be null or empty)
			pass("Format 4 parsing correct");
		} catch (Exception e) {
			fail("Format 4 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat5() {
		System.out.println("\nTest: Format 5 - Internal distance only");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(5);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2):0.5;");

			// Check leaf A
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");

			// Check root - only distance, no name/support
			assertEqual(0.5, root.getLength(), 0.001, "Root distance");

			pass("Format 5 parsing correct");
		} catch (Exception e) {
			fail("Format 5 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat6() {
		System.out.println("\nTest: Format 6 - Internal name only");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(6);
			DefaultPhyNode root = parser.parse("(A:0.1,B:0.2)Clade1;");

			// Check leaf
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");
			assertEqual(0.1, leafA.getLength(), 0.001, "Leaf A distance");

			// Check root - name only, no distance
			assertEqual("Clade1", root.getName(), "Root name");

			pass("Format 6 parsing correct");
		} catch (Exception e) {
			fail("Format 6 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat7() {
		System.out.println("\nTest: Format 7 - Names only");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(7);
			DefaultPhyNode root = parser.parse("(A,B)Clade1;");

			// Check leaf
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");

			// Check root
			assertEqual("Clade1", root.getName(), "Root name");

			pass("Format 7 parsing correct");
		} catch (Exception e) {
			fail("Format 7 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat8() {
		System.out.println("\nTest: Format 8 - Leaf names only");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(8);
			DefaultPhyNode root = parser.parse("(A,B)ignored;");

			// Check leaf
			EvolNode leafA = root.getChildAt(0);
			assertEqual("A", leafA.getName(), "Leaf A name");

			// Internal ignored
			pass("Format 8 parsing correct");
		} catch (Exception e) {
			fail("Format 8 parsing failed: " + e.getMessage());
		}
	}

	private static void testFormat9() {
		System.out.println("\nTest: Format 9 - Pure topology");

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(9);
			DefaultPhyNode root = parser.parse("((,),(,));");

			// Should have 2 children, each with 2 children
			assertEqual(2, root.getChildCount(), "Root child count");
			assertEqual(2, root.getChildAt(0).getChildCount(), "First child's child count");
			assertEqual(2, root.getChildAt(1).getChildCount(), "Second child's child count");

			pass("Format 9 parsing correct");
		} catch (Exception e) {
			fail("Format 9 parsing failed: " + e.getMessage());
		}
	}

	private static void testSyntaxErrors() {
		System.out.println("\nTest: Syntax Error Detection");

		// Missing semicolon
		try {
			new FormatAwareNewickParser(0).parse("(A,B)");
			fail("Should reject missing semicolon");
		} catch (NewickSyntaxException e) {
			if (e.getErrorType() == NewickSyntaxException.SyntaxErrorType.MISSING_SEMICOLON) {
				pass("Correctly detected missing semicolon");
			} else {
				fail("Wrong error type for missing semicolon");
			}
		} catch (Exception e) {
			fail("Wrong exception type: " + e.getClass().getSimpleName());
		}

		// Unmatched parenthesis - extra close
		try {
			new FormatAwareNewickParser(0).parse("(A,B));");
			fail("Should reject unmatched parenthesis");
		} catch (NewickSyntaxException e) {
			if (e.getErrorType() == NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS) {
				pass("Correctly detected unmatched parenthesis (extra close)");
			} else {
				fail("Wrong error type: " + e.getErrorType());
			}
		} catch (Exception e) {
			fail("Wrong exception type: " + e.getClass().getSimpleName());
		}

		// Unmatched parenthesis - missing close
		try {
			new FormatAwareNewickParser(0).parse("((A,B);");
			fail("Should reject missing close parenthesis");
		} catch (NewickSyntaxException e) {
			if (e.getErrorType() == NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS) {
				pass("Correctly detected unmatched parenthesis (missing close)");
			} else {
				fail("Wrong error type: " + e.getErrorType());
			}
		} catch (Exception e) {
			fail("Wrong exception type: " + e.getClass().getSimpleName());
		}

		// Empty input
		try {
			new FormatAwareNewickParser(0).parse("");
			fail("Should reject empty input");
		} catch (NewickSyntaxException e) {
			if (e.getErrorType() == NewickSyntaxException.SyntaxErrorType.EMPTY_INPUT) {
				pass("Correctly detected empty input");
			} else {
				fail("Wrong error type: " + e.getErrorType());
			}
		} catch (Exception e) {
			fail("Wrong exception type: " + e.getClass().getSimpleName());
		}
	}

	private static void testComplexTrees() {
		System.out.println("\nTest: Complex Tree Structures");

		// Real species tree
		String speciesTree = "((Human:0.1,Chimp:0.08)Hominini:0.2,(Gorilla:0.15,Orangutan:0.25)GreatApes:0.3)Hominidae:0;";
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(1);
			DefaultPhyNode root = parser.parse(speciesTree);

			assertEqual(2, root.getChildCount(), "Species tree root children");
			assertEqual("Hominidae", root.getName(), "Species tree root name");

			pass("Complex species tree parsed");
		} catch (Exception e) {
			fail("Complex species tree failed: " + e.getMessage());
		}

		// Deep nested tree
		String deepTree = "(((((A,B),C),D),E),F);";
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(8);
			DefaultPhyNode root = parser.parse(deepTree);
			assertEqual(6, countLeaves(root), "Deep tree leaf count");
			pass("Deep nested tree parsed");
		} catch (Exception e) {
			fail("Deep nested tree failed: " + e.getMessage());
		}

		// Tree with many siblings
		String wideTree = "(A,B,C,D,E,F,G,H,I,J);";
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(8);
			DefaultPhyNode root = parser.parse(wideTree);
			assertEqual(10, root.getChildCount(), "Wide tree child count");
			pass("Wide tree parsed");
		} catch (Exception e) {
			fail("Wide tree failed: " + e.getMessage());
		}

		// Tree with quoted names
		String quotedTree = "('Species A':0.1,'Species B':0.2);";
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(0);
			DefaultPhyNode root = parser.parse(quotedTree);
			assertEqual("Species A", root.getChildAt(0).getName(), "Quoted name A");
			assertEqual("Species B", root.getChildAt(1).getName(), "Quoted name B");
			pass("Quoted names parsed");
		} catch (Exception e) {
			fail("Quoted names failed: " + e.getMessage());
		}
	}

	// ==================== Helper Methods ====================

	private static void testParse(int format, String newick, int expectedLeaves, String description) {
		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(format);
			DefaultPhyNode root = parser.parse(newick);
			int leaves = countLeaves(root);
			if (leaves == expectedLeaves) {
				pass(description + " (" + leaves + " leaves)");
			} else {
				fail(description + ": expected " + expectedLeaves + " leaves, got " + leaves);
			}
		} catch (Exception e) {
			fail(description + ": " + e.getMessage());
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

	private static void assertEqual(String expected, String actual, String description) {
		if ((expected == null && actual == null) ||
		    (expected != null && expected.equals(actual))) {
			pass(description + " = '" + actual + "'");
		} else {
			fail(description + ": expected '" + expected + "', got '" + actual + "'");
		}
	}

	private static void assertEqual(int expected, int actual, String description) {
		if (expected == actual) {
			pass(description + " = " + actual);
		} else {
			fail(description + ": expected " + expected + ", got " + actual);
		}
	}

	private static void assertEqual(double expected, double actual, double tolerance, String description) {
		if (Math.abs(expected - actual) <= tolerance) {
			pass(description + " = " + actual);
		} else {
			fail(description + ": expected " + expected + ", got " + actual);
		}
	}

	private static void pass(String message) {
		System.out.println("  PASS: " + message);
		passCount++;
	}

	private static void fail(String message) {
		System.out.println("  FAIL: " + message);
		failCount++;
	}
}
