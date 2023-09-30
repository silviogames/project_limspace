package me.schmausio.limspace;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class Config
{
   public static void load_config(boolean verbose)
   {
      if (verbose) System.out.println("LOADING CONFIG");
      // loads and applies config

      // number of successful assignments
      int assigned_values = 0;

      FileHandle file = Gdx.files.internal("config.txt");
      String[] lines = file.readString().split("\n");
      for (String line : lines)
      {
         line = line.trim();
         String[] split_line = line.split(":");
         if (split_line.length != 2)
         {
            if (verbose) System.out.println("config line #" + line + "# invalid, skipped");
            continue;
         }
         int val = -1;
         try
         {
            val = Integer.parseInt(split_line[1]);
         } catch (Exception e)
         {
            if (verbose)
               System.out.println("config line #" + line + "# has no number after colon, skipped");
            continue;
         }
         for (CONF conf : CONF.values())
         {
            if (conf.toString().equals(split_line[0]))
            {
               conf.value = val;
               assigned_values++;
               break;
            }
         }
      }
      if (verbose)
      {
         System.out.println("CONFIG LOADING SUMMARY:");
         System.out.println("number of config values: " + CONF.values().length);
         System.out.println("number of loaded config lines: " + lines.length);
         System.out.println("number of assignments: " + assigned_values);
      }
   }

   public static void print_config()
   {
      // PRINTING CONF TO CONSOLE FOR TESTING
      for (CONF conf : CONF.values())
      {
         System.out.println("CONF:" + conf.toString() + " : " + conf.value);
      }
   }

   public enum CONF
   {
      WALK_SPEED,
      GRAVITY,
      JUMP_STRENGTH,
      MAX_FALLING_SPEED,
      COYOTE_TIME_MS,
      WALK_SPEED_FLOWER,
      WALK_SPEED_BOX,
      ENEMY_HIT_RADIUS,
      WALK_SPEED_PARTICLE_FLOWER,
      UP_SPEED_PARTICLE_FLOWER,
      PIG_HIT_RADIUS,
      ;

      // IF THE FILE DOES NOT SET THE VALUE A DEFAULT OF 10 MAY BE WEIRD FOR SOME OF THE CONFIGS
      public int value = 10; // loaded from file
   }
}