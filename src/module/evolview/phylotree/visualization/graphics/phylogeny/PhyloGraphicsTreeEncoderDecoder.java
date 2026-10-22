package module.evolview.phylotree.visualization.graphics.phylogeny;

import evoltree.struct.TreeCoder;
import evoltree.struct.io.BasicLeafCoderDecoder;
import module.evoltreio.parser.NewickText;
import java.util.ArrayDeque;
import module.evolview.model.tree.DefaultGraphicsPhyloNode;

public class PhyloGraphicsTreeEncoderDecoder {
    class NWKGraphicsLeafCoderDecoder<T extends DefaultGraphicsPhyloNode> extends BasicLeafCoderDecoder<T> {

        @Override
        public String codeNode(T node) {
            return NewickText.quote(node.getName()) + ":" + Double.toString(node.getLength());
        }


        @SuppressWarnings("unchecked")
        @Override
        public T createNode() {
            return (T) new DefaultGraphicsPhyloNode();
        }
    }

    public String encode(DefaultGraphicsPhyloNode tree) {
        TreeCoder<DefaultGraphicsPhyloNode> coder = new TreeCoder<>(new NWKGraphicsLeafCoderDecoder<>(), new NWKGraphicsInternalCoderDecoder<>());
        return coder.code(tree);
    }

    public DefaultGraphicsPhyloNode decode(String line) throws Exception {
        String text = line.trim();
        var stack = new ArrayDeque<DefaultGraphicsPhyloNode>();
        var internal = new NWKGraphicsInternalCoderDecoder<DefaultGraphicsPhyloNode>();
        DefaultGraphicsPhyloNode root = null;
        boolean expectNode = true;
        int position = 0;
        while (position < text.length()) {
            char c = text.charAt(position);
            if (Character.isWhitespace(c)) { position++; continue; }
            if (c == ';') {
                if (!stack.isEmpty() || expectNode || !text.substring(position + 1).isBlank())
                    throw new IllegalArgumentException("Invalid Newick terminator");
                position = text.length(); break;
            }
            if (c == '(') {
                if (!expectNode) throw new IllegalArgumentException("Missing comma");
                var node = new DefaultGraphicsPhyloNode();
                if (stack.isEmpty()) {
                    if (root != null) throw new IllegalArgumentException("Multiple trees");
                    root = node;
                } else stack.peek().addChild(node);
                stack.push(node); position++; expectNode = true;
            } else if (c == ')') {
                if (expectNode || stack.isEmpty()) throw new IllegalArgumentException("Unexpected closing parenthesis");
                var node = stack.pop();
                int end = NewickText.labelEnd(text, position + 1);
                internal.parseNode(node, text.substring(position + 1, end).trim());
                position = end; expectNode = false;
            } else if (c == ',') {
                if (expectNode || stack.isEmpty()) throw new IllegalArgumentException("Unexpected comma");
                position++; expectNode = true;
            } else {
                if (!expectNode) throw new IllegalArgumentException("Missing comma");
                int end = NewickText.labelEnd(text, position);
                var parts = NewickText.split(text.substring(position, end));
                if (parts.size() > 2) throw new IllegalArgumentException("Invalid leaf label");
                var node = new DefaultGraphicsPhyloNode();
                node.setName(NewickText.unquote(parts.getFirst()));
                if (parts.size() == 2 && !parts.get(1).isBlank()) node.setLength(Double.parseDouble(parts.get(1).trim()));
                if (stack.isEmpty()) {
                    if (root != null) throw new IllegalArgumentException("Multiple trees");
                    root = node;
                } else stack.peek().addChild(node);
                position = end; expectNode = false;
            }
        }
        if (root == null || !stack.isEmpty() || expectNode) throw new IllegalArgumentException("Incomplete Newick tree");
        return root;
    }
}


