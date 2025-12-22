package module.evoltrepipline;

/**
 * Parameter configuration utilities for evolutionary analysis.
 *
 * Note: Distance calculator configuration was moved to evoldist.operator.DistanceParameterConfigurer
 * to break circular dependency (evoldist ↔ evoltrepipline).
 *
 * This class is kept for backwards compatibility and may be deprecated in the future.
 *
 * @author yudalang
 * @deprecated Configuration methods have been moved to specific packages to break circular dependencies.
 *             Use evoldist.operator.DistanceParameterConfigurer for distance calculator configuration.
 */
@Deprecated
public class ParameterAssigner {
	// This class is now empty - all configuration methods have been moved to break circular dependencies
	// See evoldist.operator.DistanceParameterConfigurer for distance calculator configuration
	// See remnant.treeoperator.AbstractDistanceBasedTreePipeline for tree reconstruction method configuration
}
