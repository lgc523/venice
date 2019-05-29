package com.linkedin.venice.controllerapi;

import com.linkedin.venice.HttpMethod;
import com.linkedin.venice.utils.Time;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.exceptions.VeniceHttpException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.httpclient.HttpStatus;

import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.DeserializationConfig;

public class ControllerTransport implements AutoCloseable {
  private static final int CONNECTION_TIMEOUT_MS = 30 * Time.MS_PER_SECOND;
  private static final int DEFAULT_REQUEST_TIMEOUT_MS = 60 * Time.MS_PER_SECOND;

  private static final ObjectMapper objectMapper;
  private static final RequestConfig requestConfig;

  static {
    objectMapper = new ObjectMapper()
        .disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);

    requestConfig = RequestConfig.custom()
        .setConnectTimeout(CONNECTION_TIMEOUT_MS)
        .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
        .build();
  }

  private CloseableHttpAsyncClient httpClient;

  public ControllerTransport() {
    this.httpClient = HttpAsyncClients.custom()
        .setDefaultRequestConfig(this.requestConfig)
        .build();
    this.httpClient.start();
  }

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.httpClient);
  }

  public <T extends ControllerResponse> T request(String controllerUrl, ControllerRoute route, QueryParams params, Class<T> responseType)
      throws ExecutionException, TimeoutException {
    return request(controllerUrl, route, params, responseType, DEFAULT_REQUEST_TIMEOUT_MS);
  }

  public <T extends ControllerResponse> T request(String controllerUrl, ControllerRoute route, QueryParams params, Class<T> responseType, int timeoutMs)
      throws ExecutionException, TimeoutException {
    HttpMethod httpMethod = route.getHttpMethod();
    if (httpMethod.equals(HttpMethod.GET)) {
      return executeGet(controllerUrl, route.getPath(), params, responseType, timeoutMs);
    } else if (httpMethod.equals(HttpMethod.POST)) {
      return executePost(controllerUrl, route.getPath(), params, responseType, timeoutMs);
    }
    throw new VeniceException("Controller route specifies unsupported http method: " + httpMethod);
  }

  public <T extends ControllerResponse> T executeGet(String controllerUrl, String path, QueryParams params, Class<T> responseType)
      throws ExecutionException, TimeoutException {
    return executeGet(controllerUrl, path, params, responseType, DEFAULT_REQUEST_TIMEOUT_MS);
  }

  public <T extends ControllerResponse> T executeGet(String controllerUrl, String path, QueryParams params, Class<T> responseType, int timeoutMs)
      throws ExecutionException, TimeoutException {
    String encodedParams = URLEncodedUtils.format(params.getNameValuePairs(), StandardCharsets.UTF_8);
    HttpGet request = new HttpGet(controllerUrl + "/" + path + "?" + encodedParams);
    return executeRequest(request, responseType, timeoutMs);
  }

  public <T extends ControllerResponse> T executePost(String controllerUrl, String path, QueryParams params, Class<T> responseType)
      throws ExecutionException, TimeoutException {
    return executePost(controllerUrl, path, params, responseType, DEFAULT_REQUEST_TIMEOUT_MS);
  }

  public <T extends ControllerResponse> T executePost(String controllerUrl, String path, QueryParams params, Class<T> responseType, int timeoutMs)
      throws ExecutionException, TimeoutException {
    HttpPost request = new HttpPost(controllerUrl + "/" + path);
    try {
      request.setEntity(new UrlEncodedFormEntity(params.getNameValuePairs()));
    } catch (Exception e) {
      throw new VeniceException("Unable to encode controller query params", e);
    }
    return executeRequest(request, responseType, timeoutMs);
  }

  protected <T extends ControllerResponse> T executeRequest(HttpRequestBase request, Class<T> responseType, int timeoutMs)
      throws ExecutionException, TimeoutException {
    HttpResponse response;
    try {
      response = this.httpClient.execute(request, null).get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (ExecutionException | TimeoutException e) {
      throw e;
    } catch (InterruptedException e) {
      throw new ExecutionException(e);
    } catch (Exception e) {
      throw new VeniceException("Unable to submit controller request", e);
    } finally {
      request.abort();
    }

    int statusCode = response.getStatusLine().getStatusCode();

    ContentType contentType = ContentType.getOrDefault(response.getEntity());
    if (!contentType.getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())) {
      throw new VeniceHttpException(statusCode, "Controller returned unsupported content-type: " + contentType);
    }

    T result;
    try (InputStream content = response.getEntity().getContent()) {
      result = objectMapper.readValue(content, responseType);
    } catch (Exception e) {
      throw new VeniceHttpException(statusCode, "Unable to deserialize controller response", e);
    }

    if (result.isError()) {
      throw new VeniceHttpException(statusCode, result.getError());
    }

    if (statusCode != HttpStatus.SC_OK) {
      throw new VeniceHttpException(statusCode, "Controller returned an error: " + statusCode);
    }
    return result;
  }
}