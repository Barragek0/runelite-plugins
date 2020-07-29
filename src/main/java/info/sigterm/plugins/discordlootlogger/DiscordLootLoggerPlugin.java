package info.sigterm.plugins.discordlootlogger;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import static net.runelite.http.api.RuneLiteAPI.GSON;
import static net.runelite.http.api.RuneLiteAPI.JSON;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "Discord Loot Logger"
)
public class DiscordLootLoggerPlugin extends Plugin
{
	@Inject
	private DiscordLootLoggerConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OkHttpClient okHttpClient;

	private List<String> lootNpcs;

	private static String itemImageUrl(int itemId)
	{
		return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
	}

	@Override
	protected void startUp()
	{
		lootNpcs = Collections.emptyList();
	}

	@Override
	protected void shutDown()
	{
	}

	@Provides
	DiscordLootLoggerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordLootLoggerConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equalsIgnoreCase(DiscordLootLoggerConfig.GROUP))
		{
			String s = config.lootNpcs();
			lootNpcs = s != null ? Text.fromCSV(s) : Collections.emptyList();
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		NPC npc = npcLootReceived.getNpc();
		Collection<ItemStack> items = npcLootReceived.getItems();

		if (!lootNpcs.isEmpty())
		{
			for (String npcName : lootNpcs)
			{
				if (WildcardMatcher.matches(npcName, npc.getName()))
				{
					processLoot(npc, items);
					return;
				}
			}
		}
		else
		{
			processLoot(npc, items);
		}
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived playerLootReceived)
	{
		Collection<ItemStack> items = playerLootReceived.getItems();
		processLoot(playerLootReceived.getPlayer(), items);
	}

	private void processLoot(Actor from, Collection<ItemStack> items)
	{
		WebhookBody webhookBody = new WebhookBody();

		long totalValue = 0;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(from.getName()).append(":\n");
		for (ItemStack item : items)
		{
			int itemId = item.getId();
			int qty = item.getQuantity();

			int price = itemManager.getItemPrice(itemId);
			long total = (long) price * qty;

			totalValue += total;

			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			stringBuilder.append(qty).append(" x ").append(itemComposition.getName()).append("\n");
			webhookBody.getEmbeds().add(new WebhookBody.Embed(new WebhookBody.UrlEmbed(itemImageUrl(itemId))));
		}

		final int targetValue = config.lootValue();
		if (targetValue == 0 || totalValue >= targetValue)
		{
			webhookBody.setContent(stringBuilder.toString());
			sendWebhook(webhookBody);
		}
	}

	private void sendWebhook(WebhookBody webhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);

		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, GSON.toJson(webhookBody)))
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}
}
