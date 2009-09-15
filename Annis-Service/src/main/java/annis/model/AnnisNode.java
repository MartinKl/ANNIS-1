package annis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;

import annis.sqlgen.model.Join;
import annis.sqlgen.model.RankTableJoin;

public class AnnisNode implements Serializable {

	// this class is send to the front end
	private static final long serialVersionUID = 6376277416278198776L;

	// node object in database
	private long id;
	private long corpus; // FIXME: Corpus object with annotations or move to
							// graph?
	private long textId;
	private long left;
	private long right;
	private String spannedText;
	private Long tokenIndex;
	private long leftToken;
	private long rightToken;
	private Set<Annotation> nodeAnnotations;

	// annotation graph
	private AnnotationGraph graph;
	
	// node position in annotation graph
	private Set<Edge> incomingEdges;
	private Set<Edge> outgoingEdges;

	private String name;
	private String namespace;

	// node constraints
	private boolean partOfEdge;
	private boolean root;
	private boolean token;
	private TextMatching spanTextMatching;
	private List<Join> joins;
	private String variable;
	private Set<Annotation> edgeAnnotations;

	// for sql generation
	private String marker;

	public enum TextMatching {
		EXACT("~=~", "\""), REGEXP("~", "/");

		private String sqlOperator;
		private String annisQuote;

		private TextMatching(String sqlOperator, String annisQuote) {
			this.sqlOperator = sqlOperator;
			this.annisQuote = annisQuote;
		}

		public String toString() {
			return sqlOperator;
		}

		public String sqlOperator() {
			return sqlOperator;
		}

		public String quote() {
			return annisQuote;
		}
	};

	public AnnisNode() {
		nodeAnnotations = new TreeSet<Annotation>();
		edgeAnnotations = new TreeSet<Annotation>();
		incomingEdges = new HashSet<Edge>();
		outgoingEdges = new HashSet<Edge>();
		joins = new ArrayList<Join>();
	}

	public AnnisNode(long id) {
		this();
		this.id = id;
	}

	public AnnisNode(long id, long corpusRef, long textRef, long left,
			long right, String namespace, String name, long tokenIndex,
			String span, long leftToken, long rightToken) {
		this(id);

		this.corpus = corpusRef;
		this.textId = textRef;
		this.left = left;
		this.right = right;
		this.leftToken = leftToken;
		this.rightToken = rightToken;

		setNamespace(namespace);
		setName(name);
		setTokenIndex(tokenIndex);

		setSpannedText(span, TextMatching.EXACT);
	}

	public static String qName(String namespace, String name) {
		return name == null ? null : (namespace == null ? name : namespace
				+ ":" + name);
	}

	public void setSpannedText(String span) {
		setSpannedText(span, TextMatching.EXACT);
	}

	public void setSpannedText(String spannedText, TextMatching textMatching) {
		if (spannedText != null)
			Validate.notNull(textMatching);
		this.spannedText = spannedText;
		this.spanTextMatching = textMatching;
	}

	public void clearSpannedText() {
		this.spannedText = null;
		this.spanTextMatching = null;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("node ");
		sb.append(id);

		if (marker != null) {
			sb.append("; marked '");
			sb.append(marker);
			sb.append("'");
		}

		if (variable != null) {
			sb.append("; bound to '");
			sb.append(variable);
			sb.append("'");
		}

		if (name != null) {
			sb.append("; named '");
			sb.append(qName(namespace, name));
			sb.append("'");
		}

		if (token) {
			sb.append("; is a token");
		}

		if (spannedText != null) {
			sb.append("; spans ");
			String quote = spanTextMatching != null ? spanTextMatching.quote()
					: "?";
			sb.append(quote);
			sb.append(spannedText);
			sb.append(quote);
		}

		if (isRoot())
			sb.append("; root node");

		if (!nodeAnnotations.isEmpty()) {
			sb.append("; node labels: ");
			sb.append(nodeAnnotations);
		}

		if (!edgeAnnotations.isEmpty()) {
			sb.append("; edge labes: ");
			sb.append(edgeAnnotations);
		}

		for (Join join : joins) {
			sb.append("; ");
			sb.append(join);
		}

		return sb.toString();
	}

	public boolean addIncomingEdge(Edge edge) {
		return incomingEdges.add(edge);
	}

	public boolean addOutgoingEdge(Edge edge) {
		return outgoingEdges.add(edge);
	}

	public boolean addEdgeAnnotation(Annotation annotation) {
		return edgeAnnotations.add(annotation);
	}

	public boolean addNodeAnnotation(Annotation annotation) {
		return nodeAnnotations.add(annotation);
	}

	public boolean addJoin(Join join) {
		boolean result = joins.add(join);

		if (join instanceof RankTableJoin) {
			this.setPartOfEdge(true);

			AnnisNode target = join.getTarget();
			target.setPartOfEdge(true);
		}

		return result;
	}

	public String getQualifiedName() {
		return qName(namespace, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AnnisNode))
			return false;

		AnnisNode other = (AnnisNode) obj;

		return new EqualsBuilder()
			.append(this.id, other.id)
			.append(this.corpus, other.corpus)
			.append(this.textId, other.textId)
			.append(this.left, other.left)
			.append(this.right, other.right)
			.append(this.spannedText, other.spannedText)
			.append(this.leftToken, other.leftToken)
			.append(this.nodeAnnotations, other.nodeAnnotations)
			.append(this.name, other.name)
			.append(this.namespace, other.namespace)
			.append(this.partOfEdge, other.partOfEdge)
			.append(this.root, other.root)
			.append(this.token, other.token)
			.append(this.spanTextMatching, other.spanTextMatching)
			.append(this.joins, other.joins)
			.append(this.variable, other.variable)
			.append(this.edgeAnnotations, other.edgeAnnotations)
			.append(this.marker, other.marker)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	// /// Getter / Setter

	public Set<Annotation> getEdgeAnnotations() {
		return edgeAnnotations;
	}

	public void setEdgeAnnotations(Set<Annotation> edgeAnnotations) {
		this.edgeAnnotations = edgeAnnotations;
	}

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public String getMarker() {
		return marker;
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSpannedText() {
		return spannedText;
	}

	public TextMatching getSpanTextMatching() {
		return spanTextMatching;
	}

	public Set<Annotation> getNodeAnnotations() {
		return nodeAnnotations;
	}

	public void setNodeAnnotations(Set<Annotation> nodeAnnotations) {
		this.nodeAnnotations = nodeAnnotations;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public long getId() {
		return id;
	}

	public List<Join> getJoins() {
		return joins;
	}

	public boolean isToken() {
		return token;
	}

	public void setToken(boolean token) {
		this.token = token;
	}

	public boolean isPartOfEdge() {
		return partOfEdge;
	}

	public void setPartOfEdge(boolean partOfEdge) {
		this.partOfEdge = partOfEdge;
	}

	public long getCorpus() {
		return corpus;
	}

	public void setCorpus(long corpus) {
		this.corpus = corpus;
	}

	public long getTextId() {
		return textId;
	}

	public void setTextId(long textIndex) {
		this.textId = textIndex;
	}

	public long getLeft() {
		return left;
	}

	public void setLeft(long left) {
		this.left = left;
	}

	public long getRight() {
		return right;
	}

	public void setRight(long right) {
		this.right = right;
	}

	public Long getTokenIndex() {
		return tokenIndex;
	}

	public void setTokenIndex(Long tokenIndex) {
		this.tokenIndex = tokenIndex;
		// FIXME: vermengung von node und constraint semantik
		setToken(tokenIndex != null);
	}

	public long getLeftToken() {
		return leftToken;
	}

	public void setLeftToken(long leftToken) {
		this.leftToken = leftToken;
	}

	public long getRightToken() {
		return rightToken;
	}

	public void setRightToken(long rightToken) {
		this.rightToken = rightToken;
	}

	public Set<Edge> getIncomingEdges() {
		return incomingEdges;
	}

	public void setIncomingEdges(Set<Edge> incomingEdges) {
		this.incomingEdges = incomingEdges;
	}

	public Set<Edge> getOutgoingEdges() {
		return outgoingEdges;
	}

	public void setOutgoingEdges(Set<Edge> outgoingEdges) {
		this.outgoingEdges = outgoingEdges;
	}

	public AnnotationGraph getGraph() {
		return graph;
	}

	public void setGraph(AnnotationGraph graph) {
		this.graph = graph;
	}

}