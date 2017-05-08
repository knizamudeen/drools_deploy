 package com.cognizant.ipm.brms.drools.drools_deploy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DroolsDeploy {
	public static String baseURL;
	public static String serverTemplate;
	public static String groupId;
	public static String artifactId;
	public static String version;
	public static String containerVersion;

	public static void main(String args[]) {
		try {
			validate(args);
			
			baseURL = args[0];
			serverTemplate = args[1];
			groupId = args[2];
			artifactId = args[3];
			version = args[4];
			
			deployContainer();
			startContainer();
		} catch (Exception e) {
			System.out.println("Exception :" + e);
		}
	}
	
	public static void validate(String args[]) throws Exception {
		String expectedArgs = "-baseURL -serverTemplate -groupID -artifactId -version";
		if (args.length < 5) {
			throw new Exception("Insufficient arguements !!\n\nExpected :" + expectedArgs);
		}
		if (args.length > 5) {
			throw new Exception("Excessive arguements !! \n\nExpected:" + expectedArgs);
		}
	}


	/**
	 * This function is used for starting container
	 */
	public static void startContainer() {
		URL url;
		try {
			url = new URL(baseURL+"/rest/controller/management/servers/"
					+ serverTemplate + "/containers/" + artifactId + "/status/started");
			System.out.println("starting Container:" + url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setRequestProperty("X-KIE-ContentType", "xml");
			conn.setRequestProperty("Authorization", "Basic bml6YW06bml6YW0xMjM=");
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			} else {
				System.out.println("Container " + artifactId + " Started - POST SUCCESS :" + conn.getResponseCode());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function is used for starting container This function checks if
	 * container exists already ? if So, delete and create new
	 */
	public static void deployContainer() {
		boolean containerExists = checkIfContainerExists();
		System.err.println("containerExists:" + containerExists);
		/**
		 * If container exists and versions are not equal then delete and add
		 * new
		 */
		if (containerExists && (version.contains("SNAPSHOT") || !containerVersion.equalsIgnoreCase(version))) {
			deleteContainer();
			addContainer();
		}
		/**
		 * Else if container exists and versions are same - Do noting ( May not
		 * be the case for SNAPSHOTS)
		 */
		else if (containerExists && containerVersion.equalsIgnoreCase(version) && !version.contains("SNAPSHOT")) {
			System.err.println("The given released version of artifactId already exist");
		} else {
			addContainer();
		}
	}

	/**
	 * This function is used to delete the container
	 */
	private static void deleteContainer() {
		try {
			URL url = new URL(baseURL+"/rest/controller/management/servers/"
					+ serverTemplate + "/containers/" + artifactId);
			System.out.println("Deleting at url:" + url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("DELETE");
			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setRequestProperty("X-KIE-ContentType", "xml");
			conn.setRequestProperty("Authorization", "Basic bml6YW06bml6YW0xMjM=");
			conn.setUseCaches(true);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			if (conn.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
				throw new RuntimeException(
						"Failed : HTTP error code : " + conn.getResponseCode() + ", " + conn.getResponseMessage());
			} else {
				System.out.println("CONTAINER " + artifactId + " DELETED CONTAINER :" + conn.getResponseCode() + ", "
						+ conn.getResponseMessage());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function is used to add a container
	 */
	private static void addContainer() {
		try {
			URL url = new URL(baseURL+"/rest/controller/management/servers/"
					+ serverTemplate + "/containers/" + artifactId);
			System.out.println("deploying to url:" + url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setRequestProperty("X-KIE-ContentType", "xml");
			conn.setRequestProperty("Authorization", "Basic bml6YW06bml6YW0xMjM=");
			conn.setUseCaches(true);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <container-spec-details> <container-id>"
					+ artifactId + "</container-id> <container-name>" + artifactId
					+ "</container-name> <server-template-key> <server-id>test-kie-server</server-id> <server-name>"
					+ serverTemplate + "</server-name> </server-template-key> " + "<release-id> " + "<artifact-id>"
					+ artifactId + "</artifact-id> " + "<group-id>" + groupId + "</group-id> " + "<version>" + version
					+ "</version> " + "</release-id> <configs/> <status>STARTED</status> </container-spec-details>";
			System.out.println("Posting Request:\n" + prettyPrintXML(request, 4));
			OutputStream os = conn.getOutputStream();
			os.write(request.getBytes());
			os.flush();
			if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				throw new RuntimeException(
						"Failed : HTTP error code : " + conn.getResponseCode() + ", " + conn.getResponseMessage());
			} else {
				System.out.println("CONTAINER " + artifactId + " DEPLOYED SUCCESSFULLY :" + conn.getResponseCode()
						+ ", " + conn.getResponseMessage());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean checkIfContainerExists() {
		boolean existenceFlag;
		try {
			URL url = new URL(baseURL+"/rest/controller/management/servers/"
					+ serverTemplate + "/containers/" + artifactId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/xml");
			conn.setRequestProperty("X-KIE-ContentType", "xml");
			conn.setRequestProperty("Authorization", "Basic bml6YW06bml6YW0xMjM=");
			conn.setUseCaches(true);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			if (conn != null && conn.getInputStream() != null) {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder responsemsg = new StringBuilder();
				String line = null;
				while ((line = br.readLine()) != null) {
					responsemsg.append(line);
				}
				containerVersion = getVersionInfo(responsemsg.toString());
			}
			System.out.println("Version exists:" + containerVersion);
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException(
						"Failed : HTTP error code : " + conn.getResponseCode() + ", " + conn.getResponseMessage());
			} else {
				System.out.println("CONTAINER " + artifactId + " FOUND CONTAINER :" + conn.getResponseCode() + ", "
						+ conn.getResponseMessage());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (containerVersion != null) {
			existenceFlag = true;
		} else {
			existenceFlag = false;
		}
		return existenceFlag;
	}

	/**
	 * This function is used to get version Info of existing container
	 *
	 * @param responseMessage
	 * @return
	 */
	private static String getVersionInfo(String responseMessage) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		String versionInfo = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(responseMessage));
			Document doc = dBuilder.parse(is);
			NodeList nList = doc.getElementsByTagName("container-spec-details");
			Node childNode = getNode(nList, "container-spec-details");
			Node relaseNode = getNode(childNode.getChildNodes(), "release-id");
			Node releaseInfoNode = getNode(relaseNode.getChildNodes(), "version");
			versionInfo = releaseInfoNode.getTextContent();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return versionInfo;
	}

	/**
	 * Utility function to get a node from node List
	 * 
	 * @param list
	 * @param nodeName
	 * @return
	 */
	public static Node getNode(NodeList list, String nodeName) {
		Node node = null;
		for (int j = 0; j < list.getLength(); j++) {
			Node childNode = list.item(j);
			if (childNode.getNodeName().equalsIgnoreCase(nodeName)) {
				node = childNode;
			}
		}
		return node;
	}

	/**
	 * Utility function to pretyy print xml
	 * 
	 * @param input
	 * @param indent
	 * @return
	 */
	public static String prettyPrintXML(String input, int indent) {
		try {
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			return xmlOutput.getWriter().toString();
		} catch (Exception e) {
			throw new RuntimeException(e); // simple exception handling, please
			// review it
		}
	}
}
