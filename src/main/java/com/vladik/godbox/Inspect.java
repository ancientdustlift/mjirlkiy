package com.vladik.godbox;

import com.vladik.godbox.net.InspectPayloads;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class Inspect {
   public static final List<BiConsumer<LivingEntity, List<String>>> EXTRA = new ArrayList<>();
   static final Map<UUID, Inspect.Person> PEOPLE = new LinkedHashMap<>();
   static final Map<UUID, UUID> FOLLOW = new HashMap<>();
   private static int t;

   private Inspect() {
   }

   public static void init() {
      SaveExt.register("inspect", Inspect::save, Inspect::load, PEOPLE::clear);
      ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
         if (PEOPLE.containsKey(e.getUuid())) {
            Entity a = src.getAttacker();
            died(e, a == null ? "погиб" : "убит: " + a.getName().getString());
         }
      });
      EXTRA.add(Inspect::fame);
      EXTRA.add(Inspect::beliefs);
   }

   public static void registerNetwork() {
      PayloadTypeRegistry.playS2C().register(InspectPayloads.Profile.ID, InspectPayloads.Profile.CODEC);
      PayloadTypeRegistry.playS2C().register(InspectPayloads.FollowState.ID, InspectPayloads.FollowState.CODEC);
      PayloadTypeRegistry.playC2S().register(InspectPayloads.Ask.ID, InspectPayloads.Ask.CODEC);
      PayloadTypeRegistry.playC2S().register(InspectPayloads.Follow.ID, InspectPayloads.Follow.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(InspectPayloads.Ask.ID, (payload, ctx) -> {
         ServerPlayerEntity player = ctx.player();
         player.getServer().execute(() -> {
            try {
               ask(player, payload.id());
            } catch (Exception var3) {
               GodBox.LOG.error("GodBox inspect failed", (Throwable)var3);
            }
         });
      });
      ServerPlayNetworking.registerGlobalReceiver(InspectPayloads.Follow.ID, (payload, ctx) -> {
         ServerPlayerEntity player = ctx.player();
         player.getServer().execute(() -> follow(player, payload.id(), payload.on()));
      });
   }

   private static void msg(ServerPlayerEntity god, String s, Formatting f) {
      god.sendMessage(Text.literal(s).formatted(f), true);
   }

   static int year(Entity e) {
      return (int)(e.getWorld().getTime() / 600L);
   }

   static String yearText(int y) {
      return y >= 0 ? "год " + y : "за " + -y + " " + Life.years(-y) + " до летописи";
   }

   private static boolean named(MobEntity m) {
      return Life.tag(m, "gbn:") != null;
   }

   static String displayName(LivingEntity e) {
      if (Heroes.rank(e) > 0) {
         return Heroes.heroName(e);
      } else if (e instanceof MobEntity m && (named(m) || Kingdoms.of(m) != null)) {
         String rn = Life.tag(m, "gbr:");
         return Life.name(m) + (rn != null ? " " + rn : "");
      } else {
         return e.getName().getString();
      }
   }

   static Inspect.Person person(MobEntity m) {
      Inspect.Person p = PEOPLE.get(m.getUuid());
      Kingdoms.K k = Kingdoms.of(m);
      if (p == null) {
         if (k == null) {
            return null;
         }

         p = new Inspect.Person();
         p.id = m.getUuid();
         p.bornYear = year(m) - Life.age(m);
         String par = Life.tag(m, "gbp:");
         if (par != null) {
            p.note = "дитя " + par;
         }

         PEOPLE.put(p.id, p);
      }

      refresh(p, m, k);
      return p;
   }

   private static void refresh(Inspect.Person p, MobEntity m, Kingdoms.K k) {
      p.name = displayName(m).replace("|", "");
      if (k != null) {
         p.kingdom = k;
      }

      p.royal = k != null ? Dynasty.royal(k, m) : Life.tag(m, "gbroyal:") != null;
      if (p.diedYear >= 0) {
         p.diedYear = -1;
         p.note = "";
      }

      p.world = m.getWorld() instanceof ServerWorld w ? w : null;
      p.pos = m.getBlockPos();
      p.miss = 0;
   }

   public static void born(MobEntity child, MobEntity parent) {
      try {
         Inspect.Person p = person(child);
         if (p == null) {
            return;
         }

         p.bornYear = year(child);
         p.note = "";
         if (parent != null && parent != child && person(parent) != null) {
            p.parentA = parent.getUuid();
         }
      } catch (Exception var3) {
         GodBox.LOG.error("GodBox inspect: рождение", (Throwable)var3);
      }
   }

   public static void died(LivingEntity e, String note) {
      Inspect.Person p = PEOPLE.get(e.getUuid());
      if (p != null && p.diedYear < 0) {
         dead(p, year(e), note);
      }
   }

   private static void dead(Inspect.Person p, int year, String note) {
      p.diedYear = Math.max(year, p.bornYear);
      p.note = note == null ? "" : note.replace("|", "").replace("\n", " ");
      p.world = null;
      p.pos = null;
   }

   private static LivingEntity find(MinecraftServer s, UUID id) {
      for (ServerWorld w : s.getWorlds()) {
         if (w.getEntity(id) instanceof LivingEntity le && le.isAlive()) {
            return le;
         }
      }

      return null;
   }

   private static boolean inside(Inspect.Person p) {
      if (p.world != null && p.pos != null) {
         for (int dx = -32; dx <= 32; dx += 32) {
            for (int dz = -32; dz <= 32; dz += 32) {
               if (!p.world.shouldTickEntity(p.pos.add(dx, 0, dz))) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (!FOLLOW.isEmpty() && t % Math.max(1, Config.i("inspect.followTicks", 2)) == 0) {
         camera(s);
      }

      if (t % Math.max(40, Config.i("inspect.sweepTicks", 200)) == 0 && Perf.go) {
         int now = (int)(s.getOverworld().getTime() / 600L);

         for (Inspect.Person p : PEOPLE.values()) {
            if (p.diedYear < 0) {
               if (find(s, p.id) instanceof MobEntity m) {
                  refresh(p, m, Kingdoms.of(m));
               } else if (inside(p) && ++p.miss >= 3) {
                  dead(p, now, "сгинул");
               }
            }
         }

         prune();
      }
   }

   private static void prune() {
      int max = Math.max(100, Config.i("inspect.maxPeople", 4000));
      if (PEOPLE.size() > max) {
         Set<UUID> keep = new HashSet<>();
         ArrayDeque<UUID> todo = new ArrayDeque<>();

         for (Inspect.Person p : PEOPLE.values()) {
            if (p.diedYear < 0) {
               todo.add(p.id);
            }
         }

         while (!todo.isEmpty()) {
            Inspect.Person px = PEOPLE.get(todo.poll());
            if (px != null) {
               if (px.parentA != null && keep.add(px.parentA)) {
                  todo.add(px.parentA);
               }

               if (px.parentB != null && keep.add(px.parentB)) {
                  todo.add(px.parentB);
               }
            }
         }

         for (int pass = 0; pass < 3 && PEOPLE.size() > max; pass++) {
            Iterator<Inspect.Person> it = PEOPLE.values().iterator();

            while (it.hasNext() && PEOPLE.size() > max) {
               Inspect.Person px = it.next();
               if (pass == 2 || px.diedYear >= 0 && (pass == 1 || !keep.contains(px.id))) {
                  it.remove();
               }
            }
         }
      }
   }

   static void follow(ServerPlayerEntity god, UUID id, boolean on) {
      if (!on) {
         stopFollow(god, "\ud83d\udcf7 Слежка окончена");
      } else if (GodManager.isGod(god)) {
         Entity e = god.getServerWorld().getEntity(id);
         if (e instanceof LivingEntity le && le.isAlive() && e != god) {
            FOLLOW.put(god.getUuid(), id);
            String name = displayName(le);
            ServerPlayNetworking.send(god, new InspectPayloads.FollowState(true, name));
            msg(god, "\ud83d\udcf7 Камера следит: " + name, Formatting.AQUA);
         } else {
            msg(god, "Существо не найдено — следить не за кем", Formatting.GRAY);
         }
      }
   }

   static void stopFollow(ServerPlayerEntity god, String why) {
      if (FOLLOW.remove(god.getUuid()) != null) {
         ServerPlayNetworking.send(god, new InspectPayloads.FollowState(false, ""));
         if (why != null) {
            msg(god, why, Formatting.GRAY);
         }
      }
   }

   public static void onPower(ServerPlayerEntity god, Power power, boolean first) {
      if (first && !FOLLOW.isEmpty() && power != Power.INSPECT && power != Power.CIV_STATS) {
         stopFollow(god, null);
      }
   }

   private static void camera(MinecraftServer s) {
      for (Entry<UUID, UUID> en : new ArrayList<>(FOLLOW.entrySet())) {
         ServerPlayerEntity god = s.getPlayerManager().getPlayer(en.getKey());
         if (god == null) {
            FOLLOW.remove(en.getKey());
         } else if (!GodManager.isGod(god)) {
            stopFollow(god, null);
         } else if (god.getServerWorld().getEntity(en.getValue()) instanceof LivingEntity le && le.isAlive()) {
            double pitch = Math.toRadians(Math.max(35.0, Math.min(90.0, (double)god.getPitch())));
            double h = Math.max(4.0, god.getY() - le.getY());
            double back = Math.min(60.0, h / Math.max(0.05, Math.tan(pitch)));
            if (pitch > Math.toRadians(88.0)) {
               back = 0.0;
            }

            double yaw = Math.toRadians(god.getYaw());
            double x = le.getX() + Math.sin(yaw) * back;
            double z = le.getZ() - Math.cos(yaw) * back;
            double dx = x - god.getX();
            double dz = z - god.getZ();
            if (dx * dx + dz * dz > 0.36) {
               god.requestTeleport(x, god.getY(), z);
            }
         } else {
            stopFollow(god, "\ud83d\udcf7 Слежка окончена: существо погибло или скрылось из виду");
         }
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Vec3d c = Vec3d.ofCenter(pos).add(0.0, 1.0, 0.0);
      double r = Math.max(1.0, Config.d("inspect.radius", 5.0));
      LivingEntity best = null;
      LivingEntity bestCitizen = null;
      double bd = Double.MAX_VALUE;
      double bcd = Double.MAX_VALUE;

      for (LivingEntity e : Fx.living(w, c, r)) {
         if (e != god) {
            double d = e.squaredDistanceTo(c);
            if (d < bd) {
               bd = d;
               best = e;
            }

            if (d < bcd && Kingdoms.of(e) != null) {
               bcd = d;
               bestCitizen = e;
            }
         }
      }

      if (bestCitizen != null) {
         best = bestCitizen;
      }

      if (best == null) {
         msg(god, "Здесь некого осматривать", Formatting.GRAY);
      } else {
         ServerPlayNetworking.send(god, live(god, best));
      }
   }

   static void ask(ServerPlayerEntity god, UUID id) {
      if (GodManager.isGod(god)) {
         Entity e = god.getServerWorld().getEntity(id);
         if (e instanceof LivingEntity le && le.isAlive() && e != god) {
            ServerPlayNetworking.send(god, live(god, le));
         } else {
            Inspect.Person p = PEOPLE.get(id);
            if (p != null) {
               ServerPlayNetworking.send(god, record(god, p));
            }
         }
      }
   }

   private static String ageText(int a, boolean baby) {
      return a + " " + Life.years(a) + (baby ? " · ребёнок" : (a >= 50 ? " · старик" : " · взрослый"));
   }

   private static InspectPayloads.Profile live(ServerPlayerEntity god, LivingEntity e) {
      MobEntity m = e instanceof MobEntity mm ? mm : null;
      Kingdoms.K k = Kingdoms.of(e);
      Inspect.Person p = m != null ? person(m) : null;
      boolean known = m != null && (k != null || named(m));
      List<String> lines = new ArrayList<>();
      if (known) {
         lines.add("Возраст: " + ageText(Life.age(m), m.isBaby()));
      }

      boolean king = m != null && (m.getCommandTags().contains("gb_king") || k != null && Life.isKing(k, m));
      if (m != null && k != null) {
         String job;
         if (m.isBaby()) {
            job = "ребёнок";
         } else if (king) {
            job = "правитель";
         } else if (m instanceof VillagerEntity v) {
            job = Civ.jobTitle(v).isEmpty() ? "без дела" : Civ.jobTitle(v);
         } else {
            job = "воин";
         }

         lines.add("Занятие: " + job);
      }

      lines.add("Здоровье: " + (int)Math.ceil(e.getHealth()) + " / " + (int)Math.ceil(e.getMaxHealth()));
      if (known && m instanceof VillagerEntity) {
         lines.add("Характер: " + Life.TRAITS[Math.floorMod(Life.trait(m), Life.TRAITS.length)]);
      }

      StringBuilder tr = new StringBuilder();

      for (int i = 0; i < Traits.IDS.length; i++) {
         if (Traits.has(e, Traits.IDS[i])) {
            if (tr.length() > 0) {
               tr.append(", ");
            }

            tr.append(Traits.NAMES[i]);
         }
      }

      lines.add("Черты: " + (tr.length() == 0 ? "нет" : tr));
      if (m != null && k != null) {
         boolean royal = Dynasty.royal(k, m);
         if (king) {
            String rn = Life.tag(m, "gbr:");
            lines.add("Титул: правитель" + (rn != null ? ", " + Dynasty.first(m) + " " + rn : ""));
         }

         if (king || royal) {
            lines.add("Династия: " + Dynasty.houseName(k));
         }

         if (royal && !king) {
            lines.add("Королевская кровь: да");
         }
      }

      if (p != null) {
         lines.add("Родился: " + yearText(p.bornYear));
         if (p.parentA == null && p.parentB == null && m != null) {
            String par = Life.tag(m, "gbp:");
            if (par != null) {
               lines.add("Родитель: " + par);
            }
         }
      }

      for (BiConsumer<LivingEntity, List<String>> x : new ArrayList<>(EXTRA)) {
         try {
            x.accept(e, lines);
         } catch (Exception var12) {
            GodBox.LOG.error("GodBox inspect: доп. строки", (Throwable)var12);
         }
      }

      UUID f = FOLLOW.get(god.getUuid());
      return new InspectPayloads.Profile(
         e.getId(),
         e.getUuid(),
         displayName(e),
         k == null ? "" : k.title,
         k == null ? 16777215 : k.rgb,
         true,
         e.getUuid().equals(f),
         year(e),
         lines,
         p == null ? List.of() : tree(p)
      );
   }

   private static InspectPayloads.Profile record(ServerPlayerEntity god, Inspect.Person p) {
      List<String> lines = new ArrayList<>();
      boolean alive = p.diedYear < 0;
      int now = year(god);
      lines.add("Состояние: " + (alive ? "жив, но сейчас не на виду" : "умер"));
      lines.add("Родился: " + yearText(p.bornYear));
      if (alive) {
         int a = Math.max(0, now - p.bornYear);
         lines.add("Возраст: " + a + " " + Life.years(a));
      } else {
         int a = Math.max(0, p.diedYear - p.bornYear);
         lines.add("Умер: " + yearText(p.diedYear) + " (в " + a + " " + Life.years(a) + ")");
      }

      if (!p.note.isEmpty()) {
         lines.add((alive ? "Заметка: " : "Судьба: ") + p.note);
      }

      if (p.royal) {
         lines.add("Королевская кровь: да");
      }

      Kingdoms.K k = p.kingdom;
      return new InspectPayloads.Profile(-1, p.id, p.name, k == null ? "" : k.title, k == null ? 16777215 : k.rgb, alive, false, now, lines, tree(p));
   }

   private static void fame(LivingEntity e, List<String> lines) {
      int rank = Heroes.rank(e);
      Integer kills = Heroes.KILLS.get(e.getUuid());
      if (rank > 0) {
         lines.add("Слава: " + (rank == 2 ? "живая легенда" : "герой"));
      }

      if (kills != null && kills > 0) {
         lines.add("Побед: " + kills);
      }
   }

   private static void beliefs(LivingEntity e, List<String> lines) {
      Kingdoms.K k = Kingdoms.of(e);
      if (k != null && k.alive) {
         Civ.City c = Civ.city(k);
         if (c != null) {
            lines.add("Вера: " + Faith.religion(c));
            if (c.culture >= 0) {
               lines.add("Культура: " + Faith.culture(c));
            }
         }
      }
   }

   private static InspectPayloads.Node node(Inspect.Person p, int rel, UUID link) {
      return new InspectPayloads.Node(p.id, p.name, rel, link, p.diedYear < 0, p.bornYear, p.diedYear, p.kingdom == null ? 16777215 : p.kingdom.rgb, p.royal);
   }

   private static boolean childOf(Inspect.Person c, UUID parent) {
      return parent != null && (parent.equals(c.parentA) || parent.equals(c.parentB));
   }

   private static List<Inspect.Person> trim(List<Inspect.Person> in, int max) {
      in.sort((a, b) -> {
         int c = Boolean.compare(a.diedYear >= 0, b.diedYear >= 0);
         return c != 0 ? c : Integer.compare(b.bornYear, a.bornYear);
      });
      return in.size() > max ? in.subList(0, max) : in;
   }

   private static List<InspectPayloads.Node> tree(Inspect.Person self) {
      List<InspectPayloads.Node> out = new ArrayList<>();
      List<Inspect.Person> parents = new ArrayList<>();

      for (UUID u : new UUID[]{self.parentA, self.parentB}) {
         Inspect.Person p = u == null ? null : PEOPLE.get(u);
         if (p != null && p != self && !parents.contains(p)) {
            parents.add(p);
         }
      }

      for (Inspect.Person p : parents) {
         for (UUID ux : new UUID[]{p.parentA, p.parentB}) {
            Inspect.Person g = ux == null ? null : PEOPLE.get(ux);
            if (g != null && g != self && g != p) {
               out.add(node(g, 0, p.id));
            }
         }
      }

      for (Inspect.Person p : parents) {
         out.add(node(p, 1, self.id));
      }

      out.add(node(self, 2, self.id));
      List<Inspect.Person> sib = new ArrayList<>();
      List<Inspect.Person> kids = new ArrayList<>();

      for (Inspect.Person o : PEOPLE.values()) {
         if (o != self) {
            if (childOf(o, self.id)) {
               kids.add(o);
            } else if (childOf(o, self.parentA) || childOf(o, self.parentB)) {
               sib.add(o);
            }
         }
      }

      for (Inspect.Person ox : trim(sib, Math.max(0, Config.i("inspect.maxSiblings", 10)))) {
         out.add(node(ox, 3, self.id));
      }

      for (Inspect.Person ox : trim(kids, Math.max(0, Config.i("inspect.maxChildren", 16)))) {
         out.add(node(ox, 4, self.id));
      }

      return out;
   }

   private static String u(UUID id) {
      return id == null ? "-" : id.toString();
   }

   private static UUID u(String s) {
      return s.equals("-") ? null : UUID.fromString(s);
   }

   private static String field(String s) {
      return s.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
   }

   static List<String> save() {
      List<String> o = new ArrayList<>();

      for (Inspect.Person p : PEOPLE.values()) {
         o.add(
            p.id
               + "|"
               + (p.name.isEmpty() ? "?" : field(p.name))
               + "|"
               + u(p.parentA)
               + "|"
               + u(p.parentB)
               + "|"
               + (p.kingdom == null ? "-" : p.kingdom.name())
               + "|"
               + p.bornYear
               + "|"
               + p.diedYear
               + "|"
               + (p.royal ? 1 : 0)
               + "|"
               + (p.note.isEmpty() ? "-" : field(p.note))
         );
      }

      return o;
   }

   static void load(String[] a) {
      try {
         Inspect.Person p = new Inspect.Person();
         p.id = UUID.fromString(a[0]);
         p.name = a[1];
         p.parentA = u(a[2]);
         p.parentB = u(a[3]);

         try {
            if (!a[4].equals("-")) {
               p.kingdom = Kingdoms.K.valueOf(a[4]);
            }
         } catch (IllegalArgumentException var3) {
         }

         p.bornYear = Integer.parseInt(a[5]);
         p.diedYear = Integer.parseInt(a[6]);
         p.royal = a[7].equals("1");
         if (a.length > 8 && !a[8].equals("-")) {
            p.note = a[8];
         }

         PEOPLE.put(p.id, p);
      } catch (Exception var4) {
      }
   }

   public static void clear() {
      PEOPLE.clear();
      FOLLOW.clear();
      t = 0;
   }

   public static final class Person {
      public UUID id;
      public String name = "";
      public UUID parentA;
      public UUID parentB;
      public Kingdoms.K kingdom;
      public int bornYear;
      public int diedYear = -1;
      public String note = "";
      public boolean royal;
      ServerWorld world;
      BlockPos pos;
      int miss;
   }
}
