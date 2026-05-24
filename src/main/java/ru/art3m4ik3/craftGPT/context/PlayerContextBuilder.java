package ru.art3m4ik3.craftGPT.context;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerContextBuilder {

    public static String build(Player player, String lang) {
        StringBuilder ctx = new StringBuilder();

        Location loc = player.getLocation();
        World world = player.getWorld();

        ctx.append(getContextHeader(lang)).append("\n");

        // Location & environment
        ctx.append(format("location", lang,
                (int) loc.getX(), (int) loc.getY(), (int) loc.getZ())).append("\n");
        ctx.append(format("world", lang, world.getName())).append("\n");

        String biomeName = world.getBiome((int) loc.getX(), (int) loc.getZ()).name().toLowerCase().replace("_", " ");
        ctx.append(format("biome", lang, biomeName)).append("\n");

        // Time
        long time = world.getTime();
        String timeOfDay = getTimeOfDay(time, lang);
        ctx.append(format("time", lang, timeOfDay)).append("\n");

        // Weather
        String weather = getWeather(world, lang);
        ctx.append(format("weather", lang, weather)).append("\n");

        // Player state
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();
        ctx.append(format("health", lang, health, maxHealth)).append("\n");

        int food = player.getFoodLevel();
        ctx.append(format("food", lang, food)).append("\n");

        int level = player.getLevel();
        ctx.append(format("level", lang, level)).append("\n");

        GameMode gm = player.getGameMode();
        ctx.append(format("gamemode", lang, gm.name().toLowerCase())).append("\n");

        // Held item
        ItemStack held = player.getInventory().getItemInHand();
        if (held != null && held.getType().name().equals("AIR") == false) {
            String itemName = held.getType().name().toLowerCase().replace("_", " ");
            ctx.append(format("holding", lang, itemName)).append("\n");
        }

        ctx.append(getContextFooter(lang));

        return ctx.toString();
    }

    private static String getContextHeader(String lang) {
        if ("ru".equals(lang)) return "[Контекст игрока]:";
        return "[Player context]:";
    }

    private static String getContextFooter(String lang) {
        if ("ru".equals(lang)) return "[Конец контекста]";
        return "[End context]";
    }

    private static String format(String key, String lang, Object... values) {
        boolean ru = "ru".equals(lang);
        switch (key) {
            case "location": return ru
                    ? "Позиция: X=" + values[0] + " Y=" + values[1] + " Z=" + values[2]
                    : "Position: X=" + values[0] + " Y=" + values[1] + " Z=" + values[2];
            case "world": return ru ? "Мир: " + values[0] : "World: " + values[0];
            case "biome": return ru ? "Биом: " + values[0] : "Biome: " + values[0];
            case "time": return ru ? "Время суток: " + values[0] : "Time of day: " + values[0];
            case "weather": return ru ? "Погода: " + values[0] : "Weather: " + values[0];
            case "health": return ru
                    ? "Здоровье: " + values[0] + "/" + values[1]
                    : "Health: " + values[0] + "/" + values[1];
            case "food": return ru ? "Сытость: " + values[0] + "/20" : "Food: " + values[0] + "/20";
            case "level": return ru ? "Уровень опыта: " + values[0] : "XP level: " + values[0];
            case "gamemode": return ru ? "Режим игры: " + values[0] : "Game mode: " + values[0];
            case "holding": return ru ? "В руке: " + values[0] : "Holding: " + values[0];
            default: return key + ": " + values[0];
        }
    }

    private static String getTimeOfDay(long time, String lang) {
        boolean ru = "ru".equals(lang);
        if (time < 1000) return ru ? "рассвет" : "dawn";
        if (time < 6000) return ru ? "утро" : "morning";
        if (time < 12000) return ru ? "день" : "day";
        if (time < 13000) return ru ? "закат" : "sunset";
        return ru ? "ночь" : "night";
    }

    private static String getWeather(World world, String lang) {
        boolean ru = "ru".equals(lang);
        if (world.isThundering()) return ru ? "гроза" : "thunderstorm";
        if (world.hasStorm()) return ru ? "дождь" : "rain";
        return ru ? "ясно" : "clear";
    }
}
