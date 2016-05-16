package watson.analysis;

import static watson.analysis.MiscPatterns.WG_REGIONS;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.text.ITextComponent;
import watson.Configuration;
import watson.Controller;
import watson.chat.IMatchedChatHandler;

// ----------------------------------------------------------------------------
/**
 * An {@link Analysis} that runs /region info region name for all of the regions
 * listed in chat (tag wg.regions) when you right click with a wooden sword.
 * 
 * To minimise spam, the rate at which /region info commands can be issued to
 * the server is limited by a timeout.
 */
public class RegionInfoAnalysis extends Analysis
{
  // ----------------------------------------------------------------------------
  /**
   * Constructor.
   */
  public RegionInfoAnalysis()
  {
    addMatchedChatHandler(WG_REGIONS, new IMatchedChatHandler()
    {
      @Override
      public boolean onMatchedChat(ITextComponent chat, Matcher m)
      {
        wgRegions(chat, m);
        return true;
      }
    });
  }

  // --------------------------------------------------------------------------
  /**
   * Respond to wg.regions by issuing the corresponding /region info commands.
   */
  @SuppressWarnings("unused")
  void wgRegions(ITextComponent chat, Matcher m)
  {
    long now = System.currentTimeMillis();
    if (now - _lastCommandTime > (long) (Configuration.instance.getRegionInfoTimeoutSeconds() * 1000))
    {
      int regionCount = 0;

      // Group 1 contains the comma-delimited list of region names.
      // We need to pull that apart with another regexp because nesting a
      // capturing group in a non-capturing group was not working for me.
      Matcher names = _regionNames.matcher(m.group(1));
      while (names.find())
      {
        Controller.instance.serverChat("/region info " + names.group());
        ++regionCount;
      }

      // Controller.serverChat() queues up commands and issues them at a
      // controlled rate. Avoid queueing up many "/region info"s
      // when the user is spamming wood sword where there are multiple
      // overlapping regions. Add the corresponding delay for each additional
      // region queried *after the first*.
      _lastCommandTime = now
                         + (long) (1000 * Configuration.instance.getChatTimeoutSeconds())
                         * Math.max(0, regionCount - 1);

    } // if timeout has expired
  } // wgRegions

  // --------------------------------------------------------------------------
  /**
   * The last time that the /region info command was automatically issued.
   */
  protected long    _lastCommandTime;

  /**
   * Extracts region names from the captured group, which is of the form:
   * "name, name2, name3".
   */
  protected Pattern _regionNames = Pattern.compile("[a-zA-Z0-9_-]+");
} // class RegionInfoAnalysis