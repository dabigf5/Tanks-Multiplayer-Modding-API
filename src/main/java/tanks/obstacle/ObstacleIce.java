package tanks.obstacle;

import basewindow.IBatchRenderableObject;
import tanks.Drawing;
import tanks.Game;
import tanks.Movable;
import tanks.StatusEffect;
import tanks.rendering.ShaderGroundIce;
import tanks.rendering.ShaderIce;
import tanks.tank.Mine;
import tanks.tank.Tank;

public class ObstacleIce extends Obstacle
{
    public ObstacleIce(String name, double posX, double posY)
    {
        super(name, posX, posY);

        if (Game.enable3d)
            this.drawLevel = 6;
        else
            this.drawLevel = 1;

        this.destructible = false;
        this.tankCollision = false;
        this.bulletCollision = false;
        this.checkForObjects = true;
        this.enableStacking = false;

        this.isSurfaceTile = true;

        this.colorR = 200;
        this.colorG = 225;
        this.colorB = 255;
        this.colorA = 180;

        this.replaceTiles = true;
        this.renderer = ShaderIce.class;
        this.tileRenderer = ShaderGroundIce.class;

        this.description = "A slippery layer of ice";
    }

    @Override
    public void onObjectEntry(Movable m)
    {
        if (m instanceof Tank || m instanceof Mine)
            m.addStatusEffect(StatusEffect.ice, 0, 5, 10);
    }

    @Override
    public void draw()
    {
        double h = this.baseGroundHeight;

        Drawing.drawing.setColor(this.colorR, this.colorG, this.colorB, this.colorA * (h - Obstacle.draw_size / Game.tile_size * 15) / (h - 15));

        if (!Game.enable3d)
            Drawing.drawing.fillRect(this, this.posX, this.posY, Obstacle.draw_size, Obstacle.draw_size);
        else
            Drawing.drawing.fillBox(this, this.posX, this.posY, 0, Game.tile_size, Game.tile_size, 0, (byte) 61);
    }

    @Override
    public void drawTile(IBatchRenderableObject tile, double r, double g, double b, double d, double extra)
    {
        double frac = Obstacle.draw_size / Game.tile_size;

        Drawing.drawing.setColor(r, g, b);
        Drawing.drawing.fillBox(tile, this.posX, this.posY, -frac * 15 - extra, Game.tile_size, Game.tile_size, d + extra);
    }

    public double getTileHeight()
    {
        return -Obstacle.draw_size / Game.tile_size * 15;
    }

    public double getGroundHeight()
    {
        return 0;
    }
}
