package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Tasks {
   private static final List<Tasks.Task> ACTIVE = new ArrayList<>();
   private static final List<Tasks.Task> PENDING = new ArrayList<>();

   private Tasks() {
   }

   public static void add(Tasks.Task t) {
      PENDING.add(t);
   }

   public static void delay(int ticks, Runnable r) {
      add(new Tasks.Task() {
         int left = ticks;

         @Override
         public boolean tick() {
            if (--this.left <= 0) {
               r.run();
               return true;
            } else {
               return false;
            }
         }
      });
   }

   public static void tick() {
      ACTIVE.addAll(PENDING);
      PENDING.clear();
      Iterator<Tasks.Task> it = ACTIVE.iterator();

      while (it.hasNext()) {
         Tasks.Task t = it.next();

         boolean done;
         try {
            done = t.tick();
         } catch (Exception var4) {
            GodBox.LOG.error("GodBox task failed", (Throwable)var4);
            done = true;
         }

         if (done) {
            it.remove();
         }
      }
   }

   public static void clear() {
      ACTIVE.clear();
      PENDING.clear();
   }

   public interface Task {
      boolean tick();
   }
}
