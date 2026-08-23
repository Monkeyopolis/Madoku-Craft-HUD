package madoku.craft.hud;

/** Client cache for state synchronized by Madoku Craft API. */
public final class HudPayloadManager {
	private static volatile long serverDay;
	private static volatile int serverHour = 6;
	private static volatile int serverMinute;
	private static volatile boolean hasServerTime;
	private static volatile String serverSeason = "spring";
	private static volatile int serverSeasonDay;
	private static volatile int serverSeasonLengthDays = 28;
	private static volatile boolean hasServerSeason;
	private static volatile double serverTemperature = 50.0D;
	private static volatile double serverHumidity = 50.0D;
	private static volatile boolean hasServerClimate;

	private HudPayloadManager() { }
	public static void initialize() { reset(); }
	public static void reset() { clearServerTime(); clearServerSeason(); clearServerClimate(); }

	public static void setServerTime(long day, int hour, int minute) {
		serverDay = Math.max(0L, day);
		serverHour = Math.floorMod(hour, 24);
		serverMinute = Math.floorMod(minute, 60);
		hasServerTime = true;
	}
	public static void clearServerTime() { serverDay = 0L; serverHour = 6; serverMinute = 0; hasServerTime = false; }
	public static boolean hasServerTime() { return hasServerTime; }
	public static long getServerDay() { return serverDay; }
	public static int getServerHour() { return serverHour; }
	public static int getServerMinute() { return serverMinute; }

	public static void setServerSeason(String season) {
		if (season == null || season.isBlank()) { clearServerSeason(); return; }
		serverSeason = season;
		serverSeasonDay = 0;
		serverSeasonLengthDays = 28;
		hasServerSeason = true;
	}
	public static void setServerSeasonProgress(int seasonDay, int seasonLengthDays) {
		serverSeasonDay = Math.max(0, seasonDay);
		serverSeasonLengthDays = Math.max(1, seasonLengthDays);
	}
	public static void clearServerSeason() { serverSeason = "spring"; serverSeasonDay = 0; serverSeasonLengthDays = 28; hasServerSeason = false; }
	public static boolean hasServerSeason() { return hasServerSeason; }
	public static String getServerSeason() { return serverSeason; }
	public static int getServerSeasonDay() { return serverSeasonDay; }
	public static int getServerSeasonLengthDays() { return serverSeasonLengthDays; }

	public static void setServerClimate(double temperature, double humidity) {
		serverTemperature = Double.isFinite(temperature) ? temperature : 50.0D;
		serverHumidity = Double.isFinite(humidity) ? humidity : 50.0D;
		hasServerClimate = true;
	}
	public static void clearServerClimate() {
		serverTemperature = 50.0D;
		serverHumidity = 50.0D;
		hasServerClimate = false;
	}
	public static boolean hasServerClimate() { return hasServerClimate; }
	public static double getServerTemperature() { return serverTemperature; }
	public static double getServerHumidity() { return serverHumidity; }
}
