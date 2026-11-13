package com.vladik.godbox;

import com.vladik.godbox.mixin.DisplayEntityAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Models {
   public static final String PART_TAG = "godbox_part";
   public static final String MODEL_TAG = "godbox_model_";
   public static final String EXTRA_TAG = "godbox_extra_";
   private static final int ANIM_PERIOD = 2;
   private static final int LERP_TICKS = 3;
   private static final double VIEW_DIST = 200.0;
   private static Item item;
   private static final Map<LivingEntity, List<Models.Inst>> MODELS = new HashMap<>();
   private static final Set<Entity> PARTS = Collections.newSetFromMap(new IdentityHashMap<>());

   private Models() {
   }

   public static void init() {
      item = Registry.register(Registries.ITEM, Identifier.of("godbox", "model"), new Item(new Settings()));
      custom();
   }

   public static boolean custom() {
      return Config.b("models.custom", true);
   }

   public static boolean attach(LivingEntity carrier, String creature) {
      return attach(carrier, creature, true);
   }

   public static boolean attachCustom(LivingEntity carrier, String creature) {
      return custom() && attach(carrier, creature, true);
   }

   private static String headKind(LivingEntity e) {
      if (e instanceof VillagerEntity || e instanceof ZombieVillagerEntity) {
         return "villager";
      } else if (e instanceof IllagerEntity || e instanceof WanderingTraderEntity) {
         return "tall";
      } else {
         return !(e instanceof ZombieEntity) && !(e instanceof AbstractSkeletonEntity) && !(e instanceof AbstractPiglinEntity) ? null : "biped";
      }
   }

   public static ItemStack hat(String name, LivingEntity wearer) {
      ModelParts.Creature def = ModelParts.get("hats");
      String kind = headKind(wearer);
      if (def != null && item != null && kind != null && custom()) {
         String part = name + "_" + kind;

         for (ModelParts.Part p : def.parts()) {
            if (p.name().equals(part)) {
               ItemStack stack = new ItemStack(item);
               stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(p.cmd()));
               return stack;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean equipHat(MobEntity m, String name) {
      ItemStack stack = hat(name, m);
      if (stack == null) {
         return false;
      } else {
         m.equipStack(EquipmentSlot.HEAD, stack);
         m.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
         return true;
      }
   }

   public static ItemStack part(String creature, String name) {
      ModelParts.Creature def = ModelParts.get(creature);
      if (def != null && item != null && custom()) {
         for (ModelParts.Part p : def.parts()) {
            if (p.name().equals(name)) {
               ItemStack stack = new ItemStack(item);
               stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(p.cmd()));
               return stack;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   /** A free-standing, unsaved display for a model part that no creature carries, such as a projectile. */
   public static ItemDisplayEntity prop(ServerWorld w, String creature, String name, double x, double y, double z) {
      ItemStack stack = part(creature, name);
      if (stack == null) {
         return null;
      }

      ItemDisplayEntity e = new ItemDisplayEntity(EntityType.ITEM_DISPLAY, w) {
         @Override
         public boolean shouldSave() {
            return false;
         }
      };
      e.setItemStack(stack);
      e.setTeleportDuration(1);
      e.setInterpolationDuration(1);
      e.setViewRange(8.0F);
      e.setDisplayWidth(24.0F);
      e.setDisplayHeight(24.0F);
      e.setBrightness(new Brightness(15, 15));
      e.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
      return w.spawnEntity(e) ? e : null;
   }

   public static void pose(ItemDisplayEntity e, Quaternionf rotation, float scale) {
      e.setTransformation(new AffineTransformation(new Vector3f(), rotation, new Vector3f(scale, scale, scale), new Quaternionf()));
      e.getDataTracker().set(DisplayEntityAccessor.godbox$startInterpolation(), 0, true);
   }

   public static boolean isHat(ItemStack stack) {
      return item != null && stack.isOf(item);
   }

   public static boolean attachExtra(LivingEntity carrier, String creature) {
      return attach(carrier, creature, false);
   }

   private static boolean attach(LivingEntity carrier, String creature, boolean main) {
      ModelParts.Creature def = ModelParts.get(creature);
      if (def != null && item != null && carrier.isAlive() && carrier.getWorld() instanceof ServerWorld w) {
         List<Models.Inst> list = MODELS.computeIfAbsent(carrier, c -> new ArrayList<>());
         Iterator in = list.iterator();

         while (in.hasNext()) {
            Models.Inst old = (Models.Inst)in.next();
            if (old.def == def && old.main == main) {
               return true;
            }

            if (main && old.main) {
               carrier.removeCommandTag("godbox_model_" + old.def.name());
               discard(old);
               in.remove();
            }
         }

         Models.Inst inx = new Models.Inst(carrier, def, main);
         pose(inx, true);

         for (int i = 0; i < inx.ents.length; i++) {
            inx.ents[i] = spawn(w, inx, i);
         }

         follow(inx);
         list.add(inx);
         carrier.addCommandTag((main ? "godbox_model_" : "godbox_extra_") + creature);
         if (main && def.hideCarrier()) {
            carrier.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 0, false, false));
         }

         return true;
      } else {
         return false;
      }
   }

   public static void detach(LivingEntity carrier) {
      List<Models.Inst> list = MODELS.remove(carrier);
      if (list != null) {
         for (Models.Inst in : list) {
            discard(in);
            carrier.removeCommandTag((in.main ? "godbox_model_" : "godbox_extra_") + in.def.name());
            if (in.main && in.def.hideCarrier()) {
               carrier.removeStatusEffect(StatusEffects.INVISIBILITY);
            }
         }
      }
   }

   public static boolean has(LivingEntity carrier) {
      return MODELS.containsKey(carrier);
   }

   public static boolean owns(Entity part) {
      return PARTS.contains(part);
   }

   public static void trigger(LivingEntity carrier) {
      List<Models.Inst> list = MODELS.get(carrier);
      if (list != null) {
         for (Models.Inst in : list) {
            in.strikeAt = in.age;
         }
      }
   }

   public static void reattach(LivingEntity carrier) {
      if (!MODELS.containsKey(carrier) && carrier.isAlive()) {
         if (custom()) {
            for (String tag : new ArrayList<>(carrier.getCommandTags())) {
               if (tag.startsWith("godbox_model_")) {
                  attach(carrier, tag.substring("godbox_model_".length()), true);
               } else if (tag.startsWith("godbox_extra_")) {
                  attach(carrier, tag.substring("godbox_extra_".length()), false);
               }
            }
         } else {
            for (String tagx : new ArrayList<>(carrier.getCommandTags())) {
               if (tagx.startsWith("godbox_model_") || tagx.startsWith("godbox_extra_")) {
                  carrier.removeCommandTag(tagx);
                  ModelParts.Creature def = tagx.startsWith("godbox_model_") ? ModelParts.get(tagx.substring("godbox_model_".length())) : null;
                  if (def != null && def.hideCarrier()) {
                     carrier.removeStatusEffect(StatusEffects.INVISIBILITY);
                  }
               }
            }
         }
      }
   }

   public static boolean tagged(Entity e) {
      for (String tag : e.getCommandTags()) {
         if (tag.startsWith("godbox_model_") || tag.startsWith("godbox_extra_")) {
            return true;
         }
      }

      return false;
   }

   public static void clear() {
      for (List<Models.Inst> list : MODELS.values()) {
         for (Models.Inst in : list) {
            discard(in);
         }
      }

      MODELS.clear();
      PARTS.clear();
   }

   public static void tick(MinecraftServer server) {
      Iterator<Entry<LivingEntity, List<Models.Inst>>> it = MODELS.entrySet().iterator();

      while (it.hasNext()) {
         Entry<LivingEntity, List<Models.Inst>> en = it.next();
         LivingEntity m = en.getKey();
         if (!m.isRemoved() && m.isAlive()) {
            for (Models.Inst in : en.getValue()) {
               tick(in);
            }
         } else {
            for (Models.Inst in : en.getValue()) {
               if (in.def.deathFx() && m.getHealth() <= 0.0F && m.getWorld() instanceof ServerWorld w) {
                  Fx.particles(w, ParticleTypes.EXPLOSION, m.getX(), m.getY() + 2.0, m.getZ(), 12, 2.0, 2.0, 2.0, 0.0);
               }

               discard(in);
            }

            it.remove();
         }
      }
   }

   private static void tick(Models.Inst in) {
      LivingEntity m = in.carrier;
      in.age++;
      double vx = m.getX() - m.prevX;
      double vz = m.getZ() - m.prevZ;
      boolean moving = vx * vx + vz * vz > 4.0E-4;
      in.moveW = Math.max(0.0F, Math.min(1.0F, in.moveW + (moving ? 0.2F : -0.15F)));
      if (in.moveW > 0.0F) {
         in.walk += 0.05F;
      }

      if (in.main && in.def.hideCarrier() && m.getFireTicks() > 0 && m.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
         m.extinguish();
      }

      follow(in);
      if (in.age % 20 == 1) {
         in.near = playerNear(m);
      }

      if (in.near) {
         boolean striking = in.age - in.strikeAt < 40;
         if (in.age % 2 == 0 || striking && in.age == in.strikeAt + 1) {
            pose(in, false);

            for (int i = 0; i < in.ents.length; i++) {
               if (in.animated[i] && in.ents[i] != null && !in.ents[i].isRemoved()) {
                  send(in, i, false);
               }
            }
         }
      }
   }

   private static boolean playerNear(LivingEntity m) {
      if (m.getWorld() instanceof ServerWorld w) {
         for (ServerPlayerEntity p : w.getPlayers()) {
            double dx = p.getX() - m.getX();
            double dz = p.getZ() - m.getZ();
            if (dx * dx + dz * dz < 40000.0) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void follow(Models.Inst in) {
      LivingEntity m = in.carrier;
      double x = m.getX();
      double y = m.getY();
      double z = m.getZ();
      float yaw = m.getBodyYaw();
      if (!in.placed || x != in.lx || y != in.ly || z != in.lz || !(Math.abs(yaw - in.lyaw) < 0.05F)) {
         in.placed = true;
         in.lx = x;
         in.ly = y;
         in.lz = z;
         in.lyaw = yaw;

         for (Entity e : in.ents) {
            if (e != null && !e.isRemoved()) {
               e.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
            }
         }
      }
   }

   private static ItemDisplayEntity spawn(ServerWorld w, Models.Inst in, int i) {
      ModelParts.Part p = in.def.parts()[i];
      ItemDisplayEntity e = new ItemDisplayEntity(EntityType.ITEM_DISPLAY, w) {
         @Override
         public boolean shouldSave() {
            return false;
         }
      };
      ItemStack stack = new ItemStack(item);
      stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(p.cmd()));
      e.setItemStack(stack);
      e.setTeleportDuration(2);
      e.setInterpolationDuration(3);
      e.setViewRange(4.0F);
      e.setDisplayWidth(24.0F);
      e.setDisplayHeight(16.0F);
      if (p.bright()) {
         e.setBrightness(new Brightness(15, 15));
      }

      e.addCommandTag("godbox_part");
      LivingEntity m = in.carrier;
      e.refreshPositionAndAngles(m.getX(), m.getY(), m.getZ(), m.getBodyYaw(), 0.0F);
      in.ents[i] = e;
      send(in, i, true);
      if (!w.spawnEntity(e)) {
         in.ents[i] = null;
         return null;
      } else {
         PARTS.add(e);
         return e;
      }
   }

   private static void discard(Models.Inst in) {
      for (Entity e : in.ents) {
         if (e != null) {
            PARTS.remove(e);
            e.discard();
         }
      }
   }

   private static void send(Models.Inst in, int i, boolean force) {
      Vector3f t = in.pos[i];
      Quaternionf q = in.rot[i];
      float s = in.scl[i];
      float[] o = in.sent;
      int b = i * 8;
      if (!force) {
         float dot = Math.abs(q.x * o[b + 3] + q.y * o[b + 4] + q.z * o[b + 5] + q.w * o[b + 6]);
         if (Math.abs(t.x - o[b]) < 0.002F
            && Math.abs(t.y - o[b + 1]) < 0.002F
            && Math.abs(t.z - o[b + 2]) < 0.002F
            && dot > 0.999995F
            && Math.abs(s - o[b + 7]) < 0.002F) {
            return;
         }
      }

      o[b] = t.x;
      o[b + 1] = t.y;
      o[b + 2] = t.z;
      o[b + 3] = q.x;
      o[b + 4] = q.y;
      o[b + 5] = q.z;
      o[b + 6] = q.w;
      o[b + 7] = s;
      ItemDisplayEntity e = in.ents[i];
      e.setTransformation(new AffineTransformation(new Vector3f(t), new Quaternionf(q), new Vector3f(s, s, s), new Quaternionf()));
      if (!force) {
         e.getDataTracker().set(DisplayEntityAccessor.godbox$startInterpolation(), 0, true);
      }
   }

   private static void pose(Models.Inst in, boolean all) {
      ModelParts.Creature d = in.def;
      ModelParts.Part[] parts = d.parts();
      float k = d.scale() / 16.0F;
      float time = in.age / 20.0F;
      Vector3f off = new Vector3f();

      for (int i = 0; i < parts.length; i++) {
         if (all || in.animated[i]) {
            ModelParts.Part p = parts[i];
            int par = p.parent();
            float ps = par >= 0 ? in.scl[par] / d.scale() : 1.0F;
            Quaternionf rot = in.rot[i];
            if (par >= 0) {
               rot.set(in.rot[par]);
            } else {
               rot.identity();
            }

            Quaternionf pr = par >= 0 ? in.rot[par] : null;
            rot.rotateY(rad(p.ry())).rotateX(rad(p.rx())).rotateZ(rad(p.rz()));
            off.set(p.px(), p.py(), p.pz());
            float scl = ps * p.scale();

            for (ModelParts.Anim a : p.anims()) {
               boolean walkClock = a.when() == 1;
               float w = a.when() == 0 ? 1.0F : (walkClock ? in.moveW : 1.0F - in.moveW);
               float t = walkClock ? in.walk : time;
               switch (a.type()) {
                  case 2:
                     turn(rot, a.axis(), 360.0F * a.speed() * t % 360.0F);
                     break;
                  case 4:
                     int st = in.age - in.strikeAt;
                     float vx = st >= 0 ? a.amp() * strikeEnvelope(st / a.speed()) : 0.0F;
                     turn(rot, a.axis(), vx);
                     break;
                  default:
                     float v = w * a.amp() * wave(a.wave(), a.speed() * t + a.phase());
                     if (a.type() == 0) {
                        turn(rot, a.axis(), v);
                     } else if (a.type() == 1) {
                        off.setComponent(a.axis(), off.get(a.axis()) + v);
                     } else if (a.type() == 3) {
                        scl *= 1.0F + v;
                     }
               }
            }

            if (pr != null) {
               pr.transform(off);
            }

            off.mul(ps * k);
            if (par >= 0) {
               off.add(in.pos[par]);
            }

            in.pos[i].set(off);
            in.scl[i] = scl * d.scale();
         }
      }
   }

   private static float rad(float deg) {
      return deg * (float) (Math.PI / 180.0);
   }

   private static void turn(Quaternionf q, int axis, float deg) {
      if (deg != 0.0F) {
         float r = rad(deg);
         if (axis == 0) {
            q.rotateX(r);
         } else if (axis == 1) {
            q.rotateY(r);
         } else {
            q.rotateZ(r);
         }
      }
   }

   private static float wave(int wave, float cycles) {
      if (wave == 3) {
         float x = cycles - (float)Math.floor(cycles);
         if (x < 0.7F) {
            float t = x / 0.7F;
            return t * t * (3.0F - 2.0F * t);
         } else {
            return Math.max(0.0F, 1.0F - (x - 0.7F) / 0.1F);
         }
      } else {
         float s = (float)Math.sin((Math.PI * 2) * cycles);
         return wave == 1 ? Math.max(0.0F, s) : (wave == 2 ? Math.abs(s) : s);
      }
   }

   private static float strikeEnvelope(float t) {
      if (t <= 0.0F || t >= 1.0F) {
         return 0.0F;
      } else if (t < 0.25F) {
         return t / 0.25F;
      } else {
         float r = 1.0F - (t - 0.25F) / 0.75F;
         return r * r;
      }
   }

   private static final class Inst {
      final LivingEntity carrier;
      final ModelParts.Creature def;
      final boolean main;
      final ItemDisplayEntity[] ents;
      final boolean[] animated;
      final Vector3f[] pos;
      final Quaternionf[] rot;
      final float[] scl;
      final float[] sent;
      int age;
      float walk;
      float moveW;
      int strikeAt = -100000;
      boolean near = true;
      boolean placed;
      double lx;
      double ly;
      double lz;
      float lyaw;

      Inst(LivingEntity carrier, ModelParts.Creature def, boolean main) {
         this.carrier = carrier;
         this.def = def;
         this.main = main;
         int n = def.parts().length;
         this.ents = new ItemDisplayEntity[n];
         this.animated = new boolean[n];
         this.pos = new Vector3f[n];
         this.rot = new Quaternionf[n];
         this.scl = new float[n];
         this.sent = new float[n * 8];

         for (int i = 0; i < n; i++) {
            ModelParts.Part p = def.parts()[i];
            this.animated[i] = p.anims().length > 0 || p.parent() >= 0 && this.animated[p.parent()];
            this.pos[i] = new Vector3f();
            this.rot[i] = new Quaternionf();
         }
      }
   }
}
