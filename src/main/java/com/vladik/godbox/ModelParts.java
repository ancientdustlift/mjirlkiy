package com.vladik.godbox;

import java.util.HashMap;
import java.util.Map;

public final class ModelParts {
   public static final int SWING = 0;
   public static final int BOB = 1;
   public static final int SPIN = 2;
   public static final int PULSE = 3;
   public static final int STRIKE = 4;
   public static final int ALWAYS = 0;
   public static final int MOVING = 1;
   public static final int IDLE = 2;
   public static final int SIN = 0;
   public static final int POS = 1;
   public static final int ABS = 2;
   public static final int SNAP = 3;
   private static final Map<String, ModelParts.Creature> ALL = new HashMap<>();

   private ModelParts() {
   }

   public static ModelParts.Creature get(String name) {
      return ALL.get(name);
   }

   public static boolean has(String name) {
      return ALL.containsKey(name);
   }

   private static ModelParts.Anim a(int type, int axis, float amp, float speed, float phase, int when, int wave) {
      return new ModelParts.Anim(type, axis, amp, speed, phase, when, wave);
   }

   private static ModelParts.Part p(
      String name, int cmd, int parent, float px, float py, float pz, float rx, float ry, float rz, float scale, boolean bright, ModelParts.Anim... anims
   ) {
      return new ModelParts.Part(name, cmd, parent, px, py, pz, rx, ry, rz, scale, bright, anims);
   }

   private static void c_crab() {
      ALL.put(
         "crab",
         new ModelParts.Creature(
            "crab",
            3.0F,
            true,
            true,
            new ModelParts.Part[]{
               p("body", 1000, -1, 0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.4F, 0.35F, 0.0F, 0, 0), a(1, 1, 0.6F, 2.8F, 0.0F, 1, 2)),
               p("stalk_l", 1001, 0, 5.5F, 4.5F, 10.3F, -8.0F, 0.0F, -12.0F, 1.0F, false, a(0, 2, 5.0F, 0.33F, 0.25F, 0, 0), a(0, 0, 5.0F, 0.21F, 0.5F, 0, 0)),
               p("eye_l", 1002, 1, 0.0F, 6.5F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p("stalk_r", 1003, 0, -5.5F, 4.5F, 10.3F, -8.0F, 0.0F, 12.0F, 1.0F, false, a(0, 2, 5.0F, 0.33F, 0.0F, 0, 0), a(0, 0, 5.0F, 0.21F, 0.0F, 0, 0)),
               p("eye_r", 1004, 3, 0.0F, 6.5F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p("arm_l", 1005, 0, 12.5F, -1.0F, 9.5F, -10.0F, 42.0F, 0.0F, 1.3F, false, a(0, 1, 5.0F, 0.3F, 0.0F, 0, 0), a(4, 0, -22.0F, 12.0F, 0.0F, 0, 0)),
               p("pincer_l", 1006, 5, 0.0F, 0.0F, 11.0F, 12.0F, -56.0F, 0.0F, 1.0F, false, a(4, 1, -20.0F, 12.0F, 0.0F, 0, 0)),
               p("finger_l", 1007, 6, 0.0F, 2.0F, 8.5F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -30.0F, 0.4F, 0.0F, 0, 3), a(4, 0, -40.0F, 9.0F, 0.0F, 0, 0)),
               p(
                  "arm_r",
                  1008,
                  0,
                  -12.5F,
                  -1.0F,
                  9.5F,
                  -10.0F,
                  -42.0F,
                  0.0F,
                  0.95F,
                  false,
                  a(0, 1, -5.0F, 0.3F, 0.45F, 0, 0),
                  a(4, 0, -22.0F, 12.0F, 0.0F, 0, 0)
               ),
               p("pincer_r", 1009, 8, 0.0F, 0.0F, 11.0F, 12.0F, 56.0F, 0.0F, 1.0F, false, a(4, 1, 20.0F, 12.0F, 0.0F, 0, 0)),
               p("finger_r", 1010, 9, 0.0F, 2.0F, 8.5F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -30.0F, 0.4F, 0.45F, 0, 3), a(4, 0, -40.0F, 9.0F, 0.0F, 0, 0)),
               p("leg_l0", 1011, -1, 14.0F, 14.0F, 7.5F, 0.0F, -36.0F, 30.0F, 1.0F, false, a(0, 1, 15.0F, 1.4F, 0.0F, 1, 0), a(0, 2, 11.0F, 1.4F, 0.25F, 1, 1)),
               p("shin_l0", 1012, 11, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -105.0F, 1.0F, false, a(0, 2, -9.0F, 1.4F, 0.25F, 1, 1)),
               p("leg_l1", 1013, -1, 14.0F, 14.0F, 2.5F, 0.0F, -12.0F, 30.0F, 1.0F, false, a(0, 1, 15.0F, 1.4F, 0.5F, 1, 0), a(0, 2, 11.0F, 1.4F, 0.75F, 1, 1)),
               p("shin_l1", 1014, 13, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -105.0F, 1.0F, false, a(0, 2, -9.0F, 1.4F, 0.75F, 1, 1)),
               p("leg_l2", 1015, -1, 14.0F, 14.0F, -2.5F, 0.0F, 12.0F, 30.0F, 1.0F, false, a(0, 1, 15.0F, 1.4F, 0.0F, 1, 0), a(0, 2, 11.0F, 1.4F, 0.25F, 1, 1)),
               p("shin_l2", 1016, 15, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -105.0F, 1.0F, false, a(0, 2, -9.0F, 1.4F, 0.25F, 1, 1)),
               p("leg_l3", 1017, -1, 14.0F, 14.0F, -7.5F, 0.0F, 36.0F, 30.0F, 1.0F, false, a(0, 1, 15.0F, 1.4F, 0.5F, 1, 0), a(0, 2, 11.0F, 1.4F, 0.75F, 1, 1)),
               p("shin_l3", 1018, 17, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, -105.0F, 1.0F, false, a(0, 2, -9.0F, 1.4F, 0.75F, 1, 1)),
               p(
                  "leg_r0",
                  1019,
                  -1,
                  -14.0F,
                  14.0F,
                  7.5F,
                  0.0F,
                  36.0F,
                  -30.0F,
                  1.0F,
                  false,
                  a(0, 1, 15.0F, 1.4F, 0.5F, 1, 0),
                  a(0, 2, -11.0F, 1.4F, 0.75F, 1, 1)
               ),
               p("shin_r0", 1020, 19, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 105.0F, 1.0F, false, a(0, 2, 9.0F, 1.4F, 0.75F, 1, 1)),
               p(
                  "leg_r1",
                  1021,
                  -1,
                  -14.0F,
                  14.0F,
                  2.5F,
                  0.0F,
                  12.0F,
                  -30.0F,
                  1.0F,
                  false,
                  a(0, 1, 15.0F, 1.4F, 1.0F, 1, 0),
                  a(0, 2, -11.0F, 1.4F, 1.25F, 1, 1)
               ),
               p("shin_r1", 1022, 21, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 105.0F, 1.0F, false, a(0, 2, 9.0F, 1.4F, 1.25F, 1, 1)),
               p(
                  "leg_r2",
                  1023,
                  -1,
                  -14.0F,
                  14.0F,
                  -2.5F,
                  0.0F,
                  -12.0F,
                  -30.0F,
                  1.0F,
                  false,
                  a(0, 1, 15.0F, 1.4F, 0.5F, 1, 0),
                  a(0, 2, -11.0F, 1.4F, 0.75F, 1, 1)
               ),
               p("shin_r2", 1024, 23, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 105.0F, 1.0F, false, a(0, 2, 9.0F, 1.4F, 0.75F, 1, 1)),
               p(
                  "leg_r3",
                  1025,
                  -1,
                  -14.0F,
                  14.0F,
                  -7.5F,
                  0.0F,
                  -36.0F,
                  -30.0F,
                  1.0F,
                  false,
                  a(0, 1, 15.0F, 1.4F, 1.0F, 1, 0),
                  a(0, 2, -11.0F, 1.4F, 1.25F, 1, 1)
               ),
               p("shin_r3", 1026, 25, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 105.0F, 1.0F, false, a(0, 2, 9.0F, 1.4F, 1.25F, 1, 1))
            }
         )
      );
   }

   private static void c_robot() {
      ALL.put(
         "robot",
         new ModelParts.Creature(
            "robot",
            4.0F,
            true,
            true,
            new ModelParts.Part[]{
               p("pelvis", 1100, -1, 0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.5F, 0.35F, 0.0F, 0, 0), a(1, 1, 0.7F, 2.4F, 0.0F, 1, 2)),
               p(
                  "torso",
                  1101,
                  0,
                  0.0F,
                  4.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 3.0F, 0.2F, 0.0F, 0, 0),
                  a(0, 1, 4.0F, 1.2F, 0.0F, 1, 0),
                  a(4, 1, -14.0F, 12.0F, 0.0F, 0, 0)
               ),
               p("reactor", 1102, 1, 0.0F, 8.0F, 8.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.08F, 0.7F, 0.0F, 0, 0)),
               p("head", 1103, 1, 0.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 4.0F, 0.25F, 0.3F, 0, 0), a(0, 1, -3.0F, 1.2F, 0.0F, 1, 0)),
               p("visor", 1104, 3, 0.0F, 6.8F, 5.5F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.04F, 0.9F, 0.2F, 0, 0)),
               p(
                  "arm_l",
                  1105,
                  1,
                  12.0F,
                  11.5F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 14.0F, 1.2F, 0.5F, 1, 0),
                  a(0, 0, 2.0F, 0.35F, 0.2F, 0, 0),
                  a(4, 0, -85.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("forearm_l", 1106, 5, 0.5F, -13.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -8.0F, 1.2F, 0.75F, 1, 1), a(0, 0, 1.5F, 0.35F, 0.5F, 0, 0)),
               p("fist_l", 1107, 6, 0.5F, -13.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 5.0F, 1.2F, 0.5F, 1, 0)),
               p(
                  "arm_r",
                  1108,
                  1,
                  -12.0F,
                  11.5F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 14.0F, 1.2F, 0.0F, 1, 0),
                  a(0, 0, 2.0F, 0.35F, 0.7F, 0, 0),
                  a(4, 0, 32.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("cannon", 1109, 8, -0.5F, -13.0F, 0.0F, -72.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 3.0F, 1.0F, 0.4F, 0, 0)),
               p("muzzle", 1110, 9, 0.0F, -19.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.25F, 1.1F, 0.0F, 0, 0)),
               p("leg_l", 1111, 0, 4.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -24.0F, 1.1F, 0.0F, 1, 0), a(0, 0, 1.0F, 0.35F, 0.0F, 0, 0)),
               p("shin_l", 1112, 11, 0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 28.0F, 1.1F, 0.28F, 1, 1)),
               p("foot_l", 1113, 12, 0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 10.0F, 1.1F, 0.05F, 1, 0)),
               p("leg_r", 1114, 0, -4.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -24.0F, 1.1F, 0.5F, 1, 0), a(0, 0, 1.0F, 0.35F, 0.25F, 0, 0)),
               p("shin_r", 1115, 14, 0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 28.0F, 1.1F, 0.78F, 1, 1)),
               p("foot_r", 1116, 15, 0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 10.0F, 1.1F, 0.55F, 1, 0))
            }
         )
      );
   }

   private static void c_titan() {
      ALL.put(
         "titan",
         new ModelParts.Creature(
            "titan",
            1.25F,
            true,
            true,
            new ModelParts.Part[]{
               p(
                  "torso",
                  1200,
                  -1,
                  0.0F,
                  20.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(1, 1, 0.5F, 0.3F, 0.0F, 0, 0),
                  a(1, 1, 0.9F, 1.2F, 0.0F, 1, 2),
                  a(3, 1, 0.012F, 0.3F, 0.0F, 0, 0),
                  a(0, 2, 1.5F, 0.6F, 0.0F, 1, 0),
                  a(4, 0, 14.0F, 14.0F, 0.0F, 0, 0)
               ),
               p("runes", 1201, 0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.01F, 0.5F, 0.25F, 0, 0)),
               p("core", 1202, 0, 0.0F, 11.0F, 9.4F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.16F, 0.6F, 0.0F, 0, 0)),
               p("head", 1203, 0, 0.0F, 14.0F, 2.5F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 1.5F, 0.3F, 0.2F, 0, 0), a(0, 1, 2.0F, 0.17F, 0.0F, 0, 0)),
               p("eyes", 1204, 3, 0.0F, 5.2F, 6.9F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p(
                  "arm_l",
                  1205,
                  0,
                  15.5F,
                  12.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 24.0F, 1.0F, 0.5F, 1, 0),
                  a(0, 2, 1.5F, 0.3F, 0.5F, 0, 0),
                  a(4, 0, -150.0F, 14.0F, 0.0F, 0, 0)
               ),
               p("fore_l", 1206, 5, 0.0F, -13.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -8.0F, 1.0F, 0.65F, 1, 1), a(4, 0, -25.0F, 14.0F, 0.0F, 0, 0)),
               p("band_l", 1207, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p(
                  "arm_r",
                  1208,
                  0,
                  -15.5F,
                  12.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 24.0F, 1.0F, 0.0F, 1, 0),
                  a(0, 2, -1.5F, 0.3F, 0.0F, 0, 0),
                  a(4, 0, -150.0F, 14.0F, 0.0F, 0, 0)
               ),
               p("fore_r", 1209, 8, 0.0F, -13.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -8.0F, 1.0F, 0.15F, 1, 1), a(4, 0, -25.0F, 14.0F, 0.0F, 0, 0)),
               p("band_r", 1210, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p("leg_l", 1211, -1, 7.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 24.0F, 1.0F, 0.0F, 1, 0)),
               p("shin_l", 1212, 11, 0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 26.0F, 1.0F, 0.25F, 1, 1)),
               p("knee_l", 1213, 12, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.1F, 0.5F, 0.2F, 0, 0)),
               p("leg_r", 1214, -1, -7.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 24.0F, 1.0F, 0.5F, 1, 0)),
               p("shin_r", 1215, 14, 0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 26.0F, 1.0F, 0.75F, 1, 1)),
               p("knee_r", 1216, 15, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.1F, 0.5F, -0.2F, 0, 0))
            }
         )
      );
   }

   private static void c_dragon() {
      ALL.put(
         "dragon",
         new ModelParts.Creature(
            "dragon",
            3.5F,
            true,
            true,
            new ModelParts.Part[]{
               p("body", 1300, -1, 0.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 1.0F, 0.3F, 0.0F, 0, 0), a(1, 1, 0.5F, 0.6F, 0.25F, 0, 0)),
               p(
                  "neck0",
                  1301,
                  0,
                  0.0F,
                  1.5F,
                  10.5F,
                  -28.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 5.0F, 0.4F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.3F, -0.0F, 0, 0),
                  a(4, 0, 9.0F, 10.0F, 0.0F, 0, 0)
               ),
               p(
                  "neck1",
                  1302,
                  1,
                  0.0F,
                  0.0F,
                  6.0F,
                  14.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 5.0F, 0.4F, -0.16F, 0, 0),
                  a(0, 0, 3.0F, 0.3F, -0.18F, 0, 0),
                  a(4, 0, 9.0F, 10.0F, 0.0F, 0, 0)
               ),
               p(
                  "neck2",
                  1303,
                  2,
                  0.0F,
                  0.0F,
                  6.0F,
                  14.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 5.0F, 0.4F, -0.32F, 0, 0),
                  a(0, 0, 3.0F, 0.3F, -0.36F, 0, 0),
                  a(4, 0, 9.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("head", 1304, 3, 0.0F, 0.0F, 6.0F, 8.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 4.0F, 0.4F, -0.48F, 0, 0), a(4, 0, 14.0F, 10.0F, 0.0F, 0, 0)),
               p("jaw", 1305, 4, 0.0F, -2.3F, 0.5F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 7.0F, 0.35F, 0.0F, 0, 3), a(4, 0, 40.0F, 10.0F, 0.0F, 0, 0)),
               p("eyes", 1306, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p("tail0", 1307, 0, 0.0F, -0.5F, -10.5F, 4.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 8.0F, 0.45F, -0.0F, 0, 0)),
               p("tail1", 1308, 7, 0.0F, 0.0F, -7.5F, -3.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 8.0F, 0.45F, -0.15F, 0, 0)),
               p("tail2", 1309, 8, 0.0F, 0.0F, -7.0F, -3.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 8.0F, 0.45F, -0.3F, 0, 0)),
               p("tail3", 1310, 9, 0.0F, 0.0F, -6.5F, -3.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 8.0F, 0.45F, -0.45F, 0, 0)),
               p("tail_tip", 1311, 10, 0.0F, 0.0F, -6.0F, -3.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 8.0F, 0.45F, -0.6F, 0, 0)),
               p("hind_l", 1312, 0, 7.0F, -2.0F, -6.5F, -35.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 3.0F, 0.4F, 0.2F, 0, 0)),
               p("shin_l", 1313, 12, 0.0F, -2.5F, -0.5F, 75.0F, 0.0F, 0.0F, 1.0F, false),
               p("hind_r", 1314, 0, -7.0F, -2.0F, -6.5F, -35.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 3.0F, 0.4F, 0.0F, 0, 0)),
               p("shin_r", 1315, 14, 0.0F, -2.5F, -0.5F, 75.0F, 0.0F, 0.0F, 1.0F, false),
               p("wing_l", 1316, 0, 6.0F, 4.0F, 2.0F, 0.0F, 6.0F, 9.0F, 1.0F, false, a(0, 2, 20.0F, 0.5F, 0.0F, 0, 0), a(0, 2, 3.0F, 1.0F, 0.2F, 0, 0)),
               p("wingtip_l", 1317, 16, 17.0F, 0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 1.0F, false, a(0, 2, 16.0F, 0.5F, -0.14F, 0, 0), a(0, 2, 5.0F, 1.0F, 0.05F, 0, 0)),
               p("wing_r", 1318, 0, -6.0F, 4.0F, 2.0F, 0.0F, -6.0F, -9.0F, 1.0F, false, a(0, 2, -20.0F, 0.5F, 0.0F, 0, 0), a(0, 2, -3.0F, 1.0F, 0.2F, 0, 0)),
               p(
                  "wingtip_r",
                  1319,
                  18,
                  -17.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  -18.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 2, -16.0F, 0.5F, -0.14F, 0, 0),
                  a(0, 2, -5.0F, 1.0F, 0.05F, 0, 0)
               )
            }
         )
      );
   }

   private static void c_kraken() {
      ALL.put(
         "kraken",
         new ModelParts.Creature(
            "kraken",
            3.0F,
            true,
            true,
            new ModelParts.Part[]{
               p("body", 1400, -1, 0.0F, 15.0F, -2.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.5F, 0.3F, 0.0F, 0, 0), a(3, 1, 0.015F, 0.45F, 0.0F, 0, 0)),
               p("beak", 1401, 0, 0.0F, -6.0F, 14.0F, 15.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -14.0F, 0.35F, 0.0F, 0, 3), a(4, 0, -20.0F, 8.0F, 0.0F, 0, 0)),
               p("eye_l", 1402, 0, 12.5F, 3.0F, 8.0F, 0.0F, 25.0F, 0.0F, 1.0F, true),
               p("fin_l", 1403, 0, 12.0F, 2.0F, -10.0F, 0.0F, 30.0F, 0.0F, 1.0F, false, a(0, 1, 7.0F, 0.4F, 0.2F, 0, 0)),
               p("eye_r", 1404, 0, -12.5F, 3.0F, 8.0F, 0.0F, -25.0F, 0.0F, 1.0F, true),
               p("fin_r", 1405, 0, -12.0F, 2.0F, -10.0F, 0.0F, -30.0F, 0.0F, 1.0F, false, a(0, 1, -7.0F, 0.4F, 0.2F, 0, 0)),
               p(
                  "tent0_0",
                  1406,
                  0,
                  -9.84808F,
                  -5.5F,
                  13.52094F,
                  4.0F,
                  -80.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p("tent0_1", 1407, 6, 0.0F, 0.0F, 10.0F, -4.0F, 9.0F, 0.0F, 1.0F, false, a(0, 1, 10.0F, 0.35F, -0.12F, 0, 0), a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)),
               p("tent0_2", 1408, 7, 0.0F, 0.0F, 9.0F, -4.0F, 9.0F, 0.0F, 1.0F, false, a(0, 1, 13.0F, 0.35F, -0.24F, 0, 0), a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)),
               p(
                  "tent1_0",
                  1409,
                  0,
                  -8.38671F,
                  -5.5F,
                  14.63392F,
                  4.0F,
                  -57.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p(
                  "tent1_1",
                  1410,
                  9,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)
               ),
               p(
                  "tent1_2",
                  1411,
                  10,
                  0.0F,
                  0.0F,
                  9.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -13.0F, 0.35F, -0.24F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)
               ),
               p(
                  "tent2_0",
                  1412,
                  0,
                  -5.59193F,
                  -5.5F,
                  15.48711F,
                  4.0F,
                  -34.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p(
                  "tent2_1",
                  1413,
                  12,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)
               ),
               p("tent2_2", 1414, 13, 0.0F, 0.0F, 9.0F, -4.0F, 9.0F, 0.0F, 1.0F, false, a(0, 1, 13.0F, 0.35F, -0.24F, 0, 0), a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)),
               p(
                  "tent3_0",
                  1415,
                  0,
                  -1.90809F,
                  -5.5F,
                  15.94488F,
                  4.0F,
                  -11.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0),
                  a(4, 1, 14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent3_1",
                  1416,
                  15,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0),
                  a(4, 1, 14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent3_2",
                  1417,
                  16,
                  0.0F,
                  0.0F,
                  9.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -13.0F, 0.35F, -0.24F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0),
                  a(4, 1, 14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent4_0",
                  1418,
                  0,
                  1.90809F,
                  -5.5F,
                  15.94488F,
                  4.0F,
                  11.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0),
                  a(4, 1, -14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent4_1",
                  1419,
                  18,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0),
                  a(4, 1, -14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent4_2",
                  1420,
                  19,
                  0.0F,
                  0.0F,
                  9.0F,
                  -4.0F,
                  9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 13.0F, 0.35F, -0.24F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0),
                  a(4, 1, -14.0F, 8.0F, 0.0F, 0, 0),
                  a(4, 0, -16.0F, 8.0F, 0.0F, 0, 0)
               ),
               p(
                  "tent5_0",
                  1421,
                  0,
                  5.59193F,
                  -5.5F,
                  15.48711F,
                  4.0F,
                  34.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p(
                  "tent5_1",
                  1422,
                  21,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)
               ),
               p(
                  "tent5_2",
                  1423,
                  22,
                  0.0F,
                  0.0F,
                  9.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -13.0F, 0.35F, -0.24F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)
               ),
               p(
                  "tent6_0",
                  1424,
                  0,
                  8.38671F,
                  -5.5F,
                  14.63392F,
                  4.0F,
                  57.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p(
                  "tent6_1",
                  1425,
                  24,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, 10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)
               ),
               p("tent6_2", 1426, 25, 0.0F, 0.0F, 9.0F, -4.0F, 9.0F, 0.0F, 1.0F, false, a(0, 1, 13.0F, 0.35F, -0.24F, 0, 0), a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)),
               p(
                  "tent7_0",
                  1427,
                  0,
                  9.84808F,
                  -5.5F,
                  13.52094F,
                  4.0F,
                  80.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -7.0F, 0.35F, -0.0F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.0F, 0, 0)
               ),
               p(
                  "tent7_1",
                  1428,
                  27,
                  0.0F,
                  0.0F,
                  10.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -10.0F, 0.35F, -0.12F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.12F, 0, 0)
               ),
               p(
                  "tent7_2",
                  1429,
                  28,
                  0.0F,
                  0.0F,
                  9.0F,
                  -4.0F,
                  -9.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 1, -13.0F, 0.35F, -0.24F, 0, 0),
                  a(0, 0, 3.0F, 0.28F, -0.24F, 0, 0)
               )
            }
         )
      );
   }

   private static void c_ufo() {
      ALL.put(
         "ufo",
         new ModelParts.Creature(
            "ufo",
            2.5F,
            true,
            true,
            new ModelParts.Part[]{
               p("hull", 1500, -1, 0.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.8F, 0.35F, 0.0F, 0, 0), a(1, 1, 0.5F, 0.8F, 0.25F, 0, 0)),
               p("dome", 1501, 0, 0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("ring", 1502, 0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(2, 1, 360.0F, 0.25F, 0.0F, 0, 0)),
               p(
                  "emitter",
                  1503,
                  0,
                  0.0F,
                  -8.5F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  true,
                  a(3, 1, 0.12F, 1.2F, 0.0F, 0, 0),
                  a(1, 1, 0.6F, 1.2F, 0.0F, 0, 0),
                  a(4, 0, 25.0F, 10.0F, 0.0F, 0, 0)
               )
            }
         )
      );
   }

   private static void c_crystal_golem() {
      ALL.put(
         "crystal_golem",
         new ModelParts.Creature(
            "crystal_golem",
            1.0F,
            true,
            false,
            new ModelParts.Part[]{
               p(
                  "body",
                  1600,
                  -1,
                  0.0F,
                  14.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(1, 1, 0.5F, 0.4F, 0.0F, 0, 0),
                  a(1, 1, 0.5F, 2.2F, 0.0F, 1, 2),
                  a(0, 1, 3.0F, 1.1F, 0.0F, 1, 0),
                  a(4, 0, 10.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("core", 1601, 0, 0.0F, 8.5F, 6.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.1F, 0.6F, 0.0F, 0, 0)),
               p("crystals_back", 1602, 0, 0.0F, 12.0F, -7.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(3, 1, 0.03F, 0.4F, 0.0F, 0, 0)),
               p("head", 1603, 0, 0.0F, 19.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 4.0F, 0.25F, 0.3F, 0, 0), a(0, 0, 2.0F, 0.4F, 0.0F, 0, 0)),
               p("eyes", 1604, 3, 0.0F, 3.6F, 4.6F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p(
                  "arm_l",
                  1605,
                  0,
                  12.5F,
                  17.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 22.0F, 1.1F, 0.5F, 1, 0),
                  a(0, 0, 1.5F, 0.4F, 0.25F, 0, 0),
                  a(4, 0, -88.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("forearm_l", 1606, 5, 0.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -10.0F, 1.1F, 0.7F, 1, 1), a(4, 0, -25.0F, 10.0F, 0.0F, 0, 0)),
               p(
                  "arm_r",
                  1607,
                  0,
                  -12.5F,
                  17.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 22.0F, 1.1F, 0.0F, 1, 0),
                  a(0, 0, 1.5F, 0.4F, 0.0F, 0, 0),
                  a(4, 0, -88.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("forearm_r", 1608, 7, 0.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -10.0F, 1.1F, 0.2F, 1, 1), a(4, 0, -25.0F, 10.0F, 0.0F, 0, 0)),
               p("leg_l", 1609, -1, 4.5F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -22.0F, 1.1F, 0.0F, 1, 0), a(0, 0, 1.0F, 0.4F, 0.0F, 0, 0)),
               p("leg_r", 1610, -1, -4.5F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, -22.0F, 1.1F, 0.5F, 1, 0), a(0, 0, 1.0F, 0.4F, 0.5F, 0, 0))
            }
         )
      );
   }

   private static void c_fire_elemental() {
      ALL.put(
         "fire_elemental",
         new ModelParts.Creature(
            "fire_elemental",
            1.0F,
            true,
            false,
            new ModelParts.Part[]{
               p(
                  "body",
                  1700,
                  -1,
                  0.0F,
                  19.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(1, 1, 1.2F, 0.5F, 0.0F, 0, 0),
                  a(0, 2, 1.5F, 0.35F, 0.0F, 0, 0),
                  a(4, 0, 12.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("core", 1701, 0, 0.0F, 3.0F, 4.6F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.12F, 0.8F, 0.0F, 0, 0)),
               p("head", 1702, 0, 0.0F, 8.5F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 1, 4.0F, 0.3F, 0.0F, 0, 0), a(0, 0, 2.0F, 0.45F, 0.2F, 0, 0)),
               p("crest", 1703, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(0, 0, 4.0F, 0.9F, -0.0F, 0, 0)),
               p("tail", 1704, 0, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(0, 2, 6.0F, 0.7F, -0.0F, 0, 0), a(0, 0, 4.0F, 0.5F, -0.0F, 0, 0)),
               p("tail2", 1705, 4, 0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(0, 2, 10.0F, 0.7F, -0.12F, 0, 0), a(0, 0, 6.0F, 0.5F, -0.12F, 0, 0)),
               p(
                  "arm_l",
                  1706,
                  0,
                  12.5F,
                  2.0F,
                  3.0F,
                  -18.0F,
                  10.0F,
                  0.0F,
                  1.0F,
                  true,
                  a(1, 1, 1.6F, 0.6F, 0.0F, 0, 0),
                  a(0, 0, 10.0F, 0.45F, 0.0F, 0, 0),
                  a(0, 2, 4.0F, 0.3F, 0.0F, 0, 0),
                  a(4, 0, -85.0F, 10.0F, 0.0F, 0, 0)
               ),
               p(
                  "arm_r",
                  1707,
                  0,
                  -12.5F,
                  2.0F,
                  3.0F,
                  -18.0F,
                  -10.0F,
                  0.0F,
                  1.0F,
                  true,
                  a(1, 1, 1.6F, 0.6F, 0.5F, 0, 0),
                  a(0, 0, 10.0F, 0.45F, 0.5F, 0, 0),
                  a(0, 2, -4.0F, 0.3F, 0.5F, 0, 0),
                  a(4, 0, -85.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("shards", 1708, 0, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(2, 1, 360.0F, 0.4F, 0.0F, 0, 0), a(1, 1, 1.0F, 0.6F, 0.3F, 0, 0))
            }
         )
      );
   }

   private static void c_fire_skull() {
      ALL.put(
         "fire_skull",
         new ModelParts.Creature(
            "fire_skull",
            1.0F,
            true,
            false,
            new ModelParts.Part[]{
               p("skull", 1800, -1, 0.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.8F, 0.8F, 0.0F, 0, 0), a(0, 0, 3.0F, 0.6F, 0.0F, 0, 0)),
               p("jaw", 1801, 0, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 9.0F, 2.4F, 0.0F, 0, 3), a(4, 0, 34.0F, 10.0F, 0.0F, 0, 0)),
               p("flames", 1802, 0, 0.0F, 5.0F, -2.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.08F, 2.2F, 0.0F, 0, 0), a(0, 2, 5.0F, 3.0F, 0.0F, 0, 0))
            }
         )
      );
   }

   private static void c_jumpy_skull() {
      ALL.put(
         "jumpy_skull",
         new ModelParts.Creature(
            "jumpy_skull",
            1.0F,
            true,
            false,
            new ModelParts.Part[]{
               p(
                  "skull",
                  1900,
                  -1,
                  0.0F,
                  9.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(1, 1, 1.5F, 1.6F, 0.0F, 1, 2),
                  a(3, 1, 0.04F, 0.7F, 0.0F, 0, 0),
                  a(3, 1, 0.07F, 1.6F, 0.0F, 1, 0)
               ),
               p(
                  "jaw",
                  1901,
                  0,
                  0.0F,
                  -6.0F,
                  -3.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  false,
                  a(0, 0, 14.0F, 1.6F, 0.0F, 1, 3),
                  a(0, 0, 2.5F, 0.5F, 0.0F, 0, 0),
                  a(4, 0, 38.0F, 10.0F, 0.0F, 0, 0)
               ),
               p("glow", 1902, 0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true, a(3, 1, 0.05F, 1.5F, 0.0F, 0, 0))
            }
         )
      );
   }

   private static void c_hats() {
      ALL.put(
         "hats",
         new ModelParts.Creature(
            "hats",
            1.0F,
            false,
            false,
            new ModelParts.Part[]{
               p("crown", 3000, -1, -81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("hero_helmet", 3001, -1, -63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("legend_helmet", 3002, -1, -45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("mage_hat", 3003, -1, -27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("necro_hood", 3004, -1, -9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("evil_mage_hat", 3005, -1, 9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("white_circlet", 3006, -1, 27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("plague_mask", 3007, -1, 45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("druid_crown", 3008, -1, 63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("bandit_hood", 3009, -1, 81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("crown_biped", 3010, -1, -81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("crown_villager", 3011, -1, -81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("crown_tall", 3012, -1, -81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("hero_helmet_biped", 3013, -1, -63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("hero_helmet_villager", 3014, -1, -63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("hero_helmet_tall", 3015, -1, -63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("legend_helmet_biped", 3016, -1, -45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("legend_helmet_villager", 3017, -1, -45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("legend_helmet_tall", 3018, -1, -45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("mage_hat_biped", 3019, -1, -27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("mage_hat_villager", 3020, -1, -27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("mage_hat_tall", 3021, -1, -27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("necro_hood_biped", 3022, -1, -9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("necro_hood_villager", 3023, -1, -9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("necro_hood_tall", 3024, -1, -9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("evil_mage_hat_biped", 3025, -1, 9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("evil_mage_hat_villager", 3026, -1, 9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("evil_mage_hat_tall", 3027, -1, 9.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("white_circlet_biped", 3028, -1, 27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("white_circlet_villager", 3029, -1, 27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("white_circlet_tall", 3030, -1, 27.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("plague_mask_biped", 3031, -1, 45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("plague_mask_villager", 3032, -1, 45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("plague_mask_tall", 3033, -1, 45.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("druid_crown_biped", 3034, -1, 63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("druid_crown_villager", 3035, -1, 63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("druid_crown_tall", 3036, -1, 63.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("bandit_hood_biped", 3037, -1, 81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("bandit_hood_villager", 3038, -1, 81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("bandit_hood_tall", 3039, -1, 81.0F, 32.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false)
            }
         )
      );
   }

   private static void c_guns() {
      ALL.put(
         "guns",
         new ModelParts.Creature(
            "guns",
            1.0F,
            false,
            false,
            new ModelParts.Part[]{
               p("musket", 4000, -1, 0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("ball", 4001, -1, 14.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("boulder", 4002, -1, 20.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false)
            }
         )
      );
   }

   private static void c_cannon() {
      ALL.put(
         "cannon",
         new ModelParts.Creature(
            "cannon",
            1.5F,
            false,
            false,
            new ModelParts.Part[]{
               p("carriage", 4100, -1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("barrel", 4101, -1, 0.0F, 9.5F, 1.0F, -10.0F, 0.0F, 0.0F, 1.0F, false, a(4, 0, -9.0F, 14.0F, 0.0F, 0, 0))
            }
         )
      );
   }

   private static void c_catapult() {
      ALL.put(
         "catapult",
         new ModelParts.Creature(
            "catapult",
            1.5F,
            false,
            false,
            new ModelParts.Part[]{
               p("frame", 4200, -1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false),
               p("arm", 4201, -1, 0.0F, 19.0F, 0.0F, -34.0F, 0.0F, 0.0F, 1.0F, false, a(4, 0, 72.0F, 24.0F, 0.0F, 0, 0))
            }
         )
      );
   }

   private static void c_meteor() {
      ALL.put(
         "meteor",
         new ModelParts.Creature(
            "meteor",
            1.0F,
            false,
            false,
            new ModelParts.Part[]{
               p("rock", 4300, -1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true),
               p("fire", 4301, -1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, true)
            }
         )
      );
   }

   private static ModelParts.Part still(String name, int cmd, boolean bright) {
      return p(name, cmd, -1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, bright);
   }

   private static void props(String name, ModelParts.Part... parts) {
      ALL.put(name, new ModelParts.Creature(name, 1.0F, false, false, parts));
   }

   private static void c_props() {
      props(
         "bombs",
         still("bomb", 4400, false),
         still("grenade", 4401, false),
         still("napalm", 4402, false),
         still("water", 4403, false),
         still("cluster", 4404, false),
         still("santa", 4405, false),
         still("atomic", 4406, false),
         still("czar", 4407, false),
         still("antimatter", 4408, true),
         still("timed", 4409, false)
      );
      props("blackhole", still("core", 4500, true), still("ring", 4501, true));
      props("tornado", still("funnel", 4600, false), still("skirt", 4601, false), still("debris", 4602, false));
      props("wave", still("crest", 5000, false));
      props("volcano", still("bomb", 5100, true), still("plume", 5101, false));
      props("geyser", still("jet", 5200, false), still("cone", 5201, false));
      props("ship", still("ship", 4800, false));
   }

   // Staffs float beside the carrier's right hand; the model's pole foot sits 16 pixels below its centre.
   private static void staff(String name, int cmd) {
      props(name, p("staff", cmd, -1, -7.0F, 16.5F, 3.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 2, 2.5F, 0.35F, 0.0F, 0, 0), a(0, 0, 6.0F, 1.0F, 0.0F, 1, 0)));
   }

   private static void c_staffs() {
      staff("staff_necro", 4900);
      staff("staff_evil_mage", 4901);
      staff("staff_white_mage", 4902);
      staff("staff_druid", 4903);
      staff("staff_plague_doctor", 4904);
      staff("staff_mage", 4905);
   }

   private static void c_alien() {
      ALL.put(
         "alien",
         new ModelParts.Creature(
            "alien",
            1.0F,
            true,
            false,
            new ModelParts.Part[]{
               p("torso", 4701, -1, 0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(1, 1, 0.4F, 0.4F, 0.0F, 0, 0)),
               p("head", 4700, -1, 0.0F, 38.5F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 3.0F, 0.4F, 0.0F, 0, 0), a(0, 1, 6.0F, 0.15F, 0.0F, 2, 0)),
               p("arm_r", 4702, -1, -5.0F, 29.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 26.0F, 1.1F, 0.5F, 1, 0), a(0, 2, 2.0F, 0.4F, 0.0F, 0, 0)),
               p("arm_l", 4702, -1, 5.0F, 29.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 26.0F, 1.1F, 0.0F, 1, 0), a(0, 2, -2.0F, 0.4F, 0.0F, 0, 0)),
               p("leg_r", 4703, -1, -2.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 30.0F, 1.1F, 0.0F, 1, 0)),
               p("leg_l", 4703, -1, 2.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, false, a(0, 0, 30.0F, 1.1F, 0.5F, 1, 0))
            }
         )
      );
   }

   static {
      c_meteor();
      c_props();
      c_staffs();
      c_alien();
      c_crab();
      c_robot();
      c_titan();
      c_dragon();
      c_kraken();
      c_ufo();
      c_crystal_golem();
      c_fire_elemental();
      c_fire_skull();
      c_jumpy_skull();
      c_hats();
      c_guns();
      c_cannon();
      c_catapult();
   }

   public record Anim(int type, int axis, float amp, float speed, float phase, int when, int wave) {
   }

   public record Creature(String name, float scale, boolean hideCarrier, boolean deathFx, ModelParts.Part[] parts) {
   }

   public record Part(
      String name, int cmd, int parent, float px, float py, float pz, float rx, float ry, float rz, float scale, boolean bright, ModelParts.Anim[] anims
   ) {
   }
}
