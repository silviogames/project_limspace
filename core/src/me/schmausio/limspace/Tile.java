package me.schmausio.limspace;

import com.badlogic.gdx.utils.IntIntMap;

public enum Tile
{
  AIR(0),
  DUE_1(2),
  DUE_2(2),

  TRE_1(2),
  TRE_2(2),
  TRE_3(2),

  QUATTRO_1(2),
  QUATTRO_2(2),
  QUATTRO_3(2),
  QUATTRO_4(2),

  CINQUE_1(2),
  CINQUE_2(2),
  CINQUE_3(2),
  CINQUE_4(2),
  CINQUE_5(2),
  ;

  // collision types:
  // 0 -> no collision
  // 1 -> collision
  // 2 -> can be passed through from below

  public final int collision_type;

  Tile(int collision_type)
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