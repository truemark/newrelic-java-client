package io.truemark.http;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Erik R. Jensen
 */
public class QueryStringBuilder {

  // This was set as a TreeMap to ensure a consistent order was maintained with parameters
  protected Map<String, Object> params = new TreeMap<String, Object>();

  /**
   * Adds public getters as parameters to the query string.
   *
   * @param o the object to use as query parameters
   * @return the QueryStringBuilder for method chaining
   */
  public QueryStringBuilder add(Object o) {
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(o.getClass(), Object.class);
      PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
      for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
        String name = propertyDescriptor.getName();
        Method method = propertyDescriptor.getReadMethod();
        if (method != null) {
          try {
            Object value = method.invoke(o);
            if (value != null) {
              add(name, value);
            }
          } catch (IllegalAccessException x) {
            throw new IllegalArgumentException(x);
          } catch (InvocationTargetException x) {
            throw new IllegalArgumentException(x);
          }
        }
      }
      return this;
    } catch (IntrospectionException x) {
      throw new IllegalArgumentException(x);
    }
  }

  /**
   * Adds a parameter to the query string.
   *
   * @param name  the parameter name
   * @param value the parameter value
   * @return the QueryStringBuilder for method chaining
   */
  public QueryStringBuilder add(String name, Object value) {
    params.put(name, value);
    return this;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  protected void append(StringBuilder sb, String key, Object value) {
    try {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(key)
          .append("=")
          .append(URLEncoder.encode(value.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException x) {
      throw new IllegalStateException(x); // this should never happen
    }
  }

  /**
   * Returns the query string to be used with an HTTP GET request.
   *
   * @return the query string
   */
  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      Object value = entry.getValue();
      String key = entry.getKey();
      if (value instanceof Object[]) {
        for (Object ovalue : ((Object[]) value)) {
          append(sb, key, ovalue);
        }
      } else if (value instanceof Collection) {
        for (Object ovalue : ((Collection) value)) {
          append(sb, key, ovalue);
        }
      } else {
        append(sb, key, value);
      }
    }
    return sb.toString();
  }

}
