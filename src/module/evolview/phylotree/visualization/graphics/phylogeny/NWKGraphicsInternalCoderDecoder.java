package module.evolview.phylotree.visualization.graphics.phylogeny;

import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import evoltree.struct.io.AbstractNodeCoderDecoder;
import module.evolview.model.tree.DefaultGraphicsPhyloNode;

import java.util.InputMismatchException;

/**
 * 内节点可以有三个属性。
 * 
 * <pre>
 * 我们这里定义，如果三个属性都有，那么输出为 名字:bootstrap:枝长 
 * 如果只有一个枝长，那么 :枝长 
 * 如果有两个值有可能是
 * 名字:枝长 或者 bootstrap:枝长
 * </pre>
 * 
 * 所以一定至少有两个值。
 */
public class NWKGraphicsInternalCoderDecoder<T extends DefaultGraphicsPhyloNode> extends AbstractNodeCoderDecoder<T> {

	private StringBuilder sBuilder = new StringBuilder();

	@Override
	public void parseNode(T node, String str) {
		if (str.isEmpty()) return;
		String[] parts = str.split(":", -1);
		if (parts.length < 2 || parts.length > 3)
			throw new InputMismatchException("Invalid internal node: " + str);
		node.clearBootstrapValue();
		node.setName("");
		if (parts.length == 3) {
			node.setName(parts[0]);
			if (!parts[1].isEmpty()) node.setBootstrapValue(Double.parseDouble(parts[1]));
			node.setLength(Double.parseDouble(parts[2]));
			return;
		}
		Double support = Doubles.tryParse(parts[0]);
		if (support == null) node.setName(parts[0]);
		else node.setBootstrapValue(support);
		node.setLength(Double.parseDouble(parts[1]));
	}

	@Override
	public String codeNode(T node) {
		sBuilder.setLength(0);
		String name = node.isNameBootstrap() ? "" : node.getName();
		if (!Strings.isNullOrEmpty(name)) sBuilder.append(name);
		if (node.hasBootstrapValue()) {
			if (sBuilder.length() > 0) sBuilder.append(COLON);
			sBuilder.append(java.math.BigDecimal.valueOf(node.getBootstrapValue()).stripTrailingZeros().toPlainString());
		} else if (!Strings.isNullOrEmpty(name) && Doubles.tryParse(name) != null) {
			sBuilder.append(COLON);
		}
		sBuilder.append(COLON).append(convertRate2Decimal(node.getLength()));
		return sBuilder.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T createNode() {
		DefaultGraphicsPhyloNode defaultPhyNode = new DefaultGraphicsPhyloNode();
		return (T) defaultPhyNode;
	}

}
