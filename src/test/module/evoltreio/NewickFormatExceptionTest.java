package test.module.evoltreio;

import module.evoltreio.NewickFormatConfig;
import module.evoltreio.exception.*;

/**
 * Test class for Newick format parsing and exception handling.
 * Run with: java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" test.module.evoltreio.NewickFormatExceptionTest
 */
public class NewickFormatExceptionTest {

	private static int passCount = 0;
	private static int failCount = 0;

	public static void main(String[] args) {
		System.out.println("=== Testing Newick Format and Exception Handling ===\n");

		testFormatConfigValidation();
		testFormatConfigProperties();
		testSyntaxExceptionMessages();
		testNumericExceptionMessages();
		testNodeFormatExceptionMessages();
		testParseExceptionWithContext();

		System.out.println("\n=== Test Summary ===");
		System.out.println("Passed: " + passCount);
		System.out.println("Failed: " + failCount);
		System.out.println("Total: " + (passCount + failCount));
	}

	private static void testFormatConfigValidation() {
		System.out.println("Test: Format Configuration Validation");

		// Test valid formats
		for (int i = 0; i <= 9; i++) {
			try {
				NewickFormatConfig config = NewickFormatConfig.getFormat(i);
				if (config.getFormatNumber() == i) {
					pass("  Format " + i + " is valid");
				} else {
					fail("  Format " + i + " returned wrong format number");
				}
			} catch (NewickFormatException e) {
				fail("  Format " + i + " should be valid but threw exception");
			}
		}

		// Test invalid formats
		int[] invalidFormats = {-1, 10, 100, -100};
		for (int format : invalidFormats) {
			try {
				NewickFormatConfig.getFormat(format);
				fail("  Format " + format + " should be invalid but was accepted");
			} catch (NewickFormatException e) {
				pass("  Format " + format + " correctly rejected");
				// Check exception message contains useful info
				String msg = e.getMessage();
				if (msg.contains(String.valueOf(format)) && msg.contains("0") && msg.contains("9")) {
					pass("  Exception message contains format info");
				} else {
					fail("  Exception message missing format info: " + msg);
				}
			}
		}
	}

	private static void testFormatConfigProperties() {
		System.out.println("\nTest: Format Configuration Properties");

		try {
			// Format 0: leaf name:dist, internal support:dist
			NewickFormatConfig f0 = NewickFormatConfig.getFormat(0);
			assertEqual(true, f0.leafHasName(), "Format 0 leafHasName");
			assertEqual(true, f0.leafHasDistance(), "Format 0 leafHasDistance");
			assertEqual(true, f0.internalHasSupport(), "Format 0 internalHasSupport");
			assertEqual(false, f0.internalHasName(), "Format 0 internalHasName");
			assertEqual(true, f0.internalHasDistance(), "Format 0 internalHasDistance");
			assertEqual(false, f0.isStrict(), "Format 0 isStrict");

			// Format 1: leaf name:dist, internal name:dist
			NewickFormatConfig f1 = NewickFormatConfig.getFormat(1);
			assertEqual(true, f1.leafHasName(), "Format 1 leafHasName");
			assertEqual(true, f1.internalHasName(), "Format 1 internalHasName");
			assertEqual(false, f1.internalHasSupport(), "Format 1 internalHasSupport");

			// Format 2: strict version of format 0
			NewickFormatConfig f2 = NewickFormatConfig.getFormat(2);
			assertEqual(true, f2.isStrict(), "Format 2 isStrict");

			// Format 7: names only
			NewickFormatConfig f7 = NewickFormatConfig.getFormat(7);
			assertEqual(true, f7.leafHasName(), "Format 7 leafHasName");
			assertEqual(false, f7.leafHasDistance(), "Format 7 leafHasDistance");
			assertEqual(true, f7.internalHasName(), "Format 7 internalHasName");
			assertEqual(false, f7.internalHasDistance(), "Format 7 internalHasDistance");

			// Format 9: pure topology
			NewickFormatConfig f9 = NewickFormatConfig.getFormat(9);
			assertEqual(false, f9.leafHasName(), "Format 9 leafHasName");
			assertEqual(false, f9.leafHasDistance(), "Format 9 leafHasDistance");
			assertEqual(false, f9.internalHasField1(), "Format 9 internalHasField1");
			assertEqual(false, f9.internalHasField2(), "Format 9 internalHasField2");

		} catch (NewickFormatException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	private static void testSyntaxExceptionMessages() {
		System.out.println("\nTest: Syntax Exception Messages");

		// Test missing semicolon
		NewickSyntaxException e1 = new NewickSyntaxException(
				NewickSyntaxException.SyntaxErrorType.MISSING_SEMICOLON,
				"(A,B)", 4, ";", ")");
		String msg1 = e1.getMessage();
		if (msg1.contains("semicolon") && msg1.contains("Context")) {
			pass("  Missing semicolon exception has context");
		} else {
			fail("  Missing semicolon exception message: " + msg1);
		}

		// Test unmatched parenthesis
		NewickSyntaxException e2 = new NewickSyntaxException(
				NewickSyntaxException.SyntaxErrorType.UNMATCHED_PARENTHESIS,
				"((A,B);", 0, ")", "end of string");
		String msg2 = e2.getMessage();
		if (msg2.contains("parenthesis") && msg2.contains("position 0")) {
			pass("  Unmatched parenthesis exception has position");
		} else {
			fail("  Unmatched parenthesis exception message: " + msg2);
		}

		// Test empty input
		NewickSyntaxException e3 = new NewickSyntaxException(
				NewickSyntaxException.SyntaxErrorType.EMPTY_INPUT, "", 0);
		if (e3.getErrorType() == NewickSyntaxException.SyntaxErrorType.EMPTY_INPUT) {
			pass("  Empty input exception type correct");
		} else {
			fail("  Empty input exception type incorrect");
		}
	}

	private static void testNumericExceptionMessages() {
		System.out.println("\nTest: Numeric Exception Messages");

		// Test invalid branch length with comma (common mistake)
		NewickNumericException e1 = new NewickNumericException(
				NewickNumericException.NumericType.BRANCH_LENGTH,
				"0,5", "NodeA", "(A:0,5,B:0.3);", 3);
		String msg1 = e1.getMessage();
		if (msg1.contains("branch length") && msg1.contains("0,5") && msg1.contains("decimal separator")) {
			pass("  Comma decimal hint provided");
		} else {
			fail("  Missing comma hint: " + msg1);
		}

		// Test invalid support value with letters
		NewickNumericException e2 = new NewickNumericException(
				NewickNumericException.NumericType.SUPPORT_VALUE,
				"high", null, "(A,B)high:0.5;", 5);
		String msg2 = e2.getMessage();
		if (msg2.contains("support value") && msg2.contains("high")) {
			pass("  Support value exception message correct");
		} else {
			fail("  Support value exception message: " + msg2);
		}
	}

	private static void testNodeFormatExceptionMessages() {
		System.out.println("\nTest: Node Format Exception Messages");

		// Test missing distance in strict format
		NewickNodeFormatException e1 = new NewickNodeFormatException(
				NewickNodeFormatException.NodeType.LEAF,
				"TaxonA",
				NewickNodeFormatException.MissingField.DISTANCE,
				2,
				"(TaxonA,TaxonB:0.5);", 1);
		String msg1 = e1.getMessage();
		if (msg1.contains("Leaf node") && msg1.contains("TaxonA") &&
		    msg1.contains("branch length") && msg1.contains("strict")) {
			pass("  Missing distance exception message correct");
		} else {
			fail("  Missing distance exception: " + msg1);
		}

		// Test missing support value
		NewickNodeFormatException e2 = new NewickNodeFormatException(
				NewickNodeFormatException.NodeType.INTERNAL,
				null,
				NewickNodeFormatException.MissingField.SUPPORT,
				0,
				"(A,B):0.5;", 5);
		String msg2 = e2.getMessage();
		if (msg2.contains("Internal node") && msg2.contains("support value") && msg2.contains("Hint")) {
			pass("  Missing support exception with hint");
		} else {
			fail("  Missing support exception: " + msg2);
		}
	}

	private static void testParseExceptionWithContext() {
		System.out.println("\nTest: Parse Exception Context Display");

		String longNewick = "((Homo_sapiens:0.1,Pan_troglodytes:0.08)Hominini:0.2,(Gorilla:0.15,Pongo:0.25)GreatApes:0.3);";

		// Error in the middle of the string
		NewickParseException e1 = new NewickParseException(
				"Invalid character", longNewick, 30);
		String msg1 = e1.getMessage();
		if (msg1.contains("Context") && msg1.contains("position 30")) {
			pass("  Context displayed for mid-string error");
		} else {
			fail("  Context not displayed: " + msg1);
		}

		// Verify context extraction
		if (e1.getContext() != null && e1.getContext().length() > 0) {
			pass("  Context string extracted: " + e1.getContext());
		} else {
			fail("  Context string not extracted");
		}
	}

	// Helper methods
	private static void assertEqual(boolean expected, boolean actual, String description) {
		if (expected == actual) {
			pass("  " + description + " = " + actual);
		} else {
			fail("  " + description + " expected " + expected + " but got " + actual);
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
