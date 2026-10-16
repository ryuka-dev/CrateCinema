package io.github.ryukadev.cratecinema.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Tiers;

/**
 * {@code /cratecinema}:
 * <ul>
 * <li>{@code preview <show> [tier]} plays a show for the sender on the block they are looking at,
 * with the held item as the reward. Nothing is rolled, taken or granted; it never touches the
 * crate plugin.</li>
 * <li>{@code reload} reads the configuration files again. Running shows are not affected.</li>
 * </ul>
 */
public final class CinemaCommand implements BasicCommand {

    public static final String LABEL = "cratecinema";
    public static final String DESCRIPTION = "Preview shows and reload the configuration.";
    private static final String PERMISSION = "cratecinema.admin";

    private static final String[] TIER_NAMES = {"§fCommon", "§9Rare", "§dEpic", "§6Legendary"};
    private static final Color[] TIER_COLORS = {Color.WHITE, Color.fromRGB(85, 85, 255), Color.fromRGB(170, 0, 170), Color.fromRGB(255, 170, 0)};
    private static final Material[] FILLERS = {
        Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.DIAMOND,
        Material.BREAD, Material.GOLDEN_APPLE, Material.EXPERIENCE_BOTTLE, Material.ENDER_PEARL};
    private static final int REACH = 8;

    /** Preview rarities map straight to their tier. */
    private static final Tiers PREVIEW_TIERS = new Tiers(
        Map.of("preview_0", 0, "preview_1", 1, "preview_2", 2, "preview_3", 3), 0);

    private final LiveConfig config;
    private final SessionManager sessions;
    private final BooleanSupplier reload;

    /** @param reload reloads the plugin's configuration and reports whether it succeeded */
    public CinemaCommand(LiveConfig config, SessionManager sessions, BooleanSupplier reload) {
        this.config = config;
        this.sessions = sessions;
        this.reload = reload;
    }

    @Override
    public String permission() {
        return PERMISSION;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            boolean applied = this.reload.getAsBoolean();
            sender.sendMessage(applied
                ? "§aCrateCinema configuration reloaded. Shows already running are not affected."
                : "§cCould not reload; the previous configuration is still in use. See the console.");
            return;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("preview")) {
            this.preview(sender, args);
            return;
        }
        sender.sendMessage("§eUsage: /" + LABEL + " preview <show> [tier 0-" + Tiers.MAX + "] | /" + LABEL + " reload");
        sender.sendMessage("§eShows: " + this.config.showIds());
    }

    private void preview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can preview shows.");
            return;
        }
        Optional<Show> show = this.config.catalog().get(args[1].toLowerCase(Locale.ROOT));
        if (show.isEmpty()) {
            sender.sendMessage("§cUnknown show '" + args[1] + "'. Shows: " + this.config.showIds());
            return;
        }
        int tier = Tiers.MAX;
        if (args.length >= 3) {
            try {
                tier = Tiers.clamp(Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                sender.sendMessage("§cTier must be a number from 0 to " + Tiers.MAX + ".");
                return;
            }
        }
        if (this.sessions.isBusy(player.getUniqueId())) {
            sender.sendMessage("§cYou are still watching a show.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        ItemStack icon = held.getType().isAir() ? new ItemStack(Material.NETHER_STAR) : held.clone();
        RarityView rarity = new RarityView("preview_" + tier, TIER_NAMES[tier], TIER_COLORS[tier]);
        RewardView target = new RewardView(icon, "§f" + displayName(icon), rarity, 0.05);
        List<RewardView> fillers = new ArrayList<>();
        for (Material material : FILLERS) {
            fillers.add(new RewardView(new ItemStack(material), "", null, Double.NaN));
        }
        ShowRequest request = new ShowRequest(player, anchor(player), List.of(new ShowRound(target, fillers)));
        this.sessions.play(show.get(), request, () -> { }, PREVIEW_TIERS);
    }

    /** The item's own name if it has one, otherwise its material written as words. */
    @SuppressWarnings("deprecation") // legacy-string names match the rest of the label text
    private static String displayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        StringBuilder name = new StringBuilder();
        for (String word : item.getType().name().toLowerCase(Locale.ROOT).split("_")) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    /** Above the block the player looks at, or in front of them. */
    private static Location anchor(Player player) {
        Block block = player.getTargetBlockExact(REACH);
        if (block != null) {
            return block.getLocation().add(0.5, block.getBoundingBox().getHeight() + 0.5, 0.5);
        }
        return player.getEyeLocation().add(player.getEyeLocation().getDirection().setY(0).normalize().multiply(3));
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            if (args.length == 0) {
                return List.of("preview", "reload");
            }
            return List.of("preview", "reload").stream().filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return this.config.showIds().stream().filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("preview")) {
            return List.of("0", "1", "2", "3");
        }
        return List.of();
    }
}
