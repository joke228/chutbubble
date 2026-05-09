package net.chatbubbles;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.UUID;

public class LightChatBubbles extends JavaPlugin implements Listener {

    // Храним активные сообщения, чтобы удалять старые при новом сообщении
    private final HashMap<UUID, TextDisplay> activeBubbles = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("LightChatBubbles для 1.26.2 успешно загружен!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Работа с сущностями (спавн) должна быть в основном потоке сервера
        Bukkit.getScheduler().runTask(this, () -> createBubble(player, message));
    }

    private void createBubble(Player player, String message) {
        // Если у игрока уже висит сообщение — удаляем его
        if (activeBubbles.containsKey(player.getUniqueId())) {
            TextDisplay oldBubble = activeBubbles.get(player.getUniqueId());
            if (oldBubble != null) oldBubble.remove();
        }

        // Создаем текстовую панель над головой (на высоте 2.4 блока)
        TextDisplay bubble = player.getWorld().spawn(player.getLocation().add(0, 2.4, 0), TextDisplay.class, display -> {
            display.setText(message);
            display.setBillboard(TextDisplay.Billboard.CENTER); // Всегда лицом к игроку
            display.setBackgroundColor(Color.fromARGB(160, 0, 0, 0)); // Полупрозрачный черный фон
            display.setShadowed(true); // Тень у текста
            
            // Установка размера текста
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(new Vector3f(1.0f, 1.0f, 1.0f));
            display.setTransformation(transformation);
        });

        activeBubbles.put(player.getUniqueId(), bubble);

        // Постоянно перемещаем текст за игроком (каждый тик)
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (player.isOnline() && !bubble.isDead()) {
                // Плавная телепортация за игроком
                bubble.teleport(player.getLocation().add(0, 2.4, 0));
            } else {
                bubble.remove();
            }
        }, 1L, 1L);

        // Удаление через 5 секунд (100 тиков)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (bubble.isValid()) {
                bubble.remove();
                activeBubbles.remove(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }, 100L);
    }
}