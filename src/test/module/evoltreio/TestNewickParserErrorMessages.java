package test.module.evoltreio;

import module.evoltreio.exception.NewickParseException;
import module.evoltreio.parser.FormatAwareNewickParser;

/**
 * Test the Newick parser error messages.
 * This test demonstrates the improved error messages with cause and suggestion.
 */
public class TestNewickParserErrorMessages {

	public static void main(String[] args) {
		System.out.println("=== Testing Newick Parser Error Messages ===\n");

		// Test 1: Semicolon in node name
		testParse("Test 1: Semicolon in node name",
				"(A,B,(C,Prokaryota|bacteria;archaea)E)F;");

		// Test 2: Missing semicolon at end
		testParse("Test 2: Missing terminating semicolon",
				"(A,B,(C,D)E)F");

		// Test 3: Unmatched parenthesis
		testParse("Test 3: Unmatched parenthesis (missing close)",
				"(A,B,(C,D)E;");

		// Test 4: Unmatched parenthesis (extra close)
		testParse("Test 4: Unmatched parenthesis (extra close)",
				"(A,B,)C,D)E)F;");

		// Test 5: Empty input
		testParse("Test 5: Empty input", "");

		// Test 6: Valid tree (should succeed) - Using Format 1 for named internal nodes
		testParse("Test 6: Valid tree with Format 1 (should succeed)",
				"(A:0.1,B:0.2,(C:0.3,D:0.4)E:0.5)F;", 1);

		// Test 7: Valid tree with quoted name
		testParse("Test 7: Valid tree with quoted name containing semicolon (Format 1)",
				"(A:0.1,'node;name':0.2,(C:0.3,D:0.4)E:0.5)F;", 1);

		System.out.println("\n=== All tests completed ===");
	}

	private static void testParse(String testName, String newickString) {
		testParse(testName, newickString, 0);
	}

	private static void testParse(String testName, String newickString, int format) {
		System.out.println("--- " + testName + " ---");
		System.out.println("Input: " + newickString);
		System.out.println("Format: " + format);
		System.out.println();

		try {
			FormatAwareNewickParser parser = new FormatAwareNewickParser(format);
			parser.parse(newickString);
			System.out.println("Result: SUCCESS - Tree parsed successfully");
		} catch (NewickParseException e) {
			System.out.println("Result: PARSE ERROR");
			System.out.println("Error message:");
			System.out.println(e.getMessage());
		}
		System.out.println();
		System.out.println("----------------------------------------\n");
	}
}
