package me.schmausio.limspace;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
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

  static int delete_chunk_index = -1;
  static int delete_chunk_counter = 0;

  // used to load from sub directory with name of the level
  public static String level_name = "test";

  public static float global_offset_x, global_offset_y;
  public static float camera_offset_x, camera_offset_y = -50;

  public static float editor_x, editor_y;

  public static int edit_tile_ordinal = 1;
  public static int edit_wall_ordinal = 1;

  // TODO: 01.10.23 implement later when decoration is in the game
  public static int edit_layer = 0;
  // 0 -> tiles
  // 1 -> objects
  // 2 -> decoration
  // 3 -> back walls

  public static String[] edit_layer_names = new String[4];

  static
  {
    edit_layer_names[0] = "TILES";
    edit_layer_names[1] = "OBJECTS";
    edit_layer_names[2] = "DECO";
    edit_layer_names[3] = "WALLS";
  }

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

  static
  {
    list_entities.add(player);
  }

  static final int end_x = 2630, end_y = 4880;

  public static float wall_progress = 0f;
  public static float wall_width = 300;


  // GAMEOVER DATA:

  public static float gameover_opacity = 0f;

  public static Color gameover_color_back = Color.BLACK.cpy();
  public static Color gameover_color_text = Color.BLACK.cpy();

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

        // this might be entered after finished another level so I need to clean up
        list_chunks.clear();
        chunk_map.clear();
        visible_chunks_set.clear();

        json = new Json();
        // populate the list of files that should be loaded
        FileHandle chunk_dir = Gdx.files.local("levels/" + level_name + "/");
        if (chunk_dir.exists())
        {
          list_chunk_files.addAll(chunk_dir.list());
        } else
        {
          chunk_dir.mkdirs();
        }
      }
      break;
      case EDIT_CHUNKS:
      {
        System.out.println("entering chunk edit mode");

        if (list_chunks.size == 0)
        {
          Chunk start_chunk = new Chunk(0, 0);
          chunk_map.put(start_chunk.combined_pos, list_chunks.size);
          list_chunks.add(start_chunk);
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

    Entity.AI_check = false;
    if (Entity.timer_AI.update(delta))
    {
      Entity.AI_check = true;
    }

    switch (status)
    {
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
            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
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

            if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE))
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

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
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

          }
          break;
          case 2:
          { // DECO EDIT MODE

          }
          break;
          case 3:
          { // WALLS EDIT MODE

            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                c.walls.set(local_tilex, local_tiley, (byte) 0);
              }
            }

            if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE))
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                byte local_val = c.walls.get(local_tilex, local_tiley);
                if (local_val != 0)
                {
                  System.out.println("picked tile from map");
                  edit_wall_ordinal = local_val;
                }
              }
            }

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            {
              Chunk c = get_chunk(edit_chunkx, edit_chunky);
              if (c != null)
              {
                int local_tilex = edit_tilex % Chunk.CHUNK_SIZE;
                int local_tiley = edit_tiley % Chunk.CHUNK_SIZE;
                //System.out.println("removing local tile " + local_tilex + "|" + local_tiley);
                c.walls.set(local_tilex, local_tiley, (byte) edit_wall_ordinal);
              }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            {
              if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
              {
                edit_wall_ordinal--;
              } else
              {
                edit_wall_ordinal++;
              }
              if (edit_wall_ordinal < 1) edit_wall_ordinal = Tile.values().length - 1;
              if (edit_wall_ordinal >= Tile.values().length)
              {
                edit_wall_ordinal = 1;
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

        if (wall_progress >= 0.99f)
        {
          System.out.println("game over!");
          init_status(WorldStatus.GAMEOVER);
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
      Text.draw("mode " + status.toString(), 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
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

        if (Main.DEBUG)
        {
          Text.draw("editor posx " + editor_x, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;
          Text.draw("editor posy " + editor_y, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("total chunks " + list_chunks.size, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("visible chunks " + visible_chunks_set.size, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("mouse_x " + mouse_x, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;
          Text.draw("mouse_y " + mouse_y, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("edit tilex " + edit_tilex, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;
          Text.draw("edit tiley " + edit_tiley, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("edit chunkx " + edit_chunkx, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;
          Text.draw("edit chunky " + edit_chunky, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
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

        RenderUtil.render_box(0, 0, Main.SCREEN_WIDTH, 60, Color.DARK_GRAY);

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
            int offx_tile = Config.CONF.EDITOR_TILE_OFFX.value;
            int offy_tile = Config.CONF.EDITOR_TILE_OFFY.value;
            for (int i = 1; i < Tile.values().length; i++)
            {
              if (i == edit_wall_ordinal)
              {
                RenderUtil.render_box(offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1) - 1, offy_tile - 1, 18, 18, Color.GOLD);
              }
              Main.batch.draw(Res.BLOCK.sheet[i], offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1), offy_tile);
            }
          }
          break;

          case 0: // TILE MODE
          {
            int offx_tile = Config.CONF.EDITOR_TILE_OFFX.value;
            int offy_tile = Config.CONF.EDITOR_TILE_OFFY.value;
            for (int i = 1; i < Tile.values().length; i++)
            {
              if (i == edit_tile_ordinal)
              {
                RenderUtil.render_box(offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1) - 1, offy_tile - 1, 18, 18, Color.GOLD);
              }
              Main.batch.draw(Res.BLOCK.sheet[i], offx_tile + Config.CONF.EDITOR_TILE_SPACING.value * (i - 1), offy_tile);
            }
            Tile tile = Tile.safe_ord(edit_tile_ordinal);
            Text.draw("current tile: ", 100, 50, Color.LIGHT_GRAY);
            Text.draw(tile.name(), 160, 50, Color.GOLD);

          }
          break;
          case 1: // OBJECT MODE
          {

          }
          break;
          case 2: // DECO MODE
          {

          }
          break;
        }

      }
      break;
      case PLAY:
      case GAMEOVER:
      {
        if (Main.DEBUG)
        {
          Text.draw("posx " + player.posx, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("posy " + player.posy, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("vy " + player.vy, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("falling " + player.falling, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("wall progress " + wall_progress, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
          debug_off += 10;

          Text.draw("gameover progress " + gameover_opacity, 2, Main.SCREEN_HEIGHT - debug_off, Color.NAVY);
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
        RenderUtil.render_box(wall_x - wall_width, wall_y, (int) wall_width, Chunk.CHUNK_SIZE * Chunk.TILE_SIZE, Color.DARK_GRAY);

        wall_x = World.global_offset_x + (Chunk.CHUNK_SIZE * Chunk.TILE_SIZE) - (Chunk.CHUNK_SIZE / 2f * Chunk.TILE_SIZE * wall_progress);
        RenderUtil.render_box(wall_x, wall_y, (int) wall_width, Chunk.CHUNK_SIZE * Chunk.TILE_SIZE, Color.DARK_GRAY);

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

    ;
  }
}