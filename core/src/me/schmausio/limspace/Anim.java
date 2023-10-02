package me.schmausio.limspace;

public enum Anim
{
  // SIMPLE ANIMATION SYSTEM

  DUMMY(2, 1f, -1, false, 0, 1),

  CAT_IDLE_LEFT(2, 0.2f, -1, false, 0, 1),
  CAT_IDLE_RIGHT(2, 0.2f, -1, false, 0, 1),
  CAT_RUN_LEFT(2, 0.08f, -1, false, 0, 1),
  CAT_RUN_RIGHT(2, 0.08f, -1, false, 0, 1),
  CAT_FALL_LEFT(2, 0.2f, -1, false, 0, 1),
  CAT_FALL_RIGHT(2, 0.2f, -1, false, 0, 1),
  CAT_JUMP_LEFT(2, 0.2f, -1, false, 0, 1),
  CAT_JUMP_RIGHT(2, 0.2f, -1, false, 0, 1),

  CAT_SCHLECK_LEFT(3, 0.2f, -1, false, 0, 1, 2),
  CAT_SCHLECK_RIGHT(3, 0.2f, -1, false, 0, 1, 2),

  STAR_STATIC(3, 0.1f, -1, false, 0, 1, 2),
  STAR_DYNAMIC_1(2, 0.8f, -1, false, 0, 1),
  STAR_DYNAMIC_2(3, 0.4f, -1, false, 0, 1, 2),
  STAR_DYNAMIC_3(3, 0.6f, -1, false, 0, 1, 2),

  //ROCKET(2, 0.2f, -1, false, 0, 1),
  ROCKET_FIRE(2, 0.1f, -1, false, 0, 1),

  SMOKE(7, 0.2f, -1, false, 0, 1, 2, 3, 4, 5, 6),

  ;
  public static float[] returner = new float[3];
  public final int keyframe, num_frames; // number of individual frames, may appear more than once in anim
  public final boolean flip;
  public final int[] frames;
  public final float frame_time, total_time;

  Anim(int num_indiv_frames, float frame_time, int keyframe, boolean flip, int... frames)
  {
    this.num_frames = num_indiv_frames;
    this.flip = flip;
    this.frame_time = frame_time;
    this.total_time = frame_time * frames.length;
    this.frames = frames;
    this.keyframe = keyframe;
  }

  public float get_fractional_time(float current_time)
  {
    return current_time / total_time;
  }

  public float[] update(float current_time, float change)
  {
    returner[1] = 0;
    returner[2] = 0;
    // returner[0] == 0.343f -> current time after change
    // returner[1] == 1 -> keyframe
    // returner[2] == 1 -> end of animation

    // frame time is returned negative if this is keyframe!
    int old_frame = get_frame(current_time);
    float next_time = current_time + change;
    int new_frame = get_frame(next_time);
    if (new_frame != old_frame && new_frame == keyframe)
    {
      returner[1] = 1f;
    }
    if (next_time > total_time)
    {
      if (next_time > total_time * 2)
      {
        returner[0] = 0f;
      } else
      {
        returner[0] = next_time - total_time;
      }
      returner[2] = 1f;
    } else
    {
      returner[0] = next_time;
    }
    return returner;
  }

  public int get_frame(float time)
  {
    return frames[((int) (time / frame_time)) % frames.length];
  }
}