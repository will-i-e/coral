package com.linkedin.coral.presto.rel2presto;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.thrift.TException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static com.linkedin.coral.presto.rel2presto.TestUtils.*;
import static org.testng.Assert.*;

public class FuzzyUnionViewTest {

  RelToPrestoConverter rel2Presto;

  @BeforeTest
  public void beforeClass() throws HiveException, MetaException {
    TestUtils.initializeViews();
    rel2Presto = new RelToPrestoConverter();
  }

  @Test
  public void testNoSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testNoSchemaEvolutionWithMultipleTables() {
    String database = "fuzzy_union";
    String view = "union_view_with_more_than_two_tables";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT *\n"
        + "FROM (SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\")\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testNoSchemaEvolutionWithAlias() {
    String database = "fuzzy_union";
    String view = "union_view_with_alias";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablea\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testSingleBranchSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_single_branch_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tableb\"\n"
        + "UNION\n"
        + "SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablec\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testDoubleBranchSameSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_double_branch_evolved_same";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tabled\"\n"
        + "UNION\n"
        + "SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablee\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testDoubleBranchDifferentSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_double_branch_evolved_different";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablef\"\n"
        + "UNION\n"
        + "SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tableg\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testMoreThanTwoBranchesSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_more_than_two_branches_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT *\n"
        + "FROM (SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablef\"\n"
        + "UNION\n"
        + "SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tableg\")\n"
        + "UNION\n"
        + "SELECT \"a\", CAST(row(b.b1) as row(b1 varchar)) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablef\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testMapWithStructValueSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_map_with_struct_value_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", TRANSFORM_VALUES(b, (k, v) -> cast(row(v.b1) as row(b1 varchar))) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tableh\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablei\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testArrayWithStructValueSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_array_with_struct_value_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", TRANSFORM(b, x -> cast(row(x.b1) as row(b1 varchar))) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablej\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablek\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testDeeplyNestedStructSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_deeply_nested_struct_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", CAST(row(b.b1, cast(row(b.b2.b3, cast(row(b.b2.b4.b5) as row(b5 varchar))) as row(b3 varchar, b4 row(b5 varchar)))) as row(b1 varchar, b2 row(b3 varchar, b4 row(b5 varchar)))) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablel\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablem\")";

    assertTrue(expandedSql.contains(expectedSql));
  }

  @Test
  public void testDeeplyNestedComplexStructSchemaEvolution() {
    String database = "fuzzy_union";
    String view = "union_view_deeply_nested_complex_struct_evolved";
    RelNode relNode = TestUtils.toRelNodeFuzzyUnion(database, view);
    String expandedSql = rel2Presto.convert(relNode);

    String expectedSql = ""
        + "SELECT \"a\", \"b\"\n"
        + "FROM (SELECT \"a\", CAST(row(b.b1, transform_values(b.m1, (k, v) -> cast(row(v.b1, transform(v.a1, x -> cast(row(x.b1) as row(b1 varchar)))) as row(b1 varchar, a1 array(row(b1 varchar)))))) as row(b1 varchar, m1 map(varchar, row(b1 varchar, a1 array(row(b1 varchar)))))) AS \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tablen\"\n"
        + "UNION\n"
        + "SELECT \"a\", \"b\"\n"
        + "FROM \"hive\".\"fuzzy_union\".\"tableo\")";

    assertTrue(expandedSql.contains(expectedSql));
  }
}
