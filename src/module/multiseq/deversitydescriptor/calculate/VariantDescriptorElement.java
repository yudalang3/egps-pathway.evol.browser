package module.multiseq.deversitydescriptor.calculate;

import module.evoltre.mutation.IMutation;

import java.util.List;

public class VariantDescriptorElement {
	
	

	String name;
	int countableToOneBase;
	int countableFromOneBase;
	List<IMutation> mutations;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the countableToOneBase
	 */
	public int getCountableToOneBase() {
		return countableToOneBase;
	}
	/**
	 * @return the countableFromOneBase
	 */
	public int getCountableFromOneBase() {
		return countableFromOneBase;
	}
	/**
	 * @return the mutations
	 */
	public List<IMutation> getMutations() {
		return mutations;
	}
	
	

}
