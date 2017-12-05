package io.truemark.http;

import io.truemark.Version;
import io.truemark.error.NewRelicError;
import io.truemark.exception.NewRelicAuthenticationFailedException;
import io.truemark.exception.NewRelicBadRequestException;
import io.truemark.exception.NewRelicException;
import io.truemark.exception.NewRelicForbiddenException;
import io.truemark.exception.NewRelicNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;


/**
 * A simple HTTP client used to send get, update and create requests. This class uses
 * HttpURLConnection internally.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public class URLConnectionRestClient extends JacksonRestClient implements Serializable {

  //TODO add serial version id
  private static final Charset UTF8 = Charset.forName("UTF-8");
  public static final String USER_AGENT = "NewRelic Java Client " + Version.VERSION;
  protected String url;
  protected String apiKey;

  /**
   * Creates a new RestClient client.
   *
   * @param url the base url
   */
  public URLConnectionRestClient(String url, String apiKey) {
    this.url = url;
    this.apiKey = apiKey;
  }

  protected void logRequest(HttpURLConnection conn, String body) {
    StringBuilder sb = new StringBuilder("\nHTTP Request:\n")
        .append("  URL: ").append(conn.getURL()).append("\n")
        .append("  Request Method: ").append(conn.getRequestMethod()).append("\n");
    Map<String, List<String>> headers = conn.getRequestProperties();
    sb.append("  Request Headers:\n");
    for (String key : headers.keySet()) {
      if (key != null) {
        sb.append("    ").append(key).append(": ")
            .append(headers.get(key)).append("\n");
      }
    }
    sb.append("  Request Body:\n").append(body);
    log.debug(sb.toString());
  }

  protected void logResponse(HttpURLConnection conn, String body) throws IOException {
    StringBuilder sb = new StringBuilder("\nHTTP Response:\n")
        .append("  Response Code: ").append(conn.getResponseCode()).append("\n");
    Map<String, List<String>> headers = conn.getHeaderFields();
    sb.append("  Response Headers:\n");
    for (String key : headers.keySet()) {
      if (key != null) {
        sb.append("    ").append(key).append(": ")
            .append(headers.get(key)).append("\n");
      }
    }
    sb.append("  Response Body:\n").append(body);
    log.debug(sb.toString());
  }

  /**
   * Helper method to read the contents of an InputStream to a String.
   * This method will not close the stream.
   *
   * @param in the InputStream to rea
   * @return the contents of the stream as a String
   * @throws IOException if an I/O error occurs
   */
  protected String readString(InputStream in) throws IOException {
    InputStreamReader reader = new InputStreamReader(in, UTF8);
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[8192];
    for (int read = reader.read(buf); read >= 0; read = reader.read(buf)) {
      sb.append(buf, 0, read);
    }
    return sb.toString();
  }

  /**
   * Helper method to completely read the error stream.
   *
   * @param conn the connection
   * @return the error message or null
   */
  protected String readError(HttpURLConnection conn) {
    InputStream err = null;
    try {
      err = conn.getErrorStream();
      if (err != null) {
        return readString(err);
      }
    } catch (IOException x) {
      log.warn("An I/O error occurred reading the HTTP error stream: " + x.getMessage(), x);
    } finally {
      if (err != null) {
        try {
          err.close();
        } catch (IOException x) { /* do nothing */ }
      }
    }
    return null;
  }

  /**
   * Used for error handling.
   *
   * @param x    the exception thrown or null
   * @param conn the connection
   * @return the exception to throw
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  protected IOException getError(IOException x, HttpURLConnection conn) throws IOException {
    if (conn != null) {
      String error = readError(conn);
      if (log.isDebugEnabled()) {
        logResponse(conn, error);
      }
      int status = conn.getResponseCode();
      switch (status) {
        case 401:
          return new NewRelicAuthenticationFailedException(error);
        case 404:
          return new NewRelicNotFoundException(error);
        case 400:
          if (error != null) {
            return new NewRelicBadRequestException("Received error: " + error,
                readCollection(error, List.class, NewRelicError.class));
          }
          throw new NewRelicBadRequestException();
        case 403:
          return new NewRelicForbiddenException(error); // TODO
        default:
          return new NewRelicException(error, x);
      }
    }
    return x;
  }

  /**
   * Helper method to setup the connection.
   *
   * @param method the HTTP method to use
   * @param url    the URL to query
   * @return the configured connection
   * @throws IOException if an I/O error occurs
   */
  protected HttpURLConnection setup(HttpMethod method, String url) throws IOException {
    if (!url.startsWith("http")) {
      url = this.url + url;
    }
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("User-Agent", USER_AGENT);
    conn.setRequestMethod(method.toString());
    conn.setConnectTimeout(120 * 1000); // 2 minutes // TODO Make configurable
    conn.setReadTimeout(120 * 1000); // 2 minutes // TODO Make configurable
    conn.setRequestProperty("X-Api-Key", apiKey);

    return conn;
  }

  /**
   * Helper method to cleanup connection resources after use.
   *
   * @param conn the connection or null
   * @param in   the input stream or null
   * @param out  the output stream or null
   */
  protected void cleanup(HttpURLConnection conn, InputStream in, OutputStream out) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException x) { /* do nothing */ }
    }
    if (out != null) {
      try {
        out.close();
      } catch (IOException x) { /* do nothing */ }
    }
    if (conn != null) {
      conn.disconnect();
    }
  }

  @Override
  public <T> T get(String uri, T o) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = setup(HttpMethod.GET, uri);
      if (log.isDebugEnabled()) {
        logRequest(conn, null);
      }
      conn.connect();
      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, o);
      } else {
        return readValue(in, o);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, null);
    }
  }

  @Override
  public <T> T get(String uri, Class<T> clazz) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = setup(HttpMethod.GET, uri);
      if (log.isDebugEnabled()) {
        logRequest(conn, null);
      }
      conn.connect();
      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, clazz);
      } else {
        return readValue(in, clazz);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, null);
    }
  }

  @Override
  public <T, S> T get(String uri, Class<T> clazz, Class<S> parameterClass) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = setup(HttpMethod.GET, uri);
      if (log.isDebugEnabled()) {
        logRequest(conn, null);
      }
      conn.connect();
      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, clazz, parameterClass);
      } else {
        return readValue(in, clazz, parameterClass);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, null);
    }
  }

  @Override
  public <T> T update(String uri, T o) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      conn = setup(HttpMethod.PUT, uri);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      if (log.isDebugEnabled()) {
        String body = writeValue(o);
        logRequest(conn, body);
        conn.connect();
        out = conn.getOutputStream();
        out.write(body.getBytes(UTF8));
      } else {
        conn.connect();
        out = conn.getOutputStream();
        writeValue(out, o);
      }

      in = conn.getInputStream();
      String res = readString(in);
      if (res != null && !res.isEmpty()) {
        if (log.isDebugEnabled()) {
          logResponse(conn, res);
          readValue(res, o);
        } else {
          readValue(res, o);
        }
      }
      return o;
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, out);
    }
  }

  @Override
  public <T, U> T patch(String uri, Class<T> clazz, U u) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      conn = setup(HttpMethod.PATCH, uri);
      conn.setDoInput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      if (log.isDebugEnabled()) {
        String body = writeValue(u);
        logRequest(conn, body);
        conn.connect();
        out = conn.getOutputStream();
        out.write(body.getBytes(UTF8));
      } else {
        conn.connect();
        out = conn.getOutputStream();
        writeValue(out, u);
      }

      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(in, clazz);
      } else {
        return readValue(in, clazz);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, out);
    }
  }

  @Override
  public <V, T> V create(String uri, T o, Class<V> clazz) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      conn = setup(HttpMethod.POST, uri);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      if (log.isDebugEnabled()) {
        String body = writeValue(o);
        logRequest(conn, body);
        conn.connect();
        out = conn.getOutputStream();
        out.write(body.getBytes(UTF8));
      } else {
        conn.connect();
        out = conn.getOutputStream();
        writeValue(out, o);
      }

      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, clazz);
      } else {
        return readValue(in, clazz);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, out);
    }
  }

  @Override
  public <T> T create(String uri, T o) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      conn = setup(HttpMethod.POST, uri);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      if (log.isDebugEnabled()) {
        String body = writeValue(o);
        logRequest(conn, body);
        conn.connect();
        out = conn.getOutputStream();
        out.write(body.getBytes(UTF8));
      } else {
        conn.connect();
        out = conn.getOutputStream();
        writeValue(out, o);
      }

      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, o);
      } else {
        return readValue(in, o);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, out);
    }
  }

  @Override
  public void delete(String uri) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = setup(HttpMethod.DELETE, uri);
      logRequest(conn, null);
      conn.connect();
      in = conn.getInputStream();
      readString(in); // read the stream to completion
      logResponse(conn, null);
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, null);
    }
  }

  @Override
  public void execute(String uri) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = setup(HttpMethod.POST, uri);
      logRequest(conn, null);
      conn.connect();
      in = conn.getInputStream();
      readString(in); // read the stream to completion
      logResponse(conn, null);
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, null);
    }
  }

  @Override
  public <T> T postForObject(String uri, Object data, Class<T> clazz) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    OutputStream out = null;
    try {
      conn = setup(HttpMethod.POST, uri);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      if (log.isDebugEnabled()) {
        String body = writeValue(data);
        logRequest(conn, body);
        conn.connect();
        out = conn.getOutputStream();
        out.write(body.getBytes(UTF8));
      } else {
        conn.connect();
        out = conn.getOutputStream();
        writeValue(out, data);
      }

      in = conn.getInputStream();
      if (log.isDebugEnabled()) {
        String res = readString(in);
        logResponse(conn, res);
        return readValue(res, clazz);
      } else {
        return readValue(in, clazz);
      }
    } catch (IOException x) {
      throw getError(x, conn);
    } finally {
      cleanup(conn, in, out);
    }
  }

}
