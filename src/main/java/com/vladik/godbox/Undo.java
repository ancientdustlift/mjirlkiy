package com.vladik.godbox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class Undo {
   private static final int MAX_BLOCKS = 250000;
   private static final int MAX_GROUPS = 6;
   private static final int WINDOW = 2400;
   private static final ArrayDeque<Undo.Group> GROUPS = new ArrayDeque<>();
   private static Undo.Group current;
   private static boolean restoring;

   private Undo() {
   }

   public static void begin(ServerWorld w) {
      begin(w, null);
   }

   public static void begin(ServerWorld w, UUID owner) {
      current = new Undo.Group(w, owner);
      GROUPS.addLast(current);
      int mine = 0;
      Iterator<Undo.Group> it = GROUPS.descendingIterator();

      while (it.hasNext()) {
         Undo.Group g = it.next();
         if (Objects.equals(g.owner, owner)) {
            if (++mine > 6) {
               it.remove();
            }
         }
      }

      while (GROUPS.size() > 24) {
         GROUPS.removeFirst();
      }
   }

   public static boolean active(World w) {
      return current != null && !restoring && current.w == w;
   }

   public static void record(BlockPos p, BlockState old) {
      Undo.Group g = current;
      if (g != null && g.old.size() < 250000) {
         g.old.putIfAbsent(p.toImmutable(), old);
      }
   }

   public static void tick() {
      if (current != null && ++current.age > 2400) {
         current = null;
      }
   }

   public static void clear() {
      GROUPS.clear();
      current = null;
      restoring = false;
   }

   public static void undo(ServerPlayerEntity god) {
      Undo.Group g = null;
      UUID me = god.getUuid();
      Iterator<Undo.Group> it = GROUPS.descendingIterator();

      while (it.hasNext()) {
         Undo.Group last = it.next();
         if (last.owner == null || last.owner.equals(me)) {
            it.remove();
            if (!last.old.isEmpty()) {
               g = last;
               break;
            }
         }
      }

      if (current != null && (current.owner == null || current.owner.equals(me))) {
         current = null;
      }

      if (g == null) {
         god.sendMessage(Text.literal("Нечего отменять"), true);
      } else {
         final ServerWorld w = g.w;
         final List<Entry<BlockPos, BlockState>> list = new ArrayList<>(g.old.entrySet());
         int total = list.size();
         Tasks.add(new Tasks.Task() {
            int i = 0;

            @Override
            public boolean tick() {
               Undo.restoring = true;

               try {
                  for (int end = Math.min(list.size(), this.i + 20000); this.i < end; this.i++) {
                     Entry<BlockPos, BlockState> e = list.get(this.i);
                     w.setBlockState(e.getKey(), e.getValue(), 50);
                  }
               } finally {
                  Undo.restoring = false;
               }

               return this.i >= list.size();
            }
         });
         god.sendMessage(Text.literal("Отменено блоков: " + total), true);
      }
   }

   private static final class Group {
      final ServerWorld w;
      final UUID owner;
      final LinkedHashMap<BlockPos, BlockState> old = new LinkedHashMap<>();
      int age;

      Group(ServerWorld w, UUID owner) {
         this.w = w;
         this.owner = owner;
      }
   }
}
