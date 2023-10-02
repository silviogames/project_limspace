package me.schmausio.limspace;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Entity
{
  // very generic entity class, no inheritance!

  float posx, posy;
  float vx = 0, vy = 0;

  public static boolean jumped = false;

  boolean falling = true;
  boolean coyote = false;
  public EntityType type;

  float coyote_time = 0f;

  Anim anim;
  float anim_time = 0f;

  boolean looking_right = false;

  // combined value of the chunk
  public int origin_chunk = -1;

  static Timer timer_AI = new Timer(0.1f);
  static boolean AI_check = false;

  boolean dead = false;

  static int wutz_life = 3;
  static float time_blink = 0f;

  static float check_point_x = Chunk.TILE_SIZE * 10;
  static float check_point_y = Chunk.TILE_SIZE * 30;

  float despawn_time = 0f;

  // used during rocket animation
  public static boolean hide_player = false;

  // postbox
  static String message = "";
  static float time_message = 0f;
  static int message_posx, message_posy;

  static
  {
    int spawn_chunk_x = 0;
    int spawn_chunk_y = 5;
    int tilex_offset = 20;
    int tiley_offset = 14;
    //check_point_x = spawn_chunk_x * Chunk.CHUNK_SIZE * Chunk.TILE_SIZE + tilex_offset * Chunk.TILE_SIZE;
    //check_point_y = spawn_chunk_y * Chunk.CHUNK_SIZE * Chunk.TILE_SIZE + tiley_offset * Chunk.TILE_SIZE;
  }

  public Entity(float posx, float posy, EntityType type)
  {
    this.posx = posx;
    this.posy = posy;
    this.type = type;

    switch (type)
    {
      case PLAYER:
        this.posx = check_point_x;
        this.posy = check_point_y;
        break;
    }

    anim = type.anim_idle(this, vx > 0);
  }

  public void update(float delta, int index)
  {
    if (dead) return;
    if (type == EntityType.PLAYER && hide_player) return;

    switch (type)
    {
      case PLAYER:
      {
        if (AI_check)
        {
          for (int i = 0; i < World.list_entities.size; i++)
          {
            Entity ent = World.list_entities.get(i);
            if (ent.type == EntityType.ROCKET)
            {
              if (Util.simple_dist(posx, posy, ent.posx, ent.posy + 10) < 20 * 20)
              {
                hide_player = true;

                float px = World.global_offset_x + ent.posx;
                float py = World.global_offset_y + ent.posy;

                ent.dead = true;

                World.rocket_start_x = (int) px;
                World.rocket_start_y = (int) py;
                World.rocket_progress = 0f;
                World.init_status(World.WorldStatus.ROCKET_FLY);
                return;
              }
            }
          }

        }

        if (time_message > 0f)
        {
          time_message += delta;
          if (time_message >= 5f)
          {
            time_message = 0f;
            message = "";
          }
        }

        if (time_blink > 0f)
        {
          time_blink += delta;
          if (time_blink >= 0.8f)
          {
            time_blink = 0f;
          }
        }

        if (posy < -300)
        {
          this.posx = check_point_x;
          this.posy = check_point_y;
        }

        //if (Gdx.input.isKeyJustPressed(Input.Keys.E))
        //{
        //   // throwing box
        //   if (pack && !World.collision(posx + (flip ? -1 : 1) * 17, posy + 12))
        //   {
        //      box = new Entity(posx + (flip ? -1 : 1) * 17, posy + 12, EntityType.BOX);
        //      box.vx = (flip ? -1 : 1) * (Math.abs(vx) > 0.5f ? 1.4f : 0.9f);
        //      box.vy = Math.abs(vx) > 0.5f ? 3 : 1.5f;
        //      box.falling = true;
        //      World.list_spawn.add(box);
        //      pack = false;
        //   }
        //}

        if (AI_check)
        {
          for (int i = 0; i < World.list_entities.size; i++)
          {

            //// PICK UP DROPPED BOX
            //Entity find_box = World.list_entities.get(i);
            //if (!find_box.dead && find_box.type == EntityType.BOX && !pack && find_box.vx == 0 //&& find_box.vy == 0)
            //{
            //   if (Util.simple_dist(posx, posy, find_box.posx, find_box.posy) < 16 * 16)
            //   {
            //      World.list_entity_index_remove.add(i);
            //      find_box.dead = true;
            //      pack = true;
            //      break;
            //   }
            //}

            // PICK UP COLLECTABLE
            //if (find_box.type == EntityType.COLLECT_BOX)
            //{
            //   if (Util.simple_dist(posx, posy, find_box.posx, find_box.posy) < 16 * 16)
            //   {
            //      Chunk collect_box_chunk = World.get_chunk(find_box.posx, find_box.posy);
            //      collect_box_chunk.found_collect_box();
            //      find_box.dead = true;
            //      World.list_entity_index_remove.add(i);
            //      num_collected_box++;
            //      break;
            //   }
            //}

            //if (find_box.type == EntityType.POSTBOX)
            //{
            //   if (Util.simple_dist(posx, posy, find_box.posx, find_box.posy) < 20 * 20)
            //   {
            //      if (!find_box.postbox_active)
            //      {
            //         find_box.postbox_active = true;
            //         check_point_x = find_box.posx;
            //         check_point_y = find_box.posy + 5;
            //         display_message("CHECKPOINT!", (int) find_box.posx, (int) //find_box.posy);
            //         break;
            //      }
            //   }
            //}
          }
        }

        boolean pressing_move = false;
        if (Gdx.input.isKeyPressed(Input.Keys.A))
        {
          pressing_move = true;
          vx = -1;
          looking_right = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D))
        {
          pressing_move = true;
          vx = 1;
          looking_right = true;
        }

        if (!pressing_move && vx != 0)
        {
          vx = MathUtils.lerp(vx, 0, falling ? 0.03f : 0.5f);
          if (Math.abs(vx) < 0.001f) vx = 0;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE))
        {
          if (!falling)
          {
            jumped = true;
            falling = true;
            vy = Config.CONF.JUMP_STRENGTH.value;
          }
        }

        if (!falling)
        {
          if (!World.collision(posx, posy - 4, -1))
          {
            coyote = true;
          }
          if (coyote)
          {
            coyote_time += delta;
            if (coyote_time > Util.INT_TO_FLOAT(Config.CONF.COYOTE_TIME_MS.value))
            {
              falling = true;
              coyote_time = 0;
              coyote = false;
            }
          }
        }
        if (Math.abs(vx) >= 0.05)
        {
          anim = type.anim_run(this, looking_right);
        } else
        {
          anim = type.anim_idle(this, looking_right);
        }
        if (vy != 0)
        {
          anim = type.anim_fall(this, looking_right, jumped && vy > -50);
        }
      }
      break;

      //case ENEMY_FLOWER:
      //{
      //   if (AI_check)
      //   {
      //      if (Util.simple_dist(posx, posy, World.player.posx, World.player.posy) < 150 * 150)
      //      {
      //         anim = type.anim_run(this);
      //         int dir = (int) Math.signum(World.player.posx - posx);
      //         vx = dir;
      //      } else
      //      {
      //         if (Util.simple_dist(posx, posy, World.player.posx, World.player.posy) > 200 * 200)
      //         {
      //            vx = 0;
      //            anim = type.anim_idle(this);
      //         }
      //      }

      //   }

      //   if (!falling)
      //   {
      //      if (!World.collision(posx, posy - 4))
      //      {
      //         falling = true;
      //      }
      //   }

      //   // every frame!
      //   if (Util.simple_dist(posx, posy, World.player.posx, World.player.posy) < Config.CONF//.PIG_HIT_RADIUS.value)
      //   {
      //      if (time_blink == 0f)
      //      {
      //         wutz_life--;
      //         if (wutz_life <= 0)
      //         {
      //            if (!pack)
      //            {
      //               for (int i = 0; i < World.list_entities.size; i++)
      //               {
      //                  if (World.list_entities.get(i).type == EntityType.BOX)
      //                  {
      //                     World.list_entity_index_remove.add(i);
      //                     World.list_entities.get(i).dead = true;
      //                     break;
      //                  }
      //               }
      //               pack = true;
      //            }
      //
      //            World.player.posx = check_point_x;
      //            World.player.posy = check_point_y;
      //            display_message("try again!", (int) check_point_x, (int) check_point_y);
      //            wutz_life = 3;
      //            time_blink = 0f;
      //         } else
      //         {
      //            time_blink = 0.01f;
      //         }
      //      }
      //   }
      //}
      //break;

      // case PARTICLE_FLOWER:
      // {
      //    anim = Anim.PARTICLE_FLOWER;

      //    if (vx != 0)
      //    {
      //       vx = MathUtils.lerp(vx, 0, falling ? 0.01f : 0.5f);
      //       if (Math.abs(vx) < 0.001f) vx = 0;
      //    }

      //    if (vx == 0 && vy == 0)
      //    {
      //       despawn_time += delta;
      //       if (despawn_time >= 1f)
      //       {
      //          dead = true;
      //          World.list_entity_index_remove.add(index);
      //       }
      //    }
      // }
      // break;
    }

    if (type.gravity_affected)
    {
      //posx += vx * Config.CONF.WALK_SPEED.value * delta;
      if (falling)
      {
        vy -= (Config.CONF.GRAVITY.value / (type.slow_gravity ? 100f : 10f)) * delta;
        vy = MathUtils.clamp(vy, -Config.CONF.MAX_FALLING_SPEED.value, Config.CONF.MAX_FALLING_SPEED.value);
      } else
      {
      }

      float nx = posx + vx * type.walk_speed() * delta;
      float ny = posy + vy * delta;

      boolean collision_hori = World.collision(nx, posy, vy);
      boolean collision_vert = World.collision(posx, ny, vy);
      boolean collision_vert2 = World.collision(nx, ny, vy);

      int num_pixels_per_move = MathUtils.floor(Math.abs(posy - ny));

      if (num_pixels_per_move > 1)
      {
        boolean collided = false;
        for (int i = 0; i < num_pixels_per_move; i++)
        {
          float interp_posy = MathUtils.lerp(posy, ny, i / ((float) (num_pixels_per_move - 1)));
          if (World.collision(posx, interp_posy, vy))
          {
            jumped = false;
            falling = false;
            coyote_time = 0;
            coyote = false;
            if (vy >= 0)
            {
              posy = MathUtils.round(interp_posy / 16f) * 16f - 3f;
            } else
            {
              posy = MathUtils.round(interp_posy / 16f) * 16f;
            }
            vy = 0;
            collided = true;
            break;
          }
        }
        if (!collided) posy = ny;
      } else
      {
        if (collision_vert)
        {
          jumped = false;
          falling = false;
          coyote_time = 0;
          coyote = false;
          vy = 0;
        } else
        {
          posy = ny;
        }
      }

      if (collision_hori)
      {
        vx = 0;
      } else
      {
        posx = nx;
      }

      if (posx <= World.wall_position(false))
      {
        posx = World.wall_position(false);
        if (!falling && !World.collision(posx, posy - 4, -1))
        {
          falling = true;
        }
      }

      if (posx >= World.wall_position(true))
      {
        posx = World.wall_position(true);
        if (!falling && !World.collision(posx, posy - 4, -1))
        {
          falling = true;
        }
      }

      float[] ret = anim.update(anim_time, delta);
      anim_time = ret[0];
    }
  }

  public void render()
  {
    float px = World.global_offset_x + posx;
    float py = World.global_offset_y + posy;

    if(dead ) return;

    switch (type)
    {
      case PLAYER:
      {
        if (hide_player) break;
        boolean blink = time_blink > 0f && MathUtils.floor(time_blink / 0.07f) % 2 == 0;
        Main.batch.setColor(blink ? RenderUtil.color_blink : Color.WHITE);
        TextureRegion reg = Res.get_frame(anim_time, anim, false);
        Main.batch.draw(reg, px - reg.getRegionWidth() / 2f, py);
        Main.batch.setColor(Color.WHITE);

      }
      break;
      case ROCKET:
        Main.batch.draw(Res.ROCKET_CAT.sheet[0], px - Res.ROCKET_CAT.sheet[0].getRegionWidth() / 2f, py);
        break;
      //case ENEMY_MUSHROOM:
      //{
      //   TextureRegion reg = Res.get_frame(anim_time, anim, flip);
      //   Main.batch.draw(reg, px - reg.getRegionWidth() / 2f, py);
      //}
      //break;
    }
  }

  public static void display_message(String message, int message_posx, int message_posy)
  {
    time_message = 0.1f;
    Entity.message = message;
    Entity.message_posx = message_posx;
    Entity.message_posy = message_posy;
  }


  public enum EntityType
  {
    PLAYER(true, false, false),

    ROCKET(false, false, true),

    ;

    public final boolean gravity_affected;
    public final boolean slow_gravity;

    public final boolean enemy;

    EntityType(boolean wob, boolean enemy, boolean slow_gravity)
    {
      this.gravity_affected = wob;
      this.enemy = enemy;
      this.slow_gravity = slow_gravity;
    }

    public int walk_speed()
    {
      int ret = 5;
      switch (this)
      {
        case PLAYER:
          ret = Config.CONF.WALK_SPEED.value;
          break;
      }
      return ret;
    }

    public Anim anim_idle(Entity ent, boolean right)
    {
      Anim ret = Anim.DUMMY;
      switch (this)
      {
        case PLAYER:
          // TODO: 30.09.23 fix
          ret = right ? Anim.CAT_IDLE_RIGHT : Anim.CAT_IDLE_LEFT;
          break;
      }
      return ret;
    }

    public Anim anim_fall(Entity ent, boolean right, boolean jump)
    {
      Anim ret = Anim.DUMMY;
      switch (this)
      {
        case PLAYER:
          if (jump)
          {
            ret = right ? Anim.CAT_JUMP_RIGHT : Anim.CAT_JUMP_LEFT;
          } else
          {
            ret = right ? Anim.CAT_FALL_RIGHT : Anim.CAT_FALL_LEFT;
          }
          break;
      }
      return ret;
    }

    public Anim anim_run(Entity ent, boolean right)
    {
      Anim ret = Anim.DUMMY;
      switch (this)
      {
        case PLAYER:
          ret = right ? Anim.CAT_RUN_RIGHT : Anim.CAT_RUN_LEFT;
          break;
      }
      return ret;
    }
  }
}
