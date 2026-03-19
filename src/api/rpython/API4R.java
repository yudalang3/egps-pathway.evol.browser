package api.rpython;

/**
 * Compatibility-oriented R API surface for tree node extraction.
 *
 * This class now lives alongside the rest of the external language bridge
 * entry points and reuses {@link EvolTreeManipulator} for its core behavior.
 */
public class API4R extends EvolTreeManipulator {

    @Override
    public String describe() {
        return "Compatibility-oriented R API for extracting node names from phylogenetic trees.";
    }
}
