package com.stuzzhosting.support;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

class Status {
	private final String URL;

	Status( Main plugin, String stuzzpanel, String key ) {
		String user = null;
		try {
			user = username(new URL(stuzzpanel + "/index.php?api=username&k=" + key));
		} catch ( IOException ex ) {
			plugin.getLogger().log( Level.SEVERE, "Error validating API key", ex );
			Bukkit.shutdown();
		}
		URL = stuzzpanel + "/update.php?u=" + user + "&k=" + key + "&d=";
	}

	private final long[] last40Ticks = new long[40];

	final Runnable tick = new Runnable() {
		public void run() {
			synchronized ( last40Ticks ) {
				System.arraycopy( last40Ticks, 0, last40Ticks, 1, 39 );
				last40Ticks[0] = System.nanoTime();
			}
		}

	};

	final Runnable send = new Runnable() {
		public void run() {
			long nanosecondsIn40Ticks;
			synchronized ( last40Ticks ) {
				if ( last40Ticks[39] == 0 ) {
					nanosecondsIn40Ticks = 0;
				} else {
					nanosecondsIn40Ticks = System.nanoTime() - last40Ticks[39];
				}
			}

			double tickRate = 40000000000.0 / nanosecondsIn40Ticks;

			// We can either encode to JSON and then urlencode it, or since we know our data won't have anything URLs don't like, we can do it all in one step.
			StringBuilder json = new StringBuilder( URL ).append( "%7B" );

			json.append( "\"max_players\":" ).append( Bukkit.getServer().getMaxPlayers() ).append( ',' );

			json.append( "\"online_players\":%5B" );
			boolean first = true;
			for ( Player player : Bukkit.getServer().getOnlinePlayers() ) {
				if ( first ) {
					first = false;
				} else {
					json.append( ',' );
				}
				// Minecraft usernames are alphanumeric with benign punctuation allowed. Yay!
				json.append( '"' ).append( player.getName() ).append( '"' );
			}
			json.append( "%5D," );

			json.append( "\"cpu\":" ).append( getCPUUsage() ).append( ',' );

			json.append( "\"mem\":" ).append( ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1000000.0 ).append( ',' );

			json.append( "\"max_mem\":" ).append( Runtime.getRuntime().maxMemory() / 1000000.0 ).append( ',' );

			json.append( "\"tick\":" ).append( tickRate ).append(',');

			json.append( "\"chunks\":%7B" );
			first = true;
			for ( World world : Bukkit.getServer().getWorlds() ) {
				if ( first ) {
					first = false;
				} else {
					json.append( ',' );
				}

				json.append('"').append( world.getName() ).append("\":").append( world.getLoadedChunks().length );
			}
			json.append( "%7D," );

			json.append( "\"entities\":%7B" );
			first = true;
			for ( World world : Bukkit.getServer().getWorlds() ) {
				if ( first ) {
					first = false;
				} else {
					json.append( ',' );
				}

				json.append('"').append( world.getName() ).append("\":").append( world.getEntities().size() );
			}
			json.append( "%7D" );

			json.append( "%7D" );

			InputStream in = null;
			try {
				in = new URL( json.toString() ).openStream();
				in.read( new byte[1] );
			} catch ( IOException ex ) {
				// Ignore this one
			} finally {
				if ( in != null ) {
					try {
						in.close();
					} catch ( IOException ex ) {
						// Ignore that one too
					}
				}
			}
		}

		private final com.sun.management.OperatingSystemMXBean cpu;

		{
			OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
			if ( bean instanceof com.sun.management.OperatingSystemMXBean ) {
				cpu = (com.sun.management.OperatingSystemMXBean) bean;
			} else {
				cpu = null;
			}
		}

		private long lastCpuTick = System.nanoTime();

		private long lastCpuTime = 0;

		private double getCPUUsage() {
			if ( cpu == null ) {
				return -1;
			}

			long tick = System.nanoTime();
			long time = cpu.getProcessCpuTime();
			double usage = (double) ( time - lastCpuTime ) / ( tick - lastCpuTick ) * 100 / Runtime.getRuntime().availableProcessors();
			lastCpuTick = tick;
			lastCpuTime = time;

			return usage;
		}

	};

	private String username( URL url ) throws IOException {
		InputStream in = url.openStream();
		byte[] username = new byte[8];
		try {
			in.read(username);
		} finally {
			in.close();
		}
		return new String(username, "UTF-8");
	}
}
