package com.optus.apigee.kvm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class KVMAutomation {
	public static void main(String[] args) throws IOException {
		// Step 1: Load properties from application.properties file
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties properties = new Properties();
		try (InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
			properties.load(resourceStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		long startTime = System.currentTimeMillis();

		String excelFilePath;
		String readMasterFile;
		String folderBasePath;
		String cmdDevloper;

//		if (args.length < 2) {
//			System.out
//					.println("Please provide the 'excel.file.path' and 'folder.base.path' as command-line arguments.");
//			return;
//		}

		if (args.length >= 2) {
			excelFilePath = args[0];
			readMasterFile = args[1];
			folderBasePath = args[2];
			cmdDevloper = args[3];
			System.out.println("Cmd line of excel path: " + excelFilePath);
			System.out.println("Cmd line of read master file: " + readMasterFile);
			System.out.println("Cmd line of folderBasePath: " + folderBasePath);
			System.out.println("Cmd line of developer: " + cmdDevloper);
		} else {

			excelFilePath = properties.getProperty("excel.file.path");
			readMasterFile = properties.getProperty("read.file.path");
			folderBasePath = properties.getProperty("folder.base.path");
			cmdDevloper = properties.getProperty("manage.service.developer");
		}

		System.out.println("Read application properties");

		// Read Excel file

		FileInputStream inputStream = null;
		Workbook workbook = null;
		try {
			inputStream = new FileInputStream(excelFilePath);
			workbook = new XSSFWorkbook(inputStream);

			// Rest of your code here

			Sheet sheet = workbook.getSheetAt(0);

			// Step 2: Get column indexes based on headers
			int servicenameColumnIndex = getColumnIndex(sheet, "Service Name");
			int contextPathColumnIndex = getColumnIndex(sheet, "Context Path");
			int serverNameColumnIndex = getColumnIndex(sheet, "Target Server Name");
			int serverURLColumnIndex = getColumnIndex(sheet, "Target Server url");
			int base64ColumnIndex = getColumnIndex(sheet, "base64EncodedCredentials");
			int actionColumnIndex = getColumnIndex(sheet, "Action");
			int developerColumnIndex = getColumnIndex(sheet, "Developer");

			// Iterate through rows and filter based on developer value
			List<Row> filteredRows = new ArrayList<>();

			System.out.println("Read excel header");

			// Iterate through rows
			Iterator<Row> rowIterator = sheet.iterator();
			rowIterator.next();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				Cell developerCell = row.getCell(developerColumnIndex);
				String developerName = developerCell.getStringCellValue();

				if (developerName.equalsIgnoreCase(cmdDevloper)) {
					filteredRows.add(row);
				}
			}

//				if (action.equalsIgnoreCase("Skip") || !developerName.equalsIgnoreCase(cmdDevloper)) {
//					System.out.println("skipping the value for the serviceName  : " + serviceName);
//					continue;
//				}
			// Process filtered rows
			for (Row row : filteredRows) {
				Cell actionCell = row.getCell(actionColumnIndex);
				String action = actionCell.getStringCellValue();

				if (action.equalsIgnoreCase("Skip")) {
					// Skip action
					continue;
				}

				Cell servicenameCell = row.getCell(servicenameColumnIndex);
				String serviceName = servicenameCell.getStringCellValue();

				Cell contextPathCell = row.getCell(contextPathColumnIndex);
				String contexPath = contextPathCell.getStringCellValue();

				Cell serverNameCell = row.getCell(serverNameColumnIndex);
				String serverName = serverNameCell.getStringCellValue();

				Cell base64Cell = row.getCell(base64ColumnIndex);
				String base64Value = base64Cell.getStringCellValue();

				// removing the first delimeter from the context path
				String delimiter = "/";

				// Find the index of the first occurrence of the delimiter
				int index = contexPath.indexOf(delimiter);

				int endIndex = contexPath.indexOf(delimiter, index + delimiter.length());

				// Remove everything before the delimiter
				String contextPathResult = contexPath.substring(endIndex + delimiter.length());

				Cell serverURLCell = row.getCell(serverURLColumnIndex);
				String serverURL = serverURLCell.getStringCellValue();

				String protocol = "https://";
				String serverDelimiter = "/";
				int serverStartIndex = serverURL.indexOf(protocol) + protocol.length();
				int serverEndIndex = serverURL.indexOf(serverDelimiter, serverStartIndex);

				if (serverEndIndex != -1) {
					String serverURLResult = serverURL.substring(serverStartIndex, serverEndIndex);

					// Step 5: Rename xml file name
					createFileContents(folderBasePath, serviceName, contextPathResult, serverName, serverURLResult,
							base64Value, readMasterFile);
				} else {
					System.out.println("Delimiter not found in the serverURL.");
				}

//				// Step 7: Change service xml file content
//				changeServiceXmlContents(newFolder.getPath(), serviceName, contexPath);
//
//				// Step 8: Change proxy xml file content
//				changeProxyXmlContents(newFolder.getPath(), contexPath);
//
//				// Step 9: Change proxy xml file content
//				changeTargetServerContent(newFolder.getPath(), serverURL);

			}
			// End stopwatch
			long endTime = System.currentTimeMillis();

			// Calculate elapsed time
			long elapsedTime = endTime - startTime;

			// Print the elapsed time in milliseconds
			System.out.println("Processing time: " + elapsedTime + " ms");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				workbook.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	private static void createFileContents(String path, String serviceName, String contextPathResult,
			String targetServer, String serverURL, String base64Value, String readMasterFile) throws IOException {

		String filePath = path + serviceName + ".conf";

		String masterKVMFilePath = readMasterFile; // Replace with the actual path to master_kvm.conf
		String replacedContent = null;

		// Read the content of the master_kvm.conf file
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(masterKVMFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
		}

		if (base64Value == null || base64Value.isEmpty()) {
			replacedContent = content.toString()
					.replace("wsdl_target_server_name=''", "wsdl_target_server_name='" + targetServer + "'")
					.replace("target_url_path_prefix=''", "target_url_path_prefix='" + contextPathResult + "'")
					
					//logic for credstash
//					.replace("base64EncodedCredentials=\"\"", "base64EncodedCredentials=\"" + base64Value + "\"")
					.replace("additional_target_headers[host]=''",
							"additional_target_headers[host]='" + serverURL + "'");
		} else if (base64Value.equalsIgnoreCase("No Auth")) {
			// Replace the dynamic values in the content
			replacedContent = content.toString()
					.replace("wsdl_target_server_name=''", "wsdl_target_server_name='" + targetServer + "'")
					.replace("target_url_path_prefix=''", "target_url_path_prefix='" + contextPathResult + "'")
					.replace("base64EncodedCredentials=\"\"", "target_auth_type='NO_AUTH'")
					.replace("additional_target_headers[host]=''",
							"additional_target_headers[host]='" + serverURL + "'");
		} else {
			// Replace the dynamic values in the content
			replacedContent = content.toString()
					.replace("wsdl_target_server_name=''", "wsdl_target_server_name='" + targetServer + "'")
					.replace("target_url_path_prefix=''", "target_url_path_prefix='" + contextPathResult + "'")
					.replace("base64EncodedCredentials=\"\"", "base64EncodedCredentials=\"" + base64Value + "\"")
					.replace("additional_target_headers[host]=''",
							"additional_target_headers[host]='" + serverURL + "'");
		}

		// Write the replaced content to the dynamically created file
		try (FileWriter writer = new FileWriter(filePath)) {
			writer.write(replacedContent);
			System.out.println("Dynamically created file: " + filePath);
		}

//		FileWriter writer = null;
//		try {
//			writer = new FileWriter(filePath);
//			writer.write("ratelimit_quota=10\n");
//			writer.write("ratelimit_time_limit='second'\n");
//			writer.write("throughput_quota=90\n");
//			writer.write("throughput_time_limit='minute'\n");
//			writer.write("target_url_path_prefix='" + contextPathResult + "'\n");
//			writer.write("wsdl_target_server_name='" + targetServer + "'\n");
//			writer.write("base64EncodedCredentials=\"QVBJR1dPQ1A6T3B0dXMxMjM=\"\n");
//			writer.write(
//					"#base64EncodedCredentials=\"$(credstash get \"gcp.apigeex-soapapigwintemplate-target-credentials-base64encodedcredentials-${APIGEE_ENV}\")\"\n");
//			writer.write("additional_target_headers[host]='" + serverURL + "'\n");
//			writer.write("additional_target_headers[header1]='header1'\n");
//			writer.write("additional_target_headers[header2]='header2'\n");
//			writer.write("additional_target_headers[header3]='header3'\n");
//
//			System.out.println("dev-app.conf file created: " + filePath);
//		} finally {
//			if (writer != null) {
//				writer.close();
//			}
//		}
	}

	private static int getColumnIndex(Sheet sheet, String header) {
		Row headerRow = sheet.getRow(0);
		Iterator<Cell> cellIterator = headerRow.cellIterator();
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			if (cell.getStringCellValue().equalsIgnoreCase(header)) {
				return cell.getColumnIndex();
			}
		}
		return -1;
	}

}