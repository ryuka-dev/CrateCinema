package io.github.ryukadev.cratecinema.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Tiers;

/**
 * {@code /cratecinema preview <show> [tier]}: plays a show for the sender on the block they are
 * looking at, with the held item as the reward. Nothing is rolled, taken or granted; it never
 * touches the crate plugin.
 */
public final class PreviewCommand implements TabExecutor {

    private static final String[] TIER_NAMES = {"§fCommon", "§9Rare", "§dEpic", "§6Legendary"};
    private static final Color[] TIER_COLORS = {Color.WHITE, Color.fromRGB(85, 85, 255), Color.fromRGB(170, 0, 170), Color.fromRGB(255, 170, 0)};
    private static final Material[] FILLERS = {
        Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.DIAMOND,
        Material.BREAD, Material.GOLDEN_APPLE, Material.EXPERIENCE_BOTTLE, Material.ENDER_PEARL};
    private static final int REACH = 8;

    /** Preview rarities map straight to their tier. */
    private static final Tiers PREVIEW_TIERS = new Tiers(
        Map.of("preview_0", 0, "preview_1", 1, "preview_2", 2, "preview_3", 3), 0);

    private final ShowCatalog catalog;
    private final SessionManager sessions;

    public PreviewCommand(ShowCatalog catalog, SessionManager sessions) {
        this.catalog = catalog;
        this.sessions = sessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can preview shows.");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("preview")) {
            sender.sendMessage("§eUsage: /" + label + " preview <show> [tier 0-" + Tiers.MAX + "]. Shows: " + this.catalog.ids());
            return true;
        }
        Optional<Show> show = this.catalog.get(args[1].toLowerCase(Locale.ROOT));
        if (show.isEmpty()) {
            sender.sendMessage("§cUnknown show '" + args[1] + "'. Shows: " + this.catalog.ids());
            return true;
        }
        int tier = Tiers.MAX;
        if (args.length >= 3) {
            try {
                tier = Tiers.clamp(Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                sender.sendMessage("§cTier must be a number from 0 to " + Tiers.MAX + ".");
                return true;
            }
        }
        if (this.sessions.isBusy(player.getUniqueId())) {
            sender.sendMessage("§cYou are still watching a show.");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        ItemStack icon = held.getType().isAir() ? new ItemStack(Material.NETHER_STAR) : held.clone();
        RarityView rarity = new RarityView("preview_" + tier, TIER_NAMES[tier], TIER_COLORS[tier]);
        RewardView target = new RewardView(icon, "§fPreview", rarity, 0.05);
        List<RewardView> fillers = new ArrayList<>();
        for (Material material : FILLERS) {
            fillers.add(new RewardView(new ItemStack(material), "", null, Double.NaN));
        }
        ShowRequest request = new ShowRequest(player, anchor(player), List.of(new ShowRound(target, fillers)));
        this.sessions.play(show.get(), request, () -> { }, PREVIEW_TIERS);
        return true;
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
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("preview");
        }
        if (args.length == 2) {
            return this.catalog.ids().stream().filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        if (args.length == 3) {
            return List.of("0", "1", "2", "3");
        }
        return List.of();
    }
}
