package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

final class RoadNet {
   static final int NONE = Integer.MIN_VALUE;
   private static final int[][] DIRS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

   private RoadNet() {
   }

   static List<int[]> route(RoadNet.Terrain t, int ax, int az, int bx, int bz, int budget) {
      int S = 6;
      double dx = bx - ax;
      double dz = bz - az;
      double len = Math.sqrt(dx * dx + dz * dz);
      List<int[]> out = new ArrayList<>();
      out.add(new int[]{ax, az});
      if (len < 12.0) {
         out.add(new int[]{bx, bz});
         return out;
      } else {
         double ux = dx / len;
         double uz = dz / len;
         double corr = Math.max(30.0, len * 0.25);
         Map<Long, RoadNet.Node> seen = new HashMap<>();
         Map<Long, Integer> hc = new HashMap<>();
         Map<Long, Double> pc = new HashMap<>();
         PriorityQueue<RoadNet.Node> open = new PriorityQueue<>((p, q) -> Double.compare(p.f, q.f));
         RoadNet.Node s0 = new RoadNet.Node(0, 0);
         seen.put(0L, s0);
         open.add(s0);
         RoadNet.Node goal = null;

         for (int ex = 0; ex < budget && !open.isEmpty(); ex++) {
            RoadNet.Node n = open.poll();
            int x = ax + n.gx * 6;
            int z = az + n.gz * 6;
            if (Math.hypot(bx - x, bz - z) <= 9.0) {
               goal = n;
               break;
            }

            int h1 = cachedH(t, hc, x, z);

            for (int[] d : DIRS) {
               int gx = n.gx + d[0];
               int gz = n.gz + d[1];
               int px = ax + gx * 6;
               int pz = az + gz * 6;
               double along = (px - ax) * ux + (pz - az) * uz;
               double perp = Math.abs(-(px - ax) * uz + (pz - az) * ux);
               if (!(along < -12.0) && !(along > len + 12.0) && !(perp > corr)) {
                  double L = 6.0 * Math.sqrt(d[0] * d[0] + d[1] * d[1]);
                  double pen = 0.0;
                  boolean blocked = false;

                  for (int k = 1; k <= 3 && !blocked; k++) {
                     int sx = x + (px - x) * k / 3;
                     int sz = z + (pz - z) * k / 3;
                     double p = pc.computeIfAbsent(RoadNet.BlockKey.of(sx, sz), q -> t.pen(sx, sz));
                     if (p < 0.0) {
                        blocked = true;
                     } else {
                        pen += p / 3.0;
                     }
                  }

                  if (!blocked) {
                     int h2 = cachedH(t, hc, px, pz);
                     double ratio = h1 != Integer.MIN_VALUE && h2 != Integer.MIN_VALUE ? Math.abs(h2 - h1) / L : 0.0;
                     double cost = L * (1.0 + pen + 6.0 * ratio * ratio) + (ratio > 0.8 ? L * 20.0 : 0.0);
                     long key = RoadNet.BlockKey.of(gx, gz);
                     RoadNet.Node o = seen.get(key);
                     double g = n.g + cost;
                     if (o == null || !(o.g <= g)) {
                        RoadNet.Node nn = new RoadNet.Node(gx, gz);
                        nn.g = g;
                        nn.f = g + 1.15 * Math.hypot(bx - px, bz - pz);
                        nn.parent = n;
                        seen.put(key, nn);
                        open.add(nn);
                     }
                  }
               }
            }
         }

         if (goal == null) {
            return null;
         } else {
            List<int[]> rev = new ArrayList<>();

            for (RoadNet.Node n = goal; n != null; n = n.parent) {
               rev.add(new int[]{ax + n.gx * 6, az + n.gz * 6});
            }

            Collections.reverse(rev);
            rev.set(0, new int[]{ax, az});
            if (rev.size() > 1 && Math.hypot(bx - rev.get(rev.size() - 1)[0], bz - rev.get(rev.size() - 1)[1]) < 4.5) {
               rev.remove(rev.size() - 1);
            }

            rev.add(new int[]{bx, bz});
            return rev;
         }
      }
   }

   private static int cachedH(RoadNet.Terrain t, Map<Long, Integer> c, int x, int z) {
      return c.computeIfAbsent(RoadNet.BlockKey.of(x, z), k -> t.height(x, z));
   }

   static List<int[]> smooth(List<int[]> wp) {
      List<double[]> p = new ArrayList<>();

      for (int[] q : wp) {
         p.add(new double[]{q[0], q[1]});
      }

      for (int it = 0; it < 2 && p.size() > 2; it++) {
         List<double[]> n = new ArrayList<>();
         n.add(p.get(0));

         for (int i = 0; i + 1 < p.size(); i++) {
            double[] a = p.get(i);
            double[] b = p.get(i + 1);
            if (i > 0) {
               n.add(new double[]{0.75 * a[0] + 0.25 * b[0], 0.75 * a[1] + 0.25 * b[1]});
            }

            if (i + 2 < p.size()) {
               n.add(new double[]{0.25 * a[0] + 0.75 * b[0], 0.25 * a[1] + 0.75 * b[1]});
            }
         }

         n.add(p.get(p.size() - 1));
         p = n;
      }

      List<int[]> out = new ArrayList<>();
      int[] prev = null;

      for (double[] q : p) {
         int x = (int)Math.round(q[0]);
         int z = (int)Math.round(q[1]);
         if (prev == null) {
            out.add(new int[]{x, z});
            prev = out.get(0);
         } else {
            line(out, prev[0], prev[1], x, z);
            prev = out.get(out.size() - 1);
         }
      }

      return out;
   }

   private static void line(List<int[]> out, int x0, int z0, int x1, int z1) {
      int dx = Math.abs(x1 - x0);
      int dz = Math.abs(z1 - z0);
      int sx = x0 < x1 ? 1 : -1;
      int sz = z0 < z1 ? 1 : -1;
      int err = dx - dz;
      int x = x0;
      int z = z0;

      while (x != x1 || z != z1) {
         int e2 = 2 * err;
         if (e2 > -dz) {
            err -= dz;
            x += sx;
         }

         if (e2 < dx) {
            err += dx;
            z += sz;
         }

         out.add(new int[]{x, z});
      }
   }

   static boolean solveDeck(int[] raw, boolean[] water, int[] deck, int from, int to, int look) {
      int n = raw.length;
      int lo = Math.max(0, from - look);
      int hi = Math.min(n, to + look);
      int m = hi - lo;
      double[] v = new double[m];
      boolean[] known = new boolean[m];
      boolean[] pin = new boolean[m];

      for (int j = lo; j < hi; j++) {
         int k = j - lo;
         boolean fixed = deck[j] != Integer.MIN_VALUE && (j < from || j >= to);
         if (fixed) {
            v[k] = deck[j];
            known[k] = true;
            pin[k] = true;
         } else if (raw[j] != Integer.MIN_VALUE) {
            v[k] = raw[j];
            known[k] = true;
         }
      }

      int jx = lo;

      while (jx < hi) {
         int k = jx - lo;
         if (water[jx] && raw[jx] != Integer.MIN_VALUE && !pin[k]) {
            int e = jx;

            int top;
            for (top = raw[jx]; e < hi && water[e] && !pin[e - lo]; e++) {
               if (raw[e] != Integer.MIN_VALUE) {
                  top = Math.max(top, raw[e]);
               }
            }

            for (int q = jx; q < e; q++) {
               v[q - lo] = top;
               known[q - lo] = true;
               pin[q - lo] = true;
            }

            jx = e;
         } else {
            jx++;
         }
      }

      jx = -1;

      for (int k = 0; k < m; k++) {
         if (known[k]) {
            jx = k;
            break;
         }
      }

      if (jx < 0) {
         return false;
      } else {
         for (int kx = 0; kx < m; kx++) {
            if (!known[kx]) {
               int a = kx - 1;
               int b = kx + 1;

               while (a >= 0 && !known[a]) {
                  a--;
               }

               while (b < m && !known[b]) {
                  b++;
               }

               if (a < 0) {
                  v[kx] = v[b];
               } else if (b >= m) {
                  v[kx] = v[a];
               } else {
                  v[kx] = v[a] + (v[b] - v[a]) * (kx - a) / (b - a);
               }
            }
         }

         double[] s = new double[m];

         for (int kxx = 0; kxx < m; kxx++) {
            if (pin[kxx]) {
               s[kxx] = v[kxx];
            } else {
               double sum = 0.0;
               int c = 0;

               for (int d = -4; d <= 4; d++) {
                  if (kxx + d >= 0 && kxx + d < m) {
                     sum += v[kxx + d];
                     c++;
                  }
               }

               s[kxx] = Math.rint(sum / c);
            }
         }

         for (int round = 0; round < 3; round++) {
            for (int kxxx = 1; kxxx < m; kxxx++) {
               if (!pin[kxxx]) {
                  s[kxxx] = clamp(s, kxxx, -1, -2);
               }
            }

            for (int kxxxx = m - 2; kxxxx >= 0; kxxxx--) {
               if (!pin[kxxxx]) {
                  s[kxxxx] = clamp(s, kxxxx, 1, 2);
               }
            }
         }

         for (int jxx = from; jxx < to; jxx++) {
            deck[jxx] = (int)s[jxx - lo];
         }

         return true;
      }
   }

   private static double clamp(double[] s, int k, int d1, int d2) {
      double lo = -1.0E9;
      double hi = 1.0E9;
      int m = s.length;

      for (int d : new int[]{d1, d2}) {
         int q = k + d;
         if (q >= 0 && q < m) {
            lo = Math.max(lo, s[q] - 1.0);
            hi = Math.min(hi, s[q] + 1.0);
         }
      }

      return lo > hi ? s[k] : Math.max(lo, Math.min(hi, s[k]));
   }

   private static final class BlockKey {
      static long of(int x, int z) {
         return (long)x << 32 ^ z & 4294967295L;
      }
   }

   private static final class Node {
      final int gx;
      final int gz;
      double g;
      double f;
      RoadNet.Node parent;

      Node(int gx, int gz) {
         this.gx = gx;
         this.gz = gz;
      }
   }

   interface Terrain {
      int height(int var1, int var2);

      double pen(int var1, int var2);
   }
}
