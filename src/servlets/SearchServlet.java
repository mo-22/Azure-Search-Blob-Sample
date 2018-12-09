package servlets;

import static service.SearchServiceHelper.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.BlobFile;
import service.SearchServiceClient;
import service.SearchServiceHelper;

@WebServlet("/SearchServlet")
public class SearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private Properties _properties;

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		_properties = loadConfigurations();

		SearchServiceClient searchServiceClient = new SearchServiceClient(_properties);

		try {
			// Create an index "features" in the given service
			if (!searchServiceClient.createIndex()) {
				logMessage("Failed while creating index...");
				return;
			}

			// Create indexer datasource "usgs-datasource"
			if (!searchServiceClient.createDatasource()) {
				logMessage("Failed while creating indexer datasource...");
				return;
			}

			// Create an indexer using the above datasource and targeting the
			// above index "features"
			if (!searchServiceClient.createIndexer()) {
				logMessage("Failed while creating indexer...");
				return;
			}

			// Run the indexer and wait until it returns
			if (!searchServiceClient.syncIndexerData()) {
				logMessage("Failed while running indexer...");
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html; charset=UTF-8");
		request.setCharacterEncoding("UTF-8");

		String searchString = request.getParameter("SearchQuery");
		JsonArray jsonResult = doSearch(searchString);
		List<BlobFile> blobFileList = jsonToDocument(jsonResult);

		request.setAttribute("blobFileList", blobFileList);
		request.getRequestDispatcher("Search.jsp").forward(request, response);
	}

	private JsonArray doSearch(String searchString) {
		if (searchString == null || searchString.isEmpty()) {
			searchString = "*";
		}
		try {
			URL url = SearchServiceHelper.getSearchURL(_properties,
					URLEncoder.encode(searchString, java.nio.charset.StandardCharsets.UTF_8.toString()));
			HttpsURLConnection connection = SearchServiceHelper.getHttpURLConnection(url, "GET",
					_properties.getProperty("SearchServiceApiKey"));

			System.out.println(url);
			JsonReader jsonReader = Json.createReader(connection.getInputStream());
			JsonObject jsonObject = jsonReader.readObject();
			JsonArray jsonArray = jsonObject.getJsonArray("value");
			jsonReader.close();

			System.out.println(connection.getResponseMessage());
			System.out.println(connection.getResponseCode());

			if (isSuccessResponse(connection)) {
				return jsonArray;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private List<BlobFile> jsonToDocument(JsonArray jsonArray) {
		List<BlobFile> result = new ArrayList<BlobFile>();

		if (jsonArray == null) {
			return result;
		}

		for (JsonValue jsonDoc : jsonArray) {
			JsonObject object = (JsonObject) jsonDoc;

			BlobFile blobFile = new BlobFile();
			blobFile.setName(object.getString("metadata_storage_name"));
			blobFile.setPath(object.getString("metadata_storage_path"));

			result.add(blobFile);
		}

		return result;
	}

	private Properties loadConfigurations() {
		Properties properties = new Properties();

		InputStream inStream = null;

		try {
			inStream = SearchServlet.class.getClassLoader()
					.getResourceAsStream("config.properties");
			properties.load(inStream);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return properties;
	}
}
