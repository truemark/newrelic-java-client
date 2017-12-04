package io.truemark.http;

import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit test for QueryStringBuilder.
 *
 * @author Abhijeet Kale
 */
public class QueryStringBuilderTest {

  @Data
  @Accessors(chain = true)
  public class TestObject {
    private String test1;
    private String test2;
  }

  @Test
  public void testToQueryString() {
    QueryStringBuilder qb = new QueryStringBuilder();
    qb.add(new TestObject().setTest1("test1").setTest2("test2"));
    String queryString = qb.toQueryString();
    assertThat(queryString, equalTo("test1=test1&test2=test2"));
  }

  @Test
  public void testEncoddedQueryString1() {
    QueryStringBuilder qb = new QueryStringBuilder();
    qb.add("test1", "m&m");
    String queryString = qb.toQueryString();
    assertThat(queryString, equalTo("test1=m%26m"));
  }

  @Test
  public void testEncodedQuwryString2() {
    QueryStringBuilder qb = new QueryStringBuilder();
    qb.add(new TestObject().setTest1("m&m").setTest2("x=y"));
    String queryString = qb.toQueryString();
    assertThat(queryString, equalTo("test1=m%26m&test2=x%3Dy"));
  }
}
