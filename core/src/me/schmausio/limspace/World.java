package me.schmausio.limspace;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Json;

public class World
{
  public static Array<Chunk> list_chunks = new Array<>();

  // combinded cx,cy map to index of chunks in list_chunk
  public static IntIntMap chunk_map = new IntIntMap();

  static IntSet visible_chunks_set = new IntSet();
  // index that increments every frame and checks all chunks for visibility
  static int chunk_check_index = 0;

  static Smartrix sm_stars = new Smartrix(4, -1, -1);
  // STARS:
  // 0 -> star_type
  // 1 -> x position
  // 2 -> y position
  // 3 -> anim time

  static Smartrix sm_smoke = new Smartrix(6, -1, -1);
  // SMOKE:
  // 0 -> type ( always 0)
  // 1 -> x position
  // 2 -> y position
  // 3 -> anim time
  // 4 -> y off
  // 5 -> random blackness

  static int delete_chunk_index = -1;
  static int delete_chunk_counter = 0;

  // used to load from sub directory with name of the level
  public static String level_name = "foobar";

  public static float global_offset_x, global_offset_y;
  public static float camera_offset_x, camera_offset_y = -50;

  public static float editor_x, editor_y;

  public static int copy_status = 0;
  // 0 -> waiting for copy
  // 1 -> copy ongoing
  // 2 -> have something in copy buffer to paste

  public final static int editor_box_height = 60;

  public static int copy_start_tilex = -1;
  public static int copy_start_tiley = -1;
  public static int copy_end_tilex = -1;
  public static int copy_end_tiley = -1;

  public static int copy_width = 0;
  public static int copy_height = 0;

  public static Flatbyte copied_tile_data = new Flatbyte(64, 64, (byte) -1, (byte) -1);

  public static int edit_tile_ordinal = 1;
  public static int edit_object_ordinal = 1;
  public static int edit_wall_ordinal = 1;

  // TODO: 01.10.23 implement later when decoration is in the game
  public static int edit_layer = 0;
  // 0 -> tiles
  // 1 -> objects
  // 2 -> decoration
  // 3 -> copy paste

  public static String[] edit_layer_names = new String[4];


  public static int edit_tilex = 0, edit_tiley = 0;
  public static int edit_chunkx = 0, edit_chunky = 0;

  public static int mouse_x, mouse_y;
  public static Vector2 mouse_pos = new Vector2();

  public static WorldStatus status;

  static Timer timer_debug_move_chunks = new Timer(0.1f);

  static Entity player = new Entity(Chunk.TILE_SIZE * 10, Chunk.TILE_SIZE * 20, Entity.EntityType.PLAYER);

  public static Array<Entity> list_entities = new Array<>();
  // used for copy
  public static IntArray list_entity_index_remove = new IntArray();

  public static Array<Entity> list_spawn = new Array<>();

  static Array<FileHandle> list_chunk_files = new Array<>();

  static boolean debug_render = false;

  static Osc osc_box_hover = new Osc(3, 5, 2);

  static Osc osc_edit_tile = new Osc(0.2f, 0.6f, 3f, 2f);

  static Json json;

  static final int end_x = 2630, end_y = 4880;

  public static float wall_progress = 0f;
  public static float wall_width = 300;

  public static Color gameover_pause_back = Color.BLACK.cpy();

  // GAMEOVER DATA:

  public static float gameover_opacity = 0f;


  public static Color gameover_color_back = Color.BLACK.cpy();
  public static Color gameover_color_text = Color.BLACK.cpy();

  public static Color debug_text_color = Color.LIGHT_GRAY.cpy();

  // set when touching rocket to play animation
  public static int rocket_start_x, rocket_start_y;
  public static float rocket_progress = 0f;

  public static float fire_anim_progress = 0f;

  static
  {
    gameover_pause_back.a = 0.5f;

    edit_layer_names[0] = "TILES";
    edit_layer_names[1] = "OBJECTS";
    edit_layer_names[2] = "DECO";
    edit_layer_names[3] = "COPY PASTE";

    list_entities.add(player);

    for (int i = 0; i < 100; i++)
    {
      sm_stars.add_line(Star.random(), MathUtils.random(0, Main.SCREEN_WIDTH), MathUtils.random(-100, Main.SCREEN_HEIGHT + 100), MathUtils.random(0, 1000));
    }
  }

  public static void init_status(WorldStatus new_status)
  {
    switch (new_status)
    {
      case GAMEOVER:
      {
        gameover_opacity = 0f;
        gameover_color_back.a = 0f;
      }

      break;
      case LOAD_LEVEL:
      {
        wall_progress = 0f;

        player.posx = Chunk.TILE_SIZE * Config.CONF.PLAYER_SPAWN_TILEX.value;
        player.posy = Chunk.TILE_SIZE * Config.CONF.PLAYER_SPAWN_TILEY.value;
        player.vx = 0;
        player.vy = 0;

        list_entities.clear();
        list_spawn.clear();
        list_entity_index_remove.clear();

        list_entities.add(player);
        Entity.hide_player = false;

        rocket_progress = 0f;
        sm_smoke.clear_all_lines();

        // this might be entered after finished another level so I need to clean up
        list_chunks.clear();
        chunk_map.clear();
        visible_chunks_set.clear();

        json = new Json();
        // populate the list of files that should be loaded

        FileHandle chunk_dir;
        if (Main.RELEASE)
        {
          chunk_dir = Gdx.files.internal("levels/" + level_name + "/");
        } else
        {
          chunk_dir = Gdx.files.local("levels/" + level_name + "/");
        }
        if (chunk_dir.exists())
        {
          list_chunk_files.addAll(chunk_dir.list());
        } else
        {
          if (!Main.RELEASE) chunk_dir.mkdirs();
        }
      }
      break;
      case EDIT_CHUNKS:
      {
        System.out.println("entering chunk edit mode");

        if (list_chunks.size == 0)
        {
          Chunk start_chunk = new Chunk(0, 0);
          for (int i = 0; i < Chunk.CHUNK_SIZE; i++)
          {
            for (int iy = 0; iy < 6; iy++)
            {
              start_chunk.tiles.set(i, iy, (byte) 2);
            }
            start_chunk.tiles.set(i, 6, (byte) 1);
          }
          chunk_map.put(start_chunk.combined_pos, list_chunks.size);
          list_chunks.add(start_chunk);

          save_chunks();
        }

        editor_x = player.posx;
        editor_y = player.posy;
      }
      break;
    }
    status = new_status;
  }

  public static int click_tilex(float pointer_x)
  {
    float local_offset = Main.SCREEN_WIDTH / 2f + camera_offset_x;
    return MathUtils.floor(((editor_x - local_offset) + pointer_x) / Chunk.TILE_SIZE);
  }

  public static int click_tiley(float pointer_y)
  {
    float local_offset = Main.SCREEN_HEIGHT / 2f + camera_offset_y;
    return MathUtils.floor(((editor_y - local_offset) + pointer_y) / Chunk.TILE_SIZE);
  }

  public static boolean collision(float entity_x, float entity_y, float vy)
  {
    Tile tile = get_tile(entity_x, entity_y);
    if (tile.collision_type == 0) return false;

    if (tile.collision_type == 1) return true;

    if (tile.collision_type == 2)
    {
      if (vy < 0)
      {
        // object is falling (should collide with upper edge of platform)
        float local_pos = entity_y % Chunk.TILE_SIZE;
        return local_pos > 14;
      } else
      {
        return false;
      }
    }
    return false;
  }

  public static Tile get_tile(float entity_x, float entity_y)
  {
    int tx = (int) (entity_x / Chunk.TILE_SIZE);
    int ty = (int) (entity_y / Chunk.TILE_SIZE);
    tx = tx % Chunk.CHUNK_SIZE;
    ty = ty % Chunk.CHUNK_SIZE;
    Chunk c = get_chunk(entity_x, entity_y);
    if (c == null)
    {
      return Tile.AIR;
    } else
    {
      return Tile.safe_ord(c.tiles.get(tx, ty));
    }
  }

  public static int entity_pos_to_chunk_x(float entity_x)
  {
    return (int) (entity_x / (Chunk.TILE_SIZE * Chunk.CHUNK_SIZE));
  }

  public static int entity_pos_to_chunk_y(float entity_y)
  {
    return (int) (entity_y / (Chunk.TILE_SIZE * Chunk.CHUNK_SIZE));
  }

  public static Chunk get_chunk(float entity_x, float entity_y)
  {
    return get_chunk(entity_pos_to_chunk_x(entity_x), entity_pos_to_chunk_y(entity_y));
  }

  public static Chunk get_chunk(int cx, int cy)
  {
    int combinedValue = (cx << 16) | cy;
    int chunk_index = chunk_map.get(combinedValue, -1);
    if (chunk_index == -1) return null;
    return list_chunks.get(chunk_index);
  }

  public static void update(float delta)
  {
    if (Main.DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.F2))
    {
      // QUICK RELOAD THE GAME
      if (status == WorldStatus.PLAY) init_status(WorldStatus.LOAD_LEVEL);
    }

    osc_box_hover.update(delta);
    osc_edit_tile.update(delta);
    RenderUtil.color_edit_tile.a = osc_edit_tile.value();
    RenderUtil.color_edit_tile_copy_box.a = osc_edit_tile.value();

    Entity.AI_check = false;
    if (Entity.timer_AI.update(delta))
    {
      Entity.AI_check = true;
    }

    switch (status)
    {
      case ROCKET_FLY:
      {
        rocket_progress += delta / ((float) Config.CONF.ROCKET_SPEED.value);
        if (rocket_progress >= 1f)
        {
          init_status(WorldStatus.LOAD_LEVEL);
          return;
        }

        float rocket_lerped_x = MathUtils.lerp(rocket_start_x, rocket_start_x + 30, Interpolation.smooth.apply(rocket_progress));
        //rocket_lerped_x -= Res.ROCKET_CAT.sheet[1].getRegionWidth() / 2f;
        float rocket_lerped_y = MathUtils.lerp(rocket_start_y, rocket_start_y + 250, Interpolation.smooth.apply(rocket_progress));
        for (int i = 0; i < 2; i++)
        {
          int sx = (int) (rocket_lerped_x + MathUtils.random(-Config.CONF.ROCKET_SMOKE_OFFSET.value, Config.CONF.ROCKET_SMOKE_OFFSET.value));
          int sy = (int) (rocket_lerped_y + MathUtils.random(-Config.CONF.ROCKET_SMOKE_OFFSET.value, Config.CONF.ROCKET_SMOKE_OFFSET.value) - 10);
          sm_smoke.add_line(0, sx, sy, 0, 0, MathUtils.random(10, 80));
        }

        for (int i = 0; i < sm_smoke.num_lines(); i++)
        {
          if (sm_smoke.get(i, 0) != -1)
          {
            float yoff = Util.INT_TO_FLOAT(sm_smoke.get(i, 4));
            yoff += delta * Config.CONF.ROCKET_SMOKE_FALLSPEED.value;
            sm_smoke.set(i, 4, Util.FLOAT_TO_INT(yoff));

            float anim_time = Util.INT_TO_FLOAT(sm_smoke.get(i, 3));
            float[] ret_val = Anim.SMOKE.update(anim_time, delta);
            sm_smoke.set(i, 3, Util.FLOAT_TO_INT(ret_val[0]));
            if (ret_val[2] == 1)
            {
              // ANIMATION IS OVER
              sm_smoke.clear_line(i);
            }
          }
        }

        float[] ret_val = Anim.ROCKET_FIRE.update(fire_anim_progress, delta);
        fire_anim_progress = ret_val[0];

      }
      break;

      case PAUSE:
      {
        if (Main.DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
          init_status(WorldStatus.PLAY);
        }
      }
      break;

      case EDIT_CHUNKS: // MAP EDITOR
      {
        global_offset_x = -editor_x + Main.SCREEN_WIDTH / 2f + camera_offset_x;
        global_offset_y = -editor_y + Main.SCREEN_HEIGHT / 2f + camera_offset_y;

        mouse_x = Gdx.input.getX();
        mouse_y = Gdx.input.getY();
        mouse_pos.x = mouse_x;
        mouse_pos.y = mouse_y;

        mouse_pos = Main.viewport.unproject(mouse_pos);
        mouse_x = (int) mouse_pos.x;
        mouse_y = (int) mouse_pos.y;

        edit_tilex = click_tilex(mouse_x);
        edit_tiley = click_tiley(mouse_y);

        if (Main.DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.F1))
        {
          save_chunks();
          init_status(WorldStatus.PLAY);
          return;
        }

        int speed = Config.CONF.EDITOR_MOVE_SPEED.value * (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? 3 : 1);
        if (Gdx.input.isKeyPressed(Input.Keys.A))
        {
          editor_x -= speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D))
        {
          editor_x += speed * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W))
        {
          editor_y += speed * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S))
        {
          editor_y -= speed * delta;
        }

        switch (edit_layer)
        {
          case 0:
          { // TILE EDIT MODE
            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && mouse_y > editor_box_height)
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                c.tiles.set(local_tilex, local_tiley, (byte) 0);
              }
            }

            if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE) && mouse_y > editor_box_height)
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                byte local_val = c.tiles.get(local_tilex, local_tiley);
                if (local_val != 0)
                {
                  System.out.println("picked tile from map");
                  edit_tile_ordinal = local_val;
                }
              }
            }

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && mouse_y > editor_box_height)
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                c.tiles.set(local_tilex, local_tiley, (byte) edit_tile_ordinal);
              }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            {
              if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
              {
                edit_tile_ordinal--;
              } else
              {
                edit_tile_ordinal++;
              }
              if (edit_tile_ordinal < 1) edit_tile_ordinal = Tile.values().length - 1;
              if (edit_tile_ordinal >= Tile.values().length)
              {
                edit_tile_ordinal = 1;
              }
            }

          }
          break;
          case 1:
          { // OBJECT EDIT MODE

            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && mouse_y > editor_box_height)
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                c.objects.set(local_tilex, local_tiley, (byte) 0);
              }
            }

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && mouse_y > editor_box_height)
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                c.objects.set(local_tilex, local_tiley, (byte) edit_object_ordinal);
              }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            {
              if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
              {
                edit_object_ordinal--;
              } else
              {
                edit_object_ordinal++;
              }
              if (edit_object_ordinal < 1)
                edit_object_ordinal = Chunk.Chunk_Object.values().length - 1;
              if (edit_object_ordinal >= Chunk.Chunk_Object.values().length)
              {
                edit_object_ordinal = 1;
              }
            }

          }
          break;
          case 2:
          { // DECO EDIT MODE

          }
          break;
          case 3:
          { // COPY PASTE MODE (ONLY TILES RIGHT NOW!)

            if (copy_status == 0)
            {
              if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
              {
                copy_status = 1;
                copy_width = 1;
                copy_height = 1;
                copy_start_tilex = edit_tilex;
                copy_start_tiley = edit_tiley;
                copy_end_tilex = edit_tilex;
                copy_end_tiley = edit_tiley;
              }
            } else if (copy_status == 1)
            {
              if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
              {
                copy_end_tilex = edit_tilex;
                copy_end_tiley = edit_tiley;
                if (copy_end_tilex >= copy_start_tilex && copy_end_tiley >= copy_start_tiley)
                {
                  copy_width = copy_end_tilex - copy_start_tilex + 1;
                  copy_height = copy_end_tiley - copy_start_tiley + 1;
                } else
                {
                  copy_width = 0;
                  copy_height = 0;
                }
              } else
              {
                if (copy_width > 0 && copy_height > 0)
                {
                  copied_tile_data.reset();
                  // COPY THE TILE DATA
                  int offx = 0;
                  int offy = 0;
                  for (int ix = copy_start_tilex; ix <= copy_end_tilex; ix++)
                  {
                    for (int iy = copy_start_tiley; iy <= copy_end_tiley; iy++)
                    {
                      int local_tile = World.get_global_chunk_tile(ix, iy);
                      if (local_tile > 0)
                      {
                        copied_tile_data.set(offx, offy, (byte) local_tile);
                      }
                      offy++;
                    }
                    offy = 0;
                    offx++;
                  }

                  // released button after making box
                  copy_status = 2;
                  copy_end_tilex = -1;
                  copy_end_tiley = -1;
                  copy_start_tilex = -1;
                  copy_start_tiley = -1;

                  // I need those!
                  //copy_width = 0;
                  //copy_height = 0;
                } else
                {
                  copy_status = 0;
                }
              }
            } else if (copy_status == 2)
            {
              if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT))
              {
                // reset
                copy_status = 0;
                copy_width = 0;
                copy_height = 0;
              } else if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
              {
                // paste the data into map

                for (int ix = 0; ix < copy_width + 1; ix++)
                {
                  for (int iy = 0; iy < copy_height + 1; iy++)
                  {
                    int global_tilex = edit_tilex - copy_width + ix;
                    int global_tiley = edit_tiley - copy_height + iy;
                    int val = copied_tile_data.get(ix, iy);
                    if (val != -1)
                    {
                      set_global_chunk_tile(global_tilex, global_tiley, val);
                    }
                  }
                }
                copy_status = 0;
                copy_width = 0;
                copy_height = 0;
              }
            }
          }
          break;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
        {
          edit_layer = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
        {
          edit_layer = 1;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
        {
          edit_layer = 2;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4))
        {
          edit_layer = 3;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C))
        {
          Chunk c = get_chunk(edit_chunkx, edit_chunky);
          if (c == null)
          {
            System.out.println("chunk at " + edit_chunkx + "|" + edit_chunky + " does not exist");

            if (edit_chunkx >= 0 && edit_chunky >= 0)
            {
              Chunk start_chunk = new Chunk(edit_chunkx, edit_chunky);
              chunk_map.put(start_chunk.combined_pos, list_chunks.size);
              list_chunks.add(start_chunk);
              System.out.println("created the chunk");
            }

          } else
          {
            System.out.println("chunk at " + edit_chunkx + "|" + edit_chunky + " does exist with cx +" + c.cx + "|" + c.cy);
          }
        }

        chunk_visibility_check();

      }
      break;

      case LOAD_LEVEL:
      {
        // LOAD
        if (list_chunk_files.isEmpty())
        {
          init_status(WorldStatus.PLAY);
          json = null;
        } else
        {
          FileHandle chunk_file = list_chunk_files.pop();

          if (chunk_file.extension().equals("json") && chunk_file.nameWithoutExtension().length() == 7)
          {
            Chunk loaded_chunk = json.fromJson(Chunk.class, chunk_file);
            if (loaded_chunk != null)
            {
              // Combine the two values into a single int
              int combinedValue = (loaded_chunk.cx << 16) | loaded_chunk.cy;
              chunk_map.put(combinedValue, list_chunks.size);
              list_chunks.add(loaded_chunk);
            }
          }
        }
      }
      break;

      case GAMEOVER:
      {
        wall_progress += delta * (1 / (float) Config.CONF.WALL_SPEED.value / 10f);

        gameover_opacity += delta * (1 / 3f);
        if (gameover_opacity >= 1f) gameover_opacity = 1f;
        gameover_color_back.a = Interpolation.smooth2.apply(gameover_opacity);
        RenderUtil.interp_color(Color.BLACK, Color.SCARLET, gameover_color_text, Interpolation.smooth2.apply(gameover_opacity));

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
        {
          // RELOADING THE LEVEL
          init_status(WorldStatus.LOAD_LEVEL);
          return;
        }
      }
      break;

      case PLAY: // GAME LOOP
      {
        wall_progress += delta * (1 / (float) Config.CONF.WALL_SPEED.value);

        for (int i = 0; i < sm_stars.num_lines(); i++)
        {
          sm_stars.set(i, 3, Star.update_star(sm_stars.get(i, 3), sm_stars.get(i, 0), delta));
        }

        if (wall_progress >= 0.99f)
        {
          System.out.println("game over!");
          init_status(WorldStatus.GAMEOVER);
          return;
        }

        if (Main.DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
          init_status(WorldStatus.PAUSE);
          return;
        }

        if (Main.DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.F1))
        {
          init_status(WorldStatus.EDIT_CHUNKS);
          return;
        }


        for (int i = 0; i < list_entities.size; i++)
        {
          Entity ent = list_entities.get(i);
          ent.update(delta, i);
        }

        for (int i = list_entity_index_remove.size - 1; i >= 0; i--)
        {
          list_entities.removeIndex(list_entity_index_remove.get(i));
        }
        list_entity_index_remove.clear();

        if (list_spawn.size > 0)
        {
          list_entities.addAll(list_spawn);
          list_spawn.clear();
        }

        chunk_visibility_check();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F4))
        {
          Config.load_config(true);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3))
        {
          Config.print_config();
        }

            /*
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
            {
               camera_offset_x += delta * 500;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            {
               camera_offset_x -= delta * 500;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.UP))
            {
               camera_offset_y -= delta * 500;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
            {
               camera_offset_y += delta * 500;
            }
            //if (Gdx.input.isKeyJustPressed(Input.Keys.F10))
            {
               camera_offset_x = 0;
               camera_offset_y = -50;
            }
             */
      }
      break;
    }
  }

  public static void render()
  {
    int debug_off = 10;

    if (Main.DEBUG)
    {
      Text.draw("mode " + status.toString(), 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
      debug_off += 10;

      Text.draw("" + Gdx.graphics.getFramesPerSecond(), Main.SCREEN_WIDTH - 20, Main.SCREEN_HEIGHT - 16, Color.OLIVE, 2f);
    }

    switch (status)
    {
      case EDIT_CHUNKS: // RENDER
      {
        if (edit_tilex < 0 || edit_tiley < 0)
        {
          edit_chunkx = -1;
          edit_chunky = -1;
        } else
        {
          edit_chunkx = (int) (edit_tilex / ((float) Chunk.CHUNK_SIZE));
          edit_chunky = (int) (edit_tiley / ((float) Chunk.CHUNK_SIZE));
        }

        if (Main.RENDER_DEBUG_INFO)
        {
          Text.draw("editor posx " + editor_x, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;
          Text.draw("editor posy " + editor_y, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("total chunks " + list_chunks.size, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("visible chunks " + visible_chunks_set.size, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("mouse_x " + mouse_x, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;
          Text.draw("mouse_y " + mouse_y, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("edit tilex " + edit_tilex, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;
          Text.draw("edit tiley " + edit_tiley, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("edit chunkx " + edit_chunkx, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;
          Text.draw("edit chunky " + edit_chunky, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          if (delete_chunk_index != -1)
          {
            Text.draw("deleting chunk! " + delete_chunk_index + " in " + delete_chunk_counter, 10, 10, Color.SCARLET, 2f);
          }
        }

        IntSet.IntSetIterator iterator = visible_chunks_set.iterator();
        while (iterator.hasNext)
        {
          int value = iterator.next();
          Chunk c = list_chunks.get(value);
          c.render();
        }

        RenderUtil.render_box(0, 0, Main.SCREEN_WIDTH, editor_box_height, Color.DARK_GRAY);

        Text.draw("level: ", Main.SCREEN_WIDTH - 80, 50, Color.LIGHT_GRAY);
        Text.cdraw(level_name, Main.SCREEN_WIDTH - 30, 50, Color.GOLD);

        Text.draw("-EDITOR-", 10, 50, Color.LIGHT_GRAY);
        for (int i = 0; i < 4; i++)
        {
          Text.draw("[" + (i + 1) + "] " + edit_layer_names[i], 10, 39 - (i * 11), i == edit_layer ? Color.GOLD : Color.BROWN);
        }

        switch (edit_layer)
        {
          case 3: // WALL MODE
          {
            if (copy_status == 0)
            {
              Text.draw("waiting for copy", 100, 30, Color.FIREBRICK);

              float px = World.global_offset_x + edit_tilex * Chunk.TILE_SIZE;
              float py = World.global_offset_y + edit_tiley * Chunk.TILE_SIZE;
              RenderUtil.render_outer_box((int) px - 1, (int) py - 1, Chunk.TILE_SIZE + 2, Chunk.TILE_SIZE + 2, RenderUtil.color_edit_tile_copy_box);
            } else if (copy_status == 1)
            {
              Text.draw("copying ongoing", 100, 30, Color.FIREBRICK);
              if (copy_width > 0 && copy_height > 0)
              {
                float px = World.global_offset_x + copy_start_tilex * Chunk.TILE_SIZE;
                float py = World.global_offset_y + copy_start_tiley * Chunk.TILE_SIZE;
                RenderUtil.render_outer_box((int) px - 1, (int) py - 1, copy_width * Chunk.TILE_SIZE + 2, copy_height * Chunk.TILE_SIZE + 2, RenderUtil.color_edit_tile_copy_box);
              }
            } else if (copy_status == 2)
            {
              Text.draw("copied " + copy_width + "|" + copy_height, 100, 30, Color.FIREBRICK);
              if (copy_width > 0 && copy_height > 0)
              {
                for (int ix = 0; ix <= copy_width; ix++)
                {
                  for (int iy = 0; iy <= copy_height; iy++)
                  {
                    Tile tile = Tile.safe_ord(copied_tile_data.get(ix, iy));
                    float px = World.global_offset_x + (edit_tilex - copy_width) * Chunk.TILE_SIZE + ix * Chunk.TILE_SIZE;
                    float py = World.global_offset_y + (edit_tiley - copy_height) * Chunk.TILE_SIZE + iy * Chunk.TILE_SIZE;
                    Main.batch.setColor(1f, 1f, 1f, 0.5f);
                    Main.batch.draw(Res.PLATFORM.sheet[tile.ordinal()], px, py);
                  }
                }
              }
            }
          }
          break;

          case 0: // TILE MODE
          {
            // technically is drawn over the edit ui but does not matter
            float px = World.global_offset_x + edit_tilex * Chunk.TILE_SIZE;
            float py = World.global_offset_y + edit_tiley * Chunk.TILE_SIZE;
            RenderUtil.render_box(px - 1, py - 1, Chunk.TILE_SIZE + 2, Chunk.TILE_SIZE + 2, RenderUtil.color_edit_tile);

            int offx_tile = Config.CONF.EDITOR_TILE_OFFX.value;
            int offy_tile = Config.CONF.EDITOR_TILE_OFFY.value;
            for (int i = 1; i < Tile.values().length; i++)
            {
              if (i == edit_tile_ordinal)
              {
                RenderUtil.render_box(offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1) - 1, offy_tile - 1, 18, 18, Color.GOLD);
              }
              Main.batch.draw(Res.PLATFORM.sheet[i], offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1), offy_tile);
            }
            Tile tile = Tile.safe_ord(edit_tile_ordinal);
            Text.draw("current tile: ", 100, 50, Color.LIGHT_GRAY);
            Text.draw(tile.name(), 160, 50, Color.GOLD);


          }
          break;
          case 1: // OBJECT MODE
          {
            float px = World.global_offset_x + edit_tilex * Chunk.TILE_SIZE;
            float py = World.global_offset_y + edit_tiley * Chunk.TILE_SIZE;

            //RenderUtil.render_box(px - 1, py - 1, Chunk.TILE_SIZE + 2, Chunk.TILE_SIZE + 2, RenderUtil.color_edit_tile);
            Chunk.Chunk_Object object = Chunk.Chunk_Object.safe_ord(edit_object_ordinal);

            Main.batch.setColor(1f, 1f, 1f, 0.5f);
            Main.batch.draw(object.get_preview(), px - object.get_preview().getRegionWidth() / 2f, py);
            Main.batch.setColor(Color.WHITE);

            int offx_tile = Config.CONF.EDITOR_TILE_OFFX.value;
            int offy_tile = Config.CONF.EDITOR_TILE_OFFY.value;
            for (int i = 1; i < Chunk.Chunk_Object.values().length; i++)
            {
              if (i == edit_object_ordinal)
              {
                RenderUtil.render_box(offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1) - 1, offy_tile - 1, 18, 18, Color.GOLD);
              }
              Main.batch.draw(Chunk.Chunk_Object.values()[i].get_preview(), offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1), offy_tile, 16, 16);
            }
            Text.draw("current object: ", 100, 50, Color.LIGHT_GRAY);
            Text.draw(object.name(), 160, 50, Color.GOLD);

          }
          break;
          case 2: // DECO MODE
          {

          }
          break;
        }

      }
      break;
      case PLAY: // RENDER
      case GAMEOVER:
      case PAUSE:
      case ROCKET_FLY:
      {
        Main.batch.draw(Res.BACKGROUND.sheet[1], 0, 0);
        Main.batch.draw(Res.BACKGROUND.sheet[0], 0, 0);

        for (int i = 0; i < sm_stars.num_lines(); i++)
        {
          if (sm_stars.get(i, 0) != -1)
          {
            TextureRegion reg = Star.get_region(sm_stars.get(i, 0), sm_stars.get(i, 3));
            float parallax = player.posy / (sm_stars.get(i, 0) + 10);
            Main.batch.draw(reg, sm_stars.get(i, 1), sm_stars.get(i, 2) - (parallax));
          }
        }

        // PLANET RENDERING

        int planet_darkness = MathUtils.clamp(Config.CONF.PLANET_DARKNESS.value, 0, 100);
        float planet_color = planet_darkness / 100f;
        Main.batch.setColor(planet_color, planet_color, planet_color, 1f);
        float parallax = player.posy / (Config.CONF.PLANET_PARALLAX.value);
        Main.batch.draw(Res.PLANETS.sheet[0], Config.CONF.PLANET_BLUE_X.value, Config.CONF.PLANET_BLUE_Y.value - (parallax));

        Main.batch.draw(Res.PLANETS.sheet[1], Config.CONF.PLANET_RED_X.value, Config.CONF.PLANET_RED_Y.value - (parallax));


        if (Main.RENDER_DEBUG_INFO)
        {
          Text.draw("posx " + player.posx, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("posy " + player.posy, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("vy " + player.vy, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("falling " + player.falling, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("wall progress " + wall_progress, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("gameover progress " + gameover_opacity, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("num. entities " + list_entities.size, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;

          Text.draw("cat wait " + Entity.cat_wait, 2, Main.SCREEN_HEIGHT - debug_off, debug_text_color);
          debug_off += 10;
        }

        global_offset_x = -player.posx + Main.SCREEN_WIDTH / 2f + camera_offset_x;
        global_offset_y = -player.posy + Main.SCREEN_HEIGHT / 2f + camera_offset_y;

        IntSet.IntSetIterator iterator = visible_chunks_set.iterator();
        while (iterator.hasNext)
        {
          int value = iterator.next();
          Chunk c = list_chunks.get(value);
          c.render();
        }

        for (int i = 0; i < list_entities.size; i++)
        {
          Entity ent = list_entities.get(i);
          ent.render();
        }

        float wall_x = World.global_offset_x + (Chunk.CHUNK_SIZE / 2f * Chunk.TILE_SIZE * wall_progress);
        float wall_y = World.global_offset_y + 0;

        for (int i = 0; i < Config.CONF.WALL_VERTICAL_NUMBER.value; i++)
        {
          for (int iy = 0; iy < 4; iy++)
          {
            for (int ix = 0; ix < 7; ix++)
            {
              Main.batch.draw(Res.WALL_BLOCK.region, wall_x - 32 * ix - 32 - 16, wall_y + (i * Res.WALL_EDGE.region.getRegionHeight()) + iy * 32);
            }
          }

          Main.batch.draw(Res.WALL_EDGE.region, wall_x - Res.WALL_EDGE.region.getRegionWidth(), wall_y + (i * Res.WALL_EDGE.region.getRegionHeight()));
        }


        wall_x = World.global_offset_x + (Chunk.CHUNK_SIZE * Chunk.TILE_SIZE) - (Chunk.CHUNK_SIZE / 2f * Chunk.TILE_SIZE * wall_progress);

        //RenderUtil.render_box(wall_x, wall_y, (int) wall_width, Chunk.CHUNK_SIZE * Chunk.TILE_SIZE, Color.DARK_GRAY);

        for (int i = 0; i < Config.CONF.WALL_VERTICAL_NUMBER.value; i++)
        {
          for (int iy = 0; iy < 4; iy++)
          {
            for (int ix = 0; ix < 7; ix++)
            {
              Main.batch.draw(Res.WALL_BLOCK.region, wall_x + 32 * ix + 16, wall_y + (i * Res.WALL_EDGE.region.getRegionHeight()) + iy * 32);
            }
          }

          Main.batch.draw(Res.WALL_EDGE.region, wall_x, wall_y + (i * Res.WALL_EDGE.region.getRegionHeight()));
        }

        // OLD UI ELEMENTS (HEALTH, BOXCOUNT)
        //for (int i = 1; i <= 3; i++)
        //{
        //   Main.batch.setColor(i <= Entity.wutz_life ? Color.WHITE : Color.DARK_GRAY);
        //   Main.batch.draw(Res.HEART.region, i * 18 - 16, Main.SCREEN_HEIGHT - 20, 16, 16);
        //   Main.batch.setColor(Color.WHITE);
        //}

        //if (Entity.num_collected_box > 0)
        //{
        //   RenderUtil.render_box(Main.SCREEN_WIDTH - 75, Main.SCREEN_HEIGHT - 30, 7//0, 26, RenderUtil.collect_box_shadow);
        //   Main.batch.draw(Res.BOX.region, Main.SCREEN_WIDTH - 70, Main.SCREEN_HEIGHT - 28);
        //   Text.cdraw("" + Entity.num_collected_box + "/" + World.total_collect_box, Main.SCREEN_WIDTH - 27, Main.SCREEN_HEIGHT - 24, Color.GOLD, 2f);
        //}

        if (Entity.time_message > 0f)
        {
          int px = (int) World.global_offset_x + Entity.message_posx;
          int py = (int) World.global_offset_y + Entity.message_posy;
          Text.cdraw(Entity.message, px, py + 45, Color.GOLD, 1.5f);
        }

        if (status == WorldStatus.GAMEOVER)
        {
          RenderUtil.render_box(0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT, gameover_color_back);

          Text.cdraw("GAME OVER", Main.SCREEN_WIDTH / 2, Main.SCREEN_HEIGHT / 2, gameover_color_text, 2f);
          Text.cdraw("press [space] to restart", Main.SCREEN_WIDTH / 2, Main.SCREEN_HEIGHT / 2 - 20, gameover_color_text);
        } else if (status == WorldStatus.PAUSE)
        {
          RenderUtil.render_box(0, 0, Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT, gameover_pause_back);
          Text.cdraw("GAME PAUSED", Main.SCREEN_WIDTH / 2, Main.SCREEN_HEIGHT / 2, Color.GOLD, 2f);
          Text.cdraw("press [escape] to play", Main.SCREEN_WIDTH / 2, Main.SCREEN_HEIGHT / 2 - 20, Color.GOLD);
        } else if (status == WorldStatus.ROCKET_FLY)
        {
          for (int i = 0; i < sm_smoke.num_lines(); i++)
          {
            if (sm_smoke.get(i, 0) != -1)
            {
              float anim_time = Util.INT_TO_FLOAT(sm_smoke.get(i, 3));
              int frame = Anim.SMOKE.get_frame(anim_time);
              TextureRegion reg = Res.get_frame(anim_time, Anim.SMOKE);

              float yoff = Util.INT_TO_FLOAT(sm_smoke.get(i, 4));
              int sx = sm_smoke.get(i, 1);
              int sy = sm_smoke.get(i, 2);

              int blackness = sm_smoke.get(i, 5);
              float bv = blackness / 100f;
              Main.batch.setColor(bv, bv, bv, 0.5f);
              Main.batch.draw(reg, sx - Res.SMOKE.sheet[0].getRegionWidth() / 2f, sy - yoff);
              Main.batch.setColor(Color.WHITE);
            }
          }

          float rocket_lerped_x = MathUtils.lerp(rocket_start_x, rocket_start_x + 30, Interpolation.smooth.apply(rocket_progress));
          float rocket_lerped_y = MathUtils.lerp(rocket_start_y, rocket_start_y + 250, Interpolation.smooth.apply(rocket_progress));

          Main.batch.draw(Res.ROCKET_FIRE.sheet[Anim.ROCKET_FIRE.get_frame(fire_anim_progress)], rocket_lerped_x - Res.ROCKET_FIRE.sheet[0].getRegionWidth() / 2f, rocket_lerped_y - Config.CONF.ROCKET_FIRE_YOFF.value);
          Main.batch.draw(Res.ROCKET_CAT.sheet[1], rocket_lerped_x - Res.ROCKET_CAT.sheet[1].getRegionWidth() / 2f, rocket_lerped_y);
        }
      }
      break;
      case LOAD_LEVEL:
      {
        Text.cdraw("loading chunks " + list_chunk_files.size, Main.SCREEN_WIDTH / 2, Main.SCREEN_HEIGHT / 2, Color.WHITE, 2f);
      }
      break;
    }
  }

  private static void chunk_visibility_check()
  {
    if (list_chunks.size > 0)
    {
      // double check since we can remove chunks
      if (chunk_check_index >= list_chunks.size) chunk_check_index = 0;

      // rendering the chunk ins visible list
      Chunk check_chunk = list_chunks.get(chunk_check_index);
      if (check_chunk.should_be_rendered(status == WorldStatus.PLAY ? player.posx : editor_x, status == WorldStatus.PLAY ? player.posy : editor_y))
      {
        if (!visible_chunks_set.contains(chunk_check_index)) check_chunk.init();
        visible_chunks_set.add(chunk_check_index);
      } else
      {
        if (visible_chunks_set.contains(chunk_check_index)) check_chunk.unload_entities();
        visible_chunks_set.remove(chunk_check_index);
      }
      chunk_check_index++;
      if (chunk_check_index >= list_chunks.size) chunk_check_index = 0;
    }
  }

  public static void save_chunks()
  {
    if (!Main.DEBUG) return;
    Json json = new Json();
    for (int i = 0; i < list_chunks.size; i++)
    {
      Chunk c = list_chunks.get(i);

      if (c.cx < 0 || c.cy < 0)
      {
        System.out.println("not saving negative coordinate chunks");
        continue;
      }
      FileHandle chunk_file = Gdx.files.local("levels/" + level_name + "/" + Chunk.chunk_coordinate_to_savename(c.cx) + "_" + Chunk.chunk_coordinate_to_savename(c.cy) + ".json");
      chunk_file.writeString(json.toJson(c), false);
    }
  }

  public static int get_global_chunk_tile(int tilex, int tiley)
  {
    // convert from global pos to local chunk pos and get the data
    // used by copy paste code

    int chunkx = (int) (tilex / ((float) Chunk.CHUNK_SIZE));
    int chunky = (int) (tiley / ((float) Chunk.CHUNK_SIZE));
    Chunk c = get_chunk(chunkx, chunky);
    if (c != null)
    {
      int local_tilex = tilex % Chunk.CHUNK_SIZE;
      int local_tiley = tiley % Chunk.CHUNK_SIZE;
      return c.tiles.get(local_tilex, local_tiley);
    }
    return -1;
  }

  public static void set_global_chunk_tile(int tilex, int tiley, int value)
  {
    // convert from global pos to local chunk pos and get the data
    // used by copy paste code

    int chunkx = (int) (tilex / ((float) Chunk.CHUNK_SIZE));
    int chunky = (int) (tiley / ((float) Chunk.CHUNK_SIZE));
    Chunk c = get_chunk(chunkx, chunky);
    if (c != null)
    {
      int local_tilex = tilex % Chunk.CHUNK_SIZE;
      int local_tiley = tiley % Chunk.CHUNK_SIZE;
      c.tiles.set(local_tilex, local_tiley, (byte) value);
    }
  }

  public static float wall_position(boolean right)
  {
    if (right)
    {
      return (Chunk.CHUNK_SIZE * Chunk.TILE_SIZE) - wall_progress * (Chunk.CHUNK_SIZE / 2f * Chunk.TILE_SIZE);
    } else
    {
      return wall_progress * (Chunk.CHUNK_SIZE / 2f * Chunk.TILE_SIZE);
    }
  }

  public enum WorldStatus
  {
    LOAD_LEVEL,
    PLAY,
    GAMEOVER,
    PAUSE,

    EDIT_CHUNKS,

    ROCKET_FLY,
    ;
  }
}