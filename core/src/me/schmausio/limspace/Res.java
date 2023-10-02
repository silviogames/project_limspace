package me.schmausio.limspace;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;

// this enum contains all texture regions that are loaded from atlas
public enum Res
{
  //SHEET_FLOOR_TILES(3, 16, 16),

  ERROR_FRAME(),

  PLATFORM(Tile.values().length, 16, 16),

  CAT(20, 30, 34),

  BACKGROUND(2, 400,300),

  STARS(11, 7,7),

  WALL_EDGE(),

  WALL_BLOCK(),

  //BOX(),
  //BLOCK1(Tile.values().length, 16, 16),
  //PIG(10, 32, 26),
  //ENEMY_FLOWER(5, 26, 24),
  //PARTICLE_FLOWER(4, 10, 10),
  //DECORATION(Deco.values().length, 98, 123),
  //HEART(),
  //POSTBOX(2, 32, 43),
  //ENEMY_MUSHROOM(5, 20, 23),
  //SKY(),
  //PARTICLE_MUSHROOM(),
  ;

  static final Array<Anim> load_queue = new Array<>();
  public static Array<TextureRegion> animation_frames = new Array<>(); // contains all frames of all animations
  public static IntIntMap anim_to_frame = new IntIntMap(); // points from anim id to start of frame in animation_frames
  static TextureAtlas atlas;
  public static TextureRegion alphabet, pixel;
  int sheet_num = 0, sheet_width = -1, sheet_height = -1;
  TextureRegion region;
  TextureRegion[] sheet;

  Res()
  {
    // empty constructor

  }

  Res(int num, int sw, int sh)
  {
    this.sheet_num = num;
    this.sheet_width = sw;
    this.sheet_height = sh;
  }

  public static void load()
  {
    atlas = new TextureAtlas("limspace.atlas");
    alphabet = atlas.findRegion("5x6font");
    pixel = new TextureRegion(alphabet, 2, 0, 1, 1);

    for (Res r : values())
    {
      r.region = atlas.findRegion(r.name().toLowerCase());
      if (r.region == null)
      {
        System.out.println("RES: could not find " + r);
      }

      if (r.sheet_num > 0)
      {
        r.sheet = cut_sprites(r.region, r.sheet_num, r.sheet_width, r.sheet_height);
      }
    }

    load_queue.add(Anim.CAT_IDLE_LEFT);
    load_queue.add(Anim.CAT_IDLE_RIGHT);
    load_queue.add(Anim.CAT_RUN_LEFT);
    load_queue.add(Anim.CAT_RUN_RIGHT);
    load_queue.add(Anim.CAT_FALL_LEFT);
    load_queue.add(Anim.CAT_FALL_RIGHT);
    load_queue.add(Anim.CAT_SCHLECK_LEFT);
    load_queue.add(Anim.CAT_SCHLECK_RIGHT);
    load_queue.add(Anim.CAT_JUMP_LEFT);
    load_queue.add(Anim.CAT_JUMP_RIGHT);
    load_anims(CAT, 0);
    load_queue.clear();

    load_queue.add(Anim.STAR_STATIC);
    load_queue.add(Anim.STAR_DYNAMIC_1);
    load_queue.add(Anim.STAR_DYNAMIC_2);
    load_queue.add(Anim.STAR_DYNAMIC_3);
    load_anims(STARS, 0);
    load_queue.clear();

      /*
      load_queue.add(Anim.PARTICLE_BLOOD);
      load_anims(atlas.findRegion("particle_blood"), 50, 20, 0);
      load_queue.clear();

      load_queue.add(Anim.MINER_IDLE);
      load_queue.add(Anim.MINER_RUN);
      load_queue.add(Anim.MINER_HAMMER);
      load_anims(atlas.findRegion("guy"), 32, 34, 0);
      load_queue.clear();

      load_queue.add(Anim.MINER_IDLE);
      load_queue.add(Anim.MINER_SLIDE);
      load_anims(atlas.findRegion("guy"), 32, 34, 0);
      load_queue.clear();
       */

  }

  private static void fixBleeding(TextureRegion... array)
  {
    for (TextureRegion tr : array)
    {
      fixBleeding(tr);
    }
  }

  private static void fixBleeding(TextureRegion region)
  {
    float fix = 0.01f;

    float x = region.getRegionX();
    float y = region.getRegionY();
    float width = region.getRegionWidth();
    float height = region.getRegionHeight();
    float invTexWidth = 1f / region.getTexture().getWidth();
    float invTexHeight = 1f / region.getTexture().getHeight();
    region.setRegion((x + fix) * invTexWidth, (y + fix) * invTexHeight, (x + width - fix) * invTexWidth, (y + height - fix) * invTexHeight); // Trims
    // region
  }

  private static TextureRegion[] cut_sprites(TextureRegion sprite_sheet, int num_sprites, int width, int height)
  {
    // this is only working properly with a 1 pixel spacing between frames
    if (sprite_sheet == null)
    {
      System.out.println("[RES] cannot cut sprite. missing sprite_sheet!");
      return new TextureRegion[num_sprites];
    }
    TextureRegion[] r = new TextureRegion[num_sprites];
    for (int i = 0; i < num_sprites; i++)
    {
      r[i] = new TextureRegion(sprite_sheet, i * (width + 1), 0, width, height);
    }
    fixBleeding(r);
    return r;
  }

  private static void load_anims(Res res, int yoff)
  {
    load_anims(res.region, res.sheet_width, res.sheet_height, yoff);
  }

  private static void load_anims(TextureRegion source, int width, int height, int yoff)
  {
    // this method is called AFTER the load_queue has been filled with ANIMS!
    // this source Region passed to this method is expected to have enough sprites/frames that
    // the load_queue ANIMS expect.
    // this method automatically appends flipped frame variants if the ANIM in the queue needs that
    // then the frames are put in the global animation_frames array and also the ANIMS ordinal points to
    // the beginning of the sprite sheet in the global array.

    if (source == null)
    {
      Gdx.app.log("[RES] ", "could not find source region");
      return;
    }
    // load frames of animation strip into global frame list
    int xpos = 0;
    int frames = 0;

    for (int i = 0; i < load_queue.size; i++)
    {
      anim_to_frame.put(load_queue.get(i).ordinal(), animation_frames.size);
      // move start position on animation strip
      xpos = frames * (width + 1);

      for (int j = 0; j < load_queue.get(i).num_frames; j++)
      {
        // adding the frames to the global list
        animation_frames.add(new TextureRegion(source, xpos + (j * (width + 1)), yoff, width, height));
        frames++;
      }
      // now appending the flipped variants
      if (load_queue.get(i).flip)
      {
        for (int j = 0; j < load_queue.get(i).num_frames; j++)
        {
          animation_frames.add(new TextureRegion(source, xpos + (j * (width + 1)) + width, yoff, -width, height));
        }
      }
    }
  }

  public static TextureRegion get_frame(float frame_time, Anim anim)
  {
    return get_frame(frame_time, anim, false);
  }

  public static TextureRegion get_frame(float frame_time, Anim anim, boolean flipped)
  {
    if (anim == null)
    {
      return ERROR_FRAME.region;
    }
    int frame = anim_to_frame.get(anim.ordinal(), 0);
    if (flipped && anim.flip)
    {
      frame += anim.num_frames;
    }
    frame += anim.get_frame(frame_time);
    if (frame >= 0 && frame < animation_frames.size)
    {
      return animation_frames.get(frame);
    } else
    {
      return ERROR_FRAME.region;
    }
  }

  public static void dispose()
  {
    atlas.dispose();
  }
}