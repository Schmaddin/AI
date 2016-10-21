package mi.project.core.syn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpRequest offers static methods for making HTTP-Request on servers with
 * reading the answer
 */
public class HttpRequest {
	private final static String USER_AGENT = "Mozilla/5.0";

	private static boolean checkRequestString(String request) {

		if (request.contains(" "))
			return false;

		if (!request.startsWith("http"))
			return false;

		return true;
	}

	/**
	 * Request with a browser header. Try this method if request does not work
	 * (could be slower than request)
	 * 
	 * @param request
	 *            - String representing the whole HTTP-Request
	 * @return String representing the respond of the server
	 * @throws IOException
	 */
	public static String requestWithBrowserHeader(String request) throws IOException {
		if (!checkRequestString(request))
			return " ";

		System.out.println("Request: " + request);
		String strTemp;

		String total = " ";
		BufferedReader br = null;
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) (new URL(request).openConnection());
			urlConnection.setRequestMethod("GET");

			// add request header
			urlConnection.setRequestProperty("User-Agent", USER_AGENT);
			br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			while (null != (strTemp = br.readLine())) {
				total = total.concat(strTemp);

			}
			System.out.println(total);
		} catch (Exception ignore) {

		} finally {

			try {
				if (br != null)
					br.close();
				if (urlConnection != null) {
					urlConnection.disconnect();
					urlConnection.getInputStream().close();
				}
			} catch (IOException ignore) {

			}
		}

		if (total.equals(" "))
			throw new IOException();
		return total;
	}


	/**
	 * Executing HTTP-Request, returning the answer 
	 * 
	 * @param request
	 *            - String representing the whole HTTP-Request
	 * @return String representing the respond of the server
	 * @throws IOException
	 */

	public static String request(String request) throws IOException {
		if (!checkRequestString(request))
			return " ";
		System.out.println("Request: " + request);
		String strTemp = "";
		String total = " ";
		URL url = null;
		BufferedReader br = null;
		InputStreamReader inputStreamReader = null;
		try {
			url = new URL(request);
			inputStreamReader = new InputStreamReader(url.openStream());
			br = new BufferedReader(inputStreamReader);

			while (null != (strTemp = br.readLine())) {
				total = total.concat(strTemp);
				System.out.println(strTemp);
			}
		} catch (IOException ignore) {

		} finally {
			try {
				if (br != null)
					br.close();
				if (inputStreamReader != null)

					inputStreamReader.close();
			} catch (IOException ignore) {

			}
		}

		if (total.equals(" "))
			throw new IOException();

		return total;
	}
}
