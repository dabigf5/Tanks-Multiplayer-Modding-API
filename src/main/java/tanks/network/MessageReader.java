package tanks.network;

import io.netty.buffer.ByteBuf;
import tanks.Game;
import tanks.event.*;
import tanks.event.online.IOnlineServerEvent;
import tanks.gui.screen.ScreenPartyHost;
import tanks.gui.screen.ScreenPartyLobby;

import java.util.UUID;

public class MessageReader
{
	public static final int max_event_size = 1048576;

	public static int downstreamBytes;
	public static int upstreamBytes;
	public static long lastMessageTime;

	public static int upstreamBytesPerSec;
	public static int downstreamBytesPerSec;

	public boolean useQueue = true;
	public ByteBuf queue;
	protected boolean reading = false;
	protected int endpoint;

	public short queueMessage(ByteBuf m, UUID clientID)
	{
		return this.queueMessage(null, m, clientID);
	}

	public synchronized short queueMessage(ServerHandler s, ByteBuf m, UUID clientID)
	{
		short reply = -1;

		try
		{
			byte[] bytes = new byte[59];
			m.getBytes(0, bytes);

			if (useQueue)
				queue.writeBytes(m);
			else
				queue = m;

			if (queue.readableBytes() >= 4)
			{
				if (!reading)
				{
					endpoint = queue.readInt();
					downstreamBytes += endpoint + 4;
					updateLastMessageTime();

					if (endpoint > max_event_size)
					{
						if (ScreenPartyHost.isServer && s != null)
						{
							s.sendEventAndClose(new EventKick("A network exception has occurred: message size " + endpoint + " is too big!"));
						}
						else if (ScreenPartyLobby.isClient)
						{
							EventKick ev = new EventKick("A network exception has occurred: message size " + endpoint + " is too big!");
							ev.clientID = null;
							Game.eventsIn.add(ev);

							Client.handler.ctx.close();
							ScreenPartyLobby.connections.clear();
						}

						return -1;
					}
				}

				reading = true;

				while (queue.readableBytes() >= endpoint)
				{
					reply = this.readMessage(s, queue, clientID);
					queue.discardReadBytes();

					reading = false;

					if (queue.readableBytes() >= 4)
					{
						endpoint = queue.readInt();

						if (endpoint > MessageReader.max_event_size)
						{
							if (ScreenPartyHost.isServer && s != null)
							{
								s.sendEventAndClose(new EventKick("A network exception has occurred: message size " + endpoint + " is too big!"));
							}
							else if (ScreenPartyLobby.isClient)
							{
								EventKick ev = new EventKick("A network exception has occurred: message size " + endpoint + " is too big!");
								ev.clientID = null;
								Game.eventsIn.add(ev);

								Client.handler.ctx.close();
								ScreenPartyLobby.connections.clear();
							}

							return 0;
						}

						reading = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			if (s != null)
			{
				System.err.println("A network exception has occurred: " + e + " (" + s.rawUsername + "/" + s.clientID + ")");
				Game.logger.println("A network exception has occurred: " + e + " (" + s.rawUsername + "/" + s.clientID + ")");
			}
			else
			{
				System.err.println("A network exception has occurred: " + e);
				Game.logger.println("A network exception has occurred: " + e);
			}

			e.printStackTrace();
			e.printStackTrace(Game.logger);

			if (ScreenPartyHost.isServer && s != null)
			{
				s.sendEventAndClose(new EventKick("A network exception has occurred: " + e));
				//Game.screen = new ScreenHostingEnded("A network exception has occurred: " + e.toString());
			}
			else if (ScreenPartyLobby.isClient)
			{
				EventKick ev = new EventKick("A network exception has occurred: " + e);
				ev.clientID = null;
				Game.eventsIn.add(ev);

				Client.handler.ctx.close();
				ScreenPartyLobby.connections.clear();
			}
		}

		return reply;
	}

	public synchronized short readMessage(ServerHandler s, ByteBuf m, UUID clientID) throws Exception
	{
		int i = m.readInt();
		Class<? extends INetworkEvent> c = NetworkEventMap.get(i);

		if (c == null)
			throw new Exception("Invalid network event: " + i);

		INetworkEvent e = c.getConstructor().newInstance();
		e.read(m);

		if (e instanceof PersonalEvent)
			((PersonalEvent) e).clientID = clientID;

		if (e instanceof EventPing && ((EventPing) e).iteration <= 2)
			return ((EventPing) e).iteration;
		else if (e instanceof IOnlineServerEvent)
			s.sendEventAndClose(new EventKick("This is a party, please join parties through the party menu"));
		else if (e instanceof IServerThreadEvent)
			((IServerThreadEvent) e).execute(s);
		else
		{
			synchronized (Game.eventsIn)
			{
				Game.eventsIn.add(e);
			}
		}

		return -1;
	}

	public static void updateLastMessageTime()
	{
		long time = System.currentTimeMillis() / 1000;

		if (lastMessageTime < time)
		{
			lastMessageTime = time;
			upstreamBytesPerSec = upstreamBytes;
			downstreamBytesPerSec = downstreamBytes;
			upstreamBytes = 0;
			downstreamBytes = 0;
		}
	}
}