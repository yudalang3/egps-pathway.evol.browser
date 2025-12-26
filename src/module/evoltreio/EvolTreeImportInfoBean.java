package module.evoltreio;

//@JSONType(orders = {
//		"input_nwk_string",
//		"input_nwk_path",
//		"input_tableLike_path",
//		"blank_space",
//		"removeWhitespace",
//		"nwkFormat"
//		})
public class EvolTreeImportInfoBean {

	private String input_nwk_string = "";
	private String input_nwk_path = "";
	private String input_tableLike_path = "";
	private boolean removeWhitespace = false;
	private int nwkFormat = NewickFormatConfig.DEFAULT_FORMAT;

	public EvolTreeImportInfoBean() {

	}

	public String getInput_nwk_string() {
		return input_nwk_string;
	}

	public void setInput_nwk_string(String input_nwk_string) {
		this.input_nwk_string = input_nwk_string;
	}

	public String getInput_nwk_path() {
		return input_nwk_path;
	}

	public void setInput_nwk_path(String input_nwk_path) {
		this.input_nwk_path = input_nwk_path;
	}

	public String getInput_tableLike_path() {
		return input_tableLike_path;
	}

	public void setInput_tableLike_path(String input_tableLike_path) {
		this.input_tableLike_path = input_tableLike_path;
	}

	public boolean isRemoveWhitespace() {
		return removeWhitespace;
	}

	public void setRemoveWhitespace(boolean removeWhitespace) {
		this.removeWhitespace = removeWhitespace;
	}

	public int getNwkFormat() {
		return nwkFormat;
	}

	public void setNwkFormat(int nwkFormat) {
		this.nwkFormat = nwkFormat;
	}

}
