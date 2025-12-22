package module.multiseq.deversitydescriptor;

import fasta.io.FastaReader;
import module.evoltre.mutation.MutationOperator;
import module.multiseq.deversitydescriptor.calculate.VariantDescriptorElement;
import module.multiseq.deversitydescriptor.calculate.VariantStateObtainer;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Command line interface for diversity descriptor calculation.
 * 
 * This tool analyzes aligned FASTA sequences and generates variant descriptors
 * including mutation information for each sequence.
 */
public class CLI {

    public static void main(String[] args) throws IOException {
        // Create Options object
        Options options = new Options();
        
        // Add reference sequence name option (optional)
        Option refSequenceName = Option.builder("r")
                .longOpt("ref")
                .hasArg()
                .argName("reference")
                .desc("Reference sequence name (optional)")
                .build();
        
        // Add aligned FASTA file path option
        Option alignedFastaFile = Option.builder("f")
                .longOpt("fasta")
                .hasArg()
                .argName("file")
                .desc("Aligned FASTA file path")
                .required()
                .build();
        
        options.addOption(refSequenceName);
        options.addOption(alignedFastaFile);
        
        // Create parser and formatter
        DefaultParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        try {
            // Parse command line arguments
            CommandLine cmd = parser.parse(options, args);
            
            String alignedFastaFilePath = cmd.getOptionValue("fasta");
            
            VariantStateObtainer variantStateObtainer = new VariantStateObtainer();
            
            List<Pair<String, VariantDescriptorElement>> stateList;
            File inputFile = new File(alignedFastaFilePath);
            if (cmd.hasOption("ref")) {
                String refSequenceNameValue = cmd.getOptionValue("ref");
                stateList = variantStateObtainer.getStateList(inputFile, refSequenceNameValue);
            } else {

                LinkedHashMap<String, String> map = FastaReader.readFastaSequence(alignedFastaFilePath);

                String firstKey = map.keySet().iterator().next();
                stateList = variantStateObtainer.getStateList(inputFile, firstKey);
            }

            StringBuilder sb = new StringBuilder("#SeqName\tFirstPositionNotGap\tLastPositionNotGap\tmutations\n");
            for (Pair<String, VariantDescriptorElement> pair : stateList) {
                sb.append(pair.getLeft()).append("\t");
                VariantDescriptorElement right = pair.getRight();
                sb.append(right.getCountableFromOneBase()).append("\t");
                sb.append(right.getCountableToOneBase()).append("\t");
                String mutationList2FullString = MutationOperator.mutationList2FullString(right.getMutations());
                sb.append(mutationList2FullString);

                sb.append("\n");
            }

            System.out.println(sb);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("diversitydescriptor", options);
        }
    }
}