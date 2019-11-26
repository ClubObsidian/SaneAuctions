package me.badbones69.crazyauctions.api;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import me.badbones69.crazyauctions.Main;

public class MemberManager implements Listener {

	public static final String OWNER_GROUP = "owner";
	
	private static MemberManager instance;
	
	public static MemberManager get()
	{
		if(instance == null)
		{
			instance = new MemberManager();
		}
		
		return instance;
	}
	
	private AtomicBoolean databaseModified;
	private DB nameDatabase;
	private Map<UUID,String> uuidMemberNames;
	private Map<String,UUID> stringMemberNames;
	private MemberManager()
	{
		this.databaseModified = new AtomicBoolean(false);
		this.configureDB();
		this.startMemberNameUpdate();
		this.registerEvents();
	}
	
	private void registerEvents()
	{
		Bukkit.getServer().getPluginManager().registerEvents(this, Main.get());
	}
	
	public String getNameFromUUID(UUID uuid)
	{
		return this.uuidMemberNames.get(uuid);
	}
	
	public UUID getUUIDFromName(String name)
	{
		return this.stringMemberNames.get(name);
	}
	
	public synchronized void shutdown()
	{
		this.nameDatabase.close();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		String name = player.getName();
		UUID uuid = player.getUniqueId();
		
		String cacheName = this.uuidMemberNames.get(uuid);
		if(cacheName == null || !cacheName.equals(name))
		{
			this.uuidMemberNames.put(uuid, name);
			this.stringMemberNames.put(name,uuid);
			if(cacheName != null)
			{
				this.stringMemberNames.remove(cacheName);
			}
			this.databaseModified.set(true);
		}
	}
	
	private void configureDB()
	{
		File memberNamesFile = new File(Main.get().getDataFolder(), "member-names.db");
		this.nameDatabase = DBMaker.fileDB(memberNamesFile)
				.fileMmapEnable()
				.fileChannelEnable()
				.transactionEnable()
				.make();
		this.uuidMemberNames = this.nameDatabase
				.hashMap("uuid-member-names", Serializer.UUID, Serializer.STRING)
				.createOrOpen();
		this.stringMemberNames = this.nameDatabase
				.hashMap("string-member-names", Serializer.STRING, Serializer.UUID)
				.createOrOpen();
	}
	
	private void startMemberNameUpdate()
	{
		Bukkit.getScheduler().runTaskTimerAsynchronously(Main.get(), new Runnable()
		{
			@Override
			public void run()
			{
				if(databaseModified.get())
				{
					nameDatabase.commit();
					databaseModified.set(false);
				}
			}
		}, 1L, 1L);
	}
}