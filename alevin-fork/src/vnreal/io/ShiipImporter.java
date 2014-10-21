package vnreal.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import vnreal.network.substrate.SubstrateLink;
import vnreal.network.substrate.SubstrateNetwork;
import vnreal.network.substrate.SubstrateNode;
import vnreal.network.virtual.VirtualLink;
import vnreal.network.virtual.VirtualNetwork;
import vnreal.network.virtual.VirtualNode;

/**
 * This class implements an import filter to parse files generated by
 * Shiip.
 * 
 * @author Philip Huppert
 */
public class ShiipImporter {
	
	private static final double SCALE = 100.0;
	private static final Random rng = new Random();

	private static class Node {
		public Integer id;
		public Integer level; // TODO maybe import as resource/demand?
		public List<Node> outgoing = new ArrayList<Node>();
	}
	
	private ShiipImporter() {}

	/**
	 * Import Shiip topology as {@link SubstrateNetwork}.
	 * 
	 * @param file
	 *            to import from.
	 * @return the imported topology.
	 * @throws IOException
	 *             if the file could not be read.
	 * @throws ShiipImportException
	 *             if the file could not be parsed.
	 */
	public static SubstrateNetwork importNetwork(File file) throws IOException, ShiipImportException {
		FileInputStream fis = null;
		SubstrateNetwork result = null;

		try {
			fis = new FileInputStream(file);
			result = ShiipImporter.importNetwork(fis);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		return result;
	}

	/**
	 * Import Shiip topology as {@link SubstrateNetwork}.
	 * 
	 * @param input
	 *            stream to import from.
	 * @return the imported topology.
	 * @throws IOException
	 *             if the stream could not be read.
	 * @throws ShiipImportException
	 *             if the stream could not be parsed.
	 */
	public static SubstrateNetwork importNetwork(InputStream input) throws IOException, ShiipImportException {
		List<Node> nodes = ShiipImporter.parseFile(input);
		SubstrateNetwork substrate = ShiipImporter.buildSubstrateNet(nodes);
		return substrate;
	}
	
	/**
	 * Import Shiip topology as {@link VirtualNetwork}.
	 * 
	 * @param file
	 *            to import from.
	 * @param layer
	 *            of the virtual network.
	 * @return the imported topology.
	 * @throws IOException
	 *             if the file could not be read.
	 * @throws ShiipImportException
	 *             if the file could not be parsed.
	 */
	public static VirtualNetwork importNetwork(File file, int layer) throws IOException, ShiipImportException {
		FileInputStream fis = null;
		VirtualNetwork result = null;

		try {
			fis = new FileInputStream(file);
			result = ShiipImporter.importNetwork(fis, layer);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		return result;
	}

	/**
	 * Import Shiip topology as {@link VirtualNetwork}.
	 * 
	 * @param input
	 *            stream to import from.
	 * @param layer
	 *            of the virtual network.
	 * @return the imported topology.
	 * @throws IOException
	 *             if the stream could not be read.
	 * @throws ShiipImportException
	 *             if the stream could not be parsed.
	 */
	public static VirtualNetwork importNetwork(InputStream input, int layer) throws IOException, ShiipImportException {
		List<Node> nodes = ShiipImporter.parseFile(input);
		VirtualNetwork substrate = ShiipImporter.buildVirtualNet(nodes, layer);
		return substrate;
	}
	
	/**
	 * This class implements an exception that is thrown when a shiip file could
	 * not be parsed.
	 * 
	 * @author Philip Huppert
	 */
	public static class ShiipImportException extends Exception {
		private static final long serialVersionUID = 1L;
		private String message = null;

		public ShiipImportException() {
		}

		public ShiipImportException(String message) {
			this.message = message;
		}

		public ShiipImportException(Throwable cause) {
			this.initCause(cause);
		}

		public ShiipImportException(String message, Throwable cause) {
			this.initCause(cause);
			this.message = message;
		}

		@Override
		public String getMessage() {
			return this.message;
		}
	}
	
	private static List<Node> parseFile(InputStream input) throws IOException, ShiipImportException {
		List<Node> nodes = new ArrayList<Node>();
		Map<Node, List<Integer>> nodeToOutgoingIds = new HashMap<Node, List<Integer>>();
		Map<Integer, Node> idToNode = new HashMap<Integer, Node>();
		boolean inNodes = false;
		int lineNum = 0;

		BufferedReader br = new BufferedReader(new InputStreamReader(input));

		try {
			while (true) {
				String line = br.readLine();
				lineNum++;
				if (line == null) {
					break;
				}

				if (line.length() > 0) {
					char c = line.charAt(0);
					if (c >= '0' && c <= '9') {
						// Start parsing nodes on first line that starts with a digit.
						inNodes = true;
					} else {
						if (inNodes) {
							// End parsing nodes if line does not start with a digit.
							break;
						}
					}
				}

				if (inNodes) {
					Node node = new Node();

					// Parse node id.
					String sId;
					try {
						sId = line.substring(0, line.indexOf(' '));
					} catch (IndexOutOfBoundsException e) {
						throw new ShiipImportException(String.format(
								"Malformed node id in line %d: Field bounds",
								lineNum), e);
					}
					try {
						node.id = Integer.parseInt(sId);
					} catch (NumberFormatException e) {
						throw new ShiipImportException(String.format(
								"Malformed node id in line %d: Number format",
								lineNum), e);
					}
					
					// Verify that ids are continouus.
					if (nodes.size() > 0) {
						Node last = nodes.get(nodes.size()-1);
						if (last.id != node.id - 1) {
							throw new ShiipImportException(String.format(
									"Non-continoous node ids in line %d",
									lineNum));
						}
					}

					// Parse node level, if any.
					if (line.indexOf('(') != -1) {
						String sLevel;

						try {
							sLevel = line.substring(line.indexOf('(')+1, line.indexOf(')'));
						} catch (IndexOutOfBoundsException e) {
							throw new ShiipImportException(String.format(
								"Malformed node level in line %d: Field bounds",
								lineNum), e);
						}

						try {
							node.level = Integer.parseInt(sLevel);
						} catch (NumberFormatException e) {
							throw new ShiipImportException(String.format(
								"Malformed node level in line %d: Number format",
								lineNum), e);
						}
					}

					// Parse list of outgoing links.
					String sOutgoing;
					try {
						sOutgoing = line.substring(line.indexOf('[')+1, line.indexOf(']'));
					} catch (IndexOutOfBoundsException e) {
						throw new ShiipImportException(String.format(
							"Malformed outgoing list in line %d: Field bounds",
							lineNum), e);
					}
					
					String[] sOutgoingIds = sOutgoing.split(", ");
					List<Integer> outgoingIds = new ArrayList<Integer>(sOutgoingIds.length);
					for (String sOutgoingId : sOutgoingIds) {
						try {
							outgoingIds.add(Integer.parseInt(sOutgoingId));
						} catch (NumberFormatException e) {
							throw new ShiipImportException(String.format(
									"Malformed outgoing id in line %d: Number format",
									lineNum), e);
						}
					}

					nodeToOutgoingIds.put(node, outgoingIds);
					idToNode.put(node.id, node);
					nodes.add(node);
				}
			}
		} finally {
			br.close();
		}

		// Populate each nodes outgoing list with proper references to the corresponding nodes.
		for (Node node : nodes) {
			List<Integer> outgoingIds = nodeToOutgoingIds.get(node);
			for (Integer outgoingId : outgoingIds) {
				Node dest = idToNode.get(outgoingId);
				if (dest == null) {
					throw new ShiipImportException(String.format(
							"Node %d has reference to non-existant node %d",
							node.id, outgoingId));
				}
				node.outgoing.add(dest);
			}
		}

		return nodes;
	}
	
	private static SubstrateNetwork buildSubstrateNet(List<Node> nodes) {
		SubstrateNetwork network = new SubstrateNetwork(false);
		Map<Node, SubstrateNode> nodeToSnode = new HashMap<Node, SubstrateNode>();

		for (Node node : nodes) {
			SubstrateNode sNode = new SubstrateNode();
			sNode.setCoordinateX(ShiipImporter.rng.nextDouble() * SCALE);
			sNode.setCoordinateY(ShiipImporter.rng.nextDouble() * SCALE);
			nodeToSnode.put(node, sNode);
			network.addVertex(sNode);
		}

		for (Node origin : nodes) {
			SubstrateNode sOrigin = nodeToSnode.get(origin);
			for (Node dest : origin.outgoing) {
				SubstrateNode sDest = nodeToSnode.get(dest);
				SubstrateLink sLink = new SubstrateLink();
				network.addEdge(sLink, sOrigin, sDest);
			}
		}

		return network;
	}

	private static VirtualNetwork buildVirtualNet(List<Node> nodes, int layer) {
		VirtualNetwork network = new VirtualNetwork(layer);
		Map<Node, VirtualNode> nodeToVnode = new HashMap<Node, VirtualNode>();

		for (Node node : nodes) {
			VirtualNode vNode = new VirtualNode(layer);
			vNode.setCoordinateX(ShiipImporter.rng.nextDouble() * SCALE);
			vNode.setCoordinateY(ShiipImporter.rng.nextDouble() * SCALE);
			nodeToVnode.put(node, vNode);
			network.addVertex(vNode);
		}

		for (Node origin : nodes) {
			VirtualNode vOrigin = nodeToVnode.get(origin);
			for (Node dest : origin.outgoing) {
				VirtualNode vDest = nodeToVnode.get(dest);
				VirtualLink vLink = new VirtualLink(layer);
				network.addEdge(vLink, vOrigin, vDest);
			}
		}

		return network;
	}
}
