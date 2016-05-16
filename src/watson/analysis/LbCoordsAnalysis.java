package watson.analysis;

import static watson.analysis.LogBlockPatterns.LB_COORD;
import static watson.analysis.LogBlockPatterns.LB_COORD_KILLS;
import static watson.analysis.LogBlockPatterns.LB_COORD_REPLACED;
import static watson.analysis.LogBlockPatterns.LB_HEADER_BLOCK;
import static watson.analysis.LogBlockPatterns.LB_HEADER_BLOCKS;
import static watson.analysis.LogBlockPatterns.LB_HEADER_CHANGES;
import static watson.analysis.LogBlockPatterns.LB_HEADER_NO_RESULTS;
import static watson.analysis.LogBlockPatterns.LB_HEADER_RATIO;
import static watson.analysis.LogBlockPatterns.LB_HEADER_RATIO_CURRENT;
import static watson.analysis.LogBlockPatterns.LB_HEADER_SEARCHING;
import static watson.analysis.LogBlockPatterns.LB_HEADER_SUM_BLOCKS;
import static watson.analysis.LogBlockPatterns.LB_HEADER_SUM_PLAYERS;
import static watson.analysis.LogBlockPatterns.LB_HEADER_TIME_CHECK;
import static watson.analysis.LogBlockPatterns.LB_PAGE;

import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;

import net.minecraft.util.text.ITextComponent;
import watson.Configuration;
import watson.Controller;
import watson.SyncTaskQueue;
import watson.analysis.task.AddBlockEditTask;
import watson.chat.Chat;
import watson.chat.ChatComponents;
import watson.chat.Colour;
import watson.chat.IMatchedChatHandler;
import watson.db.BlockEdit;
import watson.db.BlockType;
import watson.db.BlockTypeRegistry;
import watson.db.TimeStamp;
import watson.debug.Log;

// ----------------------------------------------------------------------------
/**
 * An {@link Analysis} implementation that extracts {@link BlockEdit} instances
 * from lb.coord lines.
 */
public class LbCoordsAnalysis extends Analysis
{
  // --------------------------------------------------------------------------
  /**
   * Constructor.
   */
  public LbCoordsAnalysis()
  {
    addMatchedChatHandler(LB_COORD, new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        lbCoord(chat, m);
        // Don't echo in GUI.
        return false;
      }
    });
    addMatchedChatHandler(LB_COORD_KILLS, new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        lbCoordKills(chat, m);
        // Don't echo in GUI.
        return false;
      }
    });
    addMatchedChatHandler(LB_COORD_REPLACED, new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        lbCoordReplaced(chat, m);
        // Don't echo in GUI.
        return false;
      }
    });
    addMatchedChatHandler(LB_PAGE, new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        lbPage(chat, m);
        return true;
      }
    });

    IMatchedChatHandler headerHandler = new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        lbHeader(chat, m);
        return true;
      }
    };

    addMatchedChatHandler(LB_HEADER_NO_RESULTS, headerHandler);
    addMatchedChatHandler(LB_HEADER_CHANGES, headerHandler);
    addMatchedChatHandler(LB_HEADER_BLOCKS, headerHandler);
    addMatchedChatHandler(LB_HEADER_SUM_BLOCKS, headerHandler);
    addMatchedChatHandler(LB_HEADER_SUM_PLAYERS, headerHandler);
    addMatchedChatHandler(LB_HEADER_SEARCHING, headerHandler);
    addMatchedChatHandler(LB_HEADER_RATIO, headerHandler);
    addMatchedChatHandler(LB_HEADER_RATIO_CURRENT, headerHandler);
    addMatchedChatHandler(LB_HEADER_TIME_CHECK, headerHandler);
    addMatchedChatHandler(LB_HEADER_BLOCK, headerHandler);
  } // constructor

  // --------------------------------------------------------------------------
  /**
   * Parse creation and destruction coords results.
   */
  void lbCoord(ITextComponent chat, Matcher m)
  {
    try
    {
      // TODO: describe Matcher groups and their conversions in a config file.
      // Provide a way to get a set of named properties of a line.
      // Use reflection or JavaBeans Statement/Expression to create the
      // BlockEdit as directed by config file.

      int index = Integer.parseInt(m.group(1));
      int[] ymd = TimeStamp.parseYMD(m.group(2));
      int hour = Integer.parseInt(m.group(3));
      int minute = Integer.parseInt(m.group(4));
      int second = Integer.parseInt(m.group(5));
      long millis = TimeStamp.toMillis(ymd, hour, minute, second);

      String player = m.group(6);
      String action = m.group(7);
      String block = m.group(8);

      // If there are an extra 4 groups, then we're dealing with a sign.
      String sign1 = null, sign2 = null, sign3 = null, sign4 = null;
      int x, y, z;
      if (m.groupCount() == 15)
      {
        sign1 = m.group(9);
        sign2 = m.group(10);
        sign3 = m.group(11);
        sign4 = m.group(12);
        x = Integer.parseInt(m.group(13));
        y = Integer.parseInt(m.group(14));
        z = Integer.parseInt(m.group(15));
      }
      else
      {
        x = Integer.parseInt(m.group(9));
        y = Integer.parseInt(m.group(10));
        z = Integer.parseInt(m.group(11));
      }

      BlockType type = BlockTypeRegistry.instance.getBlockTypeByName(block);
      boolean created = action.equals("created");
      BlockEdit edit = new BlockEdit(millis, player, created, x, y, z, type);
      SyncTaskQueue.instance.addTask(new AddBlockEditTask(edit, true));

      char colourCode = getChatColourChar(x, y, z);
      String colour = Configuration.instance.getRecolourQueryResults() ? "\247" + colourCode : "";
      if (Configuration.instance.getReformatQueryResults())
      {
        // TODO: fix this :) Have a class that allows dynamic control of
        // filtered coords.
        // Hacked in re-echoing of coords so we can see TP targets.
        if (type.getId() != 1)
        {
          String signText = (sign1 != null)
            ? String.format(Locale.US, " [%s] [%s] [%s] [%s]", sign1, sign2, sign3, sign4)
            : "";

          // Only show the year if LogBlock is configured to return it.
          String year = (ymd[0] != 0)
            ? String.format(Locale.US, "%02d-", ymd[0])
            : "";
          String output = String.format(Locale.US,
            "%s(%2d) %s%02d-%02d %02d:%02d:%02d (%d,%d,%d) %C%d %s%s",
            colour, index, year, ymd[1], ymd[2], hour, minute,
            second, x, y, z, (created ? '+' : '-'), type.getId(), player, signText);
          Chat.localChat(output);
        }
      }
      else
      {
        // No reformatting of query results. Recolour?
        if (Configuration.instance.getRecolourQueryResults())
        {
          Chat.localChat(ChatComponents.getTextFormatting(colourCode), chat.getUnformattedText());
        }
        else
        {
          Chat.localChat(chat);
        }
      }

      requestNextPage();
    }
    catch (Exception ex)
    {
      Log.exception(Level.INFO, "error parsing lb coords", ex);
    }
  } // lbCoord

  // --------------------------------------------------------------------------
  /**
   * Parse kill coords results.
   */
  void lbCoordKills(ITextComponent chat, Matcher m)
  {
    try
    {
      int index = Integer.parseInt(m.group(1));
      int[] ymd = TimeStamp.parseYMD(m.group(2));
      int hour = Integer.parseInt(m.group(3));
      int minute = Integer.parseInt(m.group(4));
      int second = Integer.parseInt(m.group(5));
      long millis = TimeStamp.toMillis(ymd, hour, minute, second);

      String player = m.group(6);
      String victim = m.group(7);

      int x = Integer.parseInt(m.group(8));
      int y = Integer.parseInt(m.group(9));
      int z = Integer.parseInt(m.group(10));
      String weapon = m.group(11);

      // LogBlock doesn't distinguish between player kills and other kills, it
      // just gives the name of what was killed. Since we (hopefully) list all
      // the possible kill types that aren't players in blocks.yml, for things
      // matching the LB_COORD_KILLS pattern we will assume that anything that
      // isn't listed in blocks.yml is a player. getBlockKillTypeByName() does
      // this by assigning any unknown kill to the "unknown" kill ID, which is
      // 219, our generic player model (this is similar to how
      // getBlockTypeByName() assigns any unknown ID to 256.) The downside of
      // this is that true new/unknown kill types will be assigned a player-like
      // looking model over a typical "unknown" bright magenta box.
      BlockType type = BlockTypeRegistry.instance.getBlockKillTypeByName(victim);

      // For our purposes, we'll treat a kill like a block destruction
      BlockEdit edit = new BlockEdit(millis, player, false, x, y, z, type);
      SyncTaskQueue.instance.addTask(new AddBlockEditTask(edit, true));

      char colourCode = getChatColourChar(x, y, z);
      String colour = Configuration.instance.getRecolourQueryResults() ? "\247" + colourCode : "";
      if (Configuration.instance.getReformatQueryResults())
      {
        // TODO: fix this :) Have a class that allows dynamic control of
        // filtered coords.
        // Hacked in re-echoing of coords so we can see TP targets.
        if (type.getId() != 1)
        {
          // Only show the year if LogBlock is configured to return it.
          String year = (ymd[0] != 0)
            ? String.format(Locale.US, "%02d-", ymd[0])
            : "";
          String output = String.format(Locale.US,
            "%s(%2d) %s%02d-%02d %02d:%02d:%02d (%d,%d,%d) %s %s > %s",
            colour, index, year, ymd[1], ymd[2], hour, minute,
            second, x, y, z, player, weapon, victim);
          Chat.localChat(output);
        }
      }
      else
      {
        // No reformatting of query results. Recolour?
        if (Configuration.instance.getRecolourQueryResults())
        {
          Chat.localChat(ChatComponents.getTextFormatting(colourCode), chat.getUnformattedText());
        }
        else
        {
          Chat.localChat(chat);
        }
      }

      requestNextPage();
    }
    catch (Exception ex)
    {
      Log.exception(Level.INFO, "error parsing lb kills coords", ex);
    }
  } // lbCoordKills

  // --------------------------------------------------------------------------
  /**
   * Parse /lb coords results where the edit was replacement of one block with
   * another.
   */
  void lbCoordReplaced(ITextComponent chat, Matcher m)
  {
    try
    {
      int index = Integer.parseInt(m.group(1));
      int[] ymd = TimeStamp.parseYMD(m.group(2));
      int hour = Integer.parseInt(m.group(3));
      int minute = Integer.parseInt(m.group(4));
      int second = Integer.parseInt(m.group(5));
      long millis = TimeStamp.toMillis(ymd, hour, minute, second);

      String player = m.group(6);
      String oldBlock = m.group(7);
      // UNUSED: String newBlock = m.group(8);
      int x = Integer.parseInt(m.group(9));
      int y = Integer.parseInt(m.group(10));
      int z = Integer.parseInt(m.group(11));
      BlockType type = BlockTypeRegistry.instance.getBlockTypeByName(oldBlock);

      // Store the destruction but don't bother with the creation.
      BlockEdit edit = new BlockEdit(millis, player, false, x, y, z, type);
      SyncTaskQueue.instance.addTask(new AddBlockEditTask(edit, true));

      char colourCode = getChatColourChar(x, y, z);
      String colour = Configuration.instance.getRecolourQueryResults() ? "\247" + colourCode : "";
      if (Configuration.instance.getReformatQueryResults())
      {
        // TODO: fix this :)
        // Hacked in re-echoing of coords so we can see TP targets.
        if (type.getId() != 1)
        {
          // Only show the year if LogBlock is configured to return it.
          String year = (ymd[0] != 0)
            ? String.format(Locale.US, "%02d-", ymd[0])
            : "";

          String output = String.format(Locale.US,
            "%s(%2d) %s%02d-%02d %02d:%02d:%02d (%d,%d,%d) %C%d %s",
            colour, index, year, ymd[1], ymd[2], hour, minute, second, x, y, z, '-', type.getId(), player);
          Chat.localChat(output);
        }
      }
      else
      {
        // No reformatting of query results. Recolour?
        if (Configuration.instance.getRecolourQueryResults())
        {
          Chat.localChat(ChatComponents.getTextFormatting(colourCode), chat.getUnformattedText());
        }
        else
        {
          Chat.localChat(chat);
        }
      }
      requestNextPage();
    }
    catch (Exception ex)
    {
      // System.out.println(ex);
    }
  } // lbCoordReplaced

  // --------------------------------------------------------------------------
  /**
   * Parse page headers.
   *
   * We run "/lb page (n+1)" automatically if the number of pages of results in
   * the "/lb coords" output is less than or equal to the max_auto_pages
   * configuration setting.
   */
  @SuppressWarnings("unused")
  void lbPage(ITextComponent chat, Matcher m)
  {
    int currentPage = Integer.parseInt(m.group(1));
    int pageCount = Integer.parseInt(m.group(2));

    // Enforce the page limit here.
    if (pageCount <= Configuration.instance.getMaxAutoPages())
    {
      _currentPage = currentPage;
      _pageCount = pageCount;
    }
    else
    {
      _currentPage = _pageCount = 0;
    }
  } // lbPage

  // --------------------------------------------------------------------------
  /**
   * Sometimes you do an /lb query (e.g. "/lb time 4h block 56 sum p") that
   * results in a page header ("Page 1/3"), and immediately follow that with an
   * "/lb coords" query that doesn't have a page header. Consequently,
   * _currentPage and _pageCount can be set to the values for the preceding
   * query and requestNextPage() will attempt to page through. To prevent that,
   * we look for the various headers in /lb results and clear the counters.
   */
  @SuppressWarnings("unused")
  void lbHeader(ITextComponent chat, Matcher m)
  {
    _currentPage = _pageCount = 0;
  }

  // --------------------------------------------------------------------------
  /**
   * This method is called when coordinates are parsed out of chat to request
   * the next page of "/lb coords" results, up to the configured maximum number
   * of pages.
   */
  private void requestNextPage()
  {
    if (Configuration.instance.isAutoPage())
    {
      if (_currentPage != 0 && _currentPage < _pageCount
          && _pageCount <= Configuration.instance.getMaxAutoPages())
      {
        Controller.instance.serverChat(String.format(Locale.US, "/lb page %d", _currentPage + 1));

        // Remember that we don't need to do this again until next page is
        // parsed.
        _currentPage = _pageCount = 0;
      }
    }
  } // requestNextPage

  // --------------------------------------------------------------------------
  /**
   * Get the colour to highlight coordinates when they are re-echoed into chat.
   *
   * The colour changes whenever the
   */
  private char getChatColourChar(int x, int y, int z)
  {
    // Check whether we should advance the index.
    int dx = x - _lastX;
    int dy = y - _lastY;
    int dz = z - _lastZ;

    // Skip the sqrt().
    float distance = dx * dx + dy * dy + dz * dz;
    if (distance > _COLOUR_PROXIMITY_LIMIT * _COLOUR_PROXIMITY_LIMIT)
    {
      _colourIndex = (_colourIndex + 1) % _COLOUR_CYCLE.length;
    }
    _lastX = x;
    _lastY = y;
    _lastZ = z;

    return _COLOUR_CYCLE[_colourIndex];
  } // getChatColourChar

  // --------------------------------------------------------------------------
  /**
   * The cycle of colours used to highlight distinct ore deposits when
   * coordinates are re-echoed.
   */
  protected static final char  _COLOUR_CYCLE[]         = {Colour.red.getCode(),
                                                       Colour.orange.getCode(), Colour.yellow.getCode(),
                                                       Colour.lightgreen.getCode(), Colour.lightblue.getCode(),
                                                       Colour.purple.getCode(), Colour.magenta.getCode()};

  /**
   * The index into the _COLOUR_CYCLE array referencing the current chat colour.
   *
   * Since the very first colour is pretty much guaranteed to roll over (since
   * _lastX, _lastY, _lastZ will be nowhere near), init to the end of the cycle
   * in anticipation).
   *
   * TODO: Make the colour of echoed coordinates stable? Or at least reset when
   * the lb header is seen.
   */
  protected int                _colourIndex            = _COLOUR_CYCLE.length - 1;

  /**
   * The minimum distance that ore deposits (or blocks in general) must be
   * separated by to colour them differently when their coordinates are echoed
   * in chat.
   */
  protected static final float _COLOUR_PROXIMITY_LIMIT = 4.0f;

  /**
   * The last set of coordinates re-echoed in chat.
   */
  protected int                _lastX, _lastY, _lastZ;

  /**
   * Current page number extracted from lb.page lines.
   */
  protected int                _currentPage            = 0;

  /**
   * Total number of pages of results, from lb.page lines.
   */
  protected int                _pageCount              = 0;

} // class LbCoordsAnalysis