package watson.model;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import com.mumfrey.liteloader.gl.GL;

import watson.db.BlockType;

// ----------------------------------------------------------------------------
/**
 * Abstract base of classes that draw the 3-D model of a block.
 */
public abstract class BlockModel
{
  // --------------------------------------------------------------------------
  /**
   * Constructor.
   * 
   * @param name the name of this instance, referenced in "blocks.yml".
   */
  public BlockModel(String name)
  {
    _name = name;
  }

  // --------------------------------------------------------------------------
  /**
   * Draw the model.
   * 
   * @param blockType a description of the type of block, which includes line
   *          colour and thickness and cuboid bounds.
   * @param x world X coordinate.
   * @param y world Y coordinate.
   * @param z world Z coordinate.
   */
  public abstract void render(BlockType blockType, int x, int y, int z);

  // --------------------------------------------------------------------------
  /**
   * Return the name of this BlockModel instance.
   * 
   * @return the name of this BlockModel instance.
   */
  public String getName()
  {
    return _name;
  }

  // --------------------------------------------------------------------------
  /**
   * Render a simple wireframe box.
   * 
   * @param x1 minimum corner x.
   * @param y1 minimum corner y.
   * @param z1 minimum corner z.
   * @param x2 maximum corner x.
   * @param y2 maximum corner y.
   * @param z2 maximum corner z.
   * @param colour colour.
   * @param lineWidth line width.
   */
  protected void renderBox(double x1, double y1, double z1, double x2,
    double y2, double z2, ARGB colour, float lineWidth)
  {
    renderTaperedBox(x1, z1, x2, z2, y1, x1, z1, x2, z2, y2, colour, lineWidth);
  }

  // --------------------------------------------------------------------------
  /**
   * Render a tapered wireframe box shape (either a pyramid or inverted pyramid,
   * with the point sliced off).
   * 
   * @param xBot1 bottom x 1.
   * @param zBot1 bottom z 1.
   * @param xBot2 bottom x 2.
   * @param zBot2 bottom z 2.
   * @param yBot bottom y.
   * @param xTop1 top x 1.
   * @param zTop1 top z 1.
   * @param xTop2 top x 2.
   * @param zTop2 top z 2.
   * @param yTop top y.
   * @param colour colour.
   * @param lineWidth line width.
   */
  protected void renderTaperedBox(double xBot1, double zBot1, double xBot2,
    double zBot2, double yBot, double xTop1,
    double zTop1, double xTop2, double zTop2,
    double yTop, ARGB colour, float lineWidth)
  {
    Tessellator tess = Tessellator.getInstance();
    VertexBuffer vr = tess.getBuffer();

    // Bottom face.
    vr.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
    vr.color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
    GL11.glLineWidth(lineWidth);
    vr.pos(xBot1, yBot, zBot1);
    vr.pos(xBot2, yBot, zBot1);
    vr.pos(xBot2, yBot, zBot2);
    vr.pos(xBot1, yBot, zBot2);
    tess.draw();

    // Top face.
    vr.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
    vr.color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
    GL11.glLineWidth(lineWidth);
    vr.pos(xTop1, yTop, zTop1);
    vr.pos(xTop2, yTop, zTop1);
    vr.pos(xTop2, yTop, zTop2);
    vr.pos(xTop1, yTop, zTop2);
    tess.draw();

    // Vertical lines joining top and bottom.
    vr.begin(GL.GL_LINES, GL.VF_POSITION);
    vr.color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
    GL11.glLineWidth(lineWidth);

    vr.pos(xBot1, yBot, zBot1);
    vr.pos(xTop1, yTop, zTop1);

    vr.pos(xBot2, yBot, zBot1);
    vr.pos(xTop2, yTop, zTop1);

    vr.pos(xBot1, yBot, zBot2);
    vr.pos(xTop1, yTop, zTop2);

    vr.pos(xBot2, yBot, zBot2);
    vr.pos(xTop2, yTop, zTop2);
    tess.draw();
  } // renderTaperedBox

  // --------------------------------------------------------------------------
  /**
   * The friendly name of this BlockModel instance, as referenced by
   * "blocks.yml".
   */
  private String _name;
} // abstract class BlockModel
