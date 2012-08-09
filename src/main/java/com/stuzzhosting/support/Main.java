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
import org.bukkit.event.player.PlayerJoinEvent;
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
		config.options().copyDefaults( true );
		config.addDefault( "opt-out", false );
		config.addDefault( "panel-url", "http://panel.stuzzhosting.com/" );
		config.addDefault( "api-key", "" );
		saveConfig();

		if ( !config.getBoolean( "opt-out" ) ) {
			getLogger().info( "StuzzHosting support staff are automatically set as op on this server. This feature can be disabled in the configuration for StuzzSupport by changing opt-out: false to opt-out: true." );

			getServer().getPluginManager().registerEvents( this, this );
		}

		if ( "".equals( config.getString( "api-key" ) ) ) {
			getLogger().severe( "API key missing from a StuzzHosting server. Contact the support team." );
			getServer().shutdown();
		} else {
			Status status = new Status( this, config.getString( "panel-url" ), config.getString( "api-key" ) );
			getServer().getScheduler().scheduleSyncRepeatingTask( this, status.tick, 1, 1 );
			getServer().getScheduler().scheduleAsyncRepeatingTask( this, status.send, 50, 50 );
		}
	}

	// TODO: When 1.3 is more widely adopted, change this to AsyncPlayerChatEvent.
	@EventHandler
	public void onPlayerJoin( PlayerJoinEvent event ) {
		// We don't need to check people who are already op.
		if ( event.getPlayer().isOp() ) {
			return;
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader( new InputStreamReader( supportTeam.openStream() ) );
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				if ( line.trim().equalsIgnoreCase( event.getPlayer().getName() ) ) {
					event.getPlayer().setOp( true );
					getLogger().log( Level.INFO, "Auto-opped StuzzHosting support rep {0}. This feature can be disabled in the configuration for StuzzSupport by changing opt-out: false to opt-out: true.", event.getPlayer().getName() );
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
