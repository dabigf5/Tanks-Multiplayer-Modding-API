package tanks.tank;

import tanks.*;
import tanks.bullet.Bullet;
import tanks.gui.screen.ScreenGame;
import tanks.obstacle.Face;
import tanks.obstacle.Obstacle;

import java.util.ArrayList;
import java.util.TreeSet;

public class Ray
{
	public double size = 10;
	public double tankHitSizeMul = 1;

	public int bounces;
	public int bouncyBounces = 100;
	public double posX;
	public double posY;
	public double vX;
	public double vY;
	public double angle;

	public int maxChunkCheck = 12;

	public boolean enableBounciness = true;
	public boolean ignoreTanks = false, ignoreBullets = true;
	public boolean ignoreDestructible = false;
	public boolean ignoreShootThrough = false;

	public boolean trace = Game.traceAllRays;
	public boolean dotted = false;
	public TreeSet<Chunk> chunksToCheck = new TreeSet<>();

	public double speed = 10;

	public double age = 0;
	public int traceAge;

	public Tank tank;
	public Tank targetTank;

	public ArrayList<Double> bounceX = new ArrayList<>();
	public ArrayList<Double> bounceY = new ArrayList<>();

	public double targetX;
	public double targetY;
	public boolean acquiredTarget = false;

	public Ray(double x, double y, double angle, int bounces, Tank tank)
	{
		this.vX = speed * Math.cos(angle);
		this.vY = speed * Math.sin(angle);
		this.angle = angle;

		this.posX = x;
		this.posY = y;
		this.bounces = bounces;

		this.tank = tank;
	}

	public Ray(double x, double y, double angle, int bounces, Tank tank, double speed)
	{
		this.vX = speed * Math.cos(angle);
		this.vY = speed * Math.sin(angle);
		this.angle = angle;

		this.posX = x;
		this.posY = y;
		this.bounces = bounces;

		this.tank = tank;
	}

	public Movable getTarget(double mul, Tank targetTank)
	{
		this.targetTank = targetTank;
		this.targetTank.removeFaces();
		this.targetTank.size *= mul;
		this.targetTank.addFaces();

		Movable m = this.getTarget();

		this.targetTank.removeFaces();
		this.targetTank.size /= mul;
		this.targetTank.addFaces();

		return m;
	}

	public Ray setMaxChunks(int maxChunks)
	{
		this.maxChunkCheck = maxChunks;
		return this;
	}

	public Ray setMaxDistance(double distance)
	{
		setMaxChunks((int) (distance / Game.tile_size / Chunk.chunkSize + 1));
		return this;
	}

	public Ray setSize(double size)
	{
		this.size = size;
		return this;
	}

	public Ray moveOut(double amount)
	{
		this.posX += this.vX * amount;
		this.posY += this.vY * amount;
		return this;
	}

	public Movable getTarget()
	{
		double remainder = 0;
		acquiredTarget = true;

		if (isInsideObstacle(this.posX - size / 2, this.posY - size / 2) ||
				isInsideObstacle(this.posX + size / 2, this.posY - size / 2) ||
				isInsideObstacle(this.posX + size / 2, this.posY + size / 2) ||
				isInsideObstacle(this.posX - size / 2, this.posY + size / 2))
			return null;

		if (!ignoreTanks)
		{
			for (Movable m : Chunk.getChunk(posX, posY).movables)
			{
				if (m instanceof Tank t && m != this.tank)
				{
					if (this.posX + this.size / 2 >= t.posX - t.size / 2 &&
							this.posX - this.size / 2 <= t.posX + t.size / 2 &&
							this.posY + this.size / 2 >= t.posY - t.size / 2 &&
							this.posY - this.size / 2 <= t.posY + t.size / 2)
						return t;
				}
			}
		}

		boolean firstBounce = this.targetTank == null;
		int totalChunksChecked = 0;

		while (this.bounces >= 0 && this.bouncyBounces >= 0)
		{
			double collisionX = -1;
			double collisionY = -1;
			Result result = null;
			Chunk current = Chunk.getChunk(posX, posY);
			if (current == null)
				return null;

			chunkCheck : for (int chunksChecked = 0; chunksChecked < maxChunkCheck; chunksChecked++)
			{
				double moveXBase = Chunk.chunkSize * Game.tile_size * Math.cos(angle);
				double moveYBase = Chunk.chunkSize * Game.tile_size * Math.sin(angle);
				double moveX = moveXBase * chunksChecked, moveXPrev = moveXBase * Math.max(0, chunksChecked - 1);
				double moveY = moveYBase * chunksChecked, moveYPrev = moveYBase * Math.max(0, chunksChecked - 1);

				// todo: make this slightly more efficient
				chunksToCheck.clear();
				Chunk mid = chunksChecked > 0 ? Chunk.getChunk(posX + moveX, posY + moveY) : current;
				addChunks(current, mid);

				if (mid == null || current.manhattanDist(mid) > 1)
                    addChunks(current,
							Chunk.getChunk(posX + moveXPrev, posY + moveY),
							Chunk.getChunk(posX + moveX, posY + moveYPrev)
					);

				if (chunksToCheck.isEmpty())
					break;

				for (Chunk chunk : chunksToCheck)
				{
					if (chunk == null)
						continue;

					totalChunksChecked++;

					if (Chunk.debug && trace && bounces == 1)
					{
						Game.effects.add(Effect.createNewEffect(
								(chunk.chunkX + 0.5) * Chunk.chunkSize * Game.tile_size + (totalChunksChecked * 5),
								(chunk.chunkY + 0.5) * Chunk.chunkSize * Game.tile_size,
								150, Effect.EffectType.chain, 85
						).setRadius(totalChunksChecked));

						Game.effects.add(Effect.createNewEffect(posX + moveX, posY + moveY, 20, Effect.EffectType.laser));

						if (mid == null || current.manhattanDist(mid) > 1)
						{
							Game.effects.add(Effect.createNewEffect(posX, posY + moveY, 20, Effect.EffectType.obstaclePiece));
							Game.effects.add(Effect.createNewEffect(posX + moveX, posY, 20, Effect.EffectType.piece));
						}
					}

					Result dynamic = checkCollisionIn(chunk.faces, firstBounce, collisionX, collisionY);
					Result stat = checkCollisionIn(chunk.staticFaces, firstBounce, collisionX, collisionY);

					if (dynamic.collisionFace != null && stat.collisionFace != null)
					{
						boolean greater = dynamic.collisionFace.compareTo(stat.collisionFace) > 0;
						if (dynamic.collisionFace.horizontal ? vY > 0 : vX > 0)
							greater = !greater;
						result = greater ? dynamic : stat;
					}
					else
						result = dynamic.collisionFace != null ? dynamic : stat;

					collisionX = result.collisionX;
					collisionY = result.collisionY;

					if (result.collisionFace != null)
						break chunkCheck;
				}
			}

			if (result == null)
				return null;

			this.age += result.t();

			firstBounce = false;

			if (result.collisionFace() != null)
			{
				if (trace && ScreenGame.isUpdatingGame())
				{
					double dx = result.collisionX() - posX;
					double dy = result.collisionY() - posY;

					double steps = (Math.sqrt((Math.pow(dx, 2) + Math.pow(dy, 2)) / (1 + Math.pow(this.vX, 2) + Math.pow(this.vY, 2))) + 1);

					if (dotted)
						steps /= 2;

					double s;
					for (s = remainder; s <= steps; s++)
					{
						double x = posX + dx * s / steps;
						double y = posY + dy * s / steps;

						this.traceAge++;

						double frac = 1 / (1 + this.traceAge / 100.0);
						double z = this.tank.size / 2 + this.tank.turretSize / 2 * frac + (Game.tile_size / 4) * (1 - frac);
						if (Game.screen instanceof ScreenGame && !ScreenGame.finished)
							Game.effects.add(Effect.createNewEffect(x, y, z, Effect.EffectType.ray));
					}

					remainder = s - steps;
				}

				this.posX = result.collisionX();
				this.posY = result.collisionY();

				if (result.collisionFace().owner instanceof Movable m)
				{
					this.targetX = result.collisionX();
					this.targetY = result.collisionY();
					bounceX.add(result.collisionX());
					bounceY.add(result.collisionY());

					return m;
				}
				else if (result.collisionFace().owner instanceof Obstacle o && o.bouncy)
					this.bouncyBounces--;
				else if (result.collisionFace().owner instanceof Obstacle o && !o.allowBounce)
					this.bounces = -1;
				else
					this.bounces--;

				bounceX.add(result.collisionX());
				bounceY.add(result.collisionY());

				if (this.bounces >= 0)
				{
					if (result.corner())
					{
						this.vX = -this.vX;
						this.vY = -this.vY;
					}
					else if (result.collisionFace().horizontal)
						this.vY = -this.vY;
					else
						this.vX = -this.vX;

					this.angle = Movable.getPolarDirection(this.vX, this.vY);    // i hate quadrants
				}
			}
			else
				return null;
		}

		return null;
	}

	public Result checkCollisionIn(Chunk.FaceList faceList, boolean firstBounce, double collisionX, double collisionY)
	{
		Face collisionFace = null;
		double t = Double.MAX_VALUE;
		boolean corner = false;

		if (vX > 0)
		{
			for (Face f : faceList.leftFaces)
			{
				double size = this.size;

				if (f.owner instanceof Movable)
					size *= tankHitSizeMul;

				if (passesThrough(f))
					continue;

				if (f.startX < this.posX + size / 2 || !f.solidBullet || (f.owner == this.tank && firstBounce))
					continue;

				double y = (f.startX - size / 2 - this.posX) * vY / vX + this.posY;
				if (y >= f.startY - size / 2 && y <= f.endY + size / 2)
				{
					t = (f.startX - size / 2 - this.posX) / vX;
					collisionX = f.startX - size / 2;
					collisionY = y;
					collisionFace = f;
					break;
				}
			}
		}
		else if (vX < 0)
		{
			for (Face f : faceList.rightFaces.descendingSet())
			{
				double size = this.size;

				if (f.owner instanceof Movable)
					size *= tankHitSizeMul;

				if (passesThrough(f))
					continue;

				if (f.startX > this.posX - size / 2 || !f.solidBullet || (f.owner == this.tank && firstBounce))
					continue;

				double y = (f.startX + size / 2 - this.posX) * vY / vX + this.posY;
				if (y >= f.startY - size / 2 && y <= f.endY + size / 2)
				{
					t = (f.startX + size / 2 - this.posX) / vX;
					collisionX = f.startX + size / 2;
					collisionY = y;
					collisionFace = f;
					break;
				}
			}
		}

		if (vY > 0)
		{
			for (Face f : faceList.topFaces)
			{
				double size = this.size;

				if (f.owner instanceof Movable)
					size *= tankHitSizeMul;

				if (passesThrough(f))
					continue;

				if (f.startY < this.posY + size / 2 || !f.solidBullet || (f.owner == this.tank && firstBounce))
					continue;

				double x = (f.startY - size / 2 - this.posY) * vX / vY + this.posX;
				if (x >= f.startX - size / 2 && x <= f.endX + size / 2)
				{
					double t1 = (f.startY - size / 2 - this.posY) / vY;

					if (t1 == t)
						corner = true;
					else if (t1 < t)
					{
						collisionX = x;
						collisionY = f.startY - size / 2;
						collisionFace = f;
						t = t1;
					}
					break;
				}
			}
		}
		else if (vY < 0)
		{
			for (Face f : faceList.bottomFaces.descendingSet())
			{
				double size = this.size;

				if (f.owner instanceof Movable)
					size *= tankHitSizeMul;

				if (passesThrough(f))
					continue;

				if (f.startY > this.posY - size / 2 || !f.solidBullet || (f.owner == this.tank && firstBounce))
					continue;

				double x = (f.startY + size / 2 - this.posY) * vX / vY + this.posX;
				if (x >= f.startX - size / 2 && x <= f.endX + size / 2)
				{
					double t1 = (f.startY + size / 2 - this.posY) / vY;

					if (t1 == t)
						corner = true;
					else if (t1 < t)
					{
						collisionX = x;
						collisionY = f.startY + size / 2;
						collisionFace = f;
						t = t1;
					}
					break;
				}
			}
		}

		return new Result(t, collisionX, collisionY, collisionFace, corner);
	}

	private boolean passesThrough(Face f)
	{
		boolean passThrough = false;
		if (f.owner instanceof Obstacle o && !o.bouncy)
			passThrough = (this.ignoreDestructible && o.destructible) || (this.ignoreShootThrough && o.shouldShootThrough);

		if ((ignoreTanks && f.owner instanceof Tank) || (ignoreBullets && f.owner instanceof Bullet))
			passThrough = true;

		return passThrough;
	}

	public record Result(double t, double collisionX, double collisionY, Face collisionFace, boolean corner) {}

	public double getDist()
	{
		this.bounceX.add(0, this.posX);
		this.bounceY.add(0, this.posY);

		if (!acquiredTarget)
			this.getTarget();

		return Math.sqrt(getSquaredFinalDist());
	}

	public double getTargetDist(double mul, Tank m)
	{
		return Math.sqrt(getSquaredTargetDist(mul, m));
	}

	public double getSquaredTargetDist(double mul, Tank m)
	{
		this.bounceX.add(0, this.posX);
		this.bounceY.add(0, this.posY);

		if (this.getTarget(mul, m) != m)
			return -1;

		return getSquaredFinalDist();
	}

	private double getSquaredFinalDist()
	{
		double dist = 0;
		for (int i = 0; i < this.bounceX.size() - 1; i++)
            dist += Math.pow(this.bounceX.get(i + 1) - this.bounceX.get(i), 2) + Math.pow(this.bounceY.get(i + 1) - this.bounceY.get(i), 2);

		if (this.bounces >= 0)
			dist += Chunk.chunkToPixel(maxChunkCheck);

		return dist;
	}

	private void addChunks(Chunk compare, Chunk... chunks)
	{
		for (Chunk c : chunks)
		{
			if (c == null)
				continue;

			c.compareTo = compare;
			chunksToCheck.add(c);
		}
	}

	public double getAngleInDirection(double x, double y)
	{
		x -= this.posX;
		y -= this.posY;

		double angle = 0;
		if (x > 0)
			angle = Math.atan(y/x);
		else if (x < 0)
			angle = Math.atan(y/x) + Math.PI;
		else
		{
			if (y > 0)
				angle = Math.PI / 2;
			else if (y < 0)
				angle = Math.PI * 3 / 2;
		}

		return angle;
	}

	public boolean isInsideObstacle(double x, double y)
	{
		int ox = (int) (x / Game.tile_size);
		int oy = (int) (y / Game.tile_size);

		return !(ox >= 0 && ox < Game.currentSizeX && oy >= 0 && oy < Game.currentSizeY) || Game.isSolid(ox, oy);
	}
}
