package io.truemark.http;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.InjectableValues.Std;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Partial implementation for RestClient.
 *
 * @author Abhijeet Kale
 */
@Slf4j
public abstract class JacksonRestClient implements RestClient {

  private static final long serialVersionUID = -8805834142603047857L;
  protected ObjectMapper objectMapper;
  protected InjectableValues injectableValues;

  public JacksonRestClient() {
    objectMapper = new ObjectMapper();
    if (log.isDebugEnabled()) {
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    injectableValues = new Std()
        .addValue("restClient", this);

  }

  protected <T> T readValue(String value, Class<T> clazz) throws IOException {
    return objectMapper.reader().with(injectableValues).withType(clazz).readValue(value);
  }

  protected <T> T readValue(InputStream inputStream, Class<T> clazz) throws IOException {
    return objectMapper.reader().with(injectableValues).withType(clazz).readValue(inputStream);
  }

  protected <T> T readValue(String value, Object o) throws IOException {
    return objectMapper.reader().with(injectableValues).withValueToUpdate(o).readValue(value);
  }

  protected <T> T readValue(InputStream inputStream, Object o) throws IOException {
    return objectMapper.reader().with(injectableValues).withValueToUpdate(o).readValue(
        inputStream);
  }

  public <C, P> C readValue(String value, Class<C> clazz, Class<P> parameterClass) throws
      IOException {
    JavaType type = objectMapper.getTypeFactory().constructParametricType(clazz, parameterClass);
    return objectMapper.reader().with(injectableValues).withType(type).readValue(value);
  }

  protected <C, P> C readValue(InputStream inputStream, Class<C> clazz,
                               Class<P> parameterClass) throws IOException {
    JavaType type = objectMapper.getTypeFactory().constructParametricType(clazz, parameterClass);
    return objectMapper.reader().with(injectableValues).withType(type).readValue(inputStream);
  }

  protected String writeValue(Object o) throws IOException {
    return objectMapper.writeValueAsString(o);
  }

  protected void writeValue(OutputStream out, Object o) throws IOException {
    objectMapper.writeValue(out, o);
  }

  protected <C extends Collection<T>, T> C readCollection(String value, Class<C> collectionClass,
                                                          Class<T> typeClass)
      throws IOException {
    return objectMapper.reader().with(injectableValues).withType(
        objectMapper.getTypeFactory().constructCollectionType(collectionClass, typeClass))
        .readValue(value);
  }

}
