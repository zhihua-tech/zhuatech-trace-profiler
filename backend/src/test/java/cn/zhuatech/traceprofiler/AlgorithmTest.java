/* 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.traceprofiler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

/** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
@SuppressWarnings("unchecked")
class AlgorithmTest {
  Map<String, Object> cfg;
  List<Map<String, Object>> data;

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @BeforeEach
  void load() throws Exception {
    var fixture =
        new ObjectMapper()
            .readValue(getClass().getResourceAsStream("/static/catalog.json"), Map.class);
    cfg = (Map<String, Object>) fixture.get("settings");
    data = (List<Map<String, Object>>) fixture.get("rows");
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void boundaryCases() {
    var xs =
        List.<Map<String, Object>>of(
            Map.of("id", "root", "parent", "", "start", 0, "end", 100),
            Map.of("id", "a", "parent", "b", "start", 1, "end", 2),
            Map.of("id", "b", "parent", "a", "start", 1, "end", 2));
    assertThrows(Model.Failure.class, () -> Algorithm.compute(cfg, xs));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void enterpriseEvidenceAndBoundaries() {
    var result=Algorithm.compute(Map.of(),List.of(Map.of("id","r","parent","","start",0,"end",100),
        Map.of("id","a","parent","r","start",10,"end",70),Map.of("id","b","parent","r","start",20,"end",60),
        Map.of("id","c","parent","r","start",30,"end",50)));
    var row=((List<Map<String,Object>>)result.get("rows")).stream().filter(r->"r".equals(r.get("id"))).findFirst().orElseThrow();
    assertEquals(60L,row.get("parallelExcessMs"));assertEquals(3,row.get("peakChildren"));assertEquals(40,row.get("selfMs"));
    var adjacent=Algorithm.compute(Map.of(),List.of(Map.of("id","r","parent","","start",0,"end",10),Map.of("id","a","parent","r","start",0,"end",5),Map.of("id","b","parent","r","start",5,"end",10)));
    assertEquals(1,((Map<?,?>)adjacent.get("metrics")).get("peakChildren"));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void knownResults() {
    var result = Algorithm.compute(cfg, data);
    var m = (Map<String, Object>) result.get("metrics");
    var out = (List<Map<String, Object>>) result.get("rows");
    var first = out.getFirst();
    assertEquals(350, ((Number) m.get("elapsedMs")).intValue());
    assertEquals(
        20,
        out.stream()
            .filter(r -> r.get("id").equals("chat"))
            .findFirst()
            .orElseThrow()
            .get("selfMs"));
    assertThrows(
        Model.Failure.class,
        () ->
            Algorithm.compute(
                cfg,
                List.of(
                    Map.of("id", "a", "parent", "", "start", 0, "end", 2),
                    Map.of("id", "b", "parent", "a", "start", 0, "end", 3))));
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @Test
  void rejectsInvalidInput() {
    data.getFirst().put("parent", "infer");
    assertThrows(Model.Failure.class, () -> Algorithm.compute(cfg, data));
  }
}
