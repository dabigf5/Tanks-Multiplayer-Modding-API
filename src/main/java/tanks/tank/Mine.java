package tanks.tank;

import tanks.*;
import tanks.gui.menus.FixedMenu;
import tanks.gui.menus.Scoreboard;
import tanks.gui.screen.ScreenPartyLobby;
import tanks.hotbar.item.ItemMine;
import tanks.network.event.EventMineChangeTimer;
import tanks.network.event.EventMineRemove;
import tanks.obstacle.Obstacle;

import java.util.ArrayList;
import java.util.HashMap;

public class Mine extends Movable implements IAvoidObject
{
    public static double mine_size = 30;
    public static double mine_radius = Game.tile_size * 2.5;
    public static double mine_friction = 0.02;

    public double timer;
    public double outlineColorR;
    public double outlineColorG;
    public double outlineColorB;
    public double height = 0;

    public double triggeredTimer = 50;
    public double damage = 2;
    public boolean destroysObstacles = true;

    public boolean enableCollision = true;
    public double friction = mine_friction;
    public double frictionModifier = 1;

    public double radius = mine_radius;
    public Tank tank;
    public ItemMine item;
    public double cooldown = 0;
    public int lastBeep = Integer.MAX_VALUE;

    public int networkID = -1;

    public static int currentID = 0;
    public static ArrayList<Integer> freeIDs = new ArrayList<>();
    public static HashMap<Integer, Mine> idMap = new HashMap<>();

    public Mine(double x, double y, double timer, Tank t, ItemMine item)
    {
        super(x, y);

        this.posZ = t.posZ;
        this.size = mine_size;
        this.timer = timer;
        this.drawLevel = 2;
        this.bounciness = 0.2;
        this.tank = t;

        this.item = item;

        if (!ScreenPartyLobby.isClient)
            this.item.liveMines++;

        this.team = t.team;
        double[] outlineCol = Team.getObjectColor(t.colorR, t.colorG, t.colorB, t);
        this.outlineColorR = outlineCol[0];
        this.outlineColorG = outlineCol[1];
        this.outlineColorB = outlineCol[2];

        if (!ScreenPartyLobby.isClient)
        {
            if (freeIDs.size() > 0)
                this.networkID = freeIDs.remove(0);
            else
            {
                this.networkID = currentID;
                currentID++;
            }

            idMap.put(this.networkID, this);
        }

        for (FixedMenu m : ModAPI.fixedMenus)
        {
            if (m instanceof Scoreboard && ((Scoreboard) m).objectiveType.equals(Scoreboard.objectiveTypes.mines_placed))
            {
                Scoreboard s = ((Scoreboard) m);

                if (!s.teamPoints.isEmpty())
                    s.addTeamScore(this.team, 1);

                else if (this.tank instanceof TankPlayer)
                    s.addPlayerScore(((TankPlayer) this.tank).player, 1);

                else if (this.tank instanceof TankPlayerRemote)
                    s.addPlayerScore(((TankPlayerRemote) this.tank).player, 1);
            }
        }
    }

    public Mine(double x, double y, Tank t, ItemMine im)
    {
        this(x, y, 1000, t, im);
    }

    @Override
    public void draw()
    {
        Drawing.drawing.setColor(this.outlineColorR, this.outlineColorG, this.outlineColorB, 255, 0.5);

        if (Game.enable3d && Game.enable3dBg && Game.fancyTerrain)
        {
            this.height = Math.max(this.height, Game.sampleTerrainGroundHeight(this.posX - this.size / 2, this.posY - this.size / 2));
            this.height = Math.max(this.height, Game.sampleTerrainGroundHeight(this.posX + this.size / 2, this.posY - this.size / 2));
            this.height = Math.max(this.height, Game.sampleTerrainGroundHeight(this.posX - this.size / 2, this.posY + this.size / 2));
            this.height = Math.max(this.height, Game.sampleTerrainGroundHeight(this.posX + this.size / 2, this.posY + this.size / 2));
        }

        if (Game.enable3d)
        {
            for (double i = height; i < height + 6; i++)
            {
                double frac = ((i - height + 1) / 6 + 1) / 2;
                Drawing.drawing.setColor(this.outlineColorR * frac, this.outlineColorG  * frac, this.outlineColorB * frac, 255, 0.5);
                Drawing.drawing.fillOval(this.posX, this.posY, this.posZ + i + 1.5, this.size, this.size, true, false);
            }

            Drawing.drawing.setColor(this.outlineColorR, this.outlineColorG, this.outlineColorB, 255, 1);

            if (Game.glowEnabled)
                Drawing.drawing.fillGlow(this.posX, this.posY, this.posZ + height + 1, this.size * 4, this.size * 4, true, false);
        }
        else
        {
            Drawing.drawing.fillOval(this.posX, this.posY, this.size, this.size);

            if (Game.glowEnabled)
                Drawing.drawing.fillGlow(this.posX, this.posY, this.size * 4, this.size * 4);
        }

        Drawing.drawing.setColor(255, Math.min(1000, this.timer) / 1000.0 * 255, 0, 255, 0.5);

        if (timer < 150 && ((int) timer % 20) / 10 == 1)
            Drawing.drawing.setColor(255, 255, 0, 255, 0.5);

        if (Game.enable3d)
            Drawing.drawing.fillOval(this.posX, this.posY, this.posZ + height + 7.5, this.size * 0.8, this.size * 0.8, true, false);
        else
            Drawing.drawing.fillOval(this.posX, this.posY, this.size * 0.8, this.size * 0.8);
    }

    @Override
    public void update()
    {
        this.timer -= Panel.frameFrequency;

        this.frictionModifier = this.getAttributeValue(AttributeModifier.friction, 1);

        if (this.timer < 0)
            this.timer = 0;

        if ((this.timer <= 0 || destroy) && !ScreenPartyLobby.isClient)
            this.explode();

        int beepTime = ((int) this.timer / 10);
        if (this.timer <= 150 && beepTime % 2 == 1 && this.lastBeep != beepTime && this.tank == Game.playerTank)
        {
            Drawing.drawing.playSound("beep.ogg", 1f, 0.25f);
            this.lastBeep = beepTime;
        }

        super.update();

        this.vX *= Math.pow(1 - friction, frictionModifier * Panel.frameFrequency);
        this.vY *= Math.pow(1 - friction, frictionModifier * Panel.frameFrequency);

        if (this.enableCollision)
            checkCollision();

        boolean enemyNear = false;
        boolean allyNear = false;
        for (Movable m : Game.movables)
        {
            if (Math.pow(Math.abs(m.posX - this.posX), 2) + Math.pow(Math.abs(m.posY - this.posY), 2) < Math.pow(radius, 2))
            {
                if (m instanceof Tank && !m.destroy && ((Tank) m).targetable)
                {
                    if (Team.isAllied(m, this.tank))
                        allyNear = true;
                    else
                        enemyNear = true;
                }
            }
        }

        if (enemyNear && !allyNear && this.timer > this.triggeredTimer && !this.isRemote)
        {
            this.timer = this.triggeredTimer;
            Game.eventsOut.add(new EventMineChangeTimer(this));
        }
    }

    public void checkCollision()
    {
        if (Tank.checkCollisionWithBorder(this))
            onCollidedWith(null);

        double t = Game.tile_size;

        int x1 = (int) Math.min(Math.max(0, this.posX / t - this.size / t / 2 - 1), Game.currentSizeX);
        int y1 = (int) Math.min(Math.max(0, this.posY / t - this.size / t / 2 - 1), Game.currentSizeY);
        int x2 = (int) Math.min(Math.max(0, this.posX / t + this.size / t / 2 + 1), Game.currentSizeX);
        int y2 = (int) Math.min(Math.max(0, this.posY / t + this.size / t / 2 + 1), Game.currentSizeY);

        for (int x = x1; x < x2; x++)
        {
            for (int y = y1; y < y2; y++)
            {
                checkCollisionWith(Game.obstacleGrid[x][y]);
                checkCollisionWith(Game.surfaceTileGrid[x][y]);
            }
        }
    }

    public void checkCollisionWith(Obstacle o)
    {
        if (Tank.checkCollideWith(this, o))
            onCollidedWith(o);
    }

    public void onCollidedWith(Obstacle o)
    {

    }

    public void explode()
    {
        Game.eventsOut.add(new EventMineRemove(this));
        Game.removeMovables.add(this);

        if (!ScreenPartyLobby.isClient)
        {
            freeIDs.add(this.networkID);
            idMap.remove(this.networkID);

            Explosion e = new Explosion(this);
            e.explode();

            this.item.liveMines--;
        }
    }

    @Override
    public double getRadius()
    {
        return Math.min(Mine.mine_radius * 1.5, this.radius);
    }

    @Override
    public double getSeverity(double posX, double posY)
    {
        return this.timer;
    }
}
