package service;

import static service.SearchServiceHelper.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

/**
 * This class is responsible for implementing HTTP operations for creating Index, creating indexer, creating indexer datasource, ...
 *
 */
public class SearchServiceClient {
	private final String _apiKey;
	private final Properties _properties;

	public SearchServiceClient(Properties properties) {
		_apiKey = properties.getProperty("SearchServiceApiKey");
		_properties = properties;
	}

	public boolean createIndex() throws IOException {
		logMessage("\n Creating Index...");

		URL url = SearchServiceHelper.getCreateIndexURL(_properties);

		HttpsURLConnection connection = SearchServiceHelper.getHttpURLConnection(url, "PUT", _apiKey);
		connection.setDoOutput(true);

		// Index definition
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
		outputStreamWriter.append("{\"fields\":[");
		outputStreamWriter
				.append("{\"name\": \"id\", \"type\": \"Edm.String\", \"key\": true, \"searchable\": false},");
		outputStreamWriter
				.append("{\"name\": \"content\", \"type\": \"Edm.String\", \"retrievable\": false, \"searchable\": true, \"filterable\": false,  \"sortable\": false, \"facetable\": false, \"analyzer\" : \"ja.lucene\"},");
		outputStreamWriter
				.append("{\"name\": \"metadata_storage_name\", \"type\": \"Edm.String\", \"searchable\": false},");
		outputStreamWriter
				.append("{\"name\": \"metadata_storage_path\", \"type\": \"Edm.String\", \"searchable\": false}");
		outputStreamWriter.append("]}");
		outputStreamWriter.close();

		System.out.println(connection.getResponseMessage());
		System.out.println(connection.getResponseCode());

		return isSuccessResponse(connection);
	}

	public boolean createDatasource() throws IOException {
		logMessage("\n Creating Indexer Data Source...");

		URL url = SearchServiceHelper.getCreateIndexerDatasourceURL(_properties);
		HttpsURLConnection connection = SearchServiceHelper.getHttpURLConnection(url, "PUT", _apiKey);
		connection.setDoOutput(true);

		String dataSourceRequestBody = "{ 'name' : '" + _properties.get("DataSourceName") + "'"
				+ ",'type' : '" + _properties.getProperty("DataSourceType")
				+ "','credentials' : " + _properties.getProperty("DataSourceConnectionString")
				+ ",'container' : { 'name' : '" + _properties.getProperty("DataSourceTable") + "' }} ";

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
		outputStreamWriter.write(dataSourceRequestBody);
		outputStreamWriter.close();

		System.out.println(connection.getResponseMessage());
		System.out.println(connection.getResponseCode());

		return isSuccessResponse(connection);
	}

	public boolean createIndexer() throws IOException {
		logMessage("\n Creating Indexer...");

		URL url = SearchServiceHelper.getCreateIndexerURL(_properties);
		HttpsURLConnection connection = SearchServiceHelper.getHttpURLConnection(url, "PUT", _apiKey);
		connection.setDoOutput(true);

		String indexerRequestBody = "{ 'name' : '" + _properties.get("IndexerName")
				+ "', 'dataSourceName' : '"
				+ _properties.get("DataSourceName")
				+ "', 'targetIndexName' : '" + _properties.get("IndexName")
				+ "', 'schedule' : { 'interval' : 'PT2H'}}";

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
		outputStreamWriter.write(indexerRequestBody);
		outputStreamWriter.close();

		System.out.println(connection.getResponseMessage());
		System.out.println(connection.getResponseCode());

		return isSuccessResponse(connection);
	}

	public boolean syncIndexerData() throws IOException, InterruptedException {
		logMessage("\n Syncing data...");

		// Run indexer
		URL url = SearchServiceHelper.getRunIndexerURL(_properties);
		HttpsURLConnection connection = SearchServiceHelper.getHttpURLConnection(url, "POST", _apiKey);
		connection.setRequestProperty("Content-Length", "0");
		connection.setDoOutput(true);
		connection.getOutputStream().flush();

		System.out.println(connection.getResponseMessage());
		System.out.println(connection.getResponseCode());

		if (!isSuccessResponse(connection)) {
			return false;
		}

		// Check indexer status
		logMessage("Synchronization running...");

		boolean running = true;
		URL statusURL = SearchServiceHelper.getIndexerStatusURL(_properties);
		connection = SearchServiceHelper.getHttpURLConnection(statusURL, "GET", _apiKey);

		while (running) {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return false;
			}

			JsonReader jsonReader = Json.createReader(connection.getInputStream());
			JsonObject responseJson = jsonReader.readObject();

			if (responseJson != null) {
				JsonObject lastResultObject = responseJson.getJsonObject("lastResult");

				if (lastResultObject != null) {
					String inderxerStatus = lastResultObject.getString("status");

					if (inderxerStatus.equalsIgnoreCase("inProgress")) {
						logMessage("Synchronization running...");
						Thread.sleep(1000);

					} else {
						running = false;
						logMessage("Synchronized " + lastResultObject.getInt("itemsProcessed") + " rows...");
					}
				}
			}
		}

		return true;
	}
}
