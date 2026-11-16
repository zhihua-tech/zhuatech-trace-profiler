/* 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.traceprofiler;

import static cn.zhuatech.traceprofiler.Checks.*;

import java.util.*;

/**
 * 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 验证调用树、时间边界，合并并行子区间，计算每段独占耗时并列出热点。
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Algorithm {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  record Span(String id, String parent, int start, int end) {}

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  static Map<String, Object> compute(Map<String, Object> cfg, List<Map<String, Object>> rows) {
    unique(rows);
    var spans = new TreeMap<String, Span>();
    var children = new HashMap<String, List<Span>>();
    for (var r : rows) {
      int a = integer(r, "start", 0, 2000000000), b = integer(r, "end", a, 2000000000);
      var s = new Span(str(r, "id", false), str(r, "parent", true), a, b);
      spans.put(s.id(), s);
      children.computeIfAbsent(s.parent(), k -> new ArrayList<>()).add(s);
    }
    var roots = children.getOrDefault("", List.of());
    must(roots.size() == 1, "必须有一个根 span");
    for (var s : spans.values()) {
      var seen = new HashSet<String>();
      var x = s;
      while (!x.parent().isEmpty()) {
        must(seen.add(x.id()), "调用链存在环");
        var p = spans.get(x.parent());
        must(p != null && p.start() <= x.start() && p.end() >= x.end(), "父节点不存在或时间区间越界");
        x = p;
      }
    }
    var out = new ArrayList<Map<String, Object>>();
    int batchPeak = 0;
    for (var s : spans.values()) {
      int frontier = s.start(), covered = 0;
      long childTime = 0;
      var boundaries = new TreeMap<Integer,Integer>();
      for (var c :
          children.getOrDefault(s.id(), List.of()).stream()
              .sorted(Comparator.comparingInt(Span::start))
              .toList()) {
        childTime += (long)c.end() - c.start();
        boundaries.merge(c.start(), 1, Integer::sum);
        boundaries.merge(c.end(), -1, Integer::sum);
        covered += Math.max(0, c.end() - Math.max(frontier, c.start()));
        frontier = Math.max(frontier, c.end());
      }
      int live = 0, peak = 0;
      for (int change : boundaries.values()) { live += change; peak = Math.max(peak,live); }
      batchPeak = Math.max(batchPeak, peak);
      out.add(
          Map.of(
              "id",
              s.id(),
              "parent",
              s.parent(),
              "durationMs",
              s.end() - s.start(),
              "childrenUnionMs",
              covered,
              "selfMs",
              s.end() - s.start() - covered, "parallelExcessMs", childTime - covered, "peakChildren", peak));
    }
    out.sort(
        Comparator.comparingInt((Map<String, Object> r) -> (Integer) r.get("selfMs")).reversed());
    return result(
        Map.of(
            "elapsedMs",
            roots.getFirst().end() - roots.getFirst().start(),
            "spans",
            rows.size(),
            "peakChildren", batchPeak,
            "hottest",
            out.getFirst().get("id")),
        out);
  }
}
