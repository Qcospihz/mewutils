package cc.mewcraft.mewutils.module.death_logger;

import cc.mewcraft.mewutils.MewPlugin;
import cc.mewcraft.mewutils.module.ModuleBase;
import com.google.inject.Inject;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DeathLoggerModule extends ModuleBase implements Listener {

    private Set<EntityType> logTypes;
    private int searchRadius;

    @Inject
    public DeathLoggerModule(MewPlugin plugin) {
        super(plugin);
    }

    @Override protected void load() throws Exception {
        this.logTypes = configNode().node("entities")
                .getList(String.class, List.of())
                .stream()
                .map(s -> EntityType.valueOf(s.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
        this.searchRadius = configNode().node("search_radius").getInt();
    }

    @Override protected void enable() {
        registerListenerAndBind(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!this.logTypes.contains(event.getEntityType()))
            return;

        if (entity.getLastDamageCause() == null)
            return;

        translations().of("death")
                .resolver(
                        Placeholder.component("victim", Optional.ofNullable(entity.customName()).orElse(entity.name())),
                        Placeholder.parsed("reason", Optional.ofNullable(entity.getLastDamageCause())
                                .map(c -> c.getCause().name().toLowerCase())
                                .map(c -> translations().of("damage_cause." + c).plain())
                                .orElseGet(() -> getParentPlugin().translations().of("none").plain())),
                        Placeholder.parsed("killer", Optional.ofNullable(entity.getKiller())
                                .map(Player::getName)
                                .orElseGet(() -> entity
                                        .getLocation()
                                        .getNearbyPlayers(this.searchRadius)
                                        .stream()
                                        .map(Player::getName)
                                        .reduce((acc, name) -> acc.concat(",").concat(name))
                                        .orElse(getParentPlugin().translations().of("none").plain())
                                ))
                )
                .replace("x", entity.getLocation().getBlockX())
                .replace("y", entity.getLocation().getBlockY())
                .replace("z", entity.getLocation().getBlockZ())
                .replace("world", entity.getLocation().getWorld().getName())
                .send(Bukkit.getServer()); // broadcast to entire server
    }

}
