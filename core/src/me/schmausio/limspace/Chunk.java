package me.schmausio.limspace;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

public class Chunk
{
   public final int cx, cy; // CHUNK POSITION IN WORLD
   public final int combined_pos;
   final static int CHUNK_SIZE = 64; // in tiles
   final static int TILE_SIZE = 16; // pixels

   boolean underground = false;

   public Flatbyte content = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0);

   public Flatbyte decoration = new Flatbyte(CHUNK_SIZE, CHUNK_SIZE, (byte) 0, (byte) 0);

   // is loaded once from
   public Array<Chunk_Object> loaded_objects = new Array<>();
   public IntArray loaded_objects_tilex = new IntArray();
   public IntArray loaded_objects_tiley = new IntArray();

   public void init()
   {
      // spawn the objects
      for (int i = 0; i < loaded_objects.size; i++)
      {
         float px = loaded_objects_tilex.get(i) * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
         float py = loaded_objects_tiley.get(i) * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;
         switch (loaded_objects.get(i))
         {
            case ENEMY_FLOWER:
            {
               Entity entity = new Entity(px, py, Entity.EntityType.ENEMY_FLOWER);
               entity.origin_chunk = combined_pos;
               World.list_spawn.add(entity);
            }
            break;
            case POSTBOX:
            {
               Entity entity = new Entity(px, py, Entity.EntityType.POSTBOX);
               entity.origin_chunk = (cx << 16) | cy;
               World.list_spawn.add(entity);
            }
            break;
            case COLLECT_BOX:
            {
               Entity entity = new Entity(px, py, Entity.EntityType.COLLECT_BOX);
               entity.origin_chunk = (cx << 16) | cy;
               World.list_spawn.add(entity);
            }
            break;
            case MUSHROOM:
            {
               Entity entity = new Entity(px, py, Entity.EntityType.ENEMY_MUSHROOM);
               entity.origin_chunk = combined_pos;
               World.list_spawn.add(entity);
            }
            break;
         }
      }
   }

   public void found_collect_box()
   {
      // player found the box of this chunk, prevent it from respawning, by removing it from the loaded list
      for (int i = 0; i < loaded_objects.size; i++)
      {
         if (loaded_objects.get(i) == Chunk_Object.COLLECT_BOX)
         {
            loaded_objects.removeIndex(i);
            loaded_objects_tilex.removeIndex(i);
            loaded_objects_tiley.removeIndex(i);
            break;
         }
      }
   }

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

      for (int ix = 0; ix < CHUNK_SIZE; ix++)
      {
         for (int iy = 0; iy < CHUNK_SIZE; iy++)
         {
            content.set(ix, iy, (byte) MathUtils.random(1, 5));
         }
      }
   }

   public void render()
   {
      for (int ix = 0; ix < CHUNK_SIZE; ix++)
      {
         for (int iy = 0; iy < CHUNK_SIZE; iy++)
         {
            // using global offset
            float px = World.global_offset_x + ix * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
            float py = World.global_offset_y + iy * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;

            if (underground)
            {
               Main.batch.setColor(Color.DARK_GRAY);
               Main.batch.draw(Res.BLOCK1.sheet[1], px, py);
               Main.batch.setColor(Color.WHITE);
            }

            Main.batch.draw(Res.BLOCK1.sheet[content.get(ix, iy)], px, py);
         }
      }

      for (int ix = 0; ix < CHUNK_SIZE; ix++)
      {
         for (int iy = 0; iy < CHUNK_SIZE; iy++)
         {
            float px = World.global_offset_x + ix * TILE_SIZE + cx * CHUNK_SIZE * TILE_SIZE;
            float py = World.global_offset_y + iy * TILE_SIZE + cy * CHUNK_SIZE * TILE_SIZE;
            if (decoration.get(ix, iy) != 0)
            {
               Main.batch.setColor(Color.WHITE);
               Main.batch.draw(Res.DECORATION.sheet[decoration.get(ix, iy)], px + TILE_SIZE / 2f - Res.DECORATION.sheet_width / 2f, py);
            }
         }
      }
   }

   public boolean should_be_rendered(float player_x, float player_y)
   {
      int chunk_midx = cx * CHUNK_SIZE * TILE_SIZE + (CHUNK_SIZE * TILE_SIZE / 2);
      int chunk_midy = cy * CHUNK_SIZE * TILE_SIZE + (CHUNK_SIZE * TILE_SIZE / 2);
      return Util.euclid_norm((int) player_x, (int) player_y, chunk_midx, chunk_midy) < TILE_SIZE * CHUNK_SIZE;
   }

   public static Chunk load_from_png(FileHandle chunk_file)
   {
      String[] coords = chunk_file.nameWithoutExtension().split("_");
      int cx = Integer.parseInt(coords[0]);
      int cy = Integer.parseInt(coords[1]);
      int is_under_ground = Integer.parseInt(coords[2]);

      System.out.println("loading chunk [" + cx + "|" + cy + "]");
      Chunk chunk = new Chunk(cx, cy);

      chunk.underground = is_under_ground == 1;

      Pixmap pm = new Pixmap(chunk_file);
      Color local = new Color();
      if (pm.getWidth() != Chunk.CHUNK_SIZE || pm.getHeight() != Chunk.CHUNK_SIZE)
      {
         System.out.println("could not use file " + chunk_file.name() + " due to wrong dimensions! [" + pm.getWidth() + "|" + pm.getHeight() + "]");
         chunk = null;
      } else
      {
         for (int ix = 0; ix < Chunk.CHUNK_SIZE; ix++)
         {
            for (int iy = 0; iy < Chunk.CHUNK_SIZE; iy++)
            {
               Color.rgba8888ToColor(local, pm.getPixel(ix, CHUNK_SIZE - 1 - iy));
               Tile local_tile = Tile.from_R(MathUtils.floor(local.r * 255f));
               Chunk_Object object = Chunk_Object.from_G(MathUtils.floor(local.g * 255f));
               if (object != Chunk_Object.NONE)
               {
                  chunk.loaded_objects.add(object);
                  chunk.loaded_objects_tilex.add(ix);
                  chunk.loaded_objects_tiley.add(iy);
                  if (object == Chunk_Object.COLLECT_BOX) World.total_collect_box++;
               }
               chunk.content.set(ix, iy, (byte) local_tile.ordinal());

               Deco local_deco = Deco.from_B(MathUtils.floor(local.b * 255f));
               chunk.decoration.set(ix, iy, (byte) local_deco.ordinal());
            }
         }
      }
      pm.dispose();
      return chunk;
   }

   public enum Chunk_Object
   {
      // objects encoded in G value of a pixel in the chunk image
      NONE,
      BOX,
      ENEMY_FLOWER,

      POSTBOX,
      COLLECT_BOX,

      MUSHROOM,
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

      public static Chunk_Object from_G(int G)
      {
         return safe_ord(G / 10);
      }
   }
}