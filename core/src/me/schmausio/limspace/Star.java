package me.schmausio.limspace;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;

public enum Star
{
  STATIC_1(50),
  STATIC_2(50),
  STATIC_3(50),

  DYNAMIC_1(10),
  DYNAMIC_2(10),
  DYNAMIC_3(10),

  ;

  public static IntArray list_prob = new IntArray();

  Star(int prob)
  {
    this.probability = prob;

  }

  static
  {
    for (Star s : values())
    {
      for (int i = 0; i < s.probability; i++)
      {
        list_prob.add(s.ordinal());
      }
    }
  }

  public static int random()
  {
    return list_prob.random();
  }

  public static TextureRegion get_region(int star_type, int anim_time)
  {
    star_type = MathUtils.clamp(star_type, 0, values().length - 1);
    Star star = values()[star_type];
    TextureRegion reg = Res.ERROR_FRAME.region;
    Anim anim = null;
    switch (star)
    {
      case STATIC_1:
        reg = Res.STARS.sheet[0];
        break;
      case STATIC_2:
        reg = Res.STARS.sheet[1];
        break;
      case STATIC_3:
        reg = Res.STARS.sheet[2];
        break;
      case DYNAMIC_1:
        anim = Anim.STAR_DYNAMIC_1;
        break;
      case DYNAMIC_2:
        anim = Anim.STAR_DYNAMIC_2;
        break;
      case DYNAMIC_3:
        anim = Anim.STAR_DYNAMIC_3;
        break;
    }
    if (anim != null)
    {
      reg = Res.get_frame(Util.INT_TO_FLOAT(anim_time), anim, false);
    }
    return reg;
  }

  public final int probability;

  public static int update_star(int anim_data, int star_type, float delta)
  {
    star_type = MathUtils.clamp(star_type, 0, values().length - 1);
    Star star = values()[star_type];
    int ret = 0;
    Anim anim = null;
    switch (star)
    {
      case DYNAMIC_1:
        anim = Anim.STAR_DYNAMIC_1;
        break;
      case DYNAMIC_2:
        anim = Anim.STAR_DYNAMIC_2;
        break;
      case DYNAMIC_3:
        anim = Anim.STAR_DYNAMIC_3;
        break;
    }
    if (anim != null)
    {
      float anim_time = Util.INT_TO_FLOAT(anim_data);
      float[] ret_val = anim.update(anim_time, delta);
      ret = Util.FLOAT_TO_INT(ret_val[0]);
    }
    return ret;
  }


}