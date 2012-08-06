package com.stuzzhosting.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	private static final URL supportTeam;

	static {
		try {
			supportTeam = new URL( "http://panel.stuzzhosting.com/stuzzd/support.txt" );
		} catch ( MalformedURLException ex ) {
			throw new RuntimeException( ex );
		}
	}

	@Override
	public void onEnable() {
		FileConfiguration config = getConfig();
		config.addDefault( "opt-out", false );
		saveConfig();

		if ( config.getBoolean( "opt-out" ) ) {
			setEnabled( false );
			return;
		}

		getLogger().info( "StuzzHosting support staff are able to self-op on this server. This feature can be disabled in the configuration for StuzzSupport or by deleting StuzzSupport.jar and restarting your server." );

		getServer().getPluginManager().registerEvents( this, this );
	}

	// TODO: When 1.3 is more widely adopted, change this to AsyncPlayerChatEvent.
	@EventHandler
	public void checkOp( PlayerChatEvent event ) {
		// We don't need to check people who are already op.
		if ( event.getPlayer().isOp() ) {
			return;
		}

		// We don't need to check anything other than the /op command.
		if ( !event.getMessage().startsWith( "/op " ) ) {
			return;
		}

		// This plugin only affects self-opping.
		if ( !event.getMessage().trim().equalsIgnoreCase( "/op " + event.getPlayer().getName() ) ) {
			return;
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader( new InputStreamReader( supportTeam.openStream() ) );
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				if ( line.trim().equalsIgnoreCase( event.getPlayer().getName() ) ) {
					event.getPlayer().setOp( true );
					getLogger().log( Level.INFO, "Allowed {0} to self-op. This feature can be disabled in the configuration for StuzzSupport or by deleting StuzzSupport.jar and restarting your server.", event.getPlayer().getName() );
					return;
				}
			}
		} catch ( IOException ex ) {
			getLogger().log( Level.SEVERE, "Unable to check support team list.", ex );
		} finally {
			if ( reader != null ) {
				try {
					reader.close();
				} catch ( IOException ex ) {
					// Ignored
				}
			}
		}
	}

}
