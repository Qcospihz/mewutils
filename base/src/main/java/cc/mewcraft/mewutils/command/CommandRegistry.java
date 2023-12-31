package cc.mewcraft.mewutils.command;

import cc.mewcraft.mewutils.MewPlugin;
import cloud.commandframework.Command;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class CommandRegistry extends PaperCommandManager<CommandSender> {

    private final List<Command<CommandSender>> preparedCommands;

    public CommandRegistry(JavaPlugin plugin) throws Exception {
        super(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );

        this.preparedCommands = new ArrayList<>();

        // ---- Register Brigadier ----
        if (hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            registerBrigadier();
            CloudBrigadierManager<CommandSender, ?> brigManager = brigadierManager();
            if (brigManager != null)
                brigManager.setNativeNumberSuggestions(false);
        }

        // ---- Setup exception messages ----
        new MinecraftExceptionHandler<CommandSender>()
                .withDefaultHandlers()
                .apply(
                        this,
                        sender -> AudienceProvider.nativeAudience().apply(sender)
                );
    }

    /**
     * Adds specific command which will be registered upon the main plugin ({@link MewPlugin}) enabling.
     *
     * @param command the command to be registered
     */
    public final void prepareCommand(final Command<CommandSender> command) {
        this.preparedCommands.add(command);
    }

    @SafeVarargs
    public final void prepareCommands(final Command<CommandSender>... commands) {
        this.preparedCommands.addAll(Arrays.asList(commands));
    }

    /**
     * Registers all added commands.
     * <p>
     * This method will make the added commands effective.
     */
    public final void registerCommands() {
        for (final Command<CommandSender> added : this.preparedCommands) {
            command(added);
        }
    }

}
