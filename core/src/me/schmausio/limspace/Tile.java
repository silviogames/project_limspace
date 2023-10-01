package me.schmausio.limspace;

import com.badlogic.gdx.utils.IntIntMap;

public enum Tile
{
   AIR(0),
   ROCK_LEFT( 2),
   ROCK_MIDDLE( 2),
   ROCK_RIGHT( 2),
   ;

   // collision types:
   // 0 -> no collision
   // 1 -> collision
   // 2 -> can be passed through from below

   public final int collision_type;

   Tile( int collision_type)
   {
      this.collision_type = collision_type;
   }

   public static Tile safe_ord(int ordinal)
   {
      if (ordinal < 0 || ordinal >= values().length)
      {
         return AIR;
      } else
      {
         return values()[ordinal];
      }
   }
}