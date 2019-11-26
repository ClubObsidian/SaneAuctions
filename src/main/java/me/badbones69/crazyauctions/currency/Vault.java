package me.badbones69.crazyauctions.currency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class Vault {
	
	private static Object economy;
	private static Class<?> economyClass;
	private static Method getBalanceMethod;
	private static Method withdrawPlayerMethod;
	private static Method depositPlayerMethod;
	
	public static boolean hasVault() 
	{
		return Bukkit.getServer().getPluginManager().getPlugin("Vault") != null;
	}
	
	public static boolean setupEconomy() 
	{
		Plugin vault = Bukkit.getServer().getPluginManager().getPlugin("Vault");
		if(vault == null)
		{
			return false;
		}
		
		try 
		{
			Vault.economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		if(Vault.economyClass == null)
		{
			return false;
		}
		
		Vault.economy = Bukkit.getServer().getServicesManager().getRegistration(Vault.economyClass).getProvider();
		return Vault.economy != null;
	}

	public static Long getMoney(OfflinePlayer player) 
	{
		double balance = -1;
		if(Vault.getBalanceMethod == null)
		{
			try 
			{
				Vault.getBalanceMethod = Vault.economyClass.getDeclaredMethod("getBalance", OfflinePlayer.class);
				Vault.getBalanceMethod.setAccessible(true);
			} 
			catch (NoSuchMethodException | SecurityException e) 
			{
				e.printStackTrace();
			}
		}
		
		try 
		{
			balance = (double) Vault.getBalanceMethod.invoke(Vault.economy, player);
		} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
		}
		
		return (long) balance;
	}

	public static boolean removeMoney(OfflinePlayer playerWrapper, BigDecimal amt) 
	{
		if(amt.doubleValue() < 0)
		{
			return false;
		}
		
		if(Vault.withdrawPlayerMethod == null)
		{
			try 
			{
				Vault.withdrawPlayerMethod = Vault.economyClass.getDeclaredMethod("withdrawPlayer", OfflinePlayer.class, double.class);
				Vault.withdrawPlayerMethod.setAccessible(true);
			} 
			catch (NoSuchMethodException | SecurityException e) 
			{
				e.printStackTrace();
			}
		}
		
		double amtDouble = amt.doubleValue();
		double balance = Vault.getMoney(playerWrapper).doubleValue();
		
		if(balance >= amtDouble)
		{
			try 
			{
				Vault.withdrawPlayerMethod.invoke(Vault.economy, playerWrapper.getPlayer(), amtDouble);
				return true;
			} 
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
			{
				e.printStackTrace();
			}
		}

		return false;
	}

	public static boolean addMoney(OfflinePlayer playerWrapper, BigDecimal amt) 
	{
		if(amt.doubleValue() < 0)
			return false;
		
		if(Vault.depositPlayerMethod == null)
		{
			try 
			{
				Vault.depositPlayerMethod = Vault.economyClass.getDeclaredMethod("depositPlayer", OfflinePlayer.class, double.class);
				Vault.depositPlayerMethod.setAccessible(true);
			} 
			catch (NoSuchMethodException | SecurityException e) 
			{
				e.printStackTrace();
			}
		}
		
		try 
		{
			Vault.depositPlayerMethod.invoke(Vault.economy, playerWrapper.getPlayer(), amt.doubleValue());
		} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
		}
		return true;
	}
	
}