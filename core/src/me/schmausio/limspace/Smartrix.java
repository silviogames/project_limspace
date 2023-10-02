package me.schmausio.limspace;


// A SMART 2D MATRIX OF INTEGERS saved as 1D expandable array
// similar to FlatByte but only the columns are fixed size, the rows can be expanded

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.StringBuilder;

public class Smartrix
{
  public final int width;
  final int def_value, clear_value;
  public IntArray data;

  public int[] temp_data;

  public int[] temp_swap_data;

  // this list holds indices of lines that have been cleared (filled with clear_value) and can be reused
  private final IntArray list_empty = new IntArray();

  public Smartrix(int width, int default_value, int clear_value)
  {
    this.width = width;
    this.def_value = default_value;
    this.clear_value = clear_value;
    data = new IntArray(width * 10);

    temp_data = new int[width];
    temp_swap_data = new int[width];
  }

  public int get(int line, int offset)
  {
    return data.get((line * width) + offset);
  }

  public int[] get_line(int line)
  {
    for (int i = 0; i < width; i++)
    {
      temp_data[i] = get(line, i);
    }
    return temp_data;
  }

  public void clear_line(int line)
  {
    // // OLD: clearing the whole line to prevent
    // for (int i = 0; i < width; i++)
    // {
    //   set(line, i, clear_value);
    // }

    // here I only set the first value in the line to the clear line,
    // that first value is used when reusing a cleared line (although they should all be in the
    // array that keeps indices of cleared lines),
    // the other values in the line are left as is which would lead to dirty data when reusing the line
    // but since I paste the new line when reusing a line, I overwrite the old data there as I already loop
    // over the line, so I can omit doing this here
    set(line, 0, clear_value);
    list_empty.add(line);
  }

  public void clear_all_lines()
  {
    for (int i = 0; i < num_lines(); i++)
    {
      if (get(i, 0) != clear_value)
      {
        clear_line(i);
      }
    }
  }

  public void sort(int sorting_index, boolean ascending)
  {
    quickSort(0, num_lines() - 1, sorting_index, ascending);
  }

  public void quickSort(int low, int high, int sorting_index, boolean ascending)
  {
    if (low < high)
    {
      int pivotIndex = partition(low, high, sorting_index, ascending);
      quickSort(low, pivotIndex - 1, sorting_index, ascending);
      quickSort(pivotIndex + 1, high, sorting_index, ascending);
    }
  }

  public int partition(int low, int high, int sorting_index, boolean ascending)
  {
    int pivot = get(high, sorting_index);
    int i = low - 1;
    for (int j = low; j < high; j++)
    {
      if ((get(j, sorting_index) < pivot && ascending) || (get(j, sorting_index) > pivot && !ascending))
      {
        i++;
        swap_lines(i, j);
      }
    }
    swap_lines(i + 1, high);
    return i + 1;
  }

  public void swap_lines(int first, int second)
  {
    if (first >= num_lines() || second >= num_lines() || first < 0 || second < 0)
    {
      return;
    } else
    {
      temp_swap_data = get_line(second);
      set_line(second, get_line(first));
      set_line(first, temp_swap_data);
    }
  }

  public boolean identical_line(int line, int... data)
  {
    // compare data with line
    // can crash if data is not correct length
    for (int i = 0; i < width; i++)
    {
      if (get(line, i) != data[i])
      {
        return false;
      }
    }
    return true;
  }

  public int find_free_line_index()
  {
    if (list_empty.size > 0)
    {
      return list_empty.pop();
    }

    int found_index = -1;
    // seeks through smartix for first line that begins with the free_value
    for (int i = 0; i < num_lines(); i++)
    {
      if (get(i, 0) == clear_value)
      {
        found_index = i;
        break;
      }
    }
    if (found_index == -1)
    {
      // all lines seem full, add new and return that index
      found_index = num_lines();
      add_line();
    }

    return found_index;
  }

  public void add_line(int... data_line)
  {
    if (data_line == null)
    {
      System.out.println("[SMARTRIX] add_line: cannot add line with empty!");
    } else
    {
      // if data_line is too long, the end is ignored
      // if data_line is too short, rest is filled with def_value
      for (int i = 0; i < width; i++)
      {
        if (i < data_line.length)
        {
          data.add(data_line[i]);
        } else
        {
          data.add(def_value);
        }
      }
    }
  }

  public int reuse_line(int... data)
  {
    int index = find_free_line_index();
    paste_line(index, data);
    return index;
  }

  public void append(Smartrix other)
  {
    if (other != null && other.width <= this.width)
    {
      for (int i = 0; i < other.num_lines(); i++)
      {
        add_line(other.get_line(i));
      }
    } else
    {
      System.out.println("[SMARTRIX] ERROR cannot append other smx!");
    }
  }

  public void clear()
  {
    // list empty is not valid anymore!
    list_empty.clear();
    data.clear();
  }

  public int num_lines()
  {
    return data.size / width;
  }

  public void set(int line, int offset, int val)
  {
    data.set((line * width) + offset, val);
  }

  public void set_line(int line, int... data)
  {
    // put data into line
    // crash if invalid input data
    for (int i = 0; i < width; i++)
    {
      this.data.set((line * width) + i, data[i]);
    }
  }

  public void paste_line(int line, int... data)
  {
    // this should override a row of the matrix
    if (data == null) return;
    if (line >= num_lines()) return;
    for (int i = 0; i < width; i++)
    {
      if (i < data.length)
      {
        this.data.set((line * width) + i, data[i]);
      } else
      {
        this.data.set((line * width) + i, def_value);
      }
    }
  }

  public void print_to_console()
  {
    // may spam console if large!
    System.out.println("_________________________");
    // DEBUG PRINT
    if (num_lines() == 0)
    {
      System.out.println("[SMARTRIX] is empty");
    } else
    {
      for (int i = 0; i < num_lines(); i++)
      {
        System.out.println("line [" + i + "]");
        for (int j = 0; j < width; j++)
        {
          System.out.print(get(i, j) + ",");
        }
        System.out.print("\n");
        System.out.println("++++++++++++++++++++");
      }
    }
    System.out.println("_________________________");
  }

  public void incr(int line, int offset, int change)
  {
    data.incr((line * width) + offset, change);
  }

  // TODO: 06.01.2022 save/load news in binary
  public void print_to_csv(String file_location_and_name, boolean append)
  {
    FileHandle file = Gdx.files.local(file_location_and_name + ".csv");
    StringBuilder sb = new StringBuilder();
    int lines = data.size / width;
    for (int i = 0; i < lines; i++)
    {
      for (int j = 0; j < width; j++)
      {
        sb.append(get(i, j));
        sb.append(",");
      }
      sb.append("\n");
    }
    file.writeString(sb.toString(), append);
  }

  public void read_from_csv(String file_location_and_name)
  {
    // I though that this may be buggy when loading long files but the issue was the string memory leaking in the news menu
    // But I still need to stress test this method for longer files in an actual playtest
    FileHandle file = Gdx.files.local(file_location_and_name + ".csv");
    if (file.exists())
    {
      String[] line_data;
      int[] temp_int_data = new int[width];
      String[] lines = file.readString().split("\n");
      for (int i = 0; i < lines.length; i++)
      {
        line_data = lines[i].split(",");
        for (int j = 0; j < width; j++)
        {
          if (j >= line_data.length)
          {
            // if load is too short (should not happen!)
            temp_int_data[j] = def_value;
          } else
          {
            temp_int_data[j] = Integer.parseInt(line_data[j]);
          }
        }
        add_line(temp_int_data);
      }
    } else
    {
      System.out.println("[SMARTRIX] cannot read, file " + file_location_and_name + " does not exist!");
    }
  }
}