package me.schmausio.limspace;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Chunk implements Json.Serializable
{
  public int cx, cy; // CHUNK POSITION IN WORLD
  public int combined_pos;
  final static int CHUNK_SIZE = 64; // in tiles
  final static int TILE_SIZE = 16; // pixels

  public Flatbyte tiles = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0);
  public Flatbyte walls = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0);

  public Flatbyte decoration = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0);

  public Flatbyte objects = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) -1, (byte) -1);

  public Chunk()
  {
    // json constructor
  }

  public void init()
  {
    // this is called whenever the chunk becomes visible to the player, then the
    // objects loaded from data are spawned as updating entities

    // spawn the objects

    for (int ix = 0; ix < CHUNK_SIZE; ix++)
    {
      for (int iy = 0; iy < CHUNK_SIZE; iy++)
      {
        Chunk_Object co = Chunk_Object.safe_ord(objects.get(ix, iy));

        float px = ix * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
        float py = iy * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;

        switch (co)
        {
          case ROCKET:
          {
            Entity entity = new Entity(px, py, Entity.EntityType.ROCKET);
            entity.origin_chunk = combined_pos;
            World.list_spawn.add(entity);
          }

          break;
        }
      }
    }
  }

  //public void found_collect_box()
  //{
  //  // player found the box of this chunk, prevent it from respawning, by removing it from the loaded list
  //  for (int i = 0; i < loaded_objects.size; i++)
  //  {
  //    if (loaded_objects.get(i) == Chunk_Object.COLLECT_BOX)
  //    {
  //      loaded_objects.removeIndex(i);
  //      loaded_objects_tilex.removeIndex(i);
  //      loaded_objects_tiley.removeIndex(i);
  //      break;
  //    }
  //  }
  //}

  public void unload_entities()
  {
    for (int i = 0; i < World.list_entities.size; i++)
    {
      Entity ent = World.list_entities.get(i);
      if (ent.origin_chunk == combined_pos && !ent.dead)
      {
        // entity was spawned from this chunk
        // if still on the chunk, remove
        if (World.entity_pos_to_chunk_x(ent.posx) == cx && World.entity_pos_to_chunk_y(ent.posy) == cy)
        {
          ent.dead = true;
          World.list_entity_index_remove.add(i);
        }
      }
    }
  }

  public Chunk(int chunk_x, int chunk_y)
  {
    this.cx = chunk_x;
    this.cy = chunk_y;
    combined_pos = (cx << 16) | cy;
  }

  public void render()
  {
    if (World.status == World.WorldStatus.EDIT_CHUNKS)
    {
      float backx = World.global_offset_x + cx * CHUNK_SIZE * TILE_SIZE;
      float backy = World.global_offset_y + cy * CHUNK_SIZE * TILE_SIZE;
      Main.batch.setColor(1f, 0.2f, 0.2f, 0.4f);
      Main.batch.draw(Res.pixel, backx, backy, CHUNK_SIZE * TILE_SIZE, CHUNK_SIZE * TILE_SIZE);
      Main.batch.setColor(Color.WHITE);
    }

    for (int ix = 0; ix < CHUNK_SIZE; ix++)
    {
      for (int iy = 0; iy < CHUNK_SIZE; iy++)
      {
        // using global offset
        float px = World.global_offset_x + ix * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
        float py = World.global_offset_y + iy * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;

        // TODO: 01.10.23 at some points the walls are other tiles than the floor
        Main.batch.setColor(0.4f,0.4f,0.4f, 1f);
        Main.batch.draw(Res.BLOCK.sheet[walls.get(ix, iy)], px, py);

        Main.batch.setColor(Color.WHITE);
        Main.batch.draw(Res.BLOCK.sheet[tiles.get(ix, iy)], px, py);

        if (World.status == World.WorldStatus.EDIT_CHUNKS)
        {
          int global_tilex = CHUNK_SIZE * cx + ix;
          int global_tiley = CHUNK_SIZE * cy + iy;
          if (World.edit_tilex == global_tilex && World.edit_tiley == global_tiley)
          {
            RenderUtil.render_box(px - 1, py - 1, TILE_SIZE + 2, TILE_SIZE + 2, RenderUtil.color_edit_tile);
          }
        }
      }
    }

    // TODO: 01.10.23 render decoration
    //for (int ix = 0; ix < CHUNK_SIZE; ix++)
    //{
    //   for (int iy = 0; iy < CHUNK_SIZE; iy++)
    //   {
    //      float px = World.global_offset_x + ix * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
    //      float py = World.global_offset_y + iy * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;
    //      if (decoration.get(ix, iy) != 0)
    //      {
    //         Main.batch.setColor(Color.WHITE);
    //         Main.batch.draw(Res.DECORATION.sheet[decoration.get(ix, iy)], px + TILE_SIZE / 2f -// Res.DECORATION.sheet_width / 2f, py);
    //      }
    //   }
    //}

  }

  public boolean should_be_rendered(float player_x, float player_y)
  {
    int chunk_midx = cx * CHUNK_SIZE * TILE_SIZE + (CHUNK_SIZE * TILE_SIZE / 2);
    int chunk_midy = cy * CHUNK_SIZE * TILE_SIZE + (CHUNK_SIZE * TILE_SIZE / 2);
    return Util.euclid_norm((int) player_x, (int) player_y, chunk_midx, chunk_midy) < TILE_SIZE * CHUNK_SIZE;
  }

  //public static Chunk load_from_png(FileHandle chunk_file)
  //{
  //  String[] coords = chunk_file.nameWithoutExtension().split("_");
  //  int cx = Integer.parseInt(coords[0]);
  //  int cy = Integer.parseInt(coords[1]);

  //  System.out.println("loading chunk [" + cx + "|" + cy + "]");
  //  Chunk chunk = new Chunk(cx, cy);

  //  Pixmap pm = new Pixmap(chunk_file);
  //  Color local = new Color();
  //  if (pm.getWidth() != Chunk.CHUNK_SIZE || pm.getHeight() != Chunk.CHUNK_SIZE)
  //  {
  //    System.out.println("could not use file " + chunk_file.name() + " due to wrong dimensions! [" + //pm.getWidth() + "|" + pm.getHeight() + "]");
  //    chunk = null;
  //  } else
  //  {
  //    for (int ix = 0; ix < Chunk.CHUNK_SIZE; ix++)
  //    {
  //      for (int iy = 0; iy < Chunk.CHUNK_SIZE; iy++)
  //      {
  //        Color.rgba8888ToColor(local, pm.getPixel(ix, CHUNK_SIZE - 1 - iy));
  //        Tile local_tile = Tile.from_R(MathUtils.floor(local.r * 255f));
  //        Chunk_Object object = Chunk_Object.from_G(MathUtils.floor(local.g * 255f));
  //        if (object != Chunk_Object.NONE)
  //        {
  //          chunk.loaded_objects.add(object);
  //          chunk.loaded_objects_tilex.add(ix);
  //          chunk.loaded_objects_tiley.add(iy);
  //          //if (object == Chunk_Object.COLLECT_BOX) World.total_collect_box++;
  //        }
  //        chunk.content.set(ix, iy, (byte) local_tile.ordinal());

  //        //Deco local_deco = Deco.from_B(MathUtils.floor(local.b * 255f));
  //        //chunk.decoration.set(ix, iy, (byte) local_deco.ordinal());
  //      }
  //    }
  //  }
  //  pm.dispose();
  //  return chunk;
  //}

  @Override
  public void write(Json json)
  {
    json.writeValue("tiles", tiles);
    json.writeValue("walls", walls);
    json.writeValue("objects", objects);

    json.writeValue("cx", cx);
    json.writeValue("cy", cy);
  }

  @Override
  public void read(Json json, JsonValue jsonData)
  {
    tiles = json.readValue("tiles", Flatbyte.class, new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0), jsonData);
    objects = json.readValue("objects", Flatbyte.class, new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 1, (byte) -1), jsonData);
    walls = json.readValue("walls", Flatbyte.class, new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0), jsonData);

    cx = json.readValue("cx", Integer.class, 0, jsonData);
    cy = json.readValue("cy", Integer.class, 0, jsonData);
  }

  public static String chunk_coordinate_to_savename(int cxy)
  {
    String r = "";
    if (cxy < 10)
    {
      r = "00" + cxy;
    } else if (cxy < 100)
    {
      r = "0" + cxy;
    } else
    {
      r = "" + cxy;
    }
    return r;
  }

  public enum Chunk_Object
  {
    // objects encoded in G value of a pixel in the chunk image
    NONE,
    ROCKET,
    ;

    public static Chunk_Object safe_ord(int ordinal)
    {
      if (ordinal < 0 || ordinal >= values().length)
      {
        return NONE;
      } else
      {
        return values()[ordinal];
      }
    }
  }
}