package tanks.tank;

import basewindow.Model;
import basewindow.ModelPart;
import tanks.*;
import tanks.bullet.Bullet;
import tanks.editorselector.LevelEditorSelector;
import tanks.editorselector.RotationSelector;
import tanks.editorselector.TeamSelector;
import tanks.gui.screen.ScreenGame;
import tanks.gui.screen.ScreenPartyHost;
import tanks.gui.screen.ScreenPartyLobby;
import tanks.hotbar.item.ItemBullet;
import tanks.hotbar.item.ItemMine;
import tanks.network.event.EventCreateTank;
import tanks.network.event.EventTankAddAttributeModifier;
import tanks.network.event.EventTankUpdate;
import tanks.network.event.EventTankUpdateHealth;
import tanks.obstacle.Face;
import tanks.obstacle.ISolidObject;
import tanks.obstacle.Obstacle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static tanks.tank.TankProperty.Category.*;

public abstract class Tank extends Movable implements ISolidObject
{
    public static int disabledZ = (int) (1.25 * Game.tile_size);

    public static int updatesPerSecond = 30;
    public static long lastUpdateTime = 0;
    public static boolean shouldUpdate = false;

    public static int currentID = 0;
    public static ArrayList<Integer> freeIDs = new ArrayList<>();
    public static HashMap<Integer, Tank> idMap = new HashMap<>();

    public static ModelPart health_model;

    public RotationSelector<Tank> rotationSelector;
    public TeamSelector<Tank> teamSelector;

    public boolean fromRegistry = false;

    @TankProperty(category = appearanceBody, id = "color_model", name = "Tank body model", miscType = TankProperty.MiscType.colorModel)
    public Model colorModel = TankModels.tank.color;
    @TankProperty(category = appearanceTreads, id = "base_model", name = "Tank treads model", miscType = TankProperty.MiscType.baseModel)
    public Model baseModel = TankModels.tank.base;
    @TankProperty(category = appearanceTurretBase, id = "turret_base_model", name = "Turret base model", miscType = TankProperty.MiscType.turretBaseModel)
    public Model turretBaseModel = TankModels.tank.turretBase;
    @TankProperty(category = appearanceTurretBarrel, id = "turret_model", name = "Turret barrel model", miscType = TankProperty.MiscType.turretModel)
	public Model turretModel = TankModels.tank.turret;

	public double angle = 0;
	public double pitch = 0;

	public boolean depthTest = true;

	public boolean disabled = false;
	public boolean inControlOfMotion = true;
	public boolean positionLock = false;

	public boolean fullBrightness = false;

	public boolean tookRecoil = false;
	public double recoilSpeed = 0;

	/** If spawned by another tank, set to the tank that spawned this tank*/
	protected Tank parent = null;

	@TankProperty(category = general, id = "name", name = "Tank name")
	public String name;

	@TankProperty(category = general, id = "coin_value", name = "Coin value")
	public int coinValue = 0;

	@TankProperty(category = general, id = "base_health", name = "Hitpoints", desc = "The default bullet does one hitpoint of damage")
	public double baseHealth = 1;
	public double health = 1;

	@TankProperty(category = general, id = "invulnerable", name = "Invincible")
	public boolean invulnerable = false;

	@TankProperty(category = general, id = "targetable", name = "Should be targeted")
	public boolean targetable = true;

	@TankProperty(category = general, id = "resist_bullets", name = "Bullet immunity")
	public boolean resistBullets = false;
	@TankProperty(category = general, id = "resist_explosions", name = "Explosion immunity")
	public boolean resistExplosions = false;
	@TankProperty(category = general, id = "resist_freezing", name = "Freezing immunity")
	public boolean resistFreeze = false;

	public int networkID = -1;
	public int crusadeID = -1;

	@TankProperty(category = general, id = "description", name = "Tank description", miscType = TankProperty.MiscType.description)
	public String description = "";

    @TankProperty(category = movementGeneral, id = "max_speed", name = "Top speed")
    public double maxSpeed = 1.5;

    @TankProperty(category = movementGeneral, id = "acceleration", name = "Acceleration")
    public double acceleration = 0.05;

    @TankProperty(category = movementGeneral, id = "friction", name = "Friction")
    public double friction = 0.05;

    public double buoyancy = 0;

    public double accelerationModifier = 1;
    public double frictionModifier = 1;
    public double maxSpeedModifier = 1;
    public double damageRate = 0;

    @TankProperty(category = appearanceBody, id = "color_r", name = "Red", miscType = TankProperty.MiscType.color)
    public double colorR;
    @TankProperty(category = appearanceBody, id = "color_g", name = "Green", miscType = TankProperty.MiscType.color)
    public double colorG;
	@TankProperty(category = appearanceBody, id = "color_b", name = "Blue", miscType = TankProperty.MiscType.color)
	public double colorB;

	@TankProperty(category = appearanceGlow, id = "glow_intensity", name = "Aura intensity")
	public double glowIntensity = 0.8;
	@TankProperty(category = appearanceGlow, id = "glow_size", name = "Aura size")
	public double glowSize = 4;
	@TankProperty(category = appearanceGlow, id = "light_intensity", name = "Light intensity")
	public double lightIntensity = 1;
	@TankProperty(category = appearanceGlow, id = "light_size", name = "Light size")
	public double lightSize = 0;
	@TankProperty(category = appearanceGlow, id = "luminance", name = "Tank luminance", desc = "How bright the tank will be in dark lighting. At 0, the tank will be shaded like terrain by lighting. At 1, the tank will always be fully bright.")
	public double luminance = 0.5;

	/** Important: this option only is useful for the tank editor. Secondary color will be treated independently even if disabled. */
	@TankProperty(category = appearanceTurretBarrel, id = "enable_color2", name = "Custom color", miscType = TankProperty.MiscType.color)
	public boolean enableSecondaryColor = false;
	@TankProperty(category = appearanceTurretBarrel, id = "color_r2", name = "Red", miscType = TankProperty.MiscType.color)
	public double secondaryColorR;
	@TankProperty(category = appearanceTurretBarrel, id = "color_g2", name = "Green", miscType = TankProperty.MiscType.color)
	public double secondaryColorG;
	@TankProperty(category = appearanceTurretBarrel, id = "color_b2", name = "Blue", miscType = TankProperty.MiscType.color)
	public double secondaryColorB;
	@TankProperty(category = appearanceTurretBarrel, id = "turret_size", name = "Turret thickness")
	public double turretSize = 8;
	@TankProperty(category = appearanceTurretBarrel, id = "turret_length", name = "Turret length")
	public double turretLength = Game.tile_size;
	@TankProperty(category = appearanceTurretBarrel, id = "multiple_turrets", name = "Multiple turrets", desc = "If enabled, the turret will reflect the bullet count")
	public boolean multipleTurrets = true;

	/** Important: tertiary color values will not be used unless this option is set to true! */
	@TankProperty(category = appearanceTurretBase, id = "enable_color3", name = "Custom color", miscType = TankProperty.MiscType.color)
	public boolean enableTertiaryColor = false;
	@TankProperty(category = appearanceTurretBase, id = "color_r3", name = "Red", miscType = TankProperty.MiscType.color)
	public double tertiaryColorR;
	@TankProperty(category = appearanceTurretBase, id = "color_g3", name = "Green", miscType = TankProperty.MiscType.color)
	public double tertiaryColorG;
	@TankProperty(category = appearanceTurretBase, id = "color_b3", name = "Blue", miscType = TankProperty.MiscType.color)
	public double tertiaryColorB;

	@TankProperty(category = appearanceTracks, id = "enable_tracks", name = "Lays tracks")
	public boolean enableTracks = true;
	@TankProperty(category = appearanceTracks, id = "track_spacing", name = "Track spacing")
	public double trackSpacing = 0.4;

	//public int liveBulletMax;
	//public int liveMinesMax;

	@TankProperty(category = firingGeneral, id = "bullet", name = "Bullet")
	public ItemBullet bullet = (ItemBullet) TankPlayer.default_bullet.clone();

	@TankProperty(category = mines, id = "mine", name = "Mine")
	public ItemMine mine = (ItemMine) TankPlayer.default_mine.clone();

	/** Age in frames*/
	protected double age = 0;

	public double drawAge = 0;
	public double destroyTimer = 0;
	public boolean hasCollided = false;
	public double flashAnimation = 0;
	public double treadAnimation = 0;
	public boolean drawTread = false;

	@TankProperty(category = appearanceEmblem, id = "emblem", name = "Tank emblem", miscType = TankProperty.MiscType.emblem)
	public String emblem = null;
	@TankProperty(category = appearanceEmblem, id = "emblem_r", name = "Red", miscType = TankProperty.MiscType.color)
	public double emblemR;
	@TankProperty(category = appearanceEmblem, id = "emblem_g", name = "Green", miscType = TankProperty.MiscType.color)
	public double emblemG;
	@TankProperty(category = appearanceEmblem, id = "emblem_b", name = "Blue", miscType = TankProperty.MiscType.color)
	public double emblemB;

	public double orientation = 0;

	public double hitboxSize = 0.95;

	/** Used for custom tanks, see /music/tank for built-in tanks */
	@TankProperty(category = general, id = "music", name = "Music tracks", miscType = TankProperty.MiscType.music)
	public HashSet<String> musicTracks = new HashSet<>();

	@TankProperty(category = general, id = "explode_on_destroy", name = "Explosive", desc="If set, the tank will explode when destroyed")
	public boolean explodeOnDestroy = false;

	/** Whether this tank needs to be destroyed before the level ends. */
	@TankProperty(category = general, id = "mandatory_kill", name = "Must be destroyed", desc="Whether the tank needs to be destroyed to clear the level")
	public boolean mandatoryKill = true;

	@TankProperty(category = general, id = "collision", name = "Pushed during Collision")
	public boolean collisionPush = true;

	public boolean[][] hiddenPoints = new boolean[3][3];
	public boolean hidden = false;

	public boolean[][] canHidePoints = new boolean[3][3];
	public boolean canHide = false;

	public Turret turret;

	public boolean standardUpdateEvent = true;

	public Face[] horizontalFaces;
	public Face[] verticalFaces;

	public boolean isBoss = false;
	public Tank possessor;
	public Tank possessingTank = null;
	public boolean overridePossessedKills = true;

	public long lastFarthestInSightUpdate = 0;
	public Tank lastFarthestInSight = null;

	public Tank(String name, double x, double y, double size, double r, double g, double b)
	{
		super(x, y);
		this.size = size;
		this.colorR = r;
		this.colorG = g;
		this.colorB = b;
		turret = new Turret(this);
		this.name = name;
		this.nameTag = new NameTag(this, 0, this.size / 7 * 5, this.size / 2, this.name, r, g, b);

		this.drawLevel = 4;

		this.bullet.unlimitedStack = true;
		this.mine.unlimitedStack = true;
	}

	public void unregisterNetworkID()
	{
		if (idMap.get(this.networkID) == this)
			idMap.remove(this.networkID);

		if (!freeIDs.contains(this.networkID))
			freeIDs.add(this.networkID);
	}

	public static int nextFreeNetworkID()
	{
		if (freeIDs.size() > 0)
			return freeIDs.remove(0);
		else
		{
			currentID++;
			return currentID - 1;
		}
	}

	public void registerNetworkID()
	{
		if (ScreenPartyLobby.isClient)
			Game.exitToCrash(new RuntimeException("Do not automatically assign network IDs on client!"));

		this.networkID = nextFreeNetworkID();
		idMap.put(this.networkID, this);
	}

	public void setNetworkID(int id)
	{
		this.networkID = id;
		idMap.put(id, this);
	}

	public void fireBullet(Bullet b, double speed, double offset)
	{

	}

	public void layMine(Mine m)
	{

	}


	public void checkCollision()
	{
		if (this.size <= 0)
			return;

		for (int i = 0; i < Game.movables.size(); i++)
		{
			Movable m = Game.movables.get(i);

			if (m.skipNextUpdate)
				continue;

			if (this != m && m instanceof Tank && m.size > 0)
			{
				Tank t = (Tank) m;
				double distSq = Math.pow(this.posX - m.posX, 2) + Math.pow(this.posY - m.posY, 2);

                if (distSq <= Math.pow((this.size + t.size) / 2, 2) && Math.abs(this.posZ - t.posZ) < this.size + t.size)
                {
                    this.hasCollided = true;
                    t.hasCollided = true;

                    this.onCollidedWith(t, distSq);
                }
			}
		}

		hasCollided = false;

		this.size *= this.hitboxSize;

		checkCollisionWithBorder(this);

        double t = Game.tile_size;

        if (size > 1)
        {
            int x1 = (int) Math.min(Math.max(0, (this.posX - this.size / 2) / t - 1), Game.currentSizeX - 1);
            int y1 = (int) Math.min(Math.max(0, (this.posY - this.size / 2) / t - 1), Game.currentSizeY - 1);
            int x2 = (int) Math.min(Math.max(0, (this.posX + this.size / 2) / t + 1), Game.currentSizeX - 1);
            int y2 = (int) Math.min(Math.max(0, (this.posY + this.size / 2) / t + 1), Game.currentSizeY - 1);

            for (int x = x1; x <= x2; x++)
            {
                for (int y = y1; y <= y2; y++)
                {
                    checkCollisionWith(Game.obstacleGrid[x][y]);
                    checkCollisionWith(Game.surfaceTileGrid[x][y]);
                }
            }
        }

        this.size /= this.hitboxSize;
    }

	public static boolean checkCollisionWithBorder(Movable m)
	{
		boolean hasCollided = false;

		if (m.posX + m.size / 2 > Drawing.drawing.sizeX)
		{
			m.posX = Drawing.drawing.sizeX - m.size / 2;
			m.vX *= -m.bounciness;
			hasCollided = true;
		}
		if (m.posY + m.size / 2 > Drawing.drawing.sizeY)
		{
			m.posY = Drawing.drawing.sizeY - m.size / 2;
			m.vY *= -m.bounciness;
			hasCollided = true;
		}
		if (m.posX - m.size / 2 < 0)
		{
			m.posX = m.size / 2;
			m.vX *= -m.bounciness;
			hasCollided = true;
		}
		if (m.posY - m.size / 2 < 0)
		{
			m.posY = m.size / 2;
			m.vY *= -m.bounciness;
			hasCollided = true;
		}

		return hasCollided;
	}

	public void onCollidedWith(Tank t, double distSq)
    {
        double ourMass = this.size * this.size;
        double theirMass = t.size * t.size;

        double angle = this.getAngleInDirection(t.posX, t.posY);

        double ourV = Math.sqrt(this.vX * this.vX + this.vY * this.vY);
        double ourAngle = this.getPolarDirection();
        double ourParallelV = ourV * Math.cos(ourAngle - angle);
        double ourPerpV = ourV * Math.sin(ourAngle - angle);

        double theirV = Math.sqrt(t.vX * t.vX + t.vY * t.vY);
        double theirAngle = t.getPolarDirection();
        double theirParallelV = theirV * Math.cos(theirAngle - angle);
        double theirPerpV = theirV * Math.sin(theirAngle - angle);

        double newV = (ourParallelV * ourMass + theirParallelV * theirMass) / (ourMass + theirMass);

        double dist = Math.sqrt(distSq);
        this.moveInDirection(Math.cos(angle), Math.sin(angle), (dist - (this.size + t.size) / 2) * theirMass / (ourMass + theirMass));
        t.moveInDirection(Math.cos(Math.PI + angle), Math.sin(Math.PI + angle), (dist - (this.size + t.size) / 2) * ourMass / (ourMass + theirMass));

        if (distSq > Math.pow((this.posX + this.vX) - (t.posX + t.vX), 2) + Math.pow((this.posY + this.vY) - (t.posY + t.vY), 2))
        {
            this.setMotionInDirection(t.posX, t.posY, newV);
            this.addPolarMotion(angle + Math.PI / 2, ourPerpV);

            t.setMotionInDirection(this.posX, this.posY, -newV);
            t.addPolarMotion(angle + Math.PI / 2, theirPerpV);
        }
    }

    public void checkCollisionWith(Obstacle o)
	{
		hasCollided = checkCollideWith(this, o);
	}

    public static boolean checkCollideWith(Movable m, Obstacle o)
    {
        if (o == null)
            return false;

        if ((o.isSurfaceTile || !o.enableStacking) && m.posZ > 25)
            return false;

        if (!o.isSurfaceTile && !Game.lessThan(true, o.startHeight * Game.tile_size, m.posZ, o.startHeight * Game.tile_size + o.getTileHeight()))
            return false;

		boolean hasCollided = false;

        if ((!o.tankCollision && !o.checkForObjects) || o.startHeight >= 1)
            return false;

		double bounciness = m.bounciness + o.getBounciness();

        double horizontalDist = Math.abs(m.posX - o.posX);
        double verticalDist = Math.abs(m.posY - o.posY);

        double distX = m.posX - o.posX;
        double distY = m.posY - o.posY;

        double bound = m.size / 2 + Game.tile_size / 2;

        if (horizontalDist < bound && verticalDist < bound)
        {
            if (o.checkForObjects)
                o.onObjectEntry(m);

            if (!o.tankCollision)
                return false;

            if (!o.hasLeftNeighbor() && distX <= 0 && distX >= -bound && horizontalDist >= verticalDist)
            {
                hasCollided = true;
				m.vX *= -bounciness;
				m.vY *= bounciness > 1 ? bounciness : 1;
                m.posX += horizontalDist - bound;
            }
            else if (!o.hasUpperNeighbor() && distY <= 0 && distY >= -bound && horizontalDist <= verticalDist)
            {
                hasCollided = true;
				m.vY *= -bounciness;
				m.vX *= bounciness > 1 ? bounciness : 1;
                m.posY += verticalDist - bound;
            }
            else if (!o.hasRightNeighbor() && distX >= 0 && distX <= bound && horizontalDist >= verticalDist)
            {
                hasCollided = true;
				m.vX *= -bounciness;
				m.vY *= bounciness > 1 ? bounciness : 1;
                m.posX -= horizontalDist - bound;
            }
            else if (!o.hasLowerNeighbor() && distY >= 0 && distY <= bound && horizontalDist <= verticalDist)
            {
                hasCollided = true;
				m.vY *= -bounciness;
				m.vX *= bounciness > 1 ? bounciness : 1;
                m.posY -= verticalDist - bound;
            }
        }

		return hasCollided;
    }

    @Override
    public void preUpdate()
    {
        if (this.posZ != this.lastPosZ)
        {
            if (Math.abs(this.posZ) < disabledZ && Math.abs(this.lastPosZ) >= disabledZ)
                this.disabled = this.targetable = false;

            if (Math.abs(this.posZ) >= disabledZ && Math.abs(this.lastPosZ) < disabledZ)
                this.disabled = this.targetable = true;
        }

        super.preUpdate();
    }

    @Override
    public void update()
    {
        if (this.networkID < 0)
        {
            // If you get this crash, please make sure you call Game.addTank() to add them to movables, or use registerNetworkID()!
            Game.exitToCrash(new RuntimeException("Network ID not assigned to tank!"));
        }

        if (this.age <= 0)
        {
            if (this.resistFreeze)
                this.attributeImmunities.addAll(Arrays.asList("ice_slip", "ice_accel", "ice_max_speed", "freeze"));
        }

        this.age += Panel.frameFrequency;

        if ((this.posZ < 0 || buoyancy != 0) && this.getSpeed() > 0.1)
        {
            double groundHeight = -9999;

            double s = this.size * this.hitboxSize / 2 + 10;

            for (double x = this.posX - s; x <= this.posX + s; x += Game.tile_size)
            {
                for (double y = this.posY - s; y <= this.posY + s; y += Game.tile_size)
                    groundHeight = Math.max(groundHeight, Game.sampleTerrainGroundHeight(x, y));
            }

            this.posZ = Math.min(0, Math.max(groundHeight, this.posZ + buoyancy * Panel.frameFrequency));
        }

        this.treadAnimation += Math.sqrt(this.lastFinalVX * this.lastFinalVX + this.lastFinalVY * this.lastFinalVY) * Panel.frameFrequency;

        if (this.enableTracks && this.treadAnimation > this.size * this.trackSpacing && !this.destroy)
        {
            this.drawTread = true;

            if (this.size > 0)
                this.treadAnimation %= this.size * this.trackSpacing;
        }

		this.flashAnimation = Math.max(0, this.flashAnimation - 0.05 * Panel.frameFrequency);

		if (destroy)
		{
			if (this.destroyTimer <= 0)
			{
				Game.eventsOut.add(new EventTankUpdateHealth(this));
				this.unregisterNetworkID();
			}

			if (this.destroyTimer <= 0 && this.health <= 0)
			{
				Drawing.drawing.playSound("destroy.ogg", (float) (Game.tile_size / this.size));

				this.onDestroy();

				if (Game.effectsEnabled)
				{
					for (int i = 0; i < this.size * 2 * Game.effectMultiplier; i++)
					{
						Effect e = Effect.createNewEffect(this.posX, this.posY, this.size / 4, Effect.EffectType.piece);
						double var = 50;

						e.colR = Math.min(255, Math.max(0, this.colorR + Math.random() * var - var / 2));
						e.colG = Math.min(255, Math.max(0, this.colorG + Math.random() * var - var / 2));
						e.colB = Math.min(255, Math.max(0, this.colorB + Math.random() * var - var / 2));

						if (Game.enable3d)
							e.set3dPolarMotion(Math.random() * 2 * Math.PI, Math.atan(Math.random()), Math.random() * this.size / 50.0);
						else
							e.setPolarMotion(Math.random() * 2 * Math.PI, Math.random() * this.size / 50.0);

						Game.effects.add(e);
					}
				}
			}

			this.destroyTimer += Panel.frameFrequency;
		}

		if (this.destroyTimer > Game.tile_size)
			Game.removeMovables.add(this);

		if (this.drawTread)
		{
			this.drawTread = false;
			this.drawTread();
		}

		this.accelerationModifier = 1;
		this.frictionModifier = 1;
		this.maxSpeedModifier = 1;

		double boost = 0;
		for (int i = 0; i < this.attributes.size(); i++)
		{
			AttributeModifier a = this.attributes.get(i);

			if (a.name.equals("healray"))
			{
				if (this.health < this.baseHealth)
				{
					this.attributes.remove(a);
                    i--;
                }
            }
        }

        this.accelerationModifier = this.getAttributeValue(AttributeModifier.acceleration, this.accelerationModifier);

        if (!(this instanceof TankAIControlled))
            this.frictionModifier = this.getAttributeValue(AttributeModifier.friction, this.frictionModifier);

        this.buoyancy = this.getAttributeValue(AttributeModifier.buoyancy, this.buoyancy);

        this.damageRate = this.getAttributeValue(AttributeModifier.damage, this.damageRate);
        if (this.damageRate > 0.01)
        {
            this.health -= this.damageRate / 100 * Panel.frameFrequency;
            this.flashAnimation = 1;
            this.damageRate = 0;
        }

        this.maxSpeedModifier = this.getAttributeValue(AttributeModifier.max_speed, this.maxSpeedModifier);

		boost = this.getAttributeValue(AttributeModifier.ember_effect, boost);

		if (!ScreenGame.finished && Math.random() * Panel.frameFrequency < boost * Game.effectMultiplier && Game.effectsEnabled)
		{
			Effect e = Effect.createNewEffect(this.posX, this.posY, Game.tile_size / 2, Effect.EffectType.piece);
			double var = 50;

			e.colR = Math.min(255, Math.max(0, 255 + Math.random() * var - var / 2));
			e.colG = Math.min(255, Math.max(0, 180 + Math.random() * var - var / 2));
			e.colB = Math.min(255, Math.max(0, 0 + Math.random() * var - var / 2));

			if (Game.enable3d)
				e.set3dPolarMotion(Math.random() * 2 * Math.PI, Math.random() * Math.PI, Math.random());
			else
				e.setPolarMotion(Math.random() * 2 * Math.PI, Math.random());

			Game.effects.add(e);
		}

		super.update();

		if (this.health <= 0)
			this.destroy = true;

		this.checkCollision();

		if (!this.collisionPush)
		{
			this.posX = this.lastPosX;
			this.posY = this.lastPosY;
		}

		this.orientation = (this.orientation + Math.PI * 2) % (Math.PI * 2);

		if (this.collisionPush && !(Math.abs(this.posX - this.lastPosX) < 0.01 && Math.abs(this.posY - this.lastPosY) < 0.01) && !this.destroy && !ScreenGame.finished)
		{
			double dist = Math.sqrt(Math.pow(this.posX - this.lastPosX, 2) + Math.pow(this.posY - this.lastPosY, 2));

			double dir = Math.PI + this.getAngleInDirection(this.lastPosX, this.lastPosY);
			if (Movable.absoluteAngleBetween(this.orientation, dir) <= Movable.absoluteAngleBetween(this.orientation + Math.PI, dir))
				this.orientation -= Movable.angleBetween(this.orientation, dir) / 20 * dist;
			else
				this.orientation -= Movable.angleBetween(this.orientation + Math.PI, dir) / 20 * dist;
		}

		if (!this.isRemote && this.standardUpdateEvent && shouldUpdate && ScreenPartyHost.isServer)
			sendUpdateEvent();

		this.canHide = true;
		for (int i = 0; i < this.canHidePoints.length; i++)
		{
			for (int j = 0; j < this.canHidePoints[i].length; j++)
			{
				canHide = canHide && canHidePoints[i][j];
				canHidePoints[i][j] = false;
			}
		}

		this.hidden = true;
		for (int i = 0; i < this.hiddenPoints.length; i++)
		{
			for (int j = 0; j < this.hiddenPoints[i].length; j++)
			{
				hidden = hidden && hiddenPoints[i][j];
				hiddenPoints[i][j] = false;
			}
		}

		if (this.hasCollided)
		{
			this.tookRecoil = false;
			this.inControlOfMotion = true;
		}

		if (this.possessor != null)
			this.possessor.updatePossessing();
	}

	public void drawTread()
	{
		double a = this.getPolarDirection();
		Effect e1 = Effect.createNewEffect(this.posX, this.posY, Effect.EffectType.tread);
		Effect e2 = Effect.createNewEffect(this.posX, this.posY, Effect.EffectType.tread);
		e1.setPolarMotion(a - Math.PI / 2, this.size * 0.25);
		e2.setPolarMotion(a + Math.PI / 2, this.size * 0.25);
        e1.size = this.size / 5;
        e2.size = this.size / 5;
        e1.posX += e1.vX;
        e1.posY += e1.vY;
        e2.posX += e2.vX;
        e2.posY += e2.vY;
        e1.angle = a;
        e2.angle = a;
        e1.setPolarMotion(0, 0);
        e2.setPolarMotion(0, 0);
        this.setTrackHeight(e1);
        this.setTrackHeight(e2);
        Game.tracks.add(e1);
        Game.tracks.add(e2);
    }

	public void drawForInterface(double x, double y, double sizeMul)
	{
        double s = this.size;

        if (this.size > Game.tile_size * 1.5)
            this.size = Game.tile_size * 1.5;

        this.size *= sizeMul;
        this.drawForInterface(x, y);
        this.size = s;
    }

    @Override
    public void postInitSelectors()
    {
        this.teamSelector = (TeamSelector<Tank>) this.selectors.get(0);
        this.rotationSelector = (RotationSelector<Tank>) this.selectors.get(1);
    }

    @Override
    public void drawForInterface(double x, double y)
    {
        double x1 = this.posX;
        double y1 = this.posY;
        this.posX = x;
        this.posY = y;
        this.drawTank(true, false);
        this.posX = x1;
		this.posY = y1;	
	}

	public void drawTank(boolean forInterface, boolean interface3d)
	{
		double luminance = this.getAttributeValue(AttributeModifier.glow, this.luminance);
		double glow = this.getAttributeValue(AttributeModifier.glow, 1);

		double s = (this.size * (Game.tile_size - destroyTimer) / Game.tile_size) * Math.min(this.drawAge / Game.tile_size, 1);
		double sizeMod = 1;

		if (forInterface && !interface3d)
			s = Math.min(this.size, Game.tile_size * 1.5);

		Drawing drawing = Drawing.drawing;
		double[] teamColor = Team.getObjectColor(this.secondaryColorR, this.secondaryColorG, this.secondaryColorB, this);

		Drawing.drawing.setColor(teamColor[0] * glow * this.glowIntensity, teamColor[1] * glow * this.glowIntensity, teamColor[2] * glow * this.glowIntensity, 255, 1);

		if (Game.glowEnabled)
		{
			double size = this.glowSize * s;
			if (forInterface)
				Drawing.drawing.fillInterfaceGlow(this.posX, this.posY, size, size);
			else if (!Game.enable3d)
				Drawing.drawing.fillGlow(this.posX, this.posY, size, size);
			else
				Drawing.drawing.fillGlow(this.posX, this.posY, Math.max(this.size / 4, 11), size, size,true, false);
		}

		if (this.lightIntensity > 0 && this.lightSize > 0)
		{
			double i = this.lightIntensity;

			while (i > 0)
			{
				double size = this.lightSize * s * i / this.lightIntensity;
				Drawing.drawing.setColor(255, 255, 255, i * 255);

				if (!(forInterface && !interface3d))
					Drawing.drawing.fillForcedGlow(this.posX, this.posY, 0, size, size, false, false, false, true);

				i--;
			}
		}

		if (this.fullBrightness)
			luminance = 1;

		if (!forInterface)
		{
			for (AttributeModifier a : this.attributes)
			{
				if (a.name.equals("healray"))
				{
					double mod = 1 + 0.4 * Math.min(1, this.health - this.baseHealth);

					if (this.health > this.baseHealth)
					{
						if (!Game.enable3d)
						{
							Drawing.drawing.setColor(0, 255, 0, 255, 1);
							drawing.drawModel(this.baseModel, this.posX, this.posY, s * mod, s * mod, this.orientation);
						}
						else
						{
							Drawing.drawing.setColor(0, 255, 0, 127, 1);
							drawing.drawModel(this.baseModel, this.posX, this.posY, this.posZ, s * mod, s * mod, s - 2, this.orientation);
						}
					}
				}
			}
		}

		Drawing.drawing.setColor(teamColor[0], teamColor[1], teamColor[2], 255, luminance);

		if (forInterface)
		{
			if (interface3d)
				drawing.drawInterfaceModel(this.baseModel, this.posX, this.posY, this.posZ, s, s, s, this.orientation, 0, 0);
			else
				drawing.drawInterfaceModel(this.baseModel, this.posX, this.posY, s, s, this.orientation);
		}
		else
		{
			if (Game.enable3d)
				drawing.drawModel(this.baseModel, this.posX, this.posY, this.posZ, s, s, s, this.orientation);
			else
				drawing.drawModel(this.baseModel, this.posX, this.posY, s, s, this.orientation);
		}


		double flash = Math.min(1, this.flashAnimation);

		Drawing.drawing.setColor(this.colorR * (1 - flash) + 255 * flash, this.colorG * (1 - flash), this.colorB * (1 - flash), 255, luminance);

		if (forInterface)
		{
			if (interface3d)
				drawing.drawInterfaceModel(this.colorModel, this.posX, this.posY, this.posZ, s * sizeMod, s * sizeMod, s * sizeMod, this.orientation, 0, 0);
			else
				drawing.drawInterfaceModel(this.colorModel, this.posX, this.posY, s * sizeMod, s * sizeMod, this.orientation);
		}
		else
		{
			if (Game.enable3d)
				drawing.drawModel(this.colorModel, this.posX, this.posY, this.posZ, s, s, s, this.orientation);
			else
				drawing.drawModel(this.colorModel, this.posX, this.posY, s, s, this.orientation);
		}

		if (this.health > 1 && this.size > 0 && !forInterface)
		{
			double size = s;
			for (int i = 1; i < Math.min(health, 6); i++)
			{
				if (Game.enable3d)
					drawing.drawModel(health_model,
							this.posX, this.posY, this.posZ + s / 4,
							size, size, s,
							this.orientation, 0, 0);
				else
					drawing.drawModel(health_model,
							this.posX, this.posY,
							size, size,
							this.orientation);

				size *= 1.1;
			}
		}

		this.drawTurret(forInterface, interface3d || (!forInterface && Game.enable3d), false);

		sizeMod = 0.5;

		Drawing.drawing.setColor(this.emblemR, this.emblemG, this.emblemB, 255, luminance);
		if (this.emblem != null)
		{
			if (forInterface)
			{
				if (interface3d)
					drawing.drawInterfaceImage(0, this.emblem, this.posX, this.posY, 0.82 * s, s * sizeMod, s * sizeMod);
				else
					drawing.drawInterfaceImage(this.emblem, this.posX, this.posY, s * sizeMod, s * sizeMod);
			}
			else
			{
				if (Game.enable3d)
                    drawing.drawImage(this.angle, this.emblem, this.posX, this.posY, this.posZ + 0.82 * s, s * sizeMod, s * sizeMod);
				else
					drawing.drawImage(this.angle, this.emblem, this.posX, this.posY, s * sizeMod, s * sizeMod);
			}
		}

		if (Game.showTankIDs)
		{
			Drawing.drawing.setColor(0, 0, 0);
			Drawing.drawing.setFontSize(30);
			Drawing.drawing.drawText(this.posX, this.posY, 50, this.networkID + "");
		}

		Drawing.drawing.setColor(this.secondaryColorR, this.secondaryColorG, this.secondaryColorB);
	}

	public void drawTurret(boolean forInterface, boolean in3d, boolean transparent)
	{
		this.turret.draw(angle, pitch, forInterface, in3d, transparent);
	}

	@Override
	public void draw()
	{
		if (!Game.game.window.drawingShadow)
			drawAge += Panel.frameFrequency;

		this.updateSelectors();

		this.drawTank(false, false);

		if (this.possessor != null)
		{
			this.possessor.drawPossessing();
			this.possessor.drawGlowPossessing();
		}
	}

	public void drawOutline() 
	{
		drawAge = Game.tile_size;
		Drawing drawing = Drawing.drawing;

		Drawing.drawing.setColor(this.colorR, this.colorG, this.colorB, 127);
		drawing.fillRect(this.posX - this.size * 0.4, this.posY, this.size * 0.2, this.size);
		drawing.fillRect(this.posX + this.size * 0.4, this.posY, this.size * 0.2, this.size);
		drawing.fillRect(this.posX, this.posY - this.size * 0.4, this.size * 0.6, this.size * 0.2);
		drawing.fillRect(this.posX, this.posY + this.size * 0.4, this.size * 0.6, this.size * 0.2);

		this.drawTurret(false, false, true);

		if (this.emblem != null)
		{
			Drawing.drawing.setColor(this.emblemR, this.emblemG, this.emblemB, 127);
			drawing.drawImage(this.angle, this.emblem, this.posX, this.posY, this.size / 2, this.size / 2);
		}

		Drawing.drawing.setColor(this.secondaryColorR, this.secondaryColorG, this.secondaryColorB);
	}

	public void drawAt(double x, double y)
	{	
		double x1 = this.posX;
		double y1 = this.posY;
		this.posX = x;
		this.posY = y;
		this.drawTank(false, false);
		this.posX = x1;
		this.posY = y1;	
	}

	public void drawOutlineAt(double x, double y)
	{
		double x1 = this.posX;
		double y1 = this.posY;
		this.posX = x;
		this.posY = y;
		this.drawOutline();
		this.posX = x1;
		this.posY = y1;
	}

	@Override
	public void addAttribute(AttributeModifier m)
	{
		super.addAttribute(m);

		if (!this.isRemote)
			Game.eventsOut.add(new EventTankAddAttributeModifier(this, m, false));
	}

	@Override
	public void addUnduplicateAttribute(AttributeModifier m)
	{
		super.addUnduplicateAttribute(m);

		if (!this.isRemote)
			Game.eventsOut.add(new EventTankAddAttributeModifier(this, m, true));
	}

	public void onDestroy()
	{
		if (this.explodeOnDestroy)
		{
			Explosion e = new Explosion(this.posX, this.posY, Mine.mine_radius, 2, true, this);
			e.explode();
		}
	}

	@Override
	public Face[] getHorizontalFaces()
	{
		double s = this.size * this.hitboxSize / 2;

		if (this.horizontalFaces == null)
		{
			this.horizontalFaces = new Face[2];
			this.horizontalFaces[0] = new Face(this, this.posX - s, this.posY - s, this.posX + s, this.posY - s, true, true, true, true);
			this.horizontalFaces[1] = new Face(this, this.posX - s, this.posY + s, this.posX + s, this.posY + s, true, false,true, true);
		}
		else
		{
			this.horizontalFaces[0].update(this.posX - s, this.posY - s, this.posX + s, this.posY - s);
			this.horizontalFaces[1].update(this.posX - s, this.posY + s, this.posX + s, this.posY + s);
		}

		return this.horizontalFaces;
	}

	@Override
	public Face[] getVerticalFaces()
	{
		double s = this.size * this.hitboxSize / 2;

		if (this.verticalFaces == null)
		{
			this.verticalFaces = new Face[2];
			this.verticalFaces[0] = new Face(this, this.posX - s, this.posY - s, this.posX - s, this.posY + s, false, true, true, true);
			this.verticalFaces[1] = new Face(this, this.posX + s, this.posY - s, this.posX + s, this.posY + s, false, false, true, true);
		}
		else
		{
			this.verticalFaces[0].update(this.posX - s, this.posY - s, this.posX - s, this.posY + s);
			this.verticalFaces[1].update(this.posX + s, this.posY - s, this.posX + s, this.posY + s);
		}

		return this.verticalFaces;
	}

    public boolean damage(double amount, GameObject source)
    {
        this.health -= amount * this.getDamageMultiplier(source);

        if (this.health <= 1)
        {
            for (int i = 0; i < this.attributes.size(); i++)
            {
                if (this.attributes.get(i).type.name.equals("healray"))
                {
                    this.attributes.remove(i);
					i--;
				}
			}
		}

		Game.eventsOut.add(new EventTankUpdateHealth(this));

		Tank owner = null;

		if (source instanceof Bullet)
			owner = ((Bullet) source).tank;
		else if (source instanceof Explosion)
			owner = ((Explosion) source).tank;
		else if (source instanceof Tank)
			owner = (Tank) source;

		if (this.health > 0)
			this.flashAnimation = 1;
		else
			this.destroy = true;

		this.checkHit(owner, source);

		if (this.health > 6 && (int) (this.health + amount) != (int) (this.health))
		{
			Effect e = Effect.createNewEffect(this.posX, this.posY, this.posZ + this.size * 0.75, Effect.EffectType.shield);
			e.size = this.size;
			e.radius = this.health - 1;
			Game.effects.add(e);
		}

		return this.health <= 0;
	}

    public void checkHit(Tank owner, GameObject source)
    {
        if (Crusade.crusadeMode && Crusade.currentCrusade != null && !ScreenPartyLobby.isClient)
        {
            if (owner instanceof IServerPlayerTank)
            {
                CrusadePlayer cp = Crusade.currentCrusade.getCrusadePlayer(((IServerPlayerTank) owner).getPlayer());

                if (cp != null && this.health <= 0)
                {
                    if (this.possessor != null && this.possessor.overridePossessedKills)
						cp.addKill(this.getTopLevelPossessor());
					else
						cp.addKill(this);
				}

				if (cp != null && (source instanceof Bullet || source instanceof Explosion))
					cp.addItemHit(source);
			}

			if (owner != null && this instanceof IServerPlayerTank && this.health <= 0)
			{
				CrusadePlayer cp = Crusade.currentCrusade.getCrusadePlayer(((IServerPlayerTank) this).getPlayer());

				if (cp != null)
				{
					if (owner.possessor != null && owner.possessor.overridePossessedKills)
						cp.addDeath(owner.getTopLevelPossessor());
					else
						cp.addDeath(owner);
				}
			}
		}
	}

    public double getDamageMultiplier(GameObject source)
    {
        if (this.invulnerable || (source instanceof Bullet && this.resistBullets) || (source instanceof Explosion && this.resistExplosions))
            return 0;

        return 1;
    }

    public void setTrackHeight(Effect e)
    {
        if (Game.enable3d && Game.enable3dBg && Game.glowEnabled)
        {
            if (Math.abs(this.posZ) < 5)
            {
                e.posZ = Math.max(e.posZ, Game.sampleTerrainGroundHeight(e.posX - e.size / 2, e.posY - e.size / 2));
                e.posZ = Math.max(e.posZ, Game.sampleTerrainGroundHeight(e.posX + e.size / 2, e.posY - e.size / 2));
                e.posZ = Math.max(e.posZ, Game.sampleTerrainGroundHeight(e.posX - e.size / 2, e.posY + e.size / 2));
                e.posZ = Math.max(e.posZ, Game.sampleTerrainGroundHeight(e.posX + e.size / 2, e.posY + e.size / 2));
                e.posZ++;
            }
            else
                e.posZ = this.posZ;
        }
        else
            e.posZ = 1;
    }

    @Override
    public void registerSelectors()
    {
        this.registerSelector(new TeamSelector<Tank>());
        this.registerSelector(new RotationSelector<Tank>());
    }

    public void updatePossessing()
    {

    }

    public void drawPossessing()
    {

    }

	public void drawGlowPossessing()
	{

	}

	public double getAutoZoomRaw()
	{
		double nearest = Double.MAX_VALUE;

		double farthestInSight = -1;

		for (Movable m: Game.movables)
		{
			if (m instanceof TankAIControlled && !((TankAIControlled) m).isSupportTank() && !Team.isAllied(m, this) && m != this && !((Tank) m).hidden && !m.destroy)
			{
				double boundedX = Math.min(Math.max(this.posX, Drawing.drawing.interfaceSizeX * 0.4),
						Game.currentSizeX * Game.tile_size - Drawing.drawing.interfaceSizeX * 0.4);
				double boundedY = Math.min(Math.max(this.posY, Drawing.drawing.interfaceSizeY * 0.4),
						Game.currentSizeY * Game.tile_size - Drawing.drawing.interfaceSizeY * 0.4);

				double xDist = Math.abs(m.posX - boundedX);
				double yDist = Math.abs(m.posY - boundedY);
				double dist = Math.max(xDist / (Drawing.drawing.interfaceSizeX),
						yDist / (Drawing.drawing.interfaceSizeY)) * 3;

				if (dist < nearest)
				{
					nearest = dist;
				}

				if (dist <= 3.5 && dist > farthestInSight)
				{
					Ray r = new Ray(this.posX, this.posY, 0, 0, this);
					r.vX = m.posX - this.posX;
					r.vY = m.posY - this.posY;

					if ((m == this.lastFarthestInSight && System.currentTimeMillis() - this.lastFarthestInSightUpdate <= 1000)
							|| r.getTarget() == m)
					{
						farthestInSight = dist;
						this.lastFarthestInSight = (Tank) m;
						this.lastFarthestInSightUpdate = System.currentTimeMillis();
					}
				}
			}
		}

		return Math.max(nearest, farthestInSight);
	}

	public void setMetadata(String s)
	{
		String[] data = s.split("-");

		for (int i = 0; i < Math.min(data.length, this.selectorCount()); i++)
		{
			LevelEditorSelector<Tank> sel = (LevelEditorSelector<Tank>) this.selectors.get(saveOrder(i));
			sel.setMetadata(data[i]);
		}
	}

	public String getMetadata()
	{
		StringBuilder s = new StringBuilder();
		int sc = this.selectorCount();
		for (int i = 0; i < sc; i++)
			s.append(this.selectors.get(saveOrder(i)).getMetadata()).append("-");

		String s1 = s.toString();
		if (s1.endsWith("-"))
			return s1.substring(0, s1.length() - 1);
		return s1;
	}

	/** Override this method if both the server and clients support a custom creation event for your modded tank. */
	public void sendCreateEvent()
	{
		Game.eventsOut.add(new EventCreateTank(this));
	}

	/** Override this method if both the server and clients support a custom update event for your modded tank. */
	public void sendUpdateEvent()
	{
		Game.eventsOut.add(new EventTankUpdate(this));
	}

	public double getAutoZoom()
	{
		double dist = Math.min(3, Math.max(1, getAutoZoomRaw()));
		return 1 / dist;
	}

	public void setBufferCooldown(double value)
	{
		this.bullet.cooldown = Math.max(this.bullet.cooldown, value);
		this.mine.cooldown = Math.max(this.mine.cooldown, value);
	}

	/** This is for backwards compatibility with the base game. */
	public int saveOrder(int index)
	{
		if (index < 2)
			return 1 - index;
		return index;
	}

	public Tank getTopLevelPossessor()
	{
		if (this.possessor == null)
			return null;
		else
		{
			Tank p = this.possessor;
			while (p.possessor != null)
			{
				p = p.possessor;
			}

			return p;
		}
	}

	public Tank getBottomLevelPossessing()
	{
		Tank p = this;
		while (p.possessingTank != null)
		{
			p = p.possessingTank;
		}

		return p;
	}
}
